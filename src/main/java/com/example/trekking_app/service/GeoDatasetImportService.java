package com.example.trekking_app.service;

import com.example.trekking_app.dto.geoOffline.GeoImportResult;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class GeoDatasetImportService {
    private final GeoArchiveReader reader;
    private final GeoDatasetPersistenceService persistence;
    public GeoDatasetImportService(GeoArchiveReader reader, GeoDatasetPersistenceService persistence) {
        this.reader=reader; this.persistence=persistence;
    }
    public GeoImportResult importArchives(Path adminZip, Path protectedAreasZip, String version) throws IOException {
        if (version == null || version.isBlank() || version.length() > 100) throw new GeoOfflineException(400, "DATASET_VERSION_REQUIRED", "A dataset version label (max 100 characters) is required.");
        GeoArchiveReader.ParsedArchives parsed = reader.read(adminZip, protectedAreasZip);
        var counts = persistence.replaceAll(parsed, version.trim());
        return new GeoImportResult("IMPORTED", counts.provinces(), counts.districts(), counts.municipalities(),
                counts.protectedAreas(), parsed.adminSha256(), parsed.protectedAreaSha256(), counts.importedAt());
    }
}
