package com.phantomaddons.features.stundps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.profittracker.KuudraDrops;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.phantomaddons.utils.AlwaysOnTopRenderTypes;
import com.phantomaddons.utils.ImmediateDraw;
import com.phantomaddons.utils.WorldRenderCollector;
//?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DpsWaypoint {

    private static final Vec3[] WAYPOINTS = {
        new Vec3(-111, 79, -70),
        new Vec3(-111, 79, -72),
        new Vec3(-111, 79, -74),
    };

    private static final int   COLOR_R = 0, COLOR_G = 255, COLOR_B = 90;
    private static final int   OUTLINE_A = 255;
    private static final int   FILL_A    = 255;
    private static final float Y_OFFSET  = 0.002f;

    private static Vec3 activeWaypoint = null;

    private DpsWaypoint() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isDpsWaypointEnabled()) return;
            if (client.player == null || client.level == null) return;
            tick(client);
        });
    }

    public static void reset() {
        activeWaypoint = null;
    }

    private static void tick(Minecraft mc) {
        KuudraPhaseTracker.Phase phase = KuudraPhaseTracker.getPhase();
        if (phase == KuudraPhaseTracker.Phase.EATEN || phase == KuudraPhaseTracker.Phase.STUN
                || phase == KuudraPhaseTracker.Phase.DPS) {
            computeWaypoint(mc);
        }
    }

    private static UUID findExcludedPlayer(Minecraft mc) {
        for (Player p : mc.level.players()) {
            if (hasFullHollowArmor(p)) return p.getUUID();
        }
        for (Player p : mc.level.players()) {
            if (p.isPassenger()) return p.getUUID();
        }
        return null;
    }

    private static boolean hasFullHollowArmor(Player p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = p.getItemBySlot(slot);
            if (stack.isEmpty()) return false;
            String name = KuudraDrops.stripColor(stack.getDisplayName().getString()).toLowerCase();
            if (!name.contains("hollow")) return false;
        }
        return true;
    }

    private static void computeWaypoint(Minecraft mc) {
        if (mc.player == null) return;

        UUID excludedUuid = findExcludedPlayer(mc);
        if (excludedUuid != null && mc.player.getUUID().equals(excludedUuid)) {
            activeWaypoint = null;
            return;
        }

        List<Player> ordered = new ArrayList<>();
        for (Player p : mc.level.players()) {
            if (excludedUuid != null && p.getUUID().equals(excludedUuid)) continue;
            ordered.add(p);
        }
        ordered.sort(Comparator.comparing(Player::getScoreboardName, String.CASE_INSENSITIVE_ORDER));

        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getUUID().equals(mc.player.getUUID())) { idx = i; break; }
        }

        activeWaypoint = (idx >= 0 && idx < WAYPOINTS.length) ? WAYPOINTS[idx] : null;
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isDpsWaypointEnabled()) return;
        KuudraPhaseTracker.Phase phase = KuudraPhaseTracker.getPhase();
        if (phase != KuudraPhaseTracker.Phase.EATEN && phase != KuudraPhaseTracker.Phase.STUN
                && phase != KuudraPhaseTracker.Phase.DPS) return;
        if (activeWaypoint == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = camera.position();
        Matrix4f m  = matrices.last().pose();
        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        drawTopFace(imm, m, activeWaypoint, camPos);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        *///?} else {
        // Everything this feature draws is always-on-top (visible through walls) — see
        // renderAlwaysOnTop, which draws it via ImmediateDraw from AFTER_TRANSLUCENT_TERRAIN instead.
        //?}
    }

    //? if <26.2 {
    /*public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {}
    *///?} else {
    public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isDpsWaypointEnabled()) return;
        KuudraPhaseTracker.Phase phase = KuudraPhaseTracker.getPhase();
        if (phase != KuudraPhaseTracker.Phase.EATEN && phase != KuudraPhaseTracker.Phase.STUN
                && phase != KuudraPhaseTracker.Phase.DPS) return;
        if (activeWaypoint == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = camera.position();
        Matrix4f m  = matrices.last().pose();

        drawTopFace(m, activeWaypoint, camPos);
    }
    //?}

    //? if <26.2 {
    /*private static void drawTopFace(MultiBufferSource.BufferSource imm, Matrix4f m, Vec3 center, Vec3 camPos) {
        float x0 = (float)(center.x - 0.5 - camPos.x);
        float x1 = (float)(center.x + 0.5 - camPos.x);
        float y  = (float)(center.y         - camPos.y) + Y_OFFSET;
        float z0 = (float)(center.z - 0.5 - camPos.z);
        float z1 = (float)(center.z + 0.5 - camPos.z);

        VertexConsumer lines = imm.getBuffer(RenderTypes.lines());
        edge(lines, m, x0, y, z0, x1, y, z0);
        edge(lines, m, x1, y, z0, x1, y, z1);
        edge(lines, m, x1, y, z1, x0, y, z1);
        edge(lines, m, x0, y, z1, x0, y, z0);
        imm.endBatch(RenderTypes.lines());

        VertexConsumer quads = imm.getBuffer(RenderTypes.debugQuads());
        quad(quads, m, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1,  0, 1, 0);
        quad(quads, m, x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0,  0,-1, 0);
        imm.endBatch(RenderTypes.debugQuads());
    }
    *///?} else {
    private static void drawTopFace(Matrix4f m, Vec3 center, Vec3 camPos) {
        float x0 = (float)(center.x - 0.5 - camPos.x);
        float x1 = (float)(center.x + 0.5 - camPos.x);
        float y  = (float)(center.y         - camPos.y) + Y_OFFSET;
        float z0 = (float)(center.z - 0.5 - camPos.z);
        float z1 = (float)(center.z + 0.5 - camPos.z);

        VertexConsumer lines = ImmediateDraw.begin(AlwaysOnTopRenderTypes.lines());
        edge(lines, m, x0, y, z0, x1, y, z0);
        edge(lines, m, x1, y, z0, x1, y, z1);
        edge(lines, m, x1, y, z1, x0, y, z1);
        edge(lines, m, x0, y, z1, x0, y, z0);

        VertexConsumer quads = ImmediateDraw.begin(AlwaysOnTopRenderTypes.debugQuads());
        quad(quads, m, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1,  0, 1, 0);
        quad(quads, m, x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0,  0,-1, 0);
    }
    //?}

    private static void edge(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1) {
        float dx = x1-x0, dy = y1-y0, dz = z1-z0;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        vc.addVertex(m, x0, y0, z0).setColor(COLOR_R, COLOR_G, COLOR_B, OUTLINE_A).setNormal(dx/len, dy/len, dz/len).setLineWidth(2.0f);
        vc.addVertex(m, x1, y1, z1).setColor(COLOR_R, COLOR_G, COLOR_B, OUTLINE_A).setNormal(dx/len, dy/len, dz/len).setLineWidth(2.0f);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(COLOR_R, COLOR_G, COLOR_B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(COLOR_R, COLOR_G, COLOR_B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(COLOR_R, COLOR_G, COLOR_B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(COLOR_R, COLOR_G, COLOR_B, FILL_A).setNormal(nx, ny, nz);
    }
}
