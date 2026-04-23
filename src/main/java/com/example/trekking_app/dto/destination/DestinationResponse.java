package com.example.trekking_app.dto.destination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DestinationResponse {

    private int id;
    private String name;
    private String district;
    private String region;
}
