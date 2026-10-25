package com.example.trekking_app.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Physical difficulty of a route, computed from distance, climbing, steepness, altitude and terrain.
        EASY | MODERATE | HARD | VERY_HARD | EXPERT""", enumAsRef = true)
public enum DifficultyLevel {
    EASY, MODERATE, HARD, VERY_HARD, EXPERT;

    public int level() { return ordinal() + 1; }

    public String label() {
        StringBuilder b = new StringBuilder();
        for (String w : name().toLowerCase().split("_"))
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }

    public static DifficultyLevel max(DifficultyLevel a, DifficultyLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}