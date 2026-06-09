package com.example.trekking_app.dto.image;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ImageUploadResult {

    private String secureUrl;
    private String publicId;
}
