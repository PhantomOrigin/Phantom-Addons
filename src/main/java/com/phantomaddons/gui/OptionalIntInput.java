package com.phantomaddons.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class OptionalIntInput extends IntInput {
    public OptionalIntInput(String n, Supplier<Integer> get, Consumer<Integer> set) {
        super(n, get, set);
    }
    @Override String displayValue() {
        if (draft != null) return draft;
        int val = get.get();
        return val < 0 ? "" : String.valueOf(val);
    }
    @Override void commit() {
        if (draft == null) return;
        if (draft.isEmpty()) set.accept(-1);
        else { try { set.accept(Integer.parseInt(draft)); } catch (NumberFormatException ignored) {} }
        draft = null;
    }
}
