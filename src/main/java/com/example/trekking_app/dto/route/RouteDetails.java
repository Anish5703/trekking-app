package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.RouteCategory;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteDetails {
    private Integer id;
    private String name;
    private String createdBy;
    private int destinationId;
    private RouteCategory category;
    private DifficultyLevel difficultyLevel;
    //private Integer estimatedDays;
    private List<Integer> approxDays;
    private List<Integer> approxHours;
    private List<String> importantKeywords;
    private List<String> includedRegions;
    private Double maxElevation;
    private Double endLongitude;
    private Double endLatitude;
    private Double totalDistanceInKm;
    private List<String> imageUrls;
    private Boolean isPublished;
    private Boolean isLocked;
    private VerificationStatus verificationStatus;
    private String verifiedBy;
    private String verificationRemarks;

}
