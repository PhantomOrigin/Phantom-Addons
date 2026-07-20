package com.phantomaddons.features.supplies.nopre;

import com.phantomaddons.features.supplies.pearlwaypoints.PearlLocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NoPre {

    private static final Pattern NO_PRE_PATTERN =
            Pattern.compile("No\\s+(Shop|Triangle|Slash|Equals|X Cannon|X)\\s*!", Pattern.CASE_INSENSITIVE);

    private static final Pattern COORD_PATTERN =
            Pattern.compile("(Shop|Triangle|Slash|Equals|X Cannon|Square|X)\\s+x:\\s*([\\-\\d.]+),\\s*y:\\s*([\\-\\d.]+),\\s*z:\\s*([\\-\\d.]+)",
                    Pattern.CASE_INSENSITIVE);

    private static volatile PearlLocation missingLocation   = null;
    private static volatile double[]      doubleCoords      = null; // [x, y, z]

    private NoPre() {}

    public static void reset() {
        missingLocation = null;
        doubleCoords    = null;
    }

    public static PearlLocation getMissingLocation() { return missingLocation; }
    public static double[]      getDoubleCoords()    { return doubleCoords; }

    public static boolean onChat(String message) {
        Matcher noPre = NO_PRE_PATTERN.matcher(message);
        if (noPre.find()) {
            String name = noPre.group(1).trim();
            missingLocation = nameToLocation(name);
            return true;
        }
        
        Matcher coord = COORD_PATTERN.matcher(message);
        if (coord.find()) {
            try {
                double x = Double.parseDouble(coord.group(2));
                double y = Double.parseDouble(coord.group(3));
                double z = Double.parseDouble(coord.group(4));
                doubleCoords = new double[]{x, y, z};
            } catch (NumberFormatException ignored) {}
            return true;
        }

        return false;
    }

    private static PearlLocation nameToLocation(String name) {
        return switch (name.toLowerCase()) {
            case "shop"     -> PearlLocation.SHOP;
            case "triangle" -> PearlLocation.TRIANGLE;
            case "slash"    -> PearlLocation.SLASH;
            case "equals"   -> PearlLocation.EQUALS;
            case "x cannon" -> PearlLocation.X_CANNON;
            case "x"        -> PearlLocation.X;
            default         -> null;
        };
    }
}