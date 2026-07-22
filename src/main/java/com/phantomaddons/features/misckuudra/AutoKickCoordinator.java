package com.phantomaddons.features.misckuudra;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AutoKickCoordinator {

    private static final Set<String> claimed = Collections.synchronizedSet(new HashSet<>());

    private AutoKickCoordinator() {}

    public static boolean tryClaim(String name) {
        return claimed.add(name.toLowerCase());
    }

    public static void reset() {
        claimed.clear();
    }
}
