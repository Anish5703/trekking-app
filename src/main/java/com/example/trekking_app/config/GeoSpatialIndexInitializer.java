package com.example.trekking_app.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GeoSpatialIndexInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public GeoSpatialIndexInitializer(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void run(ApplicationArguments args) {
        // Hibernate creates the tables; PostgreSQL-specific GiST indexes are created here because JPA @Index cannot express USING GIST.
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_geo_admin_boundary_geom_gist ON geo_admin_boundary USING GIST (geom)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_geo_protected_area_geom_gist ON geo_protected_area USING GIST (geom)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_geo_admin_boundary_dataset_level ON geo_admin_boundary (dataset_id, boundary_level)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_geo_protected_area_dataset ON geo_protected_area (dataset_id)");
    }
}
