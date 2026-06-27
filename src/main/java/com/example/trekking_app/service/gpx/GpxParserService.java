package com.example.trekking_app.service.gpx;

import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;

import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.WayPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxParserService {

    private final TrackPointRepository trackPointRepo;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private final RouteRepository routeRepo;
    private final WayPointRepository wayPointRepo;
    private final GpxSegmentRepository gpxSegmentRepo;

    @Transactional
    public GpxImportResponse parseTrackPoints(MultipartFile file, Route route, Integer nextOrder) {

        String filename = file.getOriginalFilename();
        if(filename == null || !filename.endsWith(".gpx")) throw new RuntimeException("invalid file ! only gpx file allowed");
        try{
            byte[] gpxBytes = file.getBytes();
            String sha256 = sha256(gpxBytes);
            GpxSegment gpxSegment = GpxSegment.builder()
                    .sourceFileName(filename)
                    .sourceFileHash(sha256)
                    .user(route.getUser())
                    .orderIndex(nextOrder)
                    .route(route)
                    .segmentStatus(GpxSegmentStatus.TRACKPOINT)
                    .build();

            String xml = new String(gpxBytes, StandardCharsets.UTF_8)
                    .replaceFirst("^\\uFEFF", "")  // remove BOM
                    .trim();                      // remove leading space (YOUR ISSUE)

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();

            Document doc = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList wptNodes = doc.getElementsByTagName("wpt");
                if(wptNodes.getLength()==0)
                    wptNodes = doc.getElementsByTagName("trkpt");

                if(wptNodes.getLength()==0) throw new FileParsingFailedException("no trackpoints to collect");

            List<TrackPoint> trackPoints = new ArrayList<>();

            int localSequence = 0;
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
                }
                NodeList timeNodes = wpt.getElementsByTagName("time");
                if (timeNodes.getLength() > 0) {
                    timeStamp = ZonedDateTime.parse(timeNodes.item(0).getTextContent()).toLocalDateTime();
                }
                localSequence = i + 1;
                NodeList nameNodes = wpt.getElementsByTagName("name");
                if (nameNodes.getLength() > 0) {
                    try { localSequence = Integer.parseInt(nameNodes.item(0).getTextContent().trim()); }
                    catch (NumberFormatException ignored) {}
                }
                Point point = GF.createPoint(new Coordinate(longitude,latitude));

                TrackPoint trackpoint = TrackPoint.builder()
                        .route(route)
                        .gpxSegment(savedGpxSegment)
                        .latitude(latitude)
                        .longitude(longitude)
                        .elevation(elevation)
                        .localSequence(localSequence)
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


            //calculate total distance using Haversine method
            double totalDist = calculateTotalDistance(trackPoints);

            return GpxImportResponse.builder()
                    .routeId(route.getId())
                    .segmentId(gpxSegment.getId())
                    .segmentName(gpxSegment.getSourceFileName())
                    .totalTrackPoints(wptNodesLength)
                    .totalWayPoints(0)
                    .totalDistanceInKm(totalDist)
                    .build();

        } catch (Exception e) {
            log.error("GPX file parsing failed for extracting trackpoints{} , error : {}", file.getOriginalFilename(), e.getLocalizedMessage());
            throw new FileParsingFailedException("Invalid GPX file for extraction of trackpoints: " + file.getOriginalFilename());
        }
    }

    @Transactional
    public GpxImportResponse parseWayPoints(MultipartFile file , Route route ,Integer nextOrder)
    {
        String filename = file.getOriginalFilename();
        if(filename == null || !filename.endsWith(".gpx")) throw new RuntimeException("invalid file ! only gpx file allowed");
        try{
            byte[] gpxBytes = file.getBytes();
            String sha256 = sha256(gpxBytes);
            GpxSegment gpxSegment = GpxSegment.builder()
                    .sourceFileName(filename)
                    .sourceFileHash(sha256)
                    .user(route.getUser())
                    .orderIndex(nextOrder)
                    .route(route)
                    .segmentStatus(GpxSegmentStatus.WAYPOINT)
                    .build();

            String xml = new String(gpxBytes, StandardCharsets.UTF_8)
                    .replaceFirst("^\\uFEFF", "")  // remove BOM
                    .trim();                      // remove leading space (YOUR ISSUE)

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();

            Document doc = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList wptNodes = doc.getElementsByTagName("wpt");
            if(wptNodes.getLength()==0) throw new FileParsingFailedException("no trackpoints to collect");

            List<WayPoint> wayPoints = new ArrayList<>();

            int localSequence = 0;
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
                }
                NodeList timeNodes = wpt.getElementsByTagName("time");
                if (timeNodes.getLength() > 0) {
                    timeStamp = ZonedDateTime.parse(timeNodes.item(0).getTextContent()).toLocalDateTime();
                }
                localSequence = i + 1;
                NodeList nameNodes = wpt.getElementsByTagName("name");
                if (nameNodes.getLength() > 0) {
                    try { localSequence = Integer.parseInt(nameNodes.item(0).getTextContent().trim()); }
                    catch (NumberFormatException ignored) {}
                }
                Point point = GF.createPoint(new Coordinate(longitude,latitude));

                WayPoint wayPoint = WayPoint.builder()
                        .route(route)
                        .gpxSegment(savedGpxSegment)
                        .name(String.valueOf(localSequence))
                        .latitude(latitude)
                        .longitude(longitude)
                        .elevation(elevation)
                        .localSequence(localSequence)
                        .geom(point)
                        .recordedAt(timeStamp)
                        .build();

                wayPoints.add(wayPoint);
            }
            savedGpxSegment.setRecordedAt(wayPoints.getFirst().getRecordedAt());
            savedGpxSegment.setRecordedUntil(wayPoints.getLast().getRecordedAt());
            gpxSegmentRepo.save(savedGpxSegment);

            //clear and save track points
            wayPointRepo.saveAll(wayPoints);

            return GpxImportResponse.builder()
                    .routeId(route.getId())
                    .segmentId(gpxSegment.getId())
                    .segmentName(gpxSegment.getSourceFileName())
                    .totalWayPoints(wptNodesLength)
                    .totalTrackPoints(0)
                    .totalDistanceInKm(0.0)
                    .build();

        } catch (Exception e) {
            log.error("GPX file parsing failed for extracting waypoints{}", file.getOriginalFilename(), e.getLocalizedMessage());
            throw new FileParsingFailedException("Invalid GPX file for extraction of waypoints: " + file.getOriginalFilename());
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



    @Transactional
    public GpxImportResponse parseTrackPointsFromExcel(MultipartFile file, Route route, Integer nextOrder) {

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx"))
            throw new RuntimeException("Invalid file! Only .xlsx files allowed");

        try {
            byte[] fileBytes = file.getBytes();
            String sha256 = sha256(fileBytes);

            GpxSegment gpxSegment = GpxSegment.builder()
                    .sourceFileName(filename)
                    .sourceFileHash(sha256)
                    .user(route.getUser())
                    .orderIndex(nextOrder)
                    .route(route)
                    .segmentStatus(GpxSegmentStatus.TRACKPOINT)
                    .build();

            GpxSegment savedGpxSegment = gpxSegmentRepo.save(gpxSegment);

            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes));
            Sheet sheet = workbook.getSheetAt(0);

            // --- Resolve column indices from header row ---
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new FileParsingFailedException("Excel file has no header row");

            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
            }

            Integer latCol  = colIndex.get("LATITUDE");
            Integer lonCol  = colIndex.get("LONGITUDE");
            Integer eleCol  = colIndex.get("ELEVATION");
            Integer timeCol = colIndex.get("DATETIME");
            Integer seqCol  = colIndex.get("OBJECTID");

            if (latCol == null || lonCol == null)
                throw new FileParsingFailedException("Missing required columns: LATITUDE / LONGITUDE");

            // --- Parse data rows ---
            List<TrackPoint> trackPoints = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            int totalRows = sheet.getLastRowNum(); // 0-indexed, row 0 = header
            if (totalRows < 1) throw new FileParsingFailedException("No trackpoints to collect");

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                double latitude  = getNumericCell(row, latCol);
                double longitude = getNumericCell(row, lonCol);
                double elevation = eleCol != null ? getNumericCell(row, eleCol) : 0.0;

                LocalDateTime timestamp = null;
                if (timeCol != null) {
                    Cell timeCell = row.getCell(timeCol);
                    if (timeCell != null && DateUtil.isCellDateFormatted(timeCell)) {
                        timestamp = timeCell.getLocalDateTimeCellValue();
                    } else if (timeCell != null) {
                        // fallback: parse string
                        String raw = formatter.formatCellValue(timeCell).trim();
                        if (!raw.isEmpty()) {
                            timestamp = LocalDateTime.parse(raw,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }
                    }
                }

                int localSequence = i; // default: row number
                if (seqCol != null) {
                    Cell seqCell = row.getCell(seqCol);
                    if (seqCell != null && seqCell.getCellType() == CellType.NUMERIC) {
                        localSequence = (int) seqCell.getNumericCellValue();
                    }
                }

                Point point = GF.createPoint(new Coordinate(longitude, latitude));

                TrackPoint trackPoint = TrackPoint.builder()
                        .route(route)
                        .gpxSegment(savedGpxSegment)
                        .latitude(latitude)
                        .longitude(longitude)
                        .elevation(elevation)
                        .localSequence(localSequence)
                        .geom(point)
                        .recordedAt(timestamp)
                        .build();

                trackPoints.add(trackPoint);
            }

            workbook.close();

            if (trackPoints.isEmpty()) throw new FileParsingFailedException("No trackpoints to collect");

            savedGpxSegment.setRecordedAt(trackPoints.getFirst().getRecordedAt());
            savedGpxSegment.setRecordedUntil(trackPoints.getLast().getRecordedAt());
            gpxSegmentRepo.save(savedGpxSegment);

            trackPointRepo.saveAll(trackPoints);

            double totalDist = calculateTotalDistance(trackPoints);

            return GpxImportResponse.builder()
                    .routeId(route.getId())
                    .segmentId(savedGpxSegment.getId())
                    .segmentName(savedGpxSegment.getSourceFileName())
                    .totalTrackPoints(trackPoints.size())
                    .totalWayPoints(0)
                    .totalDistanceInKm(totalDist)
                    .build();

        } catch (FileParsingFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel file parsing failed for: {}, error: {}", file.getOriginalFilename(), e.getLocalizedMessage());
            throw new FileParsingFailedException("Invalid Excel file for extraction of trackpoints: " + filename);
        }
    }

    // Helper — avoids NPE on missing optional cells
    private double getNumericCell(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> Double.parseDouble(cell.getStringCellValue().trim());
            default      -> 0.0;
        };
    }

    @Transactional
    public GpxImportResponse parseWayPointsFromExcel(MultipartFile file, Route route, Integer nextOrder) {

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx"))
            throw new RuntimeException("Invalid file! Only .xlsx files allowed");

        try {
            byte[] fileBytes = file.getBytes();
            String sha256 = sha256(fileBytes);

            GpxSegment gpxSegment = GpxSegment.builder()
                    .sourceFileName(filename)
                    .sourceFileHash(sha256)
                    .user(route.getUser())
                    .orderIndex(nextOrder)
                    .route(route)
                    .segmentStatus(GpxSegmentStatus.WAYPOINT)
                    .build();

            GpxSegment savedGpxSegment = gpxSegmentRepo.save(gpxSegment);

            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes));
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new FileParsingFailedException("Excel file has no header row");

            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
            }

            Integer latCol  = colIndex.get("LATITUDE");
            Integer lonCol  = colIndex.get("LONGITUDE");
            Integer eleCol  = colIndex.get("ELEVATION");
            Integer timeCol = colIndex.get("DATETIME");
            Integer seqCol  = colIndex.get("OBJECTID");
            Integer nameCol = colIndex.get("NAME"); // optional — WayPoint has a name field

            if (latCol == null || lonCol == null)
                throw new FileParsingFailedException("Missing required columns: LATITUDE / LONGITUDE");

            List<WayPoint> wayPoints = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            int totalRows = sheet.getLastRowNum();
            if (totalRows < 1) throw new FileParsingFailedException("No waypoints to collect");

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                double latitude  = getNumericCell(row, latCol);
                double longitude = getNumericCell(row, lonCol);
                double elevation = eleCol != null ? getNumericCell(row, eleCol) : 0.0;

                LocalDateTime timestamp = null;
                if (timeCol != null) {
                    Cell timeCell = row.getCell(timeCol);
                    if (timeCell != null && DateUtil.isCellDateFormatted(timeCell)) {
                        timestamp = timeCell.getLocalDateTimeCellValue();
                    } else if (timeCell != null) {
                        String raw = formatter.formatCellValue(timeCell).trim();
                        if (!raw.isEmpty()) {
                            timestamp = LocalDateTime.parse(raw,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }
                    }
                }

                int localSequence = i;
                if (seqCol != null) {
                    Cell seqCell = row.getCell(seqCol);
                    if (seqCell != null && seqCell.getCellType() == CellType.NUMERIC) {
                        localSequence = (int) seqCell.getNumericCellValue();
                    }
                }

                // name: prefer dedicated NAME column, fallback to localSequence (mirrors GPX behavior)
                String name = String.valueOf(localSequence);
                if (nameCol != null) {
                    Cell nameCell = row.getCell(nameCol);
                    if (nameCell != null) {
                        String raw = formatter.formatCellValue(nameCell).trim();
                        if (!raw.isEmpty()) name = raw;
                    }
                }

                Point point = GF.createPoint(new Coordinate(longitude, latitude));

                WayPoint wayPoint = WayPoint.builder()
                        .route(route)
                        .gpxSegment(savedGpxSegment)
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .elevation(elevation)
                        .localSequence(localSequence)
                        .geom(point)
                        .recordedAt(timestamp)
                        .build();

                wayPoints.add(wayPoint);
            }

            workbook.close();

            if (wayPoints.isEmpty()) throw new FileParsingFailedException("No waypoints to collect");

            savedGpxSegment.setRecordedAt(wayPoints.getFirst().getRecordedAt());
            savedGpxSegment.setRecordedUntil(wayPoints.getLast().getRecordedAt());
            gpxSegmentRepo.save(savedGpxSegment);

            wayPointRepo.saveAll(wayPoints);

            return GpxImportResponse.builder()
                    .routeId(route.getId())
                    .segmentId(savedGpxSegment.getId())
                    .segmentName(savedGpxSegment.getSourceFileName())
                    .totalWayPoints(wayPoints.size())
                    .totalTrackPoints(0)
                    .totalDistanceInKm(0.0)
                    .build();

        } catch (FileParsingFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel file parsing failed for extracting waypoints: {}, error: {}", file.getOriginalFilename(), e.getLocalizedMessage());
            throw new FileParsingFailedException("Invalid Excel file for extraction of waypoints: " + filename);
        }
    }


}
