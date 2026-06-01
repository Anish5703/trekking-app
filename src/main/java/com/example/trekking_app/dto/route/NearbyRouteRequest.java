package com.example.trekking_app.dto.route;

import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NearbyRouteRequest {

    @NonNull
    private Double longitude;
    @NonNull
    private Double latitude;
    @Builder.Default
    private Double radiusMeters = 5000.0;
    private Integer limit = 20;

}
