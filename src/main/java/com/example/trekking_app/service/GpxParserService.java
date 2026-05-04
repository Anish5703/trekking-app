package com.example.trekking_app.service;

import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.locationtech.jts.geom.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxParserService {

    private final TrackPointRepository trackPointRepo;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private final RouteRepository routeRepo;
    private final GpxSegmentRepository gpxSegmentRepo;

    @Transactional
    public GpxImportResponse parse(MultipartFile file, Route route, Integer nextOrder) {
        try {
            byte[] gpxBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String sha256 = sha256(gpxBytes);

            GpxSegment gpxSegment = GpxSegment.builder()
                    .sourceFileName(filename)
                    .sourceFileHash(sha256)
                    .user(route.getUser())
                    .orderIndex(nextOrder)
                    .route(route)
                    .status(GpxSegmentStatus.DRAFT)
                    .build();

            String xml = new String(gpxBytes, StandardCharsets.UTF_8)
                    .replaceFirst("^\\uFEFF", "")  // remove BOM
                    .trim();                      // remove leading space (YOUR ISSUE)

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();

            Document doc = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList wptNodes = doc.getElementsByTagName("wpt");
            List<TrackPoint> trackPoints = new ArrayList<>();
            List<Coordinate> lineCoords = new ArrayList<>();

            double maxEle = Double.MIN_VALUE;
            double minEle = Double.MAX_VALUE;

            int sequenceOrder = 0;
            LocalDateTime timeStamp = null;
            double latitude , longitude , elevation = 0 ;

            int wptNodesLength = wptNodes.getLength();

            GpxSegment savedGpxSegment = gpxSegmentRepo.save(gpxSegment);

            for(int i=0; i<wptNodesLength ; i++)
            {
                Element wpt = (Element) wptNodes.item(i);

                if(wpt.getAttribute("lat").isEmpty() || wpt.getAttribute("lon").isEmpty() )
                    throw new FileParsingFailedException("Unsupported file");

                latitude = Double.parseDouble(wpt.getAttribute("lat"));
                longitude = Double.parseDouble(wpt.getAttribute("lon"));

                NodeList eleNodes = wpt.getElementsByTagName("ele");
                if (eleNodes.getLength() > 0) {
                    elevation = Double.parseDouble(eleNodes.item(0).getTextContent());
                    minEle = Math.min(minEle, elevation);
                    maxEle = Math.max(maxEle, elevation);
                }
                NodeList timeNodes = wpt.getElementsByTagName("time");
                if (timeNodes.getLength() > 0) {
                    timeStamp = ZonedDateTime.parse(timeNodes.item(0).getTextContent()).toLocalDateTime();
                }
                sequenceOrder = i + 1;
                NodeList nameNodes = wpt.getElementsByTagName("name");
                if (nameNodes.getLength() > 0) {
                    try { sequenceOrder = Integer.parseInt(nameNodes.item(0).getTextContent().trim()); }
                    catch (NumberFormatException ignored) {}
                }

                minEle = Math.min(minEle , elevation);
                maxEle = Math.max(maxEle , elevation);

                Point point = GF.createPoint(new Coordinate(longitude,latitude));


                TrackPoint trackpoint = TrackPoint.builder()
                        .route(route)
                        .gpxSegment(savedGpxSegment)
                        .latitude(latitude)
                        .longitude(longitude)
                        .elevation(elevation)
                        .localSequence(sequenceOrder)
                        .geom(point)
                        .recordedAt(timeStamp)
                        .build();

                trackPoints.add(trackpoint);
            }
           savedGpxSegment.setRecordedAt(trackPoints.getFirst().getRecordedAt());
            savedGpxSegment.setRecordedUntil(trackPoints.getLast().getRecordedAt());
            gpxSegmentRepo.save(savedGpxSegment);

            //clear and save track points
            trackPointRepo.saveAll(trackPoints);

            //build route line string geometry from all track points
           LineString path = generateRoutePath(trackPoints,route);
           route.setPath(path);

            //update route stats
            if(minEle != Double.MAX_VALUE)
            {
                route.setMinElevation(minEle);
                route.setMaxElevation(maxEle);
            }
            //calculate total distance using Haversine method
            double totalDist = calculateTotalDistance(trackPoints);

            route.setDistanceInKm(Math.round(totalDist*100.0)/100.0);
            routeRepo.save(route);

            GpxImportResponse gpxImportResponse = GpxImportResponse.builder()
                    .routeId(route.getId())
                    .segmentId(gpxSegment.getId())
                    .segmentName(gpxSegment.getSourceFileName())
                    .totalTrackPoints(wptNodesLength)
                    .totalDistanceInKm(totalDist)
                    .build();
            return gpxImportResponse;

        } catch (Exception e) {
            log.error("GPX parse failed for {}", file.getOriginalFilename(), e);
            throw new FileParsingFailedException("Invalid GPX file: " + file.getOriginalFilename());
        }
    }

    /** Helper methods */

    private static String textOf(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String t = nl.item(0).getTextContent();
        return (t == null || t.isBlank()) ? null : t.trim();
    }
    private static Double parseDoubleOrNull(String s) {
        try { return s == null ? null : Double.parseDouble(s); } catch (Exception e) { return null; }
    }
    private static LocalDateTime parseTime(String s) {
        try { return s == null ? null : ZonedDateTime.parse(s).toLocalDateTime(); }
        catch (Exception e) { return null; }
    }
    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest(bytes)) sb.append(String.format("%02x", b));
        return sb.toString();
    }
    /** Haversine sum — accurate enough for trekking-distance summaries. */
    private static double approxDistanceInKm(List<Coordinate> cs) {
        double total = 0; final double R = 6371.0088;
        for (int i = 1; i < cs.size(); i++) {
            double lat1 = Math.toRadians(cs.get(i-1).y), lat2 = Math.toRadians(cs.get(i).y);
            double dLat = lat2 - lat1, dLon = Math.toRadians(cs.get(i).x - cs.get(i-1).x);
            double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                    + Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)*Math.sin(dLon/2);
            total += 2 * R * Math.asin(Math.sqrt(a));
        }
        return total;
    }
    public double haversineKm(double lat1 , double lon1 , double lat2 , double lon2)
    {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon /2) * Math.sin(dLon / 2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));

    }

    public double calculateTotalDistance(List<TrackPoint> trackPoints)
    {
        double totalDist = 0;
        for(int i =1 ; i < trackPoints.size(); i++)
        {
            totalDist += haversineKm(trackPoints.get(i-1).getLatitude(),trackPoints.get(i-1).getLongitude(),
                    trackPoints.get(i).getLatitude() , trackPoints.get(i).getLongitude());
        }
        return totalDist;
    }

    public LineString generateRoutePath(List<TrackPoint> trackPoints,Route route)
    {
        if (trackPoints.size() < 2) {
            route.setPath(null);
            return null;
        }

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        Coordinate[] coordinates = trackPoints.stream()
                .map(tp -> new Coordinate(tp.getLongitude(), tp.getLatitude()))
                .toArray(Coordinate[]::new);

        LineString path = geometryFactory.createLineString(coordinates);
        return path;
    }
}
