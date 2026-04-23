package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.GpxImportResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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
             InputStream is = file.getInputStream();
              Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
              doc.getDocumentElement().normalize();

             NodeList wptNodes = doc.getElementsByTagName("wpt");
             List<TrackPoint> trackPoints = new ArrayList<>();
             List<Coordinate> lineCoords = new ArrayList<>();

             double maxEle = Double.MIN_VALUE;
             double minEle = Double.MAX_VALUE;

             int sequenceOrder;
             LocalDateTime timeStamp;
             double latitude , longitude , elevation = 0 ;

             int wptNodesLength = wptNodes.getLength();
             for(int i=0; i<wptNodesLength ; i++)
             {
                 Element wpt = (Element) wptNodes.item(i);
                  latitude = Double.parseDouble(wpt.getAttribute("lat"));
                  longitude = Double.parseDouble(wpt.getAttribute("lon"));
                  elevation = Double.parseDouble(wpt.getAttribute("ele"));
                  sequenceOrder = Integer.parseInt(wpt.getAttribute("name"));
                  timeStamp = LocalDateTime.parse(wpt.getAttribute("time"));

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
             trackPointRepo.deleteByRouteId(routeId);
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
             route.setDistance(Math.round(totalDist*100.0)/100.0);
             routeRepo.save(route);

             GpxImportResponse gpxImportResponse = GpxImportResponse.builder()
                     .routeId(routeId).routeName(route.getName())
                     .numberOfTrackPoints(wptNodesLength)
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
