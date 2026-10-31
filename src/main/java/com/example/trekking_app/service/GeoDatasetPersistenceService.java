package com.example.trekking_app.service;

import com.example.trekking_app.entity.*;
import com.example.trekking_app.repository.*;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKBWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class GeoDatasetPersistenceService {
    private final GeoDatasetRepository datasets;
    private final AdminBoundaryRepository boundaries;
    private final ProtectedAreaRepository protectedAreas;
    private final ObjectMapper objectMapper;

    public GeoDatasetPersistenceService(GeoDatasetRepository datasets, AdminBoundaryRepository boundaries,
                                        ProtectedAreaRepository protectedAreas, ObjectMapper objectMapper) {
        this.datasets=datasets; this.boundaries=boundaries; this.protectedAreas=protectedAreas; this.objectMapper=objectMapper;
    }

    @Transactional
    public ImportCounts replaceAll(GeoArchiveReader.ParsedArchives data, String version) {
        GeoDataset admin = dataset("nepal-admin-boundaries", "HERMES Nepal administrative boundaries",
                version, "See source archive license.txt", "Source attribution required; redistribution disabled pending approval.");
        GeoDataset pa = dataset("nepal-protected-areas", "Protected Areas of Nepal", version,
                "See source archive license.txt", "Source attribution required; redistribution disabled pending approval.");
        boundaries.deleteForDataset(admin.getId());
        protectedAreas.deleteForDataset(pa.getId());
        boundaries.flush(); protectedAreas.flush();

        List<NamedPolygon> provinces = named(data.provinces(), AdminBoundary.Level.PROVINCE, "PR_NAME", "PROVINCE", "NAME");
        List<NamedPolygon> districts = named(data.districts(), AdminBoundary.Level.DISTRICT, "DISTRICT", "DIST_NAME", "NAME");
        List<NamedPolygon> locals = new ArrayList<>();
        for (GeoArchiveReader.RawFeature row : data.locals()) {
            String type = GeoArchiveReader.text(row.attributes(), "TYPE", "LOCAL_TYPE", "CATEGORY");
            if (!isMunicipality(type)) continue;
            String name = GeoArchiveReader.text(row.attributes(), "LOCAL", "LOCAL_NAME", "NAME");
            if (name == null) throw new IllegalArgumentException("Municipality feature is missing its LOCAL name field.");
            locals.add(new NamedPolygon(row, name, type, AdminBoundary.Level.MUNICIPALITY, code("MUNICIPALITY", name, row.geometry())));
        }
        if (provinces.size()!=7 || districts.size()!=77 || locals.size()!=753 || data.protectedAreas().size()!=33) {
            throw new GeoOfflineException(422, "GEO_SOURCE_VALIDATION_FAILED", "Source-data validation failed. Expected 7 provinces, 77 districts, 753 municipalities, and 33 protected areas; got " + provinces.size()+", "+districts.size()+", "+locals.size()+", "+data.protectedAreas().size()+".");
        }

        List<AdminBoundary> rows = new ArrayList<>();
        for (NamedPolygon p : provinces) rows.add(new AdminBoundary(AdminBoundary.Level.PROVINCE, p.code, p.name,
                provinceDisplayName(p.name), null, null, p.name, admin, p.row.geometry(), json(p.row.attributes())));
        for (NamedPolygon d : districts) {
            NamedPolygon parent = bestParent(d.row.geometry(), provinces);
            rows.add(new AdminBoundary(AdminBoundary.Level.DISTRICT, d.code, d.name, d.name, parent.code,
                    d.name, parent.name, admin, d.row.geometry(), json(d.row.attributes())));
        }
        for (NamedPolygon m : locals) {
            NamedPolygon parent = bestParent(m.row.geometry(), districts);
            NamedPolygon province = bestParent(m.row.geometry(), provinces);
            rows.add(new AdminBoundary(AdminBoundary.Level.MUNICIPALITY, m.code, m.name, m.name, parent.code,
                    parent.name, province.name, admin, m.row.geometry(), json(m.row.attributes())));
        }
        boundaries.saveAll(rows);
        List<ProtectedArea> paRows = new ArrayList<>();
        for (GeoArchiveReader.RawFeature f : data.protectedAreas()) {
            String code = GeoArchiveReader.text(f.attributes(), "PAs_Name", "PA_NAME", "CODE", "NAME");
            if (code == null) throw new GeoOfflineException(422, "GEO_SOURCE_FIELD_MISSING", "A protected-area feature is missing its source code.");
            String type = data.protectedAreaTypes().get(code);
            if (type == null || type.isBlank()) throw new GeoOfflineException(422, "GEO_PROTECTED_AREA_TYPE_MISSING", "Could not match a protected-area code to its category source layer.");
            String name = GeoArchiveReader.text(f.attributes(), "PA_NAME", "NAME", "PAs_Name");
            Double area = number(GeoArchiveReader.text(f.attributes(), "Area_sqkm", "AREA_SQKM", "AREA"));
            paRows.add(new ProtectedArea(code, name == null ? code : name, name == null ? code : name,
                    type, area, pa, f.geometry(), json(f.attributes())));
        }
        protectedAreas.saveAll(paRows);
        datasets.flush();
        admin.replaceImport(version, rows.size(), data.adminSha256());
        pa.replaceImport(version, paRows.size(), data.protectedAreaSha256());
        datasets.save(admin); datasets.save(pa);
        return new ImportCounts(provinces.size(), districts.size(), locals.size(), paRows.size(), Instant.now());
    }

    private GeoDataset dataset(String key, String name, String version, String license, String attribution) {
        return datasets.findByDatasetKey(key).orElseGet(() -> datasets.save(new GeoDataset(key,name,version,license,attribution)));
    }
    private List<NamedPolygon> named(List<GeoArchiveReader.RawFeature> rows, AdminBoundary.Level level, String... candidates) {
        List<NamedPolygon> result = new ArrayList<>();
        for (GeoArchiveReader.RawFeature row : rows) {
            String name = GeoArchiveReader.text(row.attributes(), candidates);
            if (name == null) throw new GeoOfflineException(422, "GEO_SOURCE_FIELD_MISSING", "A " + level + " feature is missing its expected name field.");
            result.add(new NamedPolygon(row,name,null,level,code(level.name(),name,row.geometry())));
        }
        return result;
    }
    private NamedPolygon bestParent(MultiPolygon child, List<NamedPolygon> parents) {
        NamedPolygon best = null; double maxArea = -1;
        for (NamedPolygon parent : parents) {
            if (!parent.row.geometry().getEnvelopeInternal().intersects(child.getEnvelopeInternal())) continue;
            double area;
            try { area = child.intersection(parent.row.geometry()).getArea(); }
            catch (RuntimeException e) { area = 0; }
            if (area > maxArea) { maxArea=area; best=parent; }
        }
        if (best == null || maxArea <= 0 || maxArea / Math.max(child.getArea(), 1e-15) < 0.5) throw new GeoOfflineException(422, "GEO_PARENT_MATCH_FAILED", "Could not match a geographic feature to its parent boundary with sufficient overlap.");
        return best;
    }
    private boolean isMunicipality(String type) {
        if (type == null) return false;
        String v = type.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return v.equals("gaunpalika") || v.equals("nagarpalika") || v.equals("upamahanagarpalika") || v.equals("mahanagarpalika");
    }
    private String provinceDisplayName(String name) {
        String n = name.trim();
        return switch (n.toLowerCase(Locale.ROOT)) {
            case "province no 1", "province 1" -> "Koshi";
            case "province no 2", "province 2" -> "Madhesh";
            case "province no 3", "province 3" -> "Bagmati";
            case "province no 4", "province 4" -> "Gandaki";
            case "province no 5", "province 5" -> "Lumbini";
            case "province no 6", "province 6" -> "Karnali";
            case "province no 7", "province 7" -> "Sudurpashchim";
            default -> n;
        };
    }
    private String code(String level, String name, MultiPolygon geometry) {
        // Stable source-derived key; names alone are not unique for local levels.
        String basis = level + "|" + name.trim().toLowerCase(Locale.ROOT);
        byte[] wkb = new WKBWriter(2, true).write(geometry);
        return level.substring(0, Math.min(3, level.length())) + "-" + com.example.trekking_app.util.GeoHash.sha256(basis + "|" + com.example.trekking_app.util.GeoHash.sha256(wkb)).substring(0,24);
    }
    private String json(Map<String,Object> attributes) {
        try { return objectMapper.writeValueAsString(attributes); }
        catch (JsonProcessingException e) { throw new GeoOfflineException(422, "GEO_SOURCE_ATTRIBUTES_INVALID", "Could not preserve source shapefile attributes."); }
    }
    private Double number(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Double.valueOf(value.trim()); } catch (NumberFormatException e) { return null; }
    }
    public record ImportCounts(int provinces, int districts, int municipalities, int protectedAreas, Instant importedAt) {}
    private record NamedPolygon(GeoArchiveReader.RawFeature row, String name, String type, AdminBoundary.Level level, String code) {}
}
