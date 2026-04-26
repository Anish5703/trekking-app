package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.GpxImportResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.exception.route.RouteNotFoundException;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GpxParserService {

    private final RouteRepository routeRepo;
    private final DestinationRepository destinationRepo;
    private final TrackPointRepository trackPointRepo;
    private  final GeometryFactory GF ;

    public GpxParserService(RouteRepository routeRepo, DestinationRepository destinationRepo , TrackPointRepository trackPointRepo)
    {
      this.routeRepo = routeRepo;
      this.destinationRepo = destinationRepo;
      this.trackPointRepo = trackPointRepo;
      this.GF  = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Transactional
    public ApiResponse<GpxImportResponse> importGpx(MultipartFile file, int routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new RouteNotFoundException("No route found with id "+routeId)
        );

         try
         {
             String xml = new String(file.getBytes(), StandardCharsets.UTF_8)
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
                         .route(route).latitude(latitude).longitude(longitude)
                         .elevation(elevation).timeStamp(timeStamp)
                         .sequenceOrder(sequenceOrder).geom(point).build();

                 trackPoints.add(trackpoint);
                 lineCoords.add(new Coordinate(longitude,latitude));
             }
             //clear and save track points
             trackPointRepo.deleteAllByRoute_Id(routeId);
             trackPointRepo.saveAll(trackPoints);

             //build route line string geometry from all track points
             if(lineCoords.size() >= 2)
             {
                 LineString routeLine = GF.createLineString(lineCoords.toArray(new Coordinate[0]));
                 route.setRouteGeometry(routeLine);
             }

             //update route stats
             if(minEle != Double.MAX_VALUE)
             {
                route.setMinElevation(minEle);
                route.setMaxElevation(maxEle);
             }
             //calculate total distance using Haversine method
             double totalDist = 0;
             for(int i =1 ; i < trackPoints.size(); i++)
             {
                 totalDist += haversineKm(trackPoints.get(i-1).getLatitude(),trackPoints.get(i-1).getLongitude(),
                                           trackPoints.get(i).getLatitude() , trackPoints.get(i).getLongitude());
             }
             route.setDistanceInKm(Math.round(totalDist*100.0)/100.0);
             routeRepo.save(route);

             GpxImportResponse gpxImportResponse = GpxImportResponse.builder()
                     .routeId(routeId).routeName(route.getName())
                     .numberOfTrackPoints(wptNodesLength)
                     .distanceInKm(totalDist)
                     .addedBy(route.getUser().getName())
                     .build();

             return new ApiResponse<>(gpxImportResponse,"Gpx file imported ",200);
         }
         catch (IOException | ParserConfigurationException | SAXException e)
         {
             log.error("Gpx Parsing failed : {}",e.getLocalizedMessage());
             throw new FileParsingFailedException("Failed to parse Gpx file");
         }
    }

    public ApiResponse<Void> deleteGpx(int routeId)
    {
        boolean routeExists = routeRepo.existsById(routeId);
        if(!routeExists) throw new ResourceNotFoundException("route","id",routeId);

        boolean trackPointExists = trackPointRepo.existsByRoute_Id(routeId);
        if(!trackPointExists) throw new ResourceNotFoundException("trackpoints","route id",routeId);

        try{
            trackPointRepo.deleteAllByRoute_Id(routeId);
            return new ApiResponse<>(null,"Gpx file deleted",200);
        }
        catch (Exception e)
        {
            log.error("Failed to delete gpx : {}",e.getLocalizedMessage());
            throw new ResourceDeletionFailedException("gpx file","route id",routeId);
        }
    }

    private double haversineKm(double lat1 , double lon1 , double lat2 , double lon2)
    {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                   + Math.cos(Math.toRadians(lat2))
                   * Math.sin(dLon /2) * Math.sin(dLon / 2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));

    }
}
