package com.example.trekking_app.service.image;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.image.ImageUploadResult;
import com.example.trekking_app.dto.image.ImageUploadSummary;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ImageRepository imageRepository;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        Executor executor = new SyncTaskExecutor();

        imageService = new ImageService(cloudinaryService, imageRepository, executor);
    }

    @Test
    void testUploadImages_Success() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy image".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of());

        ImageUploadResult result = ImageUploadResult.builder()
                .secureUrl("https://cloudinary.com/everest.jpg")
                .publicId("routes/1/everest")
                .build();

        when(cloudinaryService.uploadImage(any(), anyString()))
                .thenReturn(result);

        ApiResponse<ImageUploadSummary> response =
                imageService.uploadImages(
                        List.of(file),
                        EntityType.ROUTE,
                        1
                );

        assertEquals(201, response.getStatus());

        ImageUploadSummary summary = response.getData();

        assertEquals(1, summary.getUploaded().size());
        assertEquals("everest", summary.getUploaded().get(0));
        assertTrue(summary.getSkipped().isEmpty());
        assertTrue(summary.getFailed().isEmpty());

        verify(cloudinaryService).uploadImage(any(), anyString());
        verify(imageRepository).saveAll(anyList());
    }

    @Test
    void testUploadImages_InvalidExtension() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "notes.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of());

        ApiResponse<ImageUploadSummary> response =
                imageService.uploadImages(
                        List.of(file),
                        EntityType.ROUTE,
                        1
                );

        assertEquals(200, response.getStatus());

        ImageUploadSummary summary = response.getData();

        assertTrue(summary.getUploaded().isEmpty());
        assertEquals(1, summary.getSkipped().size());
        assertTrue(summary.getFailed().isEmpty());

        verify(cloudinaryService, never())
                .uploadImage(any(), anyString());

        verify(imageRepository, never())
                .saveAll(anyList());
    }

    @Test
    void testUploadImages_ImageAlreadyExists() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of("everest"));

        ApiResponse<ImageUploadSummary> response =
                imageService.uploadImages(
                        List.of(file),
                        EntityType.ROUTE,
                        1
                );

        assertEquals(200, response.getStatus());

        ImageUploadSummary summary = response.getData();

        assertTrue(summary.getUploaded().isEmpty());
        assertEquals(1, summary.getSkipped().size());
        assertTrue(summary.getFailed().isEmpty());

        verify(cloudinaryService, never())
                .uploadImage(any(), anyString());

        verify(imageRepository, never())
                .saveAll(anyList());
    }

    @Test
    void testUploadImages_MultipleImages_Success() {

        MultipartFile file1 = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        MultipartFile file2 = new MockMultipartFile(
                "image",
                "annapurna.png",
                "image/png",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of());

        when(cloudinaryService.uploadImage(any(), anyString()))
                .thenReturn(
                        ImageUploadResult.builder()
                                .secureUrl("url")
                                .publicId("publicId")
                                .build()
                );

        ApiResponse<ImageUploadSummary> response =
                imageService.uploadImages(
                        List.of(file1, file2),
                        EntityType.ROUTE,
                        1
                );

        ImageUploadSummary summary = response.getData();

        assertEquals(2, summary.getUploaded().size());

        verify(cloudinaryService, times(2))
                .uploadImage(any(), anyString());

        verify(imageRepository).saveAll(anyList());
    }

    @Test
    void testUploadImages_UploadFails() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of());

        when(cloudinaryService.uploadImage(any(), anyString()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThrows(
                FileParsingFailedException.class,
                () -> imageService.uploadImages(
                        List.of(file),
                        EntityType.ROUTE,
                        1
                )
        );

        verify(imageRepository, never())
                .saveAll(anyList());
    }

    @Test
    void testUploadImages_FirstImageMarkedAsPrimary() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of());

        when(cloudinaryService.uploadImage(any(), anyString()))
                .thenReturn(
                        ImageUploadResult.builder()
                                .secureUrl("url")
                                .publicId("publicId")
                                .build()
                );

        imageService.uploadImages(
                List.of(file),
                EntityType.ROUTE,
                1
        );

        ArgumentCaptor<List<Image>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(imageRepository).saveAll(captor.capture());

        List<Image> images = captor.getValue();

        assertEquals(1, images.size());

        // Change this getter if your Image entity uses a different one.
        assertTrue(images.get(0).getIsPrimary());
    }

    @Test
    void testUploadImages_NotFirstUpload_NotPrimary() {

        MultipartFile file = new MockMultipartFile(
                "image",
                "everest.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        when(imageRepository.findOriginalNamesByEntityTypeAndEntityId(
                EntityType.ROUTE,
                1
        )).thenReturn(Set.of("existing"));

        when(cloudinaryService.uploadImage(any(), anyString()))
                .thenReturn(
                        ImageUploadResult.builder()
                                .secureUrl("url")
                                .publicId("publicId")
                                .build()
                );

        imageService.uploadImages(
                List.of(file),
                EntityType.ROUTE,
                1
        );

        ArgumentCaptor<List<Image>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(imageRepository).saveAll(captor.capture());

        List<Image> images = captor.getValue();

        assertFalse(images.get(0).getIsPrimary());
    }
}