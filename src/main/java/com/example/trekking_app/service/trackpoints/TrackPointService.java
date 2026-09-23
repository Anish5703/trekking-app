package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GapPreviewDto;
import com.example.trekking_app.dto.route.FillGapsRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointCreateRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.model.*;
import com.example.trekking_app.dto.trackpoint.TrackPointBatchRequest;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
import com.example.trekking_app.service.trailSegment.TrailSegmentService;
import com.example.trekking_app.service.xlsx.XlsxParserHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.locationtech.jts.geom.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackPointService {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;
    private final TrailSegmentService trailSegmentService;
    private final XlsxParserHelper trailSegmentHelper;
    private final TrailSegmentRepository trailSegmentRepo;
    private final GpxSegmentRepository gpxSegmentRepo;
    private final TrackPointMapper trackPointMapper = new TrackPointMapper();
    private final GpxMergeService gpxMergeService;
    private final GeoJsonMapper geoJsonMapper;
    private final GeometryFactory GF = new GeometryFactory(new PrecisionModel(),4326);



    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getAllTrackPoints(@NonNull Integer routeId ,
                                                                   @NonNull Integer page ,
                                                                   @NonNull Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page, size);
        Page<TrackPointResponse> trackPointResponses = trackPointRepo.findByRoute_IdOrderByGlobalSequenceAsc(routeId,pageable).map(trackPointMapper::toTrackPointResponse);
        String message = trackPointResponses.isEmpty()
                ? "no trackpoints found for route"
                : "trackpoints fetched";

        return new ApiResponse<>(trackPointResponses, message, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TrackPointResponse>> getTrackPointByLatAndLong(@NonNull Integer routeId , @NonNull Double lat, @NonNull Double lon)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        List<TrackPointResponse> tps = trackPointRepo.findByRoute_IdAndLatitudeAndLongitude(route.getId(),lat,lon).get().stream().map(trackPointMapper::toTrackPointResponse).toList();
        if(tps.isEmpty()) throw new ResourceNotFoundException(String.format("trackpoint not found with lat %f and lon %f",lat,lon));
        return new ApiResponse<>(tps,"trackpoint fetched",200);
    }


    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getActiveTrackPoints(@NonNull Integer routeId,
                                                                      Integer page ,
                                                                       Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<TrackPointResponse> activeTrackPoints = trackPointRepo.findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(routeId,pageable)
                .map(trackPointMapper::toTrackPointResponse);
        String message = activeTrackPoints.isEmpty()
                ? "no active trackpoints found for route"
                : "active trackpoints fetched";
        return new ApiResponse<>(activeTrackPoints, message, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getInactiveTrackPoints(@NonNull Integer routeId ,
                                                                        Integer page , Integer size)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<TrackPointResponse> inactiveTrackPoints = trackPointRepo.findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(routeId,pageable)
                .map(trackPointMapper::toTrackPointResponse);
        String message = inactiveTrackPoints.isEmpty()
                ? "no deleted trackpoints found  "
                : "deleted trackpoints fetched";
        return new ApiResponse<>(inactiveTrackPoints,message,200);
    }

    @Deprecated(since = "version 2.0")
    public ApiResponse<TrackPointResponse> updateTrackPoint_V1(@NonNull Integer routeId , @NonNull Integer trackPointId , @NonNull TrackPointRequest trackPointRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        TrackPoint trackPoint = trackPointRepo.findByIdAndRoute_Id(trackPointId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
        );
        if(route.getRouteStatus().equals(RouteStatus.MERGING)) throw new ResourceUpdateFailedException("failed to update trackpoint since the associated routes trackpoints are merging");
        try {
            trackPoint = trackPointMapper.toUpdateTrackPoint(trackPoint,trackPointRequest);
            trackPointRepo.save(trackPoint);
            TrackPointResponse tpResponse = trackPointMapper.toTrackPointResponse(trackPoint);
             gpxMergeService.mergeTrackPoints(routeId);
            return new ApiResponse<>(tpResponse, "trackpoint updated", 200);
        }
        catch (Exception e)
        {
            log.error("Trackpoint update failed : {}",e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("trackpoint","id",trackPointId);
        }

    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<TrackPointResponse> updateTrackPoint(@NonNull Integer routeId,
                                                            @NonNull Integer trackPointId,
                                                            @NonNull TrackPointRequest trackPointRequest) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        if(route.getIsLocked()==true) throw new ResourceUpdateFailedException("route is locked by SUPERADMIN ! No update allowed");
        TrackPoint trackPoint = trackPointRepo.findByIdAndRoute_Id(trackPointId, routeId)
                .orElseThrow(() -> new ResourceNotFoundException("trackpoint", "id", trackPointId));

        if (route.getRouteStatus().equals(RouteStatus.MERGING))
            throw new ResourceUpdateFailedException(
                    "failed to update trackpoint since the associated route's trackpoints are merging");

        try {
            // Step 1 — resolve affected segments BEFORE coords/sequence change
            List<SegmentRefreshContext> contexts = trailSegmentService.resolveAffectedSegments(routeId, trackPoint);

            // Step 2 — update trackpoint and reassign global sequences
            trackPoint = trackPointMapper.toUpdateTrackPoint(trackPoint, trackPointRequest);
            trackPointRepo.save(trackPoint);
            TrackPointResponse tpResponse = trackPointMapper.toTrackPointResponse(trackPoint);
            gpxMergeService.mergeTrackPoints(routeId);

            // Step 3 — refresh affected segments using pre-resolved trackpoint IDs
            if (!contexts.isEmpty()) {
                List<TrailSegment> toSave = new ArrayList<>();
                for (SegmentRefreshContext ctx : contexts) {
                    // re-fetch by ID to get updated globalSequence post-merge
                    TrackPoint startTp = trackPointRepo.findById(ctx.startTp().getId()).orElse(ctx.startTp());
                    TrackPoint endTp = trackPointRepo.findById(ctx.endTp().getId()).orElse(ctx.endTp());

                    LineString newPath = trailSegmentHelper.generateTrailSegmentPath(startTp, endTp, route);
                    ctx.segment().setPath(newPath);
                    toSave.add(ctx.segment());
                }
                trailSegmentRepo.saveAll(toSave);
                log.info("Refreshed {} trail segments after trackpoint {} update", toSave.size(), trackPointId);
            }

            return new ApiResponse<>(tpResponse, "trackpoint updated", 200);

        } catch (Exception e) {
            log.error("Trackpoint update failed : {}", e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("trackpoint", "id", trackPointId);
        }

    }




    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> recoverTrackPoint(@NonNull Integer routeId, @NonNull Integer trackPointId)
    {
        TrackPoint tp = trackPointRepo.findByIdAndRoute_Id(trackPointId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trackpoint","route id and trackpoint id",String.format("%d and %d respectively",routeId,trackPointId))
        );
        if(!tp.getIsDeleted() || !tp.getStatus().equals(TrackPointStatus.SOFT_DELETED))
            throw new ResourceUpdateFailedException("trackpoint recovery failed ! it was never deleted");
        tp.setStatus(TrackPointStatus.RECOVERED);
        tp.setIsDeleted(false);
        trackPointRepo.save(tp);
        return new ApiResponse<>(null,"trackpoint recovered",200);

    }
    @CacheEvict(value = "route-geoJson" ,key="#routeId")
    @Transactional
    public ApiResponse<Void> recoverTrackPoints(
            @NonNull Integer routeId,
            @NonNull TrackPointBatchRequest tpIds) {

        List<TrackPoint> trackPoints =
                trackPointRepo.findAllByIdInAndRoute_IdAndIsDeletedTrueAndStatus(
                        tpIds.getTrackPointIds(),
                        routeId,
                        TrackPointStatus.SOFT_DELETED
                );

        if (trackPoints.size() != tpIds.getTrackPointIds().size()) {
            throw new ResourceUpdateFailedException(
                    "One or more track points were not found, do not belong to the specified route, or are not soft deleted."
            );
        }

        trackPoints.forEach(tp -> {
            tp.setIsDeleted(false);
            tp.setStatus(TrackPointStatus.RECOVERED);
        });

        trackPointRepo.saveAll(trackPoints);

        return new ApiResponse<>(null, "Track points recovered successfully.", 200);
    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> deleteTrackPoint(Integer routeId, Integer trackPointId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        if(route.getIsLocked()==true) throw new ResourceUpdateFailedException("route is locked by SUPERADMIN ! No update allowed");
        TrackPoint trackPoint = trackPointRepo.findById(trackPointId).orElseThrow(
                () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
        );
        try{
            trackPoint.setIsDeleted(true);
            trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);
            trackPointRepo.save(trackPoint);
            gpxMergeService.mergeTrackPoints(route.getId());
            return new ApiResponse<>(null, "trackpoint deleted", 200);
        } catch (Exception e) {
            throw new ResourceDeletionFailedException("trackpoint", "id", trackPointId);
        }
    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> deleteListOfTrackPoint(@NonNull Integer routeId, @NonNull TrackPointBatchRequest req)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        if(route.getIsLocked()==true) throw new ResourceUpdateFailedException("route is locked by SUPERADMIN ! No update allowed");
        if(req.getTrackPointIds().isEmpty()) throw new ResourceDeletionFailedException("no trackpoints selected for deletion");
        List<TrackPoint> tps = new ArrayList<>();
        for (Integer trackPointId : req.getTrackPointIds())
        {
            TrackPoint trackPoint = trackPointRepo.findById(trackPointId).orElseThrow(
                    () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
            );
            trackPoint.setIsDeleted(true);
            trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);
            tps.add(trackPoint);
        }
        try
        {
            trackPointRepo.saveAll(tps);
            gpxMergeService.mergeTrackPoints(route.getId());
            return new ApiResponse<>(null, req.getTrackPointIds().size()+" trackpoints deleted", 200);
        }
        catch (Exception e) {
            throw new ResourceDeletionFailedException("failed to delete trackpoints");
        }

    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> createTrackpoint(@NonNull Integer routeId, @NonNull TrackPointCreateRequest tpCreateReq) {

        if (routeId < 1 || tpCreateReq.getLatitude() < 1.0 || tpCreateReq.getLongitude() < 1.0)
            throw new IllegalArgumentException("values cannot be less than 1");

        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        if(route.getIsLocked()==true) throw new ResourceUpdateFailedException("route is locked by SUPERADMIN ! No update allowed");


        GpxSegment gpxSegment;
        int newLocalSequence;

        if (tpCreateReq.getPrecedingTpId() != null && tpCreateReq.getPrecedingTpId()!=0) {
            TrackPoint precedingTp = trackPointRepo.findById(tpCreateReq.getPrecedingTpId())
                    .orElseThrow(() -> new ResourceNotFoundException("trackpoint", "id", tpCreateReq.getPrecedingTpId()));

            if (!precedingTp.getRoute().getId().equals(route.getId()))
                throw new IllegalArgumentException("preceding trackpoint does not belong to route " + routeId);

            gpxSegment = precedingTp.getGpxSegment();

            trackPointRepo.shiftLocalSequencesAfter(gpxSegment.getId(), precedingTp.getLocalSequence());

            newLocalSequence = precedingTp.getLocalSequence() + 1;
        } else {
            int nextOrderIndex = gpxSegmentRepo
                    .findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(routeId, GpxSegmentStatus.TRACKPOINT)
                    .map(g -> g.getOrderIndex() + 1)
                    .orElse(0);

            gpxSegment = gpxSegmentRepo.save(
                    GpxSegment.builder()
                            .route(route)
                            .sourceFileName("Auto Created GPX " + UUID.randomUUID())
                            .sourceFileHash("MANUAL-" + UUID.randomUUID())
                            .orderIndex(nextOrderIndex)
                            .segmentStatus(GpxSegmentStatus.TRACKPOINT)
                            .user(route.getUser())
                            .build()
            );
            newLocalSequence = 1;
        }

        Point geom = GF.createPoint(
                new Coordinate(tpCreateReq.getLongitude(), tpCreateReq.getLatitude())
        );

        TrackPoint newTp = TrackPoint.builder()
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(tpCreateReq.getLatitude())
                .longitude(tpCreateReq.getLongitude())
                .localSequence(newLocalSequence)
                .geom(geom)
                .status(TrackPointStatus.ACTIVE)
                .recordedAt(LocalDateTime.now())
                .build();

        trackPointRepo.save(newTp);

        // re-derive globalSequence across the route since a mid-route insert shifts downstream points
        trackPointRepo.updateGlobalSequences(routeId);

        // rebuild route path from updated trackpoint order
        List<TrackPoint> orderedTps = trackPointRepo
                .findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route)
                .orElse(List.of());

        if (orderedTps.size() >= 2) {
            Coordinate[] coords = orderedTps.stream()
                    .map(tp -> new Coordinate(tp.getLongitude(), tp.getLatitude()))
                    .toArray(Coordinate[]::new);
            route.setPath(GF.createLineString(coords));
            route.setRouteStatus(RouteStatus.MERGED);
            routeRepo.save(route);
        }

        return new ApiResponse<>(null,"new trackpoint created successfully",201);
    }


    public List<GapPreviewDto> detectGaps(@NonNull Integer routeId, double thresholdMeters) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));

        List<TrackPoint> tps = trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route)
                .orElse(List.of())
                .stream()
                .filter(tp -> tp.getGpxSegment().getSegmentStatus() == GpxSegmentStatus.TRACKPOINT)
                .toList();

        List<GapPreviewDto> gaps = new ArrayList<>();
        for (int i = 1; i < tps.size(); i++) {
            TrackPoint prev = tps.get(i - 1);
            TrackPoint curr = tps.get(i);
            double dist = GeoUtils.haversineMeters(prev.getLongitude(), prev.getLatitude(),
                    curr.getLongitude(), curr.getLatitude());
            if (dist > thresholdMeters) {
                gaps.add(GapPreviewDto.builder()
                        .precedingTpId(prev.getId())
                        .followingTpId(curr.getId())
                        .from(List.of(prev.getLongitude(), prev.getLatitude()))
                        .to(List.of(curr.getLongitude(), curr.getLatitude()))
                        .distanceMeters(Math.round(dist * 100) / 100.0)
                        .build());
            }
        }
        return gaps;
    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> fillGaps(@NonNull Integer routeId, @NonNull FillGapsRequest request, @NonNull GraphHopperProfile profile) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        if(route.getIsLocked()==true) throw new ResourceUpdateFailedException("route is locked by SUPERADMIN ! No update allowed");


        double threshold = request.getThresholdMeters() != null ? request.getThresholdMeters() : 200.0;
        List<GapPreviewDto> gaps = detectGaps(routeId, threshold);

        if (request.getPrecedingTpIds() != null && !request.getPrecedingTpIds().isEmpty()) {
            Set<Integer> selected = new HashSet<>(request.getPrecedingTpIds());
            gaps = gaps.stream().filter(g -> selected.contains(g.getPrecedingTpId())).toList();
        }
        if (gaps.isEmpty())
            throw new IllegalArgumentException("no matching gaps found to fill");

        int filledCount = 0;
        for (GapPreviewDto gap : gaps) { // already ascending sequence order
            filledCount += fillSingleGap(route, gap , profile);
        }

        trackPointRepo.updateGlobalSequences(routeId);
        gpxMergeService.mergeTrackPoints(route.getId());
        String message = ("Filled " + gaps.size() + " gap(s) with " + filledCount + " trackpoints");
        return new ApiResponse<>(null,message,201);

    }

    @Transactional
    private int fillSingleGap(Route route, GapPreviewDto gap , @NonNull GraphHopperProfile profile) {
        TrackPoint precedingTp = trackPointRepo.findById(gap.getPrecedingTpId())
                .orElseThrow(() -> new ResourceNotFoundException("trackpoint", "id", gap.getPrecedingTpId()));

        GeoJsonFeature bridge = geoJsonMapper.trailGapRoutingService.findBridgePath(
                gap.getFrom(), gap.getTo(), "gap-" + gap.getPrecedingTpId() , profile);
        if (bridge == null) {
            log.warn("no bridge path found for gap after tp {}", gap.getPrecedingTpId());
            return 0;
        }

        List<List<Double>> coords = extractLineCoords(bridge.getGeometry());
        if (coords.isEmpty()) return 0;

        GpxSegment originalSegment = precedingTp.getGpxSegment();
        int originalOrderIndex = originalSegment.getOrderIndex();

        // points AFTER precedingTp within the same segment — must be pushed past the bridge
        List<TrackPoint> tailPoints = trackPointRepo
                .findByGpxSegmentIdAndLocalSequenceGreaterThanOrderByLocalSequenceAsc(
                        originalSegment.getId(), precedingTp.getLocalSequence());

        boolean needsSplit = !tailPoints.isEmpty();
        int shiftAmount = needsSplit ? 2 : 1;
        gpxSegmentRepo.shiftOrderIndexAfter(route.getId(), originalOrderIndex, shiftAmount);

        GpxSegment bridgeSegment = gpxSegmentRepo.save(
                GpxSegment.builder()
                        .route(route)
                        .sourceFileName("GraphHopper Gap Fill " + UUID.randomUUID())
                        .sourceFileHash("GH-GAP-" + UUID.randomUUID())
                        .orderIndex(originalOrderIndex + 1)
                        .segmentStatus(GpxSegmentStatus.TRACKPOINT)
                        .user(route.getUser())
                        .build()
        );

        List<TrackPoint> bridgeTps = new ArrayList<>(coords.size());
        int seq = 1;
        for (List<Double> c : coords) {
            double lon = c.get(0), lat = c.get(1);
            bridgeTps.add(TrackPoint.builder()
                    .route(route)
                    .gpxSegment(bridgeSegment)
                    .latitude(lat)
                    .longitude(lon)
                    .localSequence(seq++)
                    .geom(GF.createPoint(new Coordinate(lon, lat)))
                    .status(TrackPointStatus.ACTIVE)
                    .recordedAt(LocalDateTime.now())
                    .build());
        }
        trackPointRepo.saveAll(bridgeTps);

        if (needsSplit) {
            GpxSegment tailSegment = gpxSegmentRepo.save(
                    GpxSegment.builder()
                            .route(route)
                            .sourceFileName(originalSegment.getSourceFileName())
                            .sourceFileHash(deriveSplitHash(originalSegment.getSourceFileHash(),originalOrderIndex+2))
                            .orderIndex(originalOrderIndex + 2)
                            .segmentStatus(originalSegment.getSegmentStatus())
                            .user(route.getUser())
                            .build()
            );
            int tailSeq = 1;
            for (TrackPoint tp : tailPoints) {
                tp.setGpxSegment(tailSegment);
                tp.setLocalSequence(tailSeq++);
            }
            trackPointRepo.saveAll(tailPoints);
        }

        return bridgeTps.size();
    }

    @SuppressWarnings("unchecked")
    private List<List<Double>> extractLineCoords(GeoJsonGeometry geometry) {
        Object raw = geometry.getCoordinates();
        if ("Point".equals(geometry.getType())) return List.of((List<Double>) raw);
        return (List<List<Double>>) raw;
    }

    @Transactional(readOnly = true)
    public ApiResponse<TrackPointResponse> getTrackpoint(@NonNull Integer routeId,@NonNull Integer trackpointId)
    {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        TrackPointResponse tp = trackPointRepo.findByRoute_IdAndId(route.getId(),trackpointId).map(trackPointMapper::toTrackPointResponse)
                .orElseThrow(() -> new ResourceNotFoundException("trackpoint","id",trackpointId));
        return new ApiResponse<>(tp,"trackpoint fetched",200);
    }

    private String deriveSplitHash(String originalHash, long orderIndex) {
        return originalHash == null ? null
                : DigestUtils.sha256Hex(originalHash + ":" + orderIndex).substring(0, 40);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Integer> getLastPrecedingTrackpointId(@NonNull Integer routeId)
    {
        if(routeId<1) throw new IllegalArgumentException("Id's cannot be lesser than 1");
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        boolean isTpsExists = trackPointRepo.existsByRoute_Id(route.getId());
        if(!isTpsExists) throw new ResourceNotFoundException("no trackpoints available in this route");
        TrackPoint lastTrackpoint = trackPointRepo.findTopByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId());
        return new ApiResponse<>(lastTrackpoint.getId(),"last trackpoint id fetched",200);

    }
}
