package com.example.trekking_app.service.image;

import com.example.trekking_app.dto.ImageUploadResult;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.CloudinaryFolders;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.ImageRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final CloudinaryService cloudinaryService;
    private final ImageRepository imageRepo;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    public List<String> uploadImage(@NonNull MultipartFile[] files, @NonNull EntityType entityType, @NonNull Integer entityId) {
        List<String> imageUrls = new ArrayList<>();
        String folder = CloudinaryFolders.resolvePath(entityType, entityId);
        for (MultipartFile file : files) {
            if (!isValidImageExtension(file))
            {
                log.warn("Skipping file {} — unsupported extension", file.getOriginalFilename());
                continue;
            }


                String originalName = file.getOriginalFilename().split(".")[0];
                boolean exists = imageRepo.existsByOriginalNameAndEntityTypeAndEntityId(originalName, entityType, entityId);
                if (exists)
                {
                    log.info("Skipping {} — already uploaded for {} id={}", originalName, entityType, entityId);
                    continue;
                }

                try
                {
                    ImageUploadResult result = cloudinaryService.uploadImage(file,folder);
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

                } catch (Exception e)
                {
                    log.error("Upload failed for file={} entity={} id={} : {}", originalName, entityType, entityId, e.getMessage());
                    throw new FileParsingFailedException("failed to upload image for "+entityType+" "+entityId);
                }

        }
        return imageUrls;
    }

    private boolean isValidImageExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return false;
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }


}