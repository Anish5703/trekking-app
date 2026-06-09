package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.image.ImageResponse;
import com.example.trekking_app.entity.Image;

public class ImageMapper {

    public ImageResponse toImageResponse(Image image)
    {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }
}
