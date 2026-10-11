package com.phantomaddons.features.pv;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class PvPlayerModel {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final UUID uuid;
    private final String name;
    private volatile PlayerSkin skin;
    private RemotePlayer entity;
    private ItemStack[] pendingArmor;

    float yaw = 0f;
    float pitch = 0f;

    private final ItemStack[] lobbyArmor;
    private volatile boolean lookupDone;
    private final long createdAt = System.currentTimeMillis();
    private static final long SKIN_WAIT_MS = 6000;

    private static final java.util.Map<UUID, CompletableFuture<java.util.Optional<PlayerSkin>>> SKINS =
            new java.util.concurrent.ConcurrentHashMap<>();

    PvPlayerModel(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.lobbyArmor = lobbyArmor(uuid);
        var conn = Minecraft.getInstance().getConnection();
        var info = conn != null ? conn.getPlayerInfo(uuid) : null;
        if (info != null) {
            PlayerSkin known = info.getSkin();
            if (known != null && !known.equals(DefaultPlayerSkin.get(uuid))) {
                skin = known;
                lookupDone = true;
                SKINS.putIfAbsent(uuid, CompletableFuture.completedFuture(java.util.Optional.of(known)));
                return;
            }
        }
        CompletableFuture<java.util.Optional<PlayerSkin>> f = SKINS.computeIfAbsent(uuid, id -> CompletableFuture
                .supplyAsync(() -> profileFor(id, name), PvItems.EXECUTOR)
                .thenCompose(profile -> Minecraft.getInstance().getSkinManager().get(profile)));
        f.whenComplete((opt, err) -> {
            if (err != null) SKINS.remove(uuid, f);
            lookupDone = true;
        });
        if (f.isDone() && !f.isCompletedExceptionally()) f.join().ifPresent(s -> skin = s);
        else f.thenAccept(opt -> opt.ifPresent(s -> skin = s));
    }

    // ── Saved skin references ─────────────────────────────────────────────────
    private static final long SAVED_SKIN_MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000;
    private static final java.util.Map<String, com.google.gson.JsonObject> SAVED = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean savedLoaded;

    private static java.nio.file.Path savedFile() {
        return com.phantomaddons.utils.PhantomPaths.resolve("pv_cache").resolve("skins.json");
    }

    private static synchronized void loadSaved() {
        if (savedLoaded) return;
        savedLoaded = true;
        try {
            if (!java.nio.file.Files.exists(savedFile())) return;
            var root = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(savedFile())).getAsJsonObject();
            long now = System.currentTimeMillis();
            for (var e : root.entrySet()) {
                var o = e.getValue().getAsJsonObject();
                if (now - o.get("savedAt").getAsLong() < SAVED_SKIN_MAX_AGE_MS) SAVED.put(e.getKey(), o);
            }
        } catch (java.io.IOException | RuntimeException ignored) {}
    }

    private static synchronized void save(UUID uuid, com.mojang.authlib.properties.Property textures) {
        var o = new com.google.gson.JsonObject();
        o.addProperty("value", textures.value());
        if (textures.signature() != null) o.addProperty("signature", textures.signature());
        o.addProperty("savedAt", System.currentTimeMillis());
        SAVED.put(uuid.toString(), o);
        var root = new com.google.gson.JsonObject();
        SAVED.forEach(root::add);
        try {
            java.nio.file.Files.createDirectories(savedFile().getParent());
            java.nio.file.Files.writeString(savedFile(), root.toString());
        } catch (java.io.IOException ignored) {}
    }

    private static GameProfile profileFor(UUID id, String name) {
        loadSaved();
        var saved = SAVED.get(id.toString());
        if (saved != null) {
            com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> props =
                    com.google.common.collect.LinkedHashMultimap.create();
            props.put("textures", new com.mojang.authlib.properties.Property("textures", saved.get("value").getAsString(),
                    saved.has("signature") ? saved.get("signature").getAsString() : null));
            return new GameProfile(id, name, new com.mojang.authlib.properties.PropertyMap(props));
        }
        ProfileResult result = Minecraft.getInstance().services().sessionService().fetchProfile(id, true);
        if (result == null) return new GameProfile(id, name);
        for (var p : result.profile().properties().get("textures")) { save(id, p); break; }
        return result.profile();
    }

    boolean ready() {
        return skin != null || lookupDone || System.currentTimeMillis() - createdAt > SKIN_WAIT_MS;
    }

    ItemStack[] lobbyArmor() { return lobbyArmor; }

    private static ItemStack[] lobbyArmor(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        net.minecraft.world.entity.player.Player p = mc.level.getPlayerByUUID(uuid);
        if (p == null) return null;
        ItemStack[] out = new ItemStack[4];
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            out[i] = p.getItemBySlot(ARMOR_SLOTS[i]).copy();
            any |= !out[i].isEmpty();
        }
        return any ? out : null;
    }

    void setArmor(ItemStack[] armor) {
        pendingArmor = armor;
        if (entity != null) applyArmor();
    }

    void tick() {
        if (entity != null) entity.tickCount++;
    }

    private RemotePlayer entity() {
        if (entity != null) return entity;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        entity = new RemotePlayer(mc.level, new GameProfile(uuid, name)) {
            {
                getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F); // show all skin layers
            }
            @Override public PlayerSkin getSkin() {
                PlayerSkin s = skin;
                return s != null ? s : DefaultPlayerSkin.get(uuid);
            }
        };
        applyArmor();
        return entity;
    }

    private void applyArmor() {
        if (pendingArmor == null) return;
        for (int i = 0; i < 4; i++) entity.setItemSlot(ARMOR_SLOTS[i], pendingArmor[i].copy());
    }

    void render(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, float partialTick) {
        RemotePlayer e = entity();
        if (e == null) return;

        int scale = Math.max(8, (int) ((y1 - y0) / 2.5f));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180f));
        pose.mul(camera);

        EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e).createRenderState(e, partialTick);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        state.lightCoords = 0xF000F0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180f + yaw;
            living.yRot = 0f;
            living.xRot = 0f;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1f;
        }
        Vector3f translation = new Vector3f(0f, state.boundingBoxHeight / 2f + 0.0625f, 0f);
        ctx.entity(state, scale, translation, pose, camera, x0, y0, x1, y1);
    }
}
