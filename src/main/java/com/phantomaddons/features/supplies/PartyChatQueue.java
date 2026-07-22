package com.phantomaddons.features.supplies;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PartyChatQueue {

    private static final long MIN_INTERVAL_MS = 200;

    private static long lastSentMs = 0;
    private static final Deque<String> queue = new ArrayDeque<>();

    private PartyChatQueue() {}

    public static void send(String message) {
        queue.addLast("pc " + message);
    }

    public static void sendCommand(String command) {
        queue.addLast(command);
    }

    public static void tick(Minecraft mc) {
        if (queue.isEmpty()) return;
        if (mc.getConnection() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSentMs < MIN_INTERVAL_MS) return;

        String command = queue.pollFirst();
        mc.getConnection().sendCommand(command);
        lastSentMs = now;
    }

    public static void reset() {
        queue.clear();
    }
}
