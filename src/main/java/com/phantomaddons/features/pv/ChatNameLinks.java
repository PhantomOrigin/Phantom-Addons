package com.phantomaddons.features.pv;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatNameLinks {

    private static final Pattern MSG = Pattern.compile("^/(?:msg|w|tell|r) (\\w{3,16}) ?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOCIAL = Pattern.compile("^/socialoptions (\\w{3,16})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PV_COMMAND = Pattern.compile("^/(?:sbpv pv|pv|skyblocker pv|skyblockpv pv) (\\w{3,16})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIEW_PROFILE = Pattern.compile("^/viewprofile \\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOVER_LINE = Pattern.compile(
            "^(?:Click here to view|Click to open) (\\w{3,16})'s profile(?: in .*)?\\.?$", Pattern.CASE_INSENSITIVE);

    private static final Identifier LATE = Identifier.fromNamespaceAndPath("phantomaddons", "late");

    private ChatNameLinks() {}

    public static void register() {
        ClientReceiveMessageEvents.MODIFY_GAME.addPhaseOrdering(Event.DEFAULT_PHASE, LATE);
        ClientReceiveMessageEvents.MODIFY_GAME.register(LATE, (message, overlay) -> overlay ? message : rewrite(message));
    }

    private static boolean enabled() {
        return PhantomConfig.isPvClickNames() && PhantomConfig.isPvEnabled();
    }

    public static String nameFor(Style style) {
        if (style == null) return null;
        ClickEvent click = style.getClickEvent();
        if (click instanceof ClickEvent.SuggestCommand suggest) {
            Matcher m = MSG.matcher(suggest.command());
            return m.matches() ? m.group(1) : null;
        }
        if (!(click instanceof ClickEvent.RunCommand run)) return null;
        String cmd = run.command();
        Matcher m = SOCIAL.matcher(cmd);
        if (m.matches()) return m.group(1);
        m = PV_COMMAND.matcher(cmd);
        if (m.matches()) return m.group(1);
        if (VIEW_PROFILE.matcher(cmd).matches()) return nameFromHover(style);
        return null;
    }

    private static String nameFromHover(Style style) {
        if (!(style.getHoverEvent() instanceof HoverEvent.ShowText show)) return null;
        for (String line : show.value().getString().split("\n")) {
            Matcher m = HOVER_LINE.matcher(line.replaceAll("§.", "").trim());
            if (m.matches()) return m.group(1);
        }
        return null;
    }

    // ── Hover text ────────────────────────────────────────────────────────────

    static Component rewrite(Component message) {
        if (!enabled()) return message;
        try {
            return walk(message);
        } catch (RuntimeException e) {
            return message;
        }
    }

    private static MutableComponent walk(Component c) {
        MutableComponent copy = c.plainCopy().setStyle(restyle(c.getStyle()));
        for (Component sibling : c.getSiblings()) copy.append(walk(sibling));
        return copy;
    }

    private static Style restyle(Style style) {
        String name = nameFor(style);
        if (name == null || !(style.getHoverEvent() instanceof HoverEvent.ShowText show)) return style;
        if (!(style.getClickEvent() instanceof ClickEvent.RunCommand)) return style;
        return style.withHoverEvent(new HoverEvent.ShowText(hoverFor(show.value())))
                .withClickEvent(new ClickEvent.RunCommand("/pv " + name));
    }

    private record Run(Style style, String text) {}

    private static Component hoverFor(Component original) {
        List<List<Run>> lines = new ArrayList<>();
        lines.add(new ArrayList<>());
        original.visit((style, text) -> {
            String[] parts = text.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) lines.add(new ArrayList<>());
                if (!parts[i].isEmpty()) lines.get(lines.size() - 1).add(new Run(style, parts[i]));
            }
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent out = Component.empty();
        boolean first = true;
        for (List<Run> line : lines) {
            StringBuilder plain = new StringBuilder();
            for (Run r : line) plain.append(r.text);
            if (HOVER_LINE.matcher(plain.toString().replaceAll("§.", "").trim()).matches()) continue;
            if (!first) out.append(Component.literal("\n"));
            first = false;
            for (Run r : line) out.append(Component.literal(r.text).setStyle(r.style));
        }
        if (!first) out.append(Component.literal("\n"));
        out.append(Component.literal("Click to open profile viewer")
                .withStyle(net.minecraft.ChatFormatting.YELLOW));
        return out;
    }
}
