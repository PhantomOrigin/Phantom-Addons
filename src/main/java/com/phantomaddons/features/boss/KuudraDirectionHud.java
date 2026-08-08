package com.phantomaddons.features.boss;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}

public final class KuudraDirectionHud {

    private static final double X_RIGHT     = -128.0;
    private static final double X_LEFT      =  -72.0;
    private static final double Z_FRONT     =  -84.0;
    private static final double Z_BACK      = -132.0;
    private static final int    KUUDRA_SIZE  = 30;

    private static volatile boolean    active       = false;
    private static volatile String     direction    = null;
    private static          AbstractCubeMob cachedKuudra = null;

    private KuudraDirectionHud() {}

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || !PhantomConfig.isKuudraDirectionEnabled()) {
                direction = null;
                active    = false;
                return;
            }

            if (KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.SUPPLIES) {
                if (active || direction != null) {
                    active    = false;
                    direction = null;
                    cachedKuudra = null;
                }
                return;
            }

            if (!active) {
                if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SKIP) {
                    direction = null;
                    return;
                }
                active = true;
            }

            AbstractCubeMob kuudra = findKuudra(client);
            if (kuudra == null) {
                direction = null;
                return;
            }
            String d = computeDirection(kuudra.getX(), kuudra.getZ());
            direction = d;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "kuudra_direction"),
                (drawContext, tickCounter) -> {
            if (!PhantomConfig.isKuudraDirectionEnabled()) return;
            String d = direction;
            if (d == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.font == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            float cx = PhantomConfig.getDirectionHudX() * screenW;
            float cy = PhantomConfig.getDirectionHudY() * screenH;
            float scale = 3.5f * PhantomConfig.getDirectionHudScale();

            var matrices = drawContext.pose();
            matrices.pushMatrix();
            matrices.translate(cx, cy);
            matrices.scale(scale, scale);

            int textWidth = mc.font.width(d);
            drawContext.text(
                    mc.font,
                    Component.literal(d),
                    -textWidth / 2, 0,
                    0xFFFFFFFF, true);

            matrices.popMatrix();
        });
    }

    public static void reset() {
        direction    = null;
        active       = false;
        cachedKuudra = null;
    }

    private static AbstractCubeMob findKuudra(Minecraft mc) {
        if (mc.level == null) { cachedKuudra = null; return null; }

        if (cachedKuudra != null
                && cachedKuudra.isAlive()
                && cachedKuudra.getSize()   == KUUDRA_SIZE
                && cachedKuudra.getHealth() > 0) {
            return cachedKuudra;
        }

        cachedKuudra = null;
        double highestY = Double.NEGATIVE_INFINITY;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractCubeMob s)) continue;
            if (s.getSize() != KUUDRA_SIZE || s.getHealth() <= 0) continue;
            if (s.getY() > highestY) {
                highestY     = s.getY();
                cachedKuudra = s;
            }
        }
        return cachedKuudra;
    }

    private static String computeDirection(double x, double z) {
        if (x < X_RIGHT) return "RIGHT!";
        if (z > Z_FRONT) return "FRONT!";
        if (x > X_LEFT)  return "LEFT!";
        if (z < Z_BACK)  return "BACK!";
        return null;
    }
}
