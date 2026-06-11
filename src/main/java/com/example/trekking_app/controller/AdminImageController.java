package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.image.ImageResponse;
import com.example.trekking_app.dto.image.ImageUploadSummary;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/image")
@RequiredArgsConstructor
public class AdminImageController
{
    private final ImageService imageService;

    @PostMapping(value = "/upload" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadSummary>> handleUploadImage(@RequestParam EntityType entityType,
                                                                                   @RequestParam Integer entityId,
                                               @RequestPart("images") List<MultipartFile> images)
    {
       ApiResponse<ImageUploadSummary> response = imageService.uploadImages(images,entityType,entityId);
       return ResponseEntity.status(201).headers(buildRequestHeaders()).body(response);
    }
    @GetMapping()
    public ResponseEntity<ApiResponse<List<ImageResponse>>> handleGetUploadedImage(@RequestParam EntityType entityType,
                                                                                          @RequestParam Integer entityId)
    {
        ApiResponse<List<ImageResponse>> response = imageService.getImages(entityType,entityId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteImage(@PathVariable Integer imageId)
    {
        ApiResponse<Void> response = imageService.deleteImage(imageId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
