package com.phantomaddons.utils;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandCase {

    private static final Set<String> OWN_ROOTS = Set.of(
            "pa", "phantom", "phaselog", "giantlog", "kuudrastationarylog", "phantomdebug",
            "pv", "pvcard", "kuudra", "t1", "t2", "t3", "t4", "t5");

    private CommandCase() {}

    public static void register() {
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> PhantomConfig.isCaseInsensitiveCommands() ? fix(command) : command);
    }

    public static String fix(String command) {
        return fix(command, clientRoot());
    }

    public static String fixForDisplay(String text, CommandNode<?> root) {
        if (text == null || text.isEmpty()) return text;
        int slash = text.charAt(0) == '/' ? 1 : 0;
        String fixed = fix(text.substring(slash), root);
        return fixed.length() == text.length() - slash ? text.substring(0, slash) + fixed : text;
    }

    public static Suggestions keepTypedCase(String input, Suggestions suggestions, CommandNode<?> root) {
        if (input == null || suggestions == null || suggestions.isEmpty() || root == null) return suggestions;
        int slash = input.startsWith("/") ? 1 : 0;
        boolean all = PhantomConfig.isCaseInsensitiveCommands();
        String firstWord = firstToken(input.substring(slash)).toLowerCase(Locale.ROOT);

        boolean changed = false;
        List<Suggestion> out = new ArrayList<>(suggestions.getList().size());
        for (Suggestion s : suggestions.getList()) {
            int start = s.getRange().getStart();
            int end = Math.min(s.getRange().getEnd(), input.length());
            String text = s.getText();
            String typed = start >= 0 && start < end ? input.substring(start, end) : "";
            boolean ours = all || (input.substring(Math.min(slash, start), Math.max(slash, Math.min(start, input.length()))).isBlank()
                    ? OWN_ROOTS.contains(text.toLowerCase(Locale.ROOT))
                    : OWN_ROOTS.contains(firstWord));
            if (ours && !typed.isEmpty() && !text.startsWith(typed)
                    && text.regionMatches(true, 0, typed, 0, typed.length())
                    && literalNames(input, start, root).contains(text)) {
                out.add(new Suggestion(s.getRange(), typed + text.substring(typed.length()), s.getTooltip()));
                changed = true;
            } else {
                out.add(s);
            }
        }
        return changed ? new Suggestions(suggestions.getRange(), out) : suggestions;
    }

    private static String firstToken(String command) {
        int end = 0;
        while (end < command.length() && !Character.isWhitespace(command.charAt(end))) end++;
        return command.substring(0, end);
    }

    private static Set<String> literalNames(String input, int tokenStart, CommandNode<?> root) {
        int n = input.length();
        int i = input.startsWith("/") ? 1 : 0;
        CommandNode<?> node = root;
        while (true) {
            while (i < n && Character.isWhitespace(input.charAt(i))) i++;
            if (i >= tokenStart) break;
            int start = i;
            while (i < n && !Character.isWhitespace(input.charAt(i))) i++;
            CommandNode<?> next = literal(node, input.substring(start, i));
            if (next == null) return Set.of();
            node = next;
        }
        Set<String> names = new HashSet<>();
        for (CommandNode<?> child : node.getChildren()) if (child instanceof LiteralCommandNode) names.add(child.getName());
        return names;
    }

    private static String fix(String command, CommandNode<?> root) {
        if (command == null || command.isEmpty()) return command;
        int n = command.length();
        int i = 0;
        while (i < n && !Character.isWhitespace(command.charAt(i))) i++;
        String rootToken = command.substring(0, i);
        String lower = rootToken.toLowerCase(Locale.ROOT);
        if (!PhantomConfig.isCaseInsensitiveCommands() && !OWN_ROOTS.contains(lower)) return command;

        CommandNode<?> node = root == null ? null : literal(root, rootToken);
        StringBuilder out = new StringBuilder(n);
        out.append(node != null ? node.getName() : lower);
        while (node != null && i < n) {
            int gap = i;
            while (i < n && Character.isWhitespace(command.charAt(i))) i++;
            out.append(command, gap, i);
            int start = i;
            while (i < n && !Character.isWhitespace(command.charAt(i))) i++;
            if (start == i) break;
            String token = command.substring(start, i);
            CommandNode<?> next = literal(node, token);
            if (next == null) {
                out.append(token);
                break;
            }
            out.append(next.getName());
            node = next;
        }
        out.append(command, i, n);
        return out.toString();
    }

    private static CommandNode<?> clientRoot() {
        try {
            var dispatcher = ClientCommands.getActiveDispatcher();
            return dispatcher == null ? null : dispatcher.getRoot();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static CommandNode<?> literal(CommandNode<?> parent, String name) {
        CommandNode<?> exact = parent.getChild(name);
        if (exact instanceof LiteralCommandNode) return exact;
        for (CommandNode<?> child : parent.getChildren()) {
            if (child instanceof LiteralCommandNode && child.getName().equalsIgnoreCase(name)) return child;
        }
        return null;
    }
}
