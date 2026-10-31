package com.example.trekking_app.controller;

import com.example.trekking_app.config.GeoOfflineProperties;
import com.example.trekking_app.dto.geoOffline.OfflinePackageManifest;
import com.example.trekking_app.dto.geoOffline.OfflinePackageResponse;
import com.example.trekking_app.service.RouteOfflinePackageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/routes/{routeId}/offline-package")
public class RouteOfflinePackageController {
    private final RouteOfflinePackageService service;
    private final GeoOfflineProperties properties;
    public RouteOfflinePackageController(RouteOfflinePackageService service, GeoOfflineProperties properties) { this.service=service; this.properties=properties; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> getPackage(@PathVariable int routeId,
            @RequestParam(required=false) Double corridorMeters,
            @RequestParam(required=false) Double simplifyMeters,
            @RequestHeader(value=HttpHeaders.IF_NONE_MATCH, required=false) String ifNoneMatch) {
        OfflinePackageResponse result = service.getPackage(routeId, corridorMeters, simplifyMeters, ifNoneMatch);
        if (result.notModified()) return ResponseEntity.status(304).eTag(unquote(result.etag()))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).contentLength(result.contentLength())
                .eTag(unquote(result.etag())).cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header("Vary", "Accept-Encoding").body(result.body());
    }

    @GetMapping(value="/manifest", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OfflinePackageManifest> manifest(@PathVariable int routeId,
            @RequestParam(required=false) Double corridorMeters, @RequestParam(required=false) Double simplifyMeters) {
        OfflinePackageResponse result = service.getPackage(routeId, corridorMeters, simplifyMeters, null);
        return ResponseEntity.ok().eTag(unquote(result.etag())).cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(new OfflinePackageManifest(routeId, result.etag(), result.contentLength(),
                        corridorMeters == null ? properties.getDefaultCorridorMeters() : corridorMeters,
                        simplifyMeters == null ? properties.getDefaultSimplifyMeters() : simplifyMeters, "1"));
    }
    private String unquote(String etag) { return etag != null && etag.length()>=2 && etag.startsWith("\"") && etag.endsWith("\"") ? etag.substring(1,etag.length()-1) : etag; }
}
