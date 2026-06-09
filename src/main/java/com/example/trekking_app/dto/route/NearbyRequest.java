package com.example.trekking_app.dto.route;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NearbyRequest {

    @NonNull
    private Double longitude;
    @NonNull
    private Double latitude;
    @Builder.Default
    private Double radiusMeters = 5000.0;
    @Builder.Default
    private Integer limit = 20;

}
