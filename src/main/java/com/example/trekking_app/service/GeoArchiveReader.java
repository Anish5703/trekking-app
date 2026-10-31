package com.example.trekking_app.service;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.Serializable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.InputStream;

@Component
public class GeoArchiveReader {
    private static final long MAX_UNZIPPED_BYTES = 1_000_000_000L;
    private static final int MAX_ENTRIES = 10_000;
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public ParsedArchives read(Path adminZip, Path protectedZip) throws IOException {
        Path root = Files.createTempDirectory("geoffline-import-");
        try {
            Path adminDir = extractZipSafely(adminZip, root.resolve("admin"));
            Path paDir = extractZipSafely(protectedZip, root.resolve("protected"));
            Path provinceFile = findShapefile(adminDir, p -> suffix(p, "_1"));
            Path districtFile = findShapefile(adminDir, p -> suffix(p, "_2"));
            Path localFile = findShapefile(adminDir, p -> suffix(p, "_3"));
            Path paGeometryFile = findShapefile(paDir, p -> p.getFileName().toString().equalsIgnoreCase("PAs.shp"));
            Path paTypesFile = findShapefile(paDir, p -> p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("pas20"));
            List<RawFeature> provinces = readPolygonLayer(provinceFile);
            List<RawFeature> districts = readPolygonLayer(districtFile);
            List<RawFeature> locals = readPolygonLayer(localFile);
            List<RawFeature> paGeometry = readPolygonLayer(paGeometryFile);
            Map<String,String> paTypes = readProtectedAreaTypes(paTypesFile);
            return new ParsedArchives(provinces, districts, locals, paGeometry, paTypes,
                    sha256(adminZip), sha256(protectedZip));
        } finally { deleteTree(root); }
    }

    private Path extractZipSafely(Path zip, Path target) throws IOException {
        Files.createDirectories(target);
        long total = 0; int count = 0;
        try (InputStream raw = Files.newInputStream(zip); ZipInputStream in = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) throw new IOException("Archive contains too many entries.");
                String name = entry.getName().replace('\\','/');
                if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) throw new IOException("Unsafe archive path.");
                Path output = target.resolve(name).normalize();
                if (!output.startsWith(target.normalize())) throw new IOException("Archive path traversal detected.");
                if (entry.isDirectory()) { Files.createDirectories(output); continue; }
                Files.createDirectories(output.getParent());
                try (var out = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) > 0) {
                        total += n;
                        if (total > MAX_UNZIPPED_BYTES) throw new IOException("Archive expands beyond the configured size limit.");
                        out.write(buffer, 0, n);
                    }
                }
            }
        }
        return target;
    }

    private Path findShapefile(Path root, java.util.function.Predicate<Path> predicate) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".shp"))
                    .filter(predicate).findFirst().orElseThrow(() -> new IOException("Required shapefile is missing in archive: " + root.getFileName()));
        }
    }
    private boolean suffix(Path p, String suffix) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(suffix.toLowerCase(Locale.ROOT) + ".shp");
    }

    private List<RawFeature> readPolygonLayer(Path file) throws IOException {
        Map<String, Serializable> params = new HashMap<>(); params.put("url", file.toUri().toURL()); params.put("charset", StandardCharsets.UTF_8.name());
        DataStore store = DataStoreFinder.getDataStore(params);
        if (store == null) throw new IOException("GeoTools could not open shapefile: " + file.getFileName());
        try {
            String typeName = store.getTypeNames()[0];
            SimpleFeatureSource source = store.getFeatureSource(typeName);
            CoordinateReferenceSystem sourceCrs = source.getSchema().getCoordinateReferenceSystem();
            if (sourceCrs == null) throw new IOException("Shapefile has no CRS (.prj missing): " + file.getFileName());
            CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4326", true);
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
            List<RawFeature> result = new ArrayList<>();
            try (SimpleFeatureIterator it = source.getFeatures().features()) {
                while (it.hasNext()) {
                    SimpleFeature feature = it.next();
                    Object defaultGeometry = feature.getDefaultGeometry();
                    if (!(defaultGeometry instanceof Geometry input)) continue;
                    Geometry transformed = JTS.transform(input, transform);
                    // Round-trip through a 2D WKB writer so POLYGONZ inputs cannot leak Z ordinates into the database.
                    Geometry geometry = new WKBReader(GF).read(new WKBWriter(2).write(transformed));
                    geometry = GeometryFixer.fix(geometry);
                    geometry = polygonalOnly(geometry);
                    if (geometry == null || geometry.isEmpty()) continue;
                    if (!geometry.isValid()) throw new IOException("Geometry remains invalid after repair in " + file.getFileName() + ".");
                    geometry.setSRID(4326);
                    Map<String,Object> attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (AttributeDescriptor d : source.getSchema().getAttributeDescriptors()) {
                        String name = d.getLocalName();
                        if (!name.equalsIgnoreCase(source.getSchema().getGeometryDescriptor().getLocalName())) attrs.put(name, feature.getAttribute(name));
                    }
                    result.add(new RawFeature(attrs, asMultiPolygon(geometry)));
                }
            }
            return result;
        } catch (Exception e) {
            if (e instanceof IOException io) throw io;
            throw new IOException("Failed reading shapefile " + file.getFileName() + ": " + e.getMessage(), e);
        } finally { store.dispose(); }
    }

    private Map<String,String> readProtectedAreaTypes(Path file) throws IOException {
        Map<String,String> result = new HashMap<>();
        for (RawFeature row : readPolygonLayer(file)) {
            String code = text(row.attributes, "PAs_Name", "PA_NAME", "CODE", "NAME");
            String type = text(row.attributes, "Type", "PA_TYPE", "TYPE");
            if (code != null && type != null) result.put(code.trim(), type.trim());
        }
        return result;
    }
    private static Geometry polygonalOnly(Geometry g) {
        if (g instanceof Polygon || g instanceof MultiPolygon) return g;
        List<Polygon> polygons = new ArrayList<>();
        collectPolygons(g, polygons);
        return polygons.isEmpty() ? null : GF.createMultiPolygon(polygons.toArray(Polygon[]::new));
    }
    private static void collectPolygons(Geometry g, List<Polygon> out) {
        if (g instanceof Polygon p) { out.add(p); return; }
        for (int i=0; i<g.getNumGeometries(); i++) {
            Geometry child = g.getGeometryN(i);
            if (child != g) collectPolygons(child, out);
        }
    }
    private static MultiPolygon asMultiPolygon(Geometry g) {
        if (g instanceof MultiPolygon mp) return mp;
        if (g instanceof Polygon p) return GF.createMultiPolygon(new Polygon[]{p});
        throw new IllegalArgumentException("Geometry is not polygonal.");
    }
    static String text(Map<String,Object> a, String... candidates) {
        for (String c : candidates) { Object v = a.get(c); if (v != null && !v.toString().isBlank()) return v.toString().trim(); }
        return null;
    }
    private String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) { byte[] b = new byte[8192]; int n; while ((n=in.read(b))>0) md.update(b,0,n); }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void deleteTree(Path root) {
        try (var paths = Files.walk(root)) { paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} }); }
        catch (IOException ignored) {}
    }

    public record RawFeature(Map<String,Object> attributes, MultiPolygon geometry) {}
    public record ParsedArchives(List<RawFeature> provinces, List<RawFeature> districts, List<RawFeature> locals,
                                 List<RawFeature> protectedAreas, Map<String,String> protectedAreaTypes,
                                 String adminSha256, String protectedAreaSha256) {}
}
