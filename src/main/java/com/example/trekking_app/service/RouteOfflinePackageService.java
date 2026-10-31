package com.example.trekking_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.trekking_app.config.GeoOfflineProperties;
import com.example.trekking_app.dto.geoOffline.OfflinePackageResponse;
import com.example.trekking_app.util.GeoHash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Slf4j
@Service
public class RouteOfflinePackageService {
    private static final String SCHEMA_VERSION = "1";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final GeoOfflineProperties props;
    private final String routeTable, routeIdColumn, routeGeometryColumn, routePublishedColumn;
    private final ReentrantLock cacheLock = new ReentrantLock();
    private final LinkedHashMap<String, CachedPackage> cache = new LinkedHashMap<>(32, .75f, true);
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final long MAX_CACHE_BYTES = 64L * 1024 * 1024;
    private long cachedBytes = 0;

    public RouteOfflinePackageService(JdbcTemplate jdbc, ObjectMapper mapper, GeoOfflineProperties props) {
        this.jdbc=jdbc; this.mapper=mapper; this.props=props;
        this.routeTable=identifier(props.getRouteTable(), true);
        this.routeIdColumn=identifier(props.getRouteIdColumn(), false);
        this.routeGeometryColumn=identifier(props.getRouteGeometryColumn(), false);
        this.routePublishedColumn=identifier(props.getRoutePublishedColumn(), false);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OfflinePackageResponse getPackage(int routeId, Double corridorInput, Double simplifyInput, String ifNoneMatch) {
        if (!props.isEnabled()) throw new GeoOfflineException(503, "GEO_OFFLINE_DISABLED", "Offline geographic packages are disabled.");
        if (routeId <= 0) throw new GeoOfflineException(400, "INVALID_ROUTE_ID", "Route id must be positive.");
        double corridor = corridorInput == null ? props.getDefaultCorridorMeters() : corridorInput;
        double simplify = simplifyInput == null ? props.getDefaultSimplifyMeters() : simplifyInput;
        if (!Double.isFinite(corridor) || corridor < props.getMinCorridorMeters() || corridor > props.getMaxCorridorMeters())
            throw new GeoOfflineException(400, "INVALID_CORRIDOR", "corridorMeters is outside the allowed range.");
        if (!Double.isFinite(simplify) || simplify < 0 || simplify > props.getMaxSimplifyMeters())
            throw new GeoOfflineException(400, "INVALID_SIMPLIFICATION", "simplifyMeters is outside the allowed range.");

        RouteFingerprint route = routeFingerprint(routeId);
        if (route.lengthKm == null || route.lengthKm <= 0) throw new GeoOfflineException(422, "ROUTE_GEOMETRY_MISSING", "This route does not have a usable line geometry.");
        if (route.lengthKm > props.getMaxRouteLengthKm()) throw new GeoOfflineException(422, "ROUTE_TOO_LONG", "The route exceeds the offline-package length limit.");
        String dataFingerprint = datasetFingerprint();
        String keyMaterial = "geo-offline|" + SCHEMA_VERSION + "|route=" + routeId + "|" + route.geometryHash + "|" + dataFingerprint + "|" + corridor + "|" + simplify;
        String etag = "\"" + GeoHash.sha256(keyMaterial) + "\"";
        if (matchesEtag(ifNoneMatch, etag)) return new OfflinePackageResponse(new byte[0], etag, -1, true);
        CachedPackage cached = getCached(etag);
        if (cached != null) return new OfflinePackageResponse(cached.bytes, etag, cached.bytes.length, false);

        byte[] body = buildPackage(routeId, route.lengthKm, corridor, simplify, dataFingerprint);
        if (body.length > props.getMaxPackageBytes()) throw new GeoOfflineException(413, "OFFLINE_PACKAGE_TOO_LARGE", "The generated offline package exceeds the configured size limit.");
        putCached(etag, body);
        return new OfflinePackageResponse(body, etag, body.length, false);
    }

    private RouteFingerprint routeFingerprint(long routeId)  {
            String sql = "SELECT md5(ST_AsEWKB(" + routeGeometryColumn + ")) AS geom_hash, " +
                    "ST_GeometryType(" + routeGeometryColumn + ") AS geom_type, ST_SRID(" + routeGeometryColumn + ") AS srid, " +
                    "CASE WHEN ST_GeometryType(" + routeGeometryColumn + ")='ST_LineString' AND ST_SRID(" + routeGeometryColumn + ")=4326 THEN ST_Length(" + routeGeometryColumn + "::geography)/1000.0 ELSE NULL END AS length_km " +
                    "FROM " + routeTable + " WHERE " + routeIdColumn + "=? AND " + routePublishedColumn + "=TRUE";
            List<RouteFingerprint> rows = query(sql, ps -> {
                try {
                    ps.setLong(1, routeId);
                } catch (SQLException e) {
                    log.error(e.getMessage());
                    throw new IllegalArgumentException("Operation failed");
                }
            }, (rs, n) -> {
                String type = rs.getString("geom_type");
                if (type == null || rs.getObject("length_km") == null)
                    return new RouteFingerprint(rs.getString("geom_hash"), null, type, rs.getInt("srid"));
                return new RouteFingerprint(rs.getString("geom_hash"), rs.getDouble("length_km"), type, rs.getInt("srid"));
            });
            if (rows.isEmpty())
                throw new GeoOfflineException(404, "RESOURCE_NOT_FOUND", "No geo-offline package  was found for this route.");
            RouteFingerprint result = rows.get(0);
            if (result.geometryHash == null || result.geometryType == null || !result.geometryType.equalsIgnoreCase("ST_LineString") || result.srid != 4326)
                throw new GeoOfflineException(422, "ROUTE_GEOMETRY_MISSING", "The route geometry must be a non-empty LineString in EPSG:4326.");
            return result;
    }

    private String datasetFingerprint() {
        List<DatasetFingerprint> rows = query("SELECT count(*) AS approved_count, " +
                        "string_agg(dataset_key || ':' || COALESCE(content_sha256, '') || ':' || COALESCE(source_version, '') || ':' || COALESCE(source_name, '') || ':' || COALESCE(license_name, '') || ':' || COALESCE(attribution, ''), ',' ORDER BY dataset_key) AS fp " +
                        "FROM geo_dataset WHERE active=TRUE " +
                        "AND dataset_key IN ('nepal-admin-boundaries','nepal-protected-areas')",
                ps -> {}, (rs, n) -> new DatasetFingerprint(rs.getInt("approved_count"), rs.getString("fp")));
        if (rows.isEmpty() || rows.get(0).approvedCount != 2 || rows.get(0).fingerprint == null || rows.get(0).fingerprint.isBlank())
            throw new GeoOfflineException(503, "GEO_DATA_NOT_APPROVED", "Both required geographic datasets must be active and explicitly approved for redistribution.");
        return rows.get(0).fingerprint;
    }

    private byte[] buildPackage(long routeId, double routeLengthKm, double corridor, double simplify, String dataFingerprint) {
        ObjectNode root = mapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION); root.put("routeId", routeId); root.put("routeLengthKm", round3(routeLengthKm));
        root.put("corridorMeters", corridor); root.put("simplifyMeters", simplify); root.put("datasetFingerprint", dataFingerprint);
        ArrayNode sources = root.putArray("sources");
        List<DatasetSource> sourceRows = query("SELECT dataset_key, source_name, source_version, license_name, attribution FROM geo_dataset " +
                        "WHERE active=TRUE AND  dataset_key IN ('nepal-admin-boundaries','nepal-protected-areas') ORDER BY dataset_key",
                ps -> {}, (rs, n) -> new DatasetSource(rs.getString("dataset_key"), rs.getString("source_name"), rs.getString("source_version"), rs.getString("license_name"), rs.getString("attribution")));
        for (DatasetSource source : sourceRows) {
            ObjectNode item = sources.addObject(); item.put("datasetKey", source.key); item.put("sourceName", source.name);
            if (source.version != null) item.put("sourceVersion", source.version); if (source.license != null) item.put("license", source.license);
            if (source.attribution != null) item.put("attribution", source.attribution);
        }
        ArrayNode boundaryArray = root.putArray("boundaries");
        Map<String,FeatureRef> refs = new LinkedHashMap<>();
        String boundarySql =
                "WITH r AS ( " +
                        "    SELECT " + routeGeometryColumn + " AS geom " +
                        "    FROM " + routeTable + " " +
                        "    WHERE " + routeIdColumn + "=? " +
                        "      AND " + routePublishedColumn + "=TRUE " +
                        "), " +
                        "c AS ( " +
                        "    SELECT " +
                        "        ST_Transform(ST_Buffer(geom::geography, ?)::geometry, 3857) AS geom3857, " +
                        "        ST_Transform( " +
                        "            ST_Transform(ST_Buffer(geom::geography, ?)::geometry, 3857), " +
                        "            4326 " +
                        "        ) AS geom4326 " +
                        "    FROM r " +
                        "), " +
                        "candidates AS ( " +
                        "    SELECT " +
                        "        b.boundary_level, " +
                        "        b.source_code, " +
                        "        b.display_name, " +
                        "        b.district_name, " +
                        "        b.province_name, " +
                        "        ST_Transform(b.geom, 3857) AS geom3857 " +
                        "    FROM geo_admin_boundary b " +
                        "    JOIN geo_dataset gd ON gd.id = b.dataset_id " +
                        "    CROSS JOIN c " +
                        "    WHERE gd.active = TRUE " +
                        "      AND ST_Intersects(b.geom, c.geom4326) " +
                        "), " +
                        "clipped AS MATERIALIZED ( " +
                        "    SELECT " +
                        "        b.boundary_level, " +
                        "        b.source_code, " +
                        "        b.display_name, " +
                        "        b.district_name, " +
                        "        b.province_name, " +
                        "        ST_CollectionExtract( " +
                        "            ST_MakeValid( " +
                        "                ST_Intersection(b.geom3857, c.geom3857) " +
                        "            ), 3 " +
                        "        ) AS geom " +
                        "    FROM candidates b " +
                        "    CROSS JOIN c " +
                        ") " +
                        "SELECT " +
                        "    boundary_level, " +
                        "    source_code, " +
                        "    display_name, " +
                        "    district_name, " +
                        "    province_name, " +
                        "    ST_AsGeoJSON( " +
                        "        ST_Transform( " +
                        "            ST_SimplifyPreserveTopology(geom, ?), " +
                        "            4326 " +
                        "        ), 6, 0 " +
                        "    ) AS geometry_json " +
                        "FROM clipped " +
                        "WHERE NOT ST_IsEmpty(geom) " +
                        "ORDER BY boundary_level, source_code";
        List<BoundaryFeature> boundaries = query(
                boundarySql,
                ps -> {
                    try {
                        ps.setLong(1, routeId);
                        ps.setDouble(2, corridor);
                        ps.setDouble(3, corridor);
                        ps.setDouble(4, simplify);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException(
                                "Failed to bind administrative boundary query parameters.",
                                e
                        );
                    }
                },
                (rs, n) -> new BoundaryFeature(
                        rs.getString("boundary_level"),
                        rs.getString("source_code"),
                        rs.getString("display_name"),
                        rs.getString("district_name"),
                        rs.getString("province_name"),
                        rs.getString("geometry_json")
                )
        );
        for (BoundaryFeature f : boundaries) {
            if (f.geometryJson == null || f.geometryJson.equals("null")) continue;
            String id = "ADMIN:" + f.level + ":" + f.code;
            ObjectNode feature = feature(id, f.geometryJson);
            ObjectNode p = feature.putObject("properties"); p.put("kind", "ADMIN"); p.put("level", f.level); p.put("sourceCode", f.code); p.put("name", f.name);
            if (f.district != null) p.put("districtName", f.district); if (f.province != null) p.put("provinceName", f.province);
            boundaryArray.add(feature); refs.put(id, new FeatureRef(id, "ADMIN", f.level, f.code, f.name));
        }

        ArrayNode paArray = root.putArray("protectedAreas");
        String paSql = "WITH r AS (SELECT " + routeGeometryColumn + " AS geom FROM " + routeTable + " WHERE " + routeIdColumn + "=? AND " + routePublishedColumn + "=TRUE), " +
                "c AS (SELECT ST_Transform(ST_Buffer(geom::geography, ?)::geometry,3857) AS geom FROM r) " +
                "SELECT p.source_code, p.display_name, p.area_type, p.area_sq_km, " +
                "ST_AsGeoJSON(ST_Transform(ST_SimplifyPreserveTopology(ST_CollectionExtract(ST_MakeValid(ST_Intersection(ST_Transform(p.geom,3857), c.geom)),3), ?),4326),6,0) AS geometry_json " +
                "FROM geo_protected_area p JOIN geo_dataset gd ON gd.id=p.dataset_id CROSS JOIN c " +
                "WHERE gd.active=TRUE  AND ST_Intersects(ST_Transform(p.geom,3857), c.geom) AND NOT ST_IsEmpty(ST_CollectionExtract(ST_MakeValid(ST_Intersection(ST_Transform(p.geom,3857), c.geom)),3)) " +
                "ORDER BY p.source_code";
        List<ProtectedFeature> pas = query(paSql, ps -> {
                    try {
                        ps.setLong(1, routeId);
                    } catch (SQLException e) {
                        log.error(e.getMessage());
                        throw new IllegalArgumentException("Operation failed");
                    }
                    try {
                        ps.setDouble(2, corridor);
                    } catch (SQLException e) {
                        log.error(e.getMessage());
                        throw new IllegalArgumentException("Operation failed");
                    }
                    try {
                        ps.setDouble(3, simplify);
                    } catch (SQLException e) {
                        log.error(e.getMessage());
                        throw new IllegalArgumentException("Operation failed");                    }
                },
                (rs, n) -> new ProtectedFeature(rs.getString("source_code"), rs.getString("display_name"), rs.getString("area_type"),
                        nullableDouble(rs, "area_sq_km"), rs.getString("geometry_json")));
        for (ProtectedFeature f : pas) {
            if (f.geometryJson == null || f.geometryJson.equals("null")) continue;
            String id = "PA:" + f.code;
            ObjectNode feature = feature(id, f.geometryJson); ObjectNode p = feature.putObject("properties");
            p.put("kind", "PROTECTED_AREA"); p.put("sourceCode", f.code); p.put("name", f.name);
            if (f.type != null) p.put("areaType", f.type); if (f.areaSqKm != null) p.put("areaSqKm", f.areaSqKm);
            paArray.add(feature); refs.put(id, new FeatureRef(id, "PROTECTED_AREA", null, f.code, f.name));
        }
        ArrayNode segments = root.putArray("segments");
        String segmentSql = "WITH r AS (SELECT " + routeGeometryColumn + " AS geom FROM " + routeTable + " WHERE " + routeIdColumn + "=? AND " + routePublishedColumn + "=TRUE), " +
                "all_areas AS (SELECT 'ADMIN' AS kind, b.boundary_level AS area_level, b.source_code AS code, b.display_name AS name, b.geom " +
                "FROM geo_admin_boundary b JOIN geo_dataset gd ON gd.id=b.dataset_id WHERE gd.active=TRUE  " +
                "UNION ALL SELECT 'PROTECTED_AREA', NULL, p.source_code, p.display_name, p.geom FROM geo_protected_area p JOIN geo_dataset gd ON gd.id=p.dataset_id WHERE gd.active=TRUE AND gd.redistribution_allowed=TRUE), " +
                "pieces AS (SELECT a.kind, a.area_level, a.code, a.name, (d).geom AS geom, r.geom AS route_geom " +
                "FROM r JOIN all_areas a ON ST_Intersects(r.geom,a.geom) CROSS JOIN LATERAL ST_Dump(ST_CollectionExtract(ST_Intersection(r.geom,a.geom),2)) d " +
                "WHERE NOT ST_IsEmpty((d).geom) AND ST_Length((d).geom::geography)>1) " +
                "SELECT kind, area_level, code, name, LEAST(ST_LineLocatePoint(route_geom, ST_StartPoint(geom)), ST_LineLocatePoint(route_geom, ST_EndPoint(geom)))*ST_Length(route_geom::geography)/1000.0 AS start_km, " +
                "GREATEST(ST_LineLocatePoint(route_geom, ST_StartPoint(geom)), ST_LineLocatePoint(route_geom, ST_EndPoint(geom)))*ST_Length(route_geom::geography)/1000.0 AS end_km " +
                "FROM pieces ORDER BY start_km, end_km, kind, code";
        List<SegmentRow> segmentRows = query(segmentSql, ps -> {
            try {
                ps.setLong(1, routeId);
            } catch (SQLException e) {
                log.error(e.getMessage());
                throw new IllegalArgumentException("Operation failed");
            }
        }, (rs, n) -> new SegmentRow(
                rs.getString("kind"), rs.getString("area_level"), rs.getString("code"), rs.getString("name"),
                rs.getDouble("start_km"), rs.getDouble("end_km")));
        Map<String,Integer> sequence = new HashMap<>();
        for (SegmentRow s : segmentRows) {
            String id = "ADMIN".equals(s.kind) ? "ADMIN:" + s.level + ":" + s.code : "PA:" + s.code;
            if (!refs.containsKey(id)) continue;
            ObjectNode item = segments.addObject(); item.put("featureId", id); item.put("kind", s.kind);
            item.put("segmentNo", sequence.merge(id, 1, Integer::sum)); item.put("startKm", round3(s.startKm)); item.put("endKm", round3(s.endKm));
            item.put("name", s.name);
        }
        try {
            byte[] bytes = mapper.writeValueAsBytes(root);
            if (bytes.length > props.getMaxPackageBytes()) throw new GeoOfflineException(413, "OFFLINE_PACKAGE_TOO_LARGE", "The generated offline package exceeds the configured size limit.");
            return bytes;
        } catch (IOException e) { throw new IllegalStateException("Could not serialize offline package.", e); }
    }

