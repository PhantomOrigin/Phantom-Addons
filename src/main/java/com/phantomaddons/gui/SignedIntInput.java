package com.phantomaddons.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.TextEditing.*;

public class SignedIntInput extends IntInput {
    public SignedIntInput(String n, Supplier<Integer> get, Consumer<Integer> set) {
        super(n, get, set);
        fieldW = 64;
    }
    @Override void onChar(char c) {
        if (!focused) return;
        if (c == '-' && (draft == null || draft.isEmpty())) { draft = "-"; cursorPos = 1; }
        else if (Character.isDigit(c)) {
            if (draft == null) draft = "";
            draft = insertAt(draft, cursorPos, c);
            cursorPos++;
        }
    }
    @Override void commit() {
        if (draft != null && !draft.isEmpty() && !draft.equals("-")) {
            try { set.accept(Integer.parseInt(draft)); } catch (NumberFormatException ignored) {}
        }
        draft = null;
    }
}
