package com.phantomaddons.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Plays a UI sound, then plays it again on a tick interval for the remaining repeats. */
public final class SoundRepeater {

    private static final class Job {
        final String soundId;
        final float volume, pitch;
        final int interval;
        int left, wait;
        Job(String soundId, float volume, float pitch, int left, int interval) {
            this.soundId = soundId; this.volume = volume; this.pitch = pitch;
            this.left = left; this.interval = interval; this.wait = interval;
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private SoundRepeater() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void play(String soundId, float volume, float pitch, int repeats, int intervalTicks) {
        playOnce(soundId, volume, pitch);
        if (repeats > 1) {
            synchronized (JOBS) {
                JOBS.add(new Job(soundId, volume, pitch, repeats - 1, Math.max(1, intervalTicks)));
            }
        }
    }

    private static void tick() {
        synchronized (JOBS) {
            for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
                Job j = it.next();
                if (--j.wait > 0) continue;
                playOnce(j.soundId, j.volume, j.pitch);
                j.wait = j.interval;
                if (--j.left <= 0) it.remove();
            }
        }
    }

    private static void playOnce(String soundId, float volume, float pitch) {
        Identifier id = Identifier.tryParse(soundId);
        if (id == null) return;
        SoundEvent ev = SoundEvent.createVariableRangeEvent(id);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
    }
}
