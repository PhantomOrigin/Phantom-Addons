package com.phantomaddons.features.misckuudra.splits;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.PhaseResult;
import com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.Split;
import com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.SupplyTime;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class SplitHud {

    private static final int LINE_H = 10;
    private static final int INDENT = 8;

    private SplitHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "split_hud"),
                SplitHud::render);
    }

    private static final long SHOW_AFTER_END_MS = 20_000L;

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker tc) {
        if (!PhantomConfig.isSplitTimerEnabled()) return;
        if (!KuudraSplitTimer.hasData()) return;

        boolean active   = KuudraSplitTimer.isRunActive();
        boolean complete = KuudraSplitTimer.isRunComplete();
        if (!active && !complete) return;
        if (complete && !active) {
            if (System.currentTimeMillis() - KuudraSplitTimer.getRunEndMs() > SHOW_AFTER_END_MS) return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float scl = PhantomConfig.getSplitHudScale();
        var matrices = ctx.pose();
        matrices.pushMatrix();
        matrices.translate((int)(PhantomConfig.getSplitHudX() * screenW), (int)(PhantomConfig.getSplitHudY() * screenH));
        matrices.scale(scl, scl);

        int x = 0, y = 0;

        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;

        drawLine(ctx, mc, x, y, "§b§lKuudra Splits:");
        y += LINE_H;

        for (Split s : KuudraSplitTimer.splitsForTier(tier)) {
            y = drawSplit(ctx, mc, x, y, s);
        }

        double overall = KuudraSplitTimer.isRunComplete()
                ? KuudraSplitTimer.getTotalRunTime()
                : KuudraSplitTimer.getOverallElapsed();
        drawLine(ctx, mc, x, y, "§bOverall:§r " + fmt(overall));
        y += LINE_H;

        if (!KuudraSplitTimer.isRunComplete()) {
            double pred = KuudraSplitTimer.getPredicted();
            if (pred > 0) {
                drawLine(ctx, mc, x, y, "§bPredicted:§r " + fmt(pred));
            }
        }

        matrices.popMatrix();
    }

    private static int drawSplit(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, Split s) {
        PhaseResult result = KuudraSplitTimer.getResult(s);
        boolean     active = s == KuudraSplitTimer.getActiveSplit();

        String label = KuudraSplitTimer.splitLabel(s);
        StringBuilder sb = new StringBuilder();
        sb.append("§b").append(label).append(":§r ");

        if (result != null) {
            sb.append(fmt(result.wallSec()));

            Double diff = KuudraSplitTimer.getPbDiff(s);
            if (diff != null) {
                sb.append("   ");
                if (diff <= 0) sb.append("§a[").append(fmtDiff(diff)).append("]");
                else            sb.append("§c[+").append(fmt1(diff)).append("]");
                sb.append("§r");
            }

            if (KuudraSplitTimer.showLag(s)) {
                double lag = result.lagSec();
                sb.append("   ");
                if (lag >= 0) sb.append("§e+").append(fmt2(lag));
                else          sb.append("§a").append(fmt2(lag));
                sb.append("§r");
            }

        } else if (active) {
            sb.append(fmt(KuudraSplitTimer.getActiveSplitElapsed()));
        } else {
            sb.append("—");
        }

        drawLine(ctx, mc, x, y, sb.toString());
        y += LINE_H;

        if (s == Split.SUPPLIES && PhantomConfig.isSupplyTimesEnabled()) {
            List<SupplyTime> times = KuudraSplitTimer.getSupplyTimes();
            for (SupplyTime st : times) {
                drawLine(ctx, mc, x + INDENT, y,
                        "§7- " + st.player() + ": "
                                + supplyTimeColor(st.timeSec()) + fmt(st.timeSec()) + "§r");
                y += LINE_H;
            }
        }
        if (s == Split.BUILD && PhantomConfig.isSupplyTimesEnabled()) {
            List<PhantomConfig.PlayerTime> freshes = KuudraSplitTimer.getFreshTimes();
            for (PhantomConfig.PlayerTime ft : freshes) {
                String pctSuffix = ft.pct >= 0 ? " §7(" + ft.pct + "%)" : "";
                drawLine(ctx, mc, x + INDENT, y,
                        "§7- §f" + ft.player + ": "
                                + freshTimeColor(ft.time) + fmt(ft.time) + "§r" + pctSuffix);
                y += LINE_H;
            }
        }

        return y;
    }

    private static void drawLine(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, String text) {
        ctx.text(mc.font, text, x, y, 0xFFFFFFFF, true);
    }

    private static String supplyTimeColor(double sec) {
        if (sec < 19) return "§f";
        if (sec < 24) return "§b";
        if (sec < 28) return "§a";
        return "§c";
    }

    private static String freshTimeColor(double sec) {
        if (sec < 8)  return "§b";
        if (sec < 12) return "§a";
        return "§c";
    }

    private static String fmt(double sec) {
        if (sec >= 60) {
            int m = (int)(sec / 60), s = (int)(sec % 60);
            return String.format("%d:%02ds", m, s);
        }
        return String.format("%.2fs", sec);
    }

    private static String fmt1(double sec) { return String.format("%.1f", sec); }

    private static String fmt2(double sec) { return String.format("%.2f", sec); }

    private static String fmtDiff(double diff) {
        return String.format("%.1f", diff);
    }
}
