package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.exception.resource.ResourceMergeFailedException;
import com.example.trekking_app.model.RouteStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxMergeService {

   private final GpxMergeHelper mergeHelper;

   public void mergeTrackPoints(@NonNull Integer routeId){
    mergeHelper.updateRouteStatus(routeId, RouteStatus.MERGING);
    try {
        mergeHelper.assignTrackPointGlobalSequences(routeId);
        mergeHelper.finalizeRoute(routeId);
    } catch (Exception e) {
        log.error("Failed to merge trackpoints for route id {}: {}", routeId, e.getLocalizedMessage());
        mergeHelper.updateRouteStatus(routeId, RouteStatus.FAILED);
        throw new ResourceMergeFailedException("trackpoints", "route id", routeId);
    }
}

public void mergeWayPoints(@NonNull Integer routeId)
{
    try{
        mergeHelper.assignWayPointGlobalSequences(routeId);
    } catch (Exception e)
    {
        log.error("Failed to merge waypoints for route id {}: {}", routeId, e.getLocalizedMessage());
        throw new ResourceMergeFailedException("waypoints", "route id", routeId);
    }
}


}
