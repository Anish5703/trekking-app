package com.example.trekking_app.service.waypoint;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.POIUploadResponse;
import com.example.trekking_app.dto.poi.XlsxPoiRow;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class XlsxIngestionService {

   private final WayPointRepository wayPointRepo;
   private final GpxSegmentRepository gpxSegmentRepo;
   private final TrackPointRepository trackPointRepo;
   private final XlsxParserService parser;
   private final POIRepository poiRepo;
   private final POITypeResolver poiTypeResolver;
    private static final GeometryFactory GEOM = new GeometryFactory(new PrecisionModel(), 4326);
    private static final ObjectMapper JSON = new ObjectMapper();


   public ApiResponse<POIUploadResponse> uploadXlsxFile(Integer routeId , Integer gpxSegmentId , MultipartFile file)
   {
       GpxSegment gpxSegment = gpxSegmentRepo.findByIdAndRoute_Id(gpxSegmentId,routeId).orElseThrow(
               () ->  new ResourceNotFoundException("gpx file","route id and gpx file id ",String.format("%s and %s respectively",routeId,gpxSegmentId))
       );

       List<TrackPoint> trackpoints =  trackPointRepo.findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(routeId,gpxSegmentId).orElseThrow(
               () -> new ResourceNotFoundException("trackpoints","gpx file id",gpxSegmentId)
       );
       Map<Integer,TrackPoint> bySeq = new HashMap<>();
       trackpoints.forEach(tp -> bySeq.put(tp.getLocalSequence(),tp));
       List<XlsxPoiRow> rows = parser.parse(file);

       POIUploadResponse summary = POIUploadResponse.builder()
               .routeId(routeId)
               .gpxSegmentId(gpxSegmentId)
               .totalRows(rows.size())
               .build();

       // Idempotent re-upload: clear existing POIs for this segment first.
       int removed = poiRepo.deleteAllByWayPoint_GpxSegment_Id(gpxSegmentId);
       if (removed > 0) log.info("Removed {} existing POIs for segment {}", removed, gpxSegmentId);

       int wpCount = 0;
       int poiCount = 0;

       for (XlsxPoiRow row : rows) {
           Integer wpNum = row.getWaypointNumber();
           if (wpNum == null) {
               summary.getSkipped().add("row with blank Waypoint Number");
               continue;
           }
           TrackPoint tp = bySeq.get(wpNum);
           if (tp == null) {
               summary.getSkipped().add("waypoint " + wpNum + " has no matching trackpoint in segment " + gpxSegmentId);
               continue;
           }

           WayPoint wp = wayPointRepo
                   .findByGpxSegment_IdAndLocalSequence(gpxSegmentId, wpNum)
                   .orElseGet(() -> WayPoint.builder()
                           .route(gpxSegment.getRoute())
                           .gpxSegment(gpxSegment)
                           .localSequence(wpNum)
                           .build());

           String displayName = pickName(row);
           wp.setName(displayName);
           wp.setWaypointNumber(String.valueOf(wpNum));
           wp.setLatitude(tp.getLatitude());
           wp.setLongitude(tp.getLongitude());
           wp.setElevation(tp.getElevation());
           wp.setGlobalSequence(tp.getGlobalSequence() == null ? 0 : tp.getGlobalSequence());
           Point geom = GEOM.createPoint(new Coordinate(tp.getLongitude(), tp.getLatitude()));
           geom.setSRID(4326);
           wp.setGeom(geom);
           wp.setLocation(geom);

           wp = wayPointRepo.save(wp);
           wpCount++;

           POIType type = poiTypeResolver.resolve(row);
           POI poi = POI.builder()
                   .route(gpxSegment.getRoute())
                   .wayPoint(wp)
                   .name(displayName)
                   .latitude(tp.getLatitude())
                   .longitude(tp.getLongitude())
                   .elevation(tp.getElevation() == null ? 0d : tp.getElevation())
                   .type(type)
                   .description(toJson(row))
                   .build();
           poiRepo.save(poi);
           poiCount++;
       }

       summary.setWayPointsUpserted(wpCount);
       summary.setPoisCreated(poiCount);

       // TODO: cacheEvictor.evictRoute(routeId) once Redis layer is wired.

       return new ApiResponse<>(summary,"XLSX ingestion successful",200);


   }
    private String pickName(XlsxPoiRow row) {
        if (notBlank(row.getName()))          return row.getName();
        if (notBlank(row.getImportantStop())) return row.getImportantStop();
        if (notBlank(row.getTrailPath()))     return row.getTrailPath();
        return "Waypoint " + row.getWaypointNumber();
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private String toJson(XlsxPoiRow row) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trailPath", row.getTrailPath());
        payload.put("startOrEnd", row.getStartOrEnd());
        payload.put("importantStop", row.getImportantStop());
        payload.put("extras", row.getExtras());
        try {
            return JSON.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise POI extras for waypoint {}", row.getWaypointNumber(), e);
            return "{}";
        }
    }
}
