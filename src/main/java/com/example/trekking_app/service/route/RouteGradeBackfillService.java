package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.grade.BackfillStatus;
import com.example.trekking_app.repository.RouteRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteGradeBackfillService {

    private final RouteGradeUpdater updater;
    private final RouteRepository routeRepo;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "route-grade-backfill");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<BackfillStatus> status = new AtomicReference<>(BackfillStatus.idle());

    /** 202 if started, 409 if a run is already in progress (this JVM only). */
    public synchronized ApiResponse<BackfillStatus> start(boolean onlyUngraded) {
        if (status.get().running())
            return new ApiResponse<>(status.get(), "A grade backfill is already running", 409);

        List<Integer> ids = onlyUngraded ? routeRepo.findUngradedIds() : routeRepo.findAllIds();
        status.set(new BackfillStatus("RUNNING", ids.size(), 0, 0, 0, Instant.now(), null));
        executor.submit(() -> run(ids));
        return new ApiResponse<>(status.get(), "Grade backfill started", 202);
    }

    public ApiResponse<BackfillStatus> currentStatus() {
        return new ApiResponse<>(status.get(), "Grade backfill status fetched successfully", 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Long> ungradedCount() {
        return new ApiResponse<>(routeRepo.countByDifficultyResultIsNull(), "Ungraded routes counted", 200);
    }

    private void run(List<Integer> ids) {
        int graded = 0, review = 0, failed = 0;
        Instant started = status.get().startedAt();
        try {
            for (Integer id : ids) {
                try {
                    if (updater.regrade(id).isPresent()) graded++; else review++;   // each call = own transaction
                } catch (Exception e) {
                    failed++;
                    log.error("Grade backfill failed for route {}", id, e);
                }
                status.set(new BackfillStatus("RUNNING", ids.size(), graded, review, failed, started, null));
            }
            status.set(new BackfillStatus("COMPLETED", ids.size(), graded, review, failed, started, Instant.now()));
            log.info("Grade backfill done: graded={}, needsReview={}, failed={}", graded, review, failed);
        } catch (Throwable t) {
            log.error("Grade backfill aborted", t);
            status.set(new BackfillStatus("FAILED", ids.size(), graded, review, failed, started, Instant.now()));
        }
    }

    @PreDestroy
    void shutdown() { executor.shutdownNow(); }
}