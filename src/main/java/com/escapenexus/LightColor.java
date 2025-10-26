package com.escapenexus;

public enum LightColor {
    RED, GREEN, BLUE, YELLOW, WHITE;

    public static LightColor fromIndex(int index) {
        return values()[Math.floorMod(index, values().length)];
    }

    public static LightColor fromString(String value) {
        return LightColor.valueOf(value.trim().toUpperCase());
    }
}
