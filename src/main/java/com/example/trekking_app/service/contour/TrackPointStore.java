package com.example.trekking_app.service.contour;

import com.example.trekking_app.dto.contour.PointData;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.repository.TrackPointRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Short DB transactions, kept separate so the HTTP call never holds a DB connection. */
@Component
class TrackPointStore {
    private static final int ID_CHUNK = 1000;   // keeps IN (...) lists within DB limits

    private final TrackPointRepository repo;

    TrackPointStore(TrackPointRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    List<PointData> load(int routeId) {
        return repo.findPointDataByRouteId(routeId);
    }

    /** Persist elevations by trackpoint id. Only fills rows still null (never overwrites existing data). */
    @Transactional
    void saveElevations(Map<Integer, Double> elevationById) {
        if (elevationById.isEmpty()) return;
        List<Integer> ids = new ArrayList<>(elevationById.keySet());
        for (int i = 0; i < ids.size(); i += ID_CHUNK) {
            List<Integer> chunk = ids.subList(i, Math.min(i + ID_CHUNK, ids.size()));
            List<TrackPoint> entities = repo.findAllById(chunk);
            for (TrackPoint t : entities) {
                Double current = t.getElevation();
                if (current == null || current == 0.0) t.setElevation(elevationById.get(t.getId()));
            }
            repo.saveAll(entities);   // JDBC-batched via hibernate.jdbc.batch_size
        }
    }
}