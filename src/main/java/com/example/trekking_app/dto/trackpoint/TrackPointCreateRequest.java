package com.example.trekking_app.dto.trackpoint;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TrackPointCreateRequest {

    @NonNull
    private Double latitude;
    @NonNull
    private Double longitude;
    private Integer precedingTpId;

}
