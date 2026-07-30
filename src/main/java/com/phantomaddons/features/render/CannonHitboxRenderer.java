package com.phantomaddons.features.render;

import com.phantomaddons.PhantomConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CannonHitboxRenderer {

    private static final int C_R = 40, C_G = 110, C_B = 255; // blue
    private static final int H_R = 255, H_G = 0, H_B = 0;    // red — the specific highlighted stand
    private static final double GROUP_XZ_TOLERANCE = 0.1;

    private static final int OUTLINE_A = 200;
    private static final int FILL_A    = 13; // ~5% opacity

    private CannonHitboxRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        boolean left  = PhantomConfig.isCannonHitboxesEnabled() && PhantomConfig.isCannonHitboxesLeftEnabled();
        boolean right = PhantomConfig.isCannonHitboxesEnabled() && PhantomConfig.isCannonHitboxesRightEnabled();
        if (!left && !right) return;
        if (!HideArmorStands.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.player.isPassenger()) return; // hide while mounted in the cannon itself

        Vec3 camPos = camera.position();
        Matrix4f m  = matrices.last().pose();
        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        java.util.List<ArmorStand> leftStands  = new java.util.ArrayList<>();
        java.util.List<ArmorStand> rightStands = new java.util.ArrayList<>();
        ArmorStand leftAnchor  = null;
        ArmorStand rightAnchor = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;
            if (stand.getCustomName() != null) continue;

            boolean isAnchor = stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.BEDROCK);
            if (left && HideArmorStands.isInLeftCannon(stand.getX(), stand.getZ())) {
                leftStands.add(stand);
                if (isAnchor) leftAnchor = stand;
            } else if (right && HideArmorStands.isInRightCannon(stand.getX(), stand.getZ())) {
                rightStands.add(stand);
                if (isAnchor) rightAnchor = stand;
            }
        }

        boolean drewAny = false;
        drewAny |= renderGroup(leftStands, leftAnchor, camPos, m, imm);
        drewAny |= renderGroup(rightStands, rightAnchor, camPos, m, imm);
        if (drewAny) imm.endBatch();
    }

    private static boolean renderGroup(java.util.List<ArmorStand> stands, ArmorStand anchor,
                                        Vec3 camPos, Matrix4f m, MultiBufferSource.BufferSource imm) {
        boolean drewAny = false;
        for (ArmorStand stand : stands) {
            boolean highlighted = anchor != null
                    && Math.abs(stand.getX() - anchor.getX()) <= GROUP_XZ_TOLERANCE
                    && Math.abs(stand.getZ() - anchor.getZ()) <= GROUP_XZ_TOLERANCE;
            int r = highlighted ? H_R : C_R, g = highlighted ? H_G : C_G, b = highlighted ? H_B : C_B;

            AABB bb = stand.getBoundingBox();
            double x1 = bb.minX - camPos.x, y1 = bb.minY - camPos.y, z1 = bb.minZ - camPos.z;
            double x2 = bb.maxX - camPos.x, y2 = bb.maxY - camPos.y, z2 = bb.maxZ - camPos.z;

            addOutline(imm.getBuffer(RenderTypes.lines()), m, x1, y1, z1, x2, y2, z2, r, g, b, OUTLINE_A);
            addFill(imm.getBuffer(RenderTypes.debugQuads()), m, x1, y1, z1, x2, y2, z2, r, g, b, FILL_A);
            drewAny = true;
        }
        return drewAny;
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   int r, int g, int b, int a) {
        float ax = (float)x1, ay = (float)y1, az = (float)z1;
        float bx = (float)x2, by = (float)y2, bz = (float)z2;
        cline(vc, m, ax, ay, az, bx, ay, az, r, g, b, a,  1, 0, 0);
        cline(vc, m, bx, ay, az, bx, ay, bz, r, g, b, a,  0, 0, 1);
        cline(vc, m, bx, ay, bz, ax, ay, bz, r, g, b, a, -1, 0, 0);
        cline(vc, m, ax, ay, bz, ax, ay, az, r, g, b, a,  0, 0,-1);
        cline(vc, m, ax, by, az, bx, by, az, r, g, b, a,  1, 0, 0);
        cline(vc, m, bx, by, az, bx, by, bz, r, g, b, a,  0, 0, 1);
        cline(vc, m, bx, by, bz, ax, by, bz, r, g, b, a, -1, 0, 0);
        cline(vc, m, ax, by, bz, ax, by, az, r, g, b, a,  0, 0,-1);
        cline(vc, m, ax, ay, az, ax, by, az, r, g, b, a, 0, 1, 0);
        cline(vc, m, bx, ay, az, bx, by, az, r, g, b, a, 0, 1, 0);
        cline(vc, m, bx, ay, bz, bx, by, bz, r, g, b, a, 0, 1, 0);
        cline(vc, m, ax, ay, bz, ax, by, bz, r, g, b, a, 0, 1, 0);
    }

    private static void cline(VertexConsumer vc, Matrix4f m,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(3.0f);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(3.0f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b, int a) {
        float ax = (float)x1, ay = (float)y1, az = (float)z1;
        float bx = (float)x2, by = (float)y2, bz = (float)z2;
        cquad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz, r,g,b,a,  0,-1, 0);
        cquad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az, r,g,b,a,  0, 1, 0);
        cquad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az, r,g,b,a,  0, 1, 0);
        cquad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az, r,g,b,a,  0,-1, 0);
        cquad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az, r,g,b,a,  0, 0,-1);
        cquad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az, r,g,b,a,  0, 0, 1);
        cquad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz, r,g,b,a,  0, 0, 1);
        cquad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz, r,g,b,a,  0, 0,-1);
        cquad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, r,g,b,a, -1, 0, 0);
        cquad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az, r,g,b,a,  1, 0, 0);
        cquad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz, r,g,b,a,  1, 0, 0);
        cquad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, r,g,b,a, -1, 0, 0);
    }

    private static void cquad(VertexConsumer vc, Matrix4f m,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
