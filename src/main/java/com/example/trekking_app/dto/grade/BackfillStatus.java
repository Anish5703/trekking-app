package com.example.trekking_app.dto.grade;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BackfillStatus(String state, int total, int graded, int needsReview, int failed,
                             Instant startedAt, Instant finishedAt) {

    public static BackfillStatus idle() { return new BackfillStatus("IDLE", 0, 0, 0, 0, null, null); }
    public boolean running() { return "RUNNING".equals(state); }
}