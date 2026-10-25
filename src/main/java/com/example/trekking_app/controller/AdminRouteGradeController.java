package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.grade.*;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.route.RouteGradeBackfillService;
import com.example.trekking_app.service.route.RouteGradeUpdater;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/route-grading")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Route Grading (admin)")
public class AdminRouteGradeController {

    private final RouteGradeBackfillService backfillService;
    private final RouteGradeUpdater gradeUpdater;


    @PostMapping("/backfill")
    @Operation(summary = "Start an async backfill. 202 if started, 409 if one is already running.")
    public ResponseEntity<ApiResponse<BackfillStatus>> handleStartBackfill(
            @RequestParam(defaultValue = "true") boolean onlyUngraded,
            @AuthenticationPrincipal UserPrincipal user) {
        log.info("Grade backfill requested by user {} (onlyUngraded={})", user.getId(), onlyUngraded);
        ApiResponse<BackfillStatus> response = backfillService.start(onlyUngraded);
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/backfill/status")
    public ResponseEntity<ApiResponse<BackfillStatus>> handleBackfillStatus() {
        ApiResponse<BackfillStatus> response = backfillService.currentStatus();
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/ungraded/count")
    @Operation(summary = "Routes with no v2 grade yet (missing data or not backfilled)")
    public ResponseEntity<ApiResponse<Long>> handleUngradedCount() {
        ApiResponse<Long> response = backfillService.ungradedCount();
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{routeId}/grade")
    @Operation(summary = "Recompute and save the grade. 409 if locked, 422 if distance/altitude/elevation data is missing.")
    public ResponseEntity<ApiResponse<GradeResult>> handleRegradeRoute(@PathVariable Integer routeId,
                                                                       @AuthenticationPrincipal UserPrincipal user) {
        log.info("Route {} regrade requested by user {}", routeId, user.getId());
        ApiResponse<GradeResult> response = gradeUpdater.regradeRoute(routeId);
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{routeId}/terrain")
    @Operation(summary = "Save terrain flags, then regrade if possible",
            description = "Terrain is always saved. If other inputs are missing, 'grade' is null and 'missing_inputs' lists them. 409 if locked.")
    public ResponseEntity<ApiResponse<TerrainUpdateResponse>> handleUpdateTerrain(@PathVariable Integer routeId,
                                                                                  @Valid @RequestBody TerrainProfile terrain,
                                                                                  @AuthenticationPrincipal UserPrincipal user) {
        log.info("Route {} terrain update requested by user {}", routeId, user.getId());
        ApiResponse<TerrainUpdateResponse> response = gradeUpdater.updateTerrain(routeId, terrain);
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/grade/preview")
    @Operation(summary = "Preview a grade from raw inputs. Nothing is saved.")
    public ResponseEntity<ApiResponse<GradeResult>> handlePreviewGrade(@Valid @RequestBody RouteInput input) {
        ApiResponse<GradeResult> response = gradeUpdater.preview(input);
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}