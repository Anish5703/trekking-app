package com.example.trekking_app.dto.destination;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DestinationRequest {

    @NotBlank(message = "destination name is required")
    @Size(min = 3 , max = 100)
    private String name;

    @NotBlank(message = "destination district is required")
    @Size(min = 3 , max = 100)
    private String district;

    @NotBlank(message = "destination region is required")
    @Size(min = 3 , max = 100)
    private String region;
}
