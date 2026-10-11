package com.phantomaddons.features.pv;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Dynamic;
import com.phantomaddons.PhantomAddons;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PvItems {

    static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "PhantomAddons-PV");
        t.setDaemon(true);
        return t;
    });

    private static final int LEGACY_DATA_VERSION = 99;
    private static final Pattern TEXTURE_VALUE = Pattern.compile("Value:\\\\?\"([A-Za-z0-9+/=]+)\\\\?\"");

    private PvItems() {}

    // ── Hypixel blobs ─────────────────────────────────────────────────────────

    public static List<ItemStack> decode(JsonElement blob) {
        JsonElement data = PvJson.path(blob, "data");
        if (data == null || !data.isJsonPrimitive()) return List.of();
        try {
            byte[] bytes = Base64.getDecoder().decode(data.getAsString());
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
            ListTag list = root.getListOrEmpty("i");
            List<ItemStack> out = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) out.add(convert(list.getCompoundOrEmpty(i)));
            return out;
        } catch (IOException | IllegalArgumentException e) {
            PhantomAddons.LOGGER.warn("[PV] failed to decode item blob", e);
            return List.of();
        }
    }

    public static CompoundTag decodeCompound(JsonElement blob) {
        JsonElement data = PvJson.path(blob, "data");
        if (data == null || !data.isJsonPrimitive()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data.getAsString());
            return NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
        } catch (IOException | IllegalArgumentException e) {
            PhantomAddons.LOGGER.warn("[PV] failed to decode compound blob", e);
            return null;
        }
    }

    private static final Map<CompoundTag, ItemStack> FROM_ATTRIBUTES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static ItemStack fromAttributes(CompoundTag attrs) {
        ItemStack cached = FROM_ATTRIBUTES.get(attrs);
        if (cached != null) return cached;
        String id = attrs.getStringOr("id", "");
        if (id.isEmpty()) return ItemStack.EMPTY;
        RepoItem repo = repoItem(id);
        if (repo == null) return repoMissing(id) ? new ItemStack(Items.BARRIER) : null;
        ItemStack stack = withCustomData(repo.stack(), attrs.copy());
        String reforge = attrs.getStringOr("modifier", "");
        String name = repo.displayName();
        if (!reforge.isEmpty()) {
            String color = name.length() >= 2 && name.charAt(0) == '§' ? name.substring(0, 2) : "";
            String pretty = Character.toUpperCase(reforge.charAt(0)) + reforge.substring(1).replace('_', ' ');
            name = color + pretty + " " + ChatFormatting.stripFormatting(name);
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        List<Component> lore = new ArrayList<>();
        long counter = attrs.getLongOr("mined_crops", 0);
        long cultivating = attrs.getLongOr("farmed_cultivating", 0);
        if (counter > 0) lore.add(Component.literal("§7Counter: §e" + String.format(java.util.Locale.ROOT, "%,d", counter)));
        if (cultivating > 0) lore.add(Component.literal("§7Cultivating: §e" + String.format(java.util.Locale.ROOT, "%,d", cultivating)));
        lore.add(Component.literal("§8In the Farming Toolkit"));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        FROM_ATTRIBUTES.put(attrs, stack);
        return stack;
    }

    public static ItemStack decodeSingle(JsonElement blob) {
        List<ItemStack> list = decode(blob);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    private static volatile boolean warmed;

    public static void warmUp() {
        if (warmed) return;
        warmed = true;
        EXECUTOR.execute(() -> {
            try {
                for (int[] item : new int[][] { { 276, 0 }, { 397, 3 }, { 310, 0 } }) {
                    CompoundTag legacy = new CompoundTag();
                    legacy.putShort("id", (short) item[0]);
                    legacy.putByte("Count", (byte) 1);
                    legacy.putShort("Damage", (short) item[1]);
                    CompoundTag tag = new CompoundTag();
                    CompoundTag display = new CompoundTag();
                    display.putString("Name", "§6Warm Up");
                    ListTag lore = new ListTag();
                    lore.add(net.minecraft.nbt.StringTag.valueOf("§7Strength: §c+1"));
                    display.put("Lore", lore);
                    tag.put("display", display);
                    CompoundTag extra = new CompoundTag();
                    extra.putString("id", "WARM_UP");
                    tag.put("ExtraAttributes", extra);
                    legacy.put("tag", tag);
                    convert(legacy);
                }
            } catch (RuntimeException e) {
                warmed = false;
            }
        });
    }

    static ItemStack convert(CompoundTag legacy) {
        if (legacy.isEmpty() || !legacy.contains("id")) return ItemStack.EMPTY;
        CompoundTag tag = legacy.getCompoundOrEmpty("tag");

        ItemStack stack = fix(legacy);
        if (stack == null) {
            // Retry with only the visual basics — some Hypixel tags trip a fixer or the codec.
            CompoundTag slim = new CompoundTag();
            slim.put("id", legacy.get("id").copy());
            slim.putByte("Count", (byte) 1);
            if (legacy.contains("Damage")) slim.put("Damage", legacy.get("Damage").copy());
            CompoundTag slimTag = new CompoundTag();
            if (tag.contains("display")) slimTag.put("display", tag.getCompoundOrEmpty("display").copy());
            slim.put("tag", slimTag);
            stack = fix(slim);
            if (stack != null) applySkullTexture(stack, tag);
        }
        if (stack == null) stack = new ItemStack(Items.BARRIER);

        String name = tag.getCompoundOrEmpty("display").getStringOr("Name", null);
        if (name != null && !stack.has(DataComponents.CUSTOM_NAME)) stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (tag.contains("ench")) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        CompoundTag extra = tag.getCompoundOrEmpty("ExtraAttributes");
        if (!extra.isEmpty()) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(extra.copy()));
        PvIconRepo.Item repo = PvIconRepo.item(extra.getStringOr("id", ""));
        if (repo != null) applyModel(stack, repo.model());
        return stack;
    }

    // ── Rarity ────────────────────────────────────────────────────────────────

    private static final String[] RARITY_WORDS = {
            "VERY SPECIAL", "SPECIAL", "ULTIMATE", "ADMIN", "DIVINE", "MYTHIC", "LEGENDARY", "EPIC", "UNCOMMON", "RARE", "COMMON" };
    private static final Pattern RARITY_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", RARITY_WORDS) + ")\\b");
    private static final int[] RARITY_RGB = {
            0xFF5555, 0xFF5555, 0xAA0000, 0xAA0000, 0x55FFFF, 0xFF55FF, 0xFFAA00, 0xAA00AA, 0x55FF55, 0x5555FF, 0xFFFFFF };

    public static int rarityColor(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return -1;
        List<Component> lines = lore.lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = ChatFormatting.stripFormatting(lines.get(i).getString());
            if (line == null || line.isBlank()) continue;
            Matcher m = RARITY_PATTERN.matcher(line);
            if (!m.find()) continue;
            for (int r = 0; r < RARITY_WORDS.length; r++) if (RARITY_WORDS[r].equals(m.group(1))) return RARITY_RGB[r];
        }
        return -1;
    }

    public static ItemStack withCustomData(ItemStack stack, CompoundTag data) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return copy;
    }

    private static ItemStack fix(CompoundTag legacy) {
        try {
            int current = SharedConstants.getCurrentVersion().dataVersion().version();
            Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, legacy.copy()), LEGACY_DATA_VERSION, current);
            return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries()), fixed.getValue())
                    .result().orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void applySkullTexture(ItemStack stack, CompoundTag tag) {
        ListTag textures = tag.getCompoundOrEmpty("SkullOwner").getCompoundOrEmpty("Properties").getListOrEmpty("textures");
        if (textures.isEmpty()) return;
        String value = textures.getCompoundOrEmpty(0).getStringOr("Value", null);
        if (value != null) stack.set(DataComponents.PROFILE, texturedProfile(value));
    }

    private static HolderLookup.Provider registries() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        throw new IllegalStateException("No registries available");
    }

    static ResolvableProfile texturedProfile(String textureValue) {
        Multimap<String, Property> props = LinkedHashMultimap.create();
        props.put("textures", new Property("textures", textureValue));
        UUID id = UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8));
        return ResolvableProfile.createResolved(new GameProfile(id, "pv", new PropertyMap(props)));
    }

    public static ItemStack head(String textureValue) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, texturedProfile(textureValue));
        return head;
    }

    // details come from the community NotEnoughUpdates repo

    public record RepoItem(String displayName, ItemStack stack) {}

    private static final String NEU_ITEMS =
            "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/";
    private static final Map<String, CompletableFuture<RepoItem>> REPO = new ConcurrentHashMap<>();

    public static RepoItem repoItem(String id) {
        if (!PvIconRepo.failed()) {
            if (!PvIconRepo.ready()) { PvIconRepo.load(); return null; }
            RepoItem local = ICON_REPO.computeIfAbsent(id, PvItems::fromIconRepo).orElse(null);
            if (local != null || isPetId(id)) return local;
        }
        CompletableFuture<RepoItem> f = repoFuture(id);
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    public static boolean repoMissing(String id) {
        if (!PvIconRepo.failed()) {
            if (!PvIconRepo.ready()) return false;
            if (ICON_REPO.computeIfAbsent(id, PvItems::fromIconRepo).isPresent()) return false;
            if (isPetId(id)) return true;
        }
        CompletableFuture<RepoItem> f = repoFuture(id);
        return f.isDone() && (f.isCompletedExceptionally() || f.join() == null);
    }

    private static final Map<String, java.util.Optional<RepoItem>> ICON_REPO = new ConcurrentHashMap<>();

    private static boolean isPetId(String id) {
        int semi = id.indexOf(';');
        return semi > 0 && PvIconRepo.pet(id.substring(0, semi)) != null;
    }

    private static java.util.Optional<RepoItem> fromIconRepo(String id) {
        int semi = id.indexOf(';');
        if (semi > 0) {
            PvIconRepo.Pet pet = PvIconRepo.pet(id.substring(0, semi));
            if (pet != null) {
                String tex;
                try { tex = pet.textures().get(Integer.parseInt(id.substring(semi + 1))); }
                catch (NumberFormatException e) { tex = null; }
                if (tex == null) return java.util.Optional.empty();
                ItemStack head = head(tex);
                head.set(DataComponents.CUSTOM_NAME, Component.literal(pet.name()));
                return java.util.Optional.of(new RepoItem(pet.name(), head));
            }
        }
        PvIconRepo.Item it = PvIconRepo.item(id);
        if (it == null && id.startsWith("HOE_") && !id.startsWith("HOE_OF_")) it = PvIconRepo.item("THEORETICAL_" + id);
        if (it == null) return java.util.Optional.empty();
        ItemStack stack;
        if (it.texture() != null) {
            stack = head(it.texture());
        } else {
            CompoundTag legacy = new CompoundTag();
            legacy.putString("id", it.itemId());
            legacy.putByte("Count", (byte) 1);
            legacy.putShort("Damage", (short) it.damage());
            stack = fix(legacy);
            if (stack == null) stack = new ItemStack(Items.BARRIER);
        }
        if (it.color() >= 0) stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(it.color()));
        if (it.glint()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        applyModel(stack, it.model());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(it.name()));
        CompoundTag data = new CompoundTag();
        data.putString("id", id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return java.util.Optional.of(new RepoItem(it.name(), stack));
    }

    static void applyModel(ItemStack stack, String model) {
        if (model == null) return;
        net.minecraft.resources.Identifier modelId = net.minecraft.resources.Identifier.tryParse(model);
        if (modelId == null) return;
        var models = Minecraft.getInstance().getModelManager();
        var missing = models.getItemModel(net.minecraft.resources.Identifier.fromNamespaceAndPath("phantomaddons", "__missing__"));
        if (models.getItemModel(modelId) != missing) stack.set(DataComponents.ITEM_MODEL, modelId);
    }

    private static CompletableFuture<RepoItem> repoFuture(String id) {
        return REPO.computeIfAbsent(id, k -> CompletableFuture.supplyAsync(() -> fetchRepoItem(k), EXECUTOR));
    }

    private static RepoItem fetchRepoItem(String id) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(
                    NEU_ITEMS + URLEncoder.encode(id, StandardCharsets.UTF_8) + ".json").toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(15_000);
            if (conn.getResponseCode() != 200) return null;
            String body;
            try (InputStream is = conn.getInputStream()) { body = new String(is.readAllBytes(), StandardCharsets.UTF_8); }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String display = PvJson.str(json, id, "displayname");
            String nbt = PvJson.str(json, "", "nbttag");
            ItemStack stack;
            Matcher m = TEXTURE_VALUE.matcher(nbt);
            if (m.find()) {
                stack = head(m.group(1));
            } else {
                CompoundTag legacy = new CompoundTag();
                legacy.putString("id", PvJson.str(json, "minecraft:barrier", "itemid"));
                legacy.putByte("Count", (byte) 1);
                legacy.putShort("Damage", (short) PvJson.num(json, 0, "damage"));
                stack = fix(legacy);
                if (stack == null) stack = new ItemStack(Items.BARRIER);
            }
            if (nbt.contains("ench:")) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(display));
            List<Component> lore = new ArrayList<>();
            JsonElement loreEl = json.get("lore");
            if (loreEl != null && loreEl.isJsonArray()) {
                for (JsonElement l : loreEl.getAsJsonArray()) lore.add(Component.literal(l.getAsString()));
            }
            if (!lore.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lore));
            if (!id.contains(";")) {
                CompoundTag data = new CompoundTag();
                data.putString("id", id);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            }
            return new RepoItem(display, stack);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
