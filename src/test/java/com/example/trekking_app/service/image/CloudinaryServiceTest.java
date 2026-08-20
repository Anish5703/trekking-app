package com.example.trekking_app.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.trekking_app.dto.image.ImageUploadResult;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;
    @InjectMocks
    private  CloudinaryService cloudinaryService;

    @Test
    void testUploadImage_Success() throws IOException {
        MultipartFile file = new MockMultipartFile("image","test.jpeg","image/jpeg","mock image".getBytes());

        Map<String,Object> response = new HashMap<>();
        response.put("secure_url","https://cloudinary.com/image.jpeg");
        response.put("public_id","trek/img1");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class),anyMap())).thenReturn(response);

        ImageUploadResult result = cloudinaryService.uploadImage(file,"trek");

        assertEquals("https://cloudinary.com/image.jpeg",result.getSecureUrl());
        assertEquals("trek/img1",result.getPublicId());

    }

    @Test
    void testUploadImage_FailsToParseFile() throws Exception {
        MultipartFile file = new MockMultipartFile("image","test.jpeg","image/jpeg","mock image".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload((any(byte[].class)),anyMap())).thenThrow(new IOException("failed to parse"));

        assertThrows(FileParsingFailedException.class,()->cloudinaryService.uploadImage(file,"trek"));
    }

    @Test
    void testDeleteImage_Success()  throws  Exception{

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(),anyMap())).thenReturn(Map.of());

        cloudinaryService.deleteImage("trek");

        verify(uploader).destroy(eq("trek"),anyMap());
    }

    @Test
    void testDeleteImage_DeletionFailed() throws Exception{
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(),anyMap())).thenThrow(new IOException("failed to delete"));


        assertThrows(RuntimeException.class,()->cloudinaryService.deleteImage("trek"));

    }

}
