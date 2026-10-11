package com.phantomaddons.features.pv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PvLisp {

    private record Fn(List<String> params, Object body) {}

    private final Map<String, Fn> functions = new HashMap<>();

    PvLisp(List<String> prelude) {
        for (String def : prelude) {
            try {
                Object form = parse(def);
                if (form instanceof List<?> l && l.size() == 4 && "defun".equals(l.get(0))) {
                    List<String> params = new ArrayList<>();
                    for (Object p : (List<?>) l.get(2)) params.add((String) p);
                    functions.put((String) l.get(1), new Fn(params, l.get(3)));
                }
            } catch (RuntimeException ignored) {}
        }
    }

    Object eval(String src, Map<String, Object> vars) {
        if (src == null || src.isBlank()) return null;
        try { return eval(parse(src), vars); } catch (RuntimeException e) { return null; }
    }

    private Object eval(Object form, Map<String, Object> env) {
        if (form instanceof Double || form instanceof Str) return form instanceof Str s ? s.v : form;
        if (form instanceof String sym) {
            if (sym.startsWith(":")) return sym;
            if (sym.equals("true")) return 1.0;
            if (sym.equals("false")) return 0.0;
            Object v = env.get(sym);
            return v != null ? v : 0.0;
        }
        List<?> l = (List<?>) form;
        if (l.isEmpty()) return null;
        String op = (String) l.get(0);
        if (op.equals("if")) return truthy(eval(l.get(1), env)) ? eval(l.get(2), env) : eval(l.get(3), env);

        List<Object> args = new ArrayList<>();
        for (int i = 1; i < l.size(); i++) args.add(eval(l.get(i), env));
        switch (op) {
            case "+": { double r = 0; for (Object a : args) r += d(a); return r; }
            case "*": { double r = 1; for (Object a : args) r *= d(a); return r; }
            case "-": return args.size() == 1 ? -d(args.get(0)) : d(args.get(0)) - d(args.get(1));
            case "/": return d(args.get(0)) / d(args.get(1));
            case "pow": return Math.pow(d(args.get(0)), d(args.get(1)));
            case "round": return (double) Math.round(d(args.get(0)));
            case "floor": return Math.floor(d(args.get(0)));
            case "ceil": return Math.ceil(d(args.get(0)));
            case "lt": return d(args.get(0)) < d(args.get(1)) ? 1.0 : 0.0;
            case "gt": return d(args.get(0)) > d(args.get(1)) ? 1.0 : 0.0;
            case "=": return equal(args.get(0), args.get(1)) ? 1.0 : 0.0;
            case "list.new": return new ArrayList<>(args);
            case "list.at": {
                if (!(args.get(0) instanceof List<?> list) || list.isEmpty()) return null;
                int i = (int) Math.max(0, Math.min(list.size() - 1, d(args.get(1))));
                return list.get(i);
            }
            default: {
                Fn fn = functions.get(op);
                if (fn == null) return null;
                Map<String, Object> local = new HashMap<>(env);
                for (int i = 0; i < fn.params.size() && i < args.size(); i++) local.put(fn.params.get(i), args.get(i));
                return eval(fn.body, local);
            }
        }
    }

    private static boolean equal(Object a, Object b) {
        if (a instanceof Double x && b instanceof Double y) return x.doubleValue() == y.doubleValue();
        return String.valueOf(a).equals(String.valueOf(b));
    }

    private static boolean truthy(Object o) {
        return o instanceof Double d ? d != 0 : o != null;
    }

    private static double d(Object o) {
        if (o instanceof Double v) return v;
        if (o instanceof String s) { try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; } }
        return 0;
    }

    static String format(Object o) {
        if (o instanceof Double v) {
            if (v == Math.rint(v)) return String.valueOf((long) v.doubleValue());
            return String.valueOf(Math.round(v * 100) / 100.0);
        }
        return String.valueOf(o);
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    private record Str(String v) {}

    private static Object parse(String src) {
        int[] pos = { 0 };
        return read(src, pos);
    }

    private static Object read(String s, int[] p) {
        skip(s, p);
        char c = s.charAt(p[0]);
        if (c == '(') {
            p[0]++;
            List<Object> list = new ArrayList<>();
            while (true) {
                skip(s, p);
                if (s.charAt(p[0]) == ')') { p[0]++; return list; }
                list.add(read(s, p));
            }
        }
        if (c == '"') {
            int start = ++p[0];
            while (s.charAt(p[0]) != '"') p[0]++;
            return new Str(s.substring(start, p[0]++));
        }
        int start = p[0];
        while (p[0] < s.length() && !Character.isWhitespace(s.charAt(p[0])) && s.charAt(p[0]) != '(' && s.charAt(p[0]) != ')') p[0]++;
        String tok = s.substring(start, p[0]);
        try { return Double.parseDouble(tok); } catch (NumberFormatException e) { return tok; }
    }

    private static void skip(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++;
    }
}
