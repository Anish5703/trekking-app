package com.example.trekking_app.service.gpx;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GpxExportService {

    private final RouteRepository routeRepo;
    private final GpxSegmentRepository gpxSegmentRepo;

    private static final DateTimeFormatter GPX_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Transactional
    public byte[] exportRouteAsGpx(@NonNull Integer routeId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));

        List<GpxSegment> segments = gpxSegmentRepo.findByRoute_IdOrderByOrderIndexAsc(routeId)
                .stream()
                .filter(s -> s.getSegmentStatus() == GpxSegmentStatus.TRACKPOINT)
                .toList();

        if (segments.isEmpty())
            throw new ResourceNotFoundException("trackpoints", "route id", routeId);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element gpx = doc.createElement("gpx");
            gpx.setAttribute("version", "1.1");
            gpx.setAttribute("creator", "trekking-app");
            gpx.setAttribute("xmlns", "http://www.topografix.com/GPX/1/1");
            doc.appendChild(gpx);

            Element metadata = doc.createElement("metadata");
            Element name = doc.createElement("name");
            name.setTextContent(route.getName());
            metadata.appendChild(name);
            gpx.appendChild(metadata);

            for (GpxSegment segment : segments) {
                List<TrackPoint> points = segment.getTrackPoints().stream()
                        .filter(tp -> !tp.getIsDeleted())
                        .sorted((a, b) -> Integer.compare(a.getLocalSequence(), b.getLocalSequence()))
                        .toList();

                if (points.isEmpty()) continue;

                Element trk = doc.createElement("trk");
                Element trkName = doc.createElement("name");
                trkName.setTextContent(segment.getSourceFileName());
                trk.appendChild(trkName);

                Element trkseg = doc.createElement("trkseg");
                for (TrackPoint tp : points) {
                    Element trkpt = doc.createElement("trkpt");
                    trkpt.setAttribute("lat", String.valueOf(tp.getLatitude()));
                    trkpt.setAttribute("lon", String.valueOf(tp.getLongitude()));

                    if (tp.getElevation() != null) {
                        Element ele = doc.createElement("ele");
                        ele.setTextContent(String.valueOf(tp.getElevation()));
                        trkpt.appendChild(ele);
                    }
                    if (tp.getRecordedAt() != null) {
                        Element time = doc.createElement("time");
                        time.setTextContent(tp.getRecordedAt().atOffset(ZoneOffset.UTC).format(GPX_TIME_FORMAT));
                        trkpt.appendChild(time);
                    }
                    trkseg.appendChild(trkpt);
                }
                trk.appendChild(trkseg);
                gpx.appendChild(trk);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("failed to build gpx file for route " + routeId, e);
        }
    }
    @Transactional(readOnly = true)
    public byte[] exportSegmentAsGpx(@NonNull Integer routeId, @NonNull Integer segmentId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));

        GpxSegment segment = gpxSegmentRepo.findByIdAndRoute_Id(segmentId, routeId)
                .orElseThrow(() -> new ResourceNotFoundException("gpx segment", "id", segmentId));

        if (segment.getSegmentStatus() != GpxSegmentStatus.TRACKPOINT)
            throw new IllegalArgumentException("segment " + segmentId + " is not a trackpoint segment");

        byte[] gpxBytes;
        try {
            gpxBytes = buildGpxForSegment(route, segment);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to build gpx for segment " + segmentId);
        }

        if (gpxBytes == null)
            throw new ResourceNotFoundException("trackpoints", "segment id", segmentId);
        return gpxBytes;
    }
    @Transactional
    private byte[] buildGpxForSegment(Route route, GpxSegment segment) throws Exception {
        List<TrackPoint> points = segment.getTrackPoints().stream()
                .filter(tp -> !tp.getIsDeleted())
                .sorted((a, b) -> Integer.compare(a.getLocalSequence(), b.getLocalSequence()))
                .toList();

        if (points.isEmpty()) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element gpx = doc.createElement("gpx");
        gpx.setAttribute("version", "1.1");
        gpx.setAttribute("creator", "trekking-app");
        gpx.setAttribute("xmlns", "http://www.topografix.com/GPX/1/1");
        doc.appendChild(gpx);

        Element metadata = doc.createElement("metadata");
        Element metaName = doc.createElement("name");
        metaName.setTextContent(route.getName());
        metadata.appendChild(metaName);
        gpx.appendChild(metadata);

        Element trk = doc.createElement("trk");
        Element trkName = doc.createElement("name");
        trkName.setTextContent(segment.getSourceFileName());
        trk.appendChild(trkName);

        Element trkseg = doc.createElement("trkseg");
        for (TrackPoint tp : points) {
            Element trkpt = doc.createElement("trkpt");
            trkpt.setAttribute("lat", String.valueOf(tp.getLatitude()));
            trkpt.setAttribute("lon", String.valueOf(tp.getLongitude()));

            if (tp.getElevation() != null) {
                Element ele = doc.createElement("ele");
                ele.setTextContent(String.valueOf(tp.getElevation()));
                trkpt.appendChild(ele);
            }
            if (tp.getRecordedAt() != null) {
                Element time = doc.createElement("time");
                time.setTextContent(tp.getRecordedAt().atOffset(ZoneOffset.UTC).format(GPX_TIME_FORMAT));
                trkpt.appendChild(time);
            }
            trkseg.appendChild(trkpt);
        }
        trk.appendChild(trkseg);
        gpx.appendChild(trk);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    public String getSegmentOriginalFilename(@NonNull Integer routeId, @NonNull Integer segmentId) {
        GpxSegment segment = gpxSegmentRepo.findByIdAndRoute_Id(segmentId, routeId)
                .orElseThrow(() -> new ResourceNotFoundException("gpx segment", "id", segmentId));

        String name = segment.getSourceFileName();
        if (name == null || name.isBlank()) return "segment-" + segmentId + ".gpx";
        return name.toLowerCase().endsWith(".gpx") ? name : name + ".gpx";
    }
}