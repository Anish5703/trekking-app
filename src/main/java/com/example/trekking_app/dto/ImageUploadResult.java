package com.example.trekking_app.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ImageUploadResult {

    private String secureUrl;
    private String publicId;
}