    private ObjectNode feature(String id, String geometryJson) {
        ObjectNode feature = mapper.createObjectNode(); feature.put("type", "Feature"); feature.put("id", id);
        try { JsonNode geometry = mapper.readTree(geometryJson); feature.set("geometry", geometry); }
        catch (IOException e) { throw new IllegalStateException("PostGIS returned invalid GeoJSON.", e); }
        return feature;
    }
    private <T> List<T> query(String sql, Consumer<PreparedStatement> binder, RowMapper<T> mapper) {
        PreparedStatementCreator creator = connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setQueryTimeout(props.getQueryTimeoutSeconds()); binder.accept(ps); return ps;
        };
        return jdbc.query(creator, mapper);
    }
    private boolean matchesEtag(String supplied, String current) {
        if (supplied == null || supplied.isBlank()) return false;
        for (String token : supplied.split(",")) {
            String t = token.trim(); if (t.equals("*") || t.equals(current) || t.equals("W/" + current)) return true;
        }
        return false;
    }
    private CachedPackage getCached(String key) {
        cacheLock.lock(); try { return cache.get(key); } finally { cacheLock.unlock(); }
    }
    private void putCached(String key, byte[] body) {
        cacheLock.lock();
        try {
            CachedPackage previous = cache.put(key, new CachedPackage(body));
            if (previous != null) cachedBytes -= previous.bytes.length;
            cachedBytes += body.length;
            while (cache.size() > MAX_CACHE_ENTRIES || cachedBytes > MAX_CACHE_BYTES) {
                String oldest = cache.keySet().iterator().next();
                CachedPackage removed = cache.remove(oldest); if (removed != null) cachedBytes -= removed.bytes.length;
            }
        } finally { cacheLock.unlock(); }
    }
    private String identifier(String value, boolean table) {
        String pattern = table ? "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?" : "[A-Za-z_][A-Za-z0-9_]*";
        if (value == null || !value.matches(pattern)) throw new IllegalArgumentException("Unsafe SQL identifier in geo.offline configuration.");
        return value;
    }
    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column); return value == null ? null : ((Number) value).doubleValue();
    }
    private double round3(double n) { return Math.round(n * 1000.0) / 1000.0; }

    private record RouteFingerprint(String geometryHash, Double lengthKm, String geometryType, int srid) {}
    private record DatasetFingerprint(int approvedCount, String fingerprint) {}
    private record DatasetSource(String key, String name, String version, String license, String attribution) {}
    private record BoundaryFeature(String level, String code, String name, String district, String province, String geometryJson) {}
    private record ProtectedFeature(String code, String name, String type, Double areaSqKm, String geometryJson) {}
    private record SegmentRow(String kind, String level, String code, String name, double startKm, double endKm) {}
    private record FeatureRef(String id, String kind, String level, String code, String name) {}
    private record CachedPackage(byte[] bytes) {}
}
