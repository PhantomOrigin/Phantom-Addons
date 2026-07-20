package com.phantomaddons.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

import java.util.ArrayList;
import java.util.List;

public class KuudraWorldRenderDispatcher {

    public interface WorldRenderer {
        void render(PoseStack matrices, Camera camera, float tickDelta);
    }

    private static final List<WorldRenderer> RENDERERS = new ArrayList<>();

    public static void register(WorldRenderer renderer) {
        RENDERERS.add(renderer);
    }

    public static void renderAll(PoseStack matrices, Camera camera, float tickDelta) {
        for (WorldRenderer r : RENDERERS) {
            r.render(matrices, camera, tickDelta);
        }
    }
}