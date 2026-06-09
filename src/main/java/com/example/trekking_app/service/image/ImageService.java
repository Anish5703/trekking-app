package com.example.trekking_app.service.image;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.image.ImageResponse;
import com.example.trekking_app.dto.image.ImageUploadResult;
import com.example.trekking_app.dto.image.ImageUploadSummary;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.mapper.ImageMapper;
import com.example.trekking_app.model.CloudinaryFolders;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.ImageRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ImageService {

    private final CloudinaryService cloudinaryService;
    private final ImageRepository imageRepo;
    private final ImageMapper imageMapper = new ImageMapper();
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private final Executor heavyTaskExecutor;

    public ImageService(CloudinaryService cloudinaryService,
                        ImageRepository imageRepo,
                        @Qualifier("heavyTaskExecutor") Executor heavyTaskExecutor) {
        this.cloudinaryService = cloudinaryService;
        this.imageRepo = imageRepo;
        this.heavyTaskExecutor = heavyTaskExecutor;
    }

    public ApiResponse<ImageUploadSummary> uploadImages(@NonNull List<MultipartFile> files,
                                                       @NonNull EntityType entityType,
                                                       @NonNull Integer entityId) {
        String folder = CloudinaryFolders.resolvePath(entityType, entityId);
        Set<String> existingNames = imageRepo.findOriginalNamesByEntityTypeAndEntityId(entityType, entityId);

        List<String> skipped = new CopyOnWriteArrayList<>();
        List<String> failed = new CopyOnWriteArrayList<>();

        List<MultipartFile> validFiles = files.stream()
                .filter(f -> {
                    String filename = f.getOriginalFilename();
                    if (!isValidImageExtension(f)) {
                        skipped.add(filename + " (unsupported extension)");
                        return false;
                    }
                    if (filename == null || filename.isBlank()) {
                        skipped.add("unknown (null filename)");
                        return false;
                    }
                    String originalName = filename.contains(".")
                            ? filename.substring(0, filename.lastIndexOf('.'))
                            : filename;
                    if (existingNames.contains(originalName)) {
                        skipped.add(originalName + " (already exists)");
                        return false;
                    }
                    return true;
                })
                .toList();

        if (validFiles.isEmpty()) {
            return new ApiResponse<>(new ImageUploadSummary(List.of(), skipped, failed), "no new images to upload", 200);
        }

        AtomicBoolean firstImage = new AtomicBoolean(true);

        List<CompletableFuture<Image>> futures = validFiles.stream()
                .map(file -> {
                    String originalName = file.getOriginalFilename().contains(".")
                            ? file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.'))
                            : file.getOriginalFilename();

                    return CompletableFuture.supplyAsync(() -> {
                        ImageUploadResult result = cloudinaryService.uploadImage(file, folder);
                        return Image.builder()
                                .url(result.getSecureUrl())
                                .publicId(result.getPublicId())
                                .originalName(originalName)
                                .entityType(entityType)
                                .entityId(entityId)
                                .isPrimary(firstImage.getAndSet(false))
                                .build();
                    }, heavyTaskExecutor).exceptionally(e -> {
                        log.error("Upload failed file={} entity={} id={}: {}", originalName, entityType, entityId, e.getMessage());
                        failed.add(originalName + " (upload failed)");
                        return null;
                    });
                })
                .toList();

        List<Image> imagesToSave = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        if (!imagesToSave.isEmpty()) {
            imageRepo.saveAll(imagesToSave);
        }

        List<String> uploaded = imagesToSave.stream().map(Image::getOriginalName).toList();
        return new ApiResponse<>(new ImageUploadSummary(uploaded, skipped, failed), "upload complete", 201);
    }

    @Deprecated(since = "version 2.0")
    @Transactional
    public ApiResponse<Void> uploadImage(@NonNull List<MultipartFile> files, @NonNull EntityType entityType, @NonNull Integer entityId) {
        List<String> imageUrls = new ArrayList<>();
        String folder = CloudinaryFolders.resolvePath(entityType, entityId);
        for (MultipartFile file : files) {
            if (!isValidImageExtension(file)) {
                log.warn("Skipping file {} — unsupported extension", file.getOriginalFilename());
                continue;
            }


            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank())
            {
                log.warn("Skipping file — null or blank filename");
                continue;
            }
            String originalName = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
            boolean exists = imageRepo.existsByOriginalNameAndEntityTypeAndEntityId(originalName, entityType, entityId);
            if (exists) {
                log.info("Skipping {} — already uploaded for {} id={}", originalName, entityType, entityId);
                continue;
            }

            try {
                ImageUploadResult result = cloudinaryService.uploadImage(file, folder);
                Image image = Image.builder()
                        .url(result.getSecureUrl())
                        .publicId(result.getPublicId())
                        .originalName(originalName)
                        .entityType(entityType)
                        .entityId(entityId)
                        .isPrimary(imageUrls.isEmpty()) // first image is primary
                        .build();

                imageRepo.save(image);
                imageUrls.add(result.getSecureUrl());


            } catch (Exception e) {
                log.error("Upload failed for file={} entity={} id={} : {}", originalName, entityType, entityId, e.getMessage());
                throw new FileParsingFailedException("failed to upload image for " + entityType + " " + entityId);
            }

        }
        return new ApiResponse<>(null, "images uploaded", 201);

    }

    private boolean isValidImageExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return false;
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }
    @Transactional
    public ApiResponse<Void> deleteImage(@NonNull Integer imageId) {
        Image image = imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

        // Delete from Cloudinary first
        cloudinaryService.deleteImage(image.getPublicId());

        imageRepo.delete(image);


        return new ApiResponse<>(null, "image deleted", 200);
    }


    public ApiResponse<List<ImageResponse>> getImages(EntityType entityType, @NonNull Integer entityId) {
        List<ImageResponse> imageResponses = imageRepo.findByEntityTypeAndEntityId(entityType, entityId).stream().map(imageMapper::toImageResponse).toList();
        if (imageResponses.isEmpty()) throw new NoResourceFoundException("images");

        return new ApiResponse<>(imageResponses, "images fetched", 200);

    }
}