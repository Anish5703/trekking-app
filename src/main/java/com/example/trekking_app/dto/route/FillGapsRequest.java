package com.example.trekking_app.dto.route;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FillGapsRequest {
    private List<Integer> precedingTpIds; // null/empty = fill all detected gaps
    private Double thresholdMeters;       // optional, default 200
}