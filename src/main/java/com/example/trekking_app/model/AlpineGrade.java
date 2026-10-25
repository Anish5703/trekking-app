package com.example.trekking_app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum AlpineGrade {
    F("F"), PD("PD"), PD_PLUS("PD+"), AD("AD"), D("D"), TD("TD");

    private final String label;
    AlpineGrade(String label) { this.label = label; }

    @JsonValue public String label() { return label; }

    @JsonCreator
    public static AlpineGrade of(String v) {
        return Arrays.stream(values()).filter(a -> a.label.equals(v)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid alpine grade: " + v));
    }
}