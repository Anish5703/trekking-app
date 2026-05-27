package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import lombok.Getter;


@Getter
public final class XlsxParserResult {
    public enum OutcomeType { TRAIL_SEGMENT, ACCOMMODATION, POI }

    private final OutcomeType outcomeType;
    private final TrailSegment trailSegment;    // non-null when TRAIL_SEGMENT
    private final Accommodation accommodation;   // non-null when ACCOMMODATION
    private final POI poi;//non-null when POI
    private final WayPoint wayPoint;
    private final Integer gpxOrderIndex;
    public XlsxParserResult(POI poi ,TrailSegment trailSegment , Accommodation accommodation,
                           WayPoint wayPoint , Integer gpxOrderIndex )
    {
        this.poi = poi;
        this.accommodation = accommodation;
        this.trailSegment = trailSegment;
        this.wayPoint = wayPoint;
        this.gpxOrderIndex = gpxOrderIndex;

        if(poi!=null) outcomeType = OutcomeType.POI;
        else if(accommodation!=null) outcomeType = OutcomeType.ACCOMMODATION;
        else if(trailSegment!=null) outcomeType = OutcomeType.TRAIL_SEGMENT;
        else throw new IllegalArgumentException("Expected one of the field to be non null among trail segment , accommodation and poi");
    }
    public static XlsxParserResult ofTrailSegment(WayPoint wp,TrailSegment ts,Integer gpxOrderIndex)
    {
        return new XlsxParserResult(null,ts,null,wp,gpxOrderIndex);
    }
    public static XlsxParserResult ofPOI(WayPoint wp , POI poi, Integer gpxOrderIndex)
    {
        return new XlsxParserResult(poi,null,null,wp,gpxOrderIndex);
    }
    public static XlsxParserResult ofAccommodation(WayPoint wp,Accommodation accommodation,Integer gpxOrderIndex)
    {
        return new XlsxParserResult(null,null,accommodation,wp,gpxOrderIndex);
    }

    // ── convenience predicates ────────────────────────────────────────────────

    public boolean isTrailSegment()  { return outcomeType == OutcomeType.TRAIL_SEGMENT; }
    public boolean isAccommodation() { return outcomeType == OutcomeType.ACCOMMODATION; }
    public boolean isPOI()           { return outcomeType == OutcomeType.POI; }
}
