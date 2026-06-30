package com.example.trekking_app.model;

import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.entity.TrailSegment;

public record SegmentRefreshContext(TrailSegment segment,
                                    TrackPoint startTp,
                                    TrackPoint endTp) {
}
