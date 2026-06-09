package com.example.trekking_app.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.trekking_app.dto.image.ImageUploadResult;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService
{

    private final Cloudinary cloudinary;

    @Transactional
    public ImageUploadResult uploadImage(MultipartFile file, String folder)
    {
        try{
            byte[] bytes = file.getBytes();
            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            ));
            return  ImageUploadResult.builder().
                    secureUrl((String) result.get("secure_url")).
                    publicId((String) result.get("public_id")).
                    build();
        }
        catch (IOException e)
        {
            throw new FileParsingFailedException("failed to upload image");
        }
    }
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Image deletion failed", e);
        }
    }
}
