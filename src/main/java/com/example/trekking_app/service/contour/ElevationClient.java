package com.example.trekking_app.service.contour;

import com.example.trekking_app.dto.contour.ElevationProperties;
import com.example.trekking_app.dto.contour.LatLon;
import com.example.trekking_app.exception.contour.ElevationUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElevationClient {

    private record Req(String locations, String interpolation) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Res(String status, List<Result> results) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Result(Double elevation) {}

    private final RestClient rest;
    private final ElevationProperties props;

    ElevationClient(RestClient elevationRestClient, ElevationProperties props) {
        this.rest = elevationRestClient;
        this.props = props;
    }

    /** Elevation (m) per input point, same order; null element = no data. Splits into batches. */
    public List<Double> elevations(List<LatLon> points) {
        List<Double> out = new ArrayList<>(points.size());
        int step = props.batchSize();
        for (int i = 0; i < points.size(); i += step) {
            List<LatLon> chunk = points.subList(i, Math.min(i + step, points.size()));
            String locations = chunk.stream()
                    .map(p -> String.format(Locale.ROOT, "%.6f,%.6f", p.lat(), p.lon()))
                    .collect(Collectors.joining("|"));
            log.debug(
                    "Requesting elevations: dataset={}, batchSize={}, " +
                            "chunkSize={}, offset={}",
                    props.dataset(),
                    props.batchSize(),
                    chunk.size(),
                    i
            );

            Res res;
               try {
                   res = rest.post()
                           .uri("/v1/{ds}", props.dataset())
                           .body(new Req(locations, "bilinear"))
                           .retrieve()                       // 4xx/5xx -> RestClientResponseException
                           .body(Res.class);
               } catch (RestClientResponseException ex) {
                   log.error(
                           "OpenTopoData HTTP failure: status={}, " +
                                   "contentType={}, responseBody={}",
                           ex.getStatusCode(),
                           ex.getResponseHeaders() != null
                                   ? ex.getResponseHeaders().getContentType()
                                   : null,
                           ex.getResponseBodyAsString(),
                           ex
                   );
                   throw new ElevationUnavailableException(
                           "OpenTopoData returned HTTP "
                                   + ex.getStatusCode(),
                           ex
                   );               }
                if (res == null || res.results() == null || res.results().size() != chunk.size())
                    throw new IllegalStateException("Unexpected elevation response");
                res.results().forEach(r -> out.add(r.elevation()));

        }
        return out;
    }

    public Optional<Double> elevation(double lat, double lon) {
        return Optional.ofNullable(elevations(List.of(new LatLon(lat, lon))).get(0));
    }
}
