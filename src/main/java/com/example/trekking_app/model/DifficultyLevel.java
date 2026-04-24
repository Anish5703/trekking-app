package com.example.trekking_app.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
        Physical difficulty level of a trek route.
        Use this to filter routes or display a difficulty badge in the UI.
        
        EASY     -> suitable for beginners, flat terrain, short distance
        MODERATE -> some elevation gain, moderate fitness required
        HARD     -> steep trails, longer distance, prior trekking experience needed
        EXPERT   -> high altitude or technical terrain, experienced trekkers only
        """,
        enumAsRef = true
)
public enum DifficultyLevel {
    EASY,
    MODERATE,
    HARD,
    EXPERT
}
