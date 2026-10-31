package com.example.trekking_app.controller;

import com.example.trekking_app.dto.geoOffline.GeoImportResult;
import com.example.trekking_app.service.GeoDatasetApprovalService;
import com.example.trekking_app.service.GeoDatasetImportService;
import com.example.trekking_app.service.GeoOfflineException;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/geo")
public class AdminGeoController {
    private final GeoDatasetImportService importer;
    private final GeoDatasetApprovalService approvals;
    public AdminGeoController(GeoDatasetImportService importer, GeoDatasetApprovalService approvals) { this.importer=importer; this.approvals=approvals; }

    @PostMapping(value="/import", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public GeoImportResult importArchives(@RequestPart("adminBoundariesZip") MultipartFile adminZip,
            @RequestPart("protectedAreasZip") MultipartFile protectedZip, @RequestParam String version) throws IOException {
        Path admin = null, protectedAreas = null;
        try {
            admin = saveTemporaryZip(adminZip, "admin-"); protectedAreas = saveTemporaryZip(protectedZip, "protected-");
            try { return importer.importArchives(admin, protectedAreas, version); }
            catch (IOException e) { throw new GeoOfflineException(422, "INVALID_GEO_ARCHIVE", "Could not read the geographic ZIP archives. Verify the expected shapefiles, CRS sidecars, and archive contents."); }
        } finally {
            if (admin != null) Files.deleteIfExists(admin);
            if (protectedAreas != null) Files.deleteIfExists(protectedAreas);
        }
    }

    @PatchMapping("/datasets/{datasetKey}/redistribution")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> setRedistribution(@PathVariable String datasetKey, @RequestBody RedistributionApproval request) {
        if (request == null) throw new GeoOfflineException(400, "INVALID_APPROVAL_REQUEST", "Approval request is required.");
        if (request.allowed() && (request.approvalReference() == null || request.approvalReference().isBlank()))
            throw new GeoOfflineException(400, "APPROVAL_REFERENCE_REQUIRED", "approvalReference must document the permission/source of approval.");
        approvals.setRedistributionApproval(datasetKey, request.allowed(), request.approvalReference());
        return Map.of("datasetKey", datasetKey, "redistributionAllowed", request.allowed());
    }

    private Path saveTemporaryZip(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) throw new GeoOfflineException(400, "GEO_ARCHIVES_REQUIRED", "Both ZIP archives are required.");
        if (file.getSize() > 250L * 1024 * 1024) throw new GeoOfflineException(413, "GEO_ARCHIVE_TOO_LARGE", "Each ZIP archive must be 250 MB or smaller.");
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) throw new GeoOfflineException(400, "INVALID_GEO_ARCHIVE_TYPE", "Only ZIP archives are accepted.");
        Path temp = Files.createTempFile(prefix, ".zip");
        try (var in = file.getInputStream()) { Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING); return temp; }
        catch (IOException | RuntimeException e) { Files.deleteIfExists(temp); throw e; }
    }
    public record RedistributionApproval(boolean allowed, String approvalReference) {}
}
