package com.phantomaddons.features.boss;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IchorRadiusTracker {

    private static final String CAST_MESSAGE = "Casting Spell: Ichor Pool!";
    private static final Pattern[] BROADCAST_PATTERNS = {
            Pattern.compile("\\[Phantom] Ichor Pool at (-?\\d+(?:\\.\\d+)?) (-?\\d+(?:\\.\\d+)?) (-?\\d+(?:\\.\\d+)?)"),
            Pattern.compile("\\[Phantom] Ichor Pool @ \\((-?\\d+(?:\\.\\d+)?), (-?\\d+(?:\\.\\d+)?), (-?\\d+(?:\\.\\d+)?)\\)"),
            Pattern.compile("\\[KIC] Casting Ichor Pool at (-?\\d+(?:\\.\\d+)?) (-?\\d+(?:\\.\\d+)?) (-?\\d+(?:\\.\\d+)?)")
    };
    private static final Pattern PARTY_SENDER_PATTERN =
            Pattern.compile("Party > (?:\\[[^\\]]+] )?([A-Za-z0-9_]+): (.+)");

    private static final long POOL_LIFETIME_MS = 20_000L;

    private static boolean poolActive  = false;
    private static long    poolCastMs  = 0L;
    private static Vec3    poolCenter  = null;

    private IchorRadiusTracker() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            onChat(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) ->
                onChat(message.getString())
        );
    }

    private static void onChat(String raw) {
        if (!PhantomConfig.isIchorRadiusEnabled()) return;
        if (!isActivePhase()) return;

        String clean = TextUtil.stripColor(raw).trim();

        if (clean.contains(CAST_MESSAGE)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            poolCenter = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            poolCastMs = System.currentTimeMillis();
            poolActive = true;
            return;
        }

        if (isSelfSentPartyMessage(clean)) return;

        for (Pattern pattern : BROADCAST_PATTERNS) {
            Matcher m = pattern.matcher(clean);
            if (!m.find()) continue;

            double x = Double.parseDouble(m.group(1));
            double y = Double.parseDouble(m.group(2));
            double z = Double.parseDouble(m.group(3));
            poolCenter = new Vec3(x, y, z);
            poolCastMs = System.currentTimeMillis();
            poolActive = true;
            return;
        }
    }

    private static boolean isSelfSentPartyMessage(String clean) {
        Matcher sender = PARTY_SENDER_PATTERN.matcher(clean);
        if (!sender.find()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return sender.group(1).equalsIgnoreCase(mc.player.getName().getString());
    }

    public static void reset() {
        poolActive = false;
        poolCenter = null;
    }

    public static Vec3 getActivePool(float[] fractionOut) {
        if (!PhantomConfig.isIchorRadiusEnabled() || !poolActive || poolCenter == null) return null;

        long elapsed = System.currentTimeMillis() - poolCastMs;
        if (elapsed >= POOL_LIFETIME_MS) {
            poolActive = false;
            return null;
        }

        if (fractionOut != null && fractionOut.length > 0) {
            fractionOut[0] = 1f - (float) elapsed / POOL_LIFETIME_MS;
        }
        return poolCenter;
    }

    private static boolean isActivePhase() {
        KuudraPhaseTracker.Phase p = KuudraPhaseTracker.getPhase();
        return p == KuudraPhaseTracker.Phase.EATEN
                || p == KuudraPhaseTracker.Phase.STUN
                || p == KuudraPhaseTracker.Phase.DPS
                || p == KuudraPhaseTracker.Phase.SKIP
                || p == KuudraPhaseTracker.Phase.BOSS
                || p == KuudraPhaseTracker.Phase.KILL
                || p == KuudraPhaseTracker.Phase.DEATH;
    }
}
