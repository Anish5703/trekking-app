package com.example.trekking_app.service;

import com.example.trekking_app.dto.geoOffline.LocationContextResponse;
import com.example.trekking_app.dto.geoOffline.LocationContextResponse.Boundary;
import com.example.trekking_app.dto.geoOffline.LocationContextResponse.ProtectedAreaSummary;
import com.example.trekking_app.dto.global.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GeoOfflineService {
    private final JdbcTemplate jdbc;
    public GeoOfflineService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public ApiResponse<LocationContextResponse> locate(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        String point = "ST_SetSRID(ST_MakePoint(?, ?), 4326)";
        Boundary municipality = firstBoundary("MUNICIPALITY", point, longitude, latitude);
        Boundary district = firstBoundary("DISTRICT", point, longitude, latitude);
        Boundary province = firstBoundary("PROVINCE", point, longitude, latitude);
        List<ProtectedAreaSummary> protectedAreas = jdbc.query(
            "SELECT pa.source_code, pa.display_name, pa.area_type, pa.area_sq_km " +
            "FROM geo_protected_area pa JOIN geo_dataset gd ON gd.id=pa.dataset_id " +
            "WHERE gd.active=true AND ST_Covers(pa.geom, " + point + ") ORDER BY pa.display_name",
            (rs, row) -> new ProtectedAreaSummary(rs.getString(1), rs.getString(2), rs.getString(3),
                (rs.getObject(4) == null ? null : ((Number) rs.getObject(4)).doubleValue())), longitude, latitude);
        LocationContextResponse response = new LocationContextResponse(latitude, longitude, municipality, district, province, protectedAreas);
        return new ApiResponse<>(response,"route located",200);
    }

    private Boundary firstBoundary(String level, String point, double longitude, double latitude) {
        List<Boundary> rows = jdbc.query(
            "SELECT b.source_code, b.display_name FROM geo_admin_boundary b " +
            "JOIN geo_dataset gd ON gd.id=b.dataset_id " +
            "WHERE gd.active=true AND b.boundary_level=? AND ST_Covers(b.geom, " + point + ") " +
            "ORDER BY b.id ASC LIMIT 1",
            (rs, row) -> new Boundary(rs.getString(1), rs.getString(2)), level, longitude, latitude);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new GeoOfflineException(400, "INVALID_COORDINATES", "Latitude or longitude is outside the valid range.");
        }
    }
}
