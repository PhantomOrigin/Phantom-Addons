package com.kuudrahelper.logging;

import com.kuudrahelper.KuudraHelperMod;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

public class PhaseLogAppender extends AbstractAppender {

    public PhaseLogAppender() {
        super("PhantomAddonsPhaseListener", null, null, false, null);
    }

    @Override
    public void append(LogEvent event) {
        String msg = event.getMessage().getFormattedMessage();
        if (!msg.contains("Phase")) return;

        if (msg.contains("Phase 4 (Stun)") || msg.contains("Phase 4")) {
            KuudraHelperMod.LOGGER.info("[PhantomAddons] Phase 4 detected");
            Minecraft.getInstance().execute(PhaseLogger::end);
        }
    }
}