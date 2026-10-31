package com.example.trekking_app.service.route;

import com.example.trekking_app.config.GradeRules;
import com.example.trekking_app.dto.grade.*;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.*;
import com.example.trekking_app.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RouteGradeService {

    private final GradeRules rules;
    private final RouteRepository routeRepo;

    @Transactional
    public GradeResult grade(RouteInput r)
    {
        List<String> reasons = new ArrayList<>(), warnings = new ArrayList<>();
        List<GradeResult.Floor> floors = new ArrayList<>();

        int pass = r.highPassM() == null ? 0 : r.highPassM();
        double alt = r.maxAltitudeM();
        double mPerKm = r.elevationGainM() / r.distanceKm();

        // 1) Points
        int distance  = rules.distance().band(r.distanceKm());
        int gain      = rules.gain().band(r.elevationGainM());
        int steepness = rules.steepness().band(mPerKm);
        int altitude  = rules.altitude().band(alt);

        var t = rules.terrain();
        int terrain = 0;
        if (pass >= 4000)            terrain += t.highPass();
        if (r.narrowOrExposed())     terrain += t.narrowOrExposed();
        if (r.landslideOrRockfall()) terrain += t.landslideOrRockfall();
        if (r.scrambling())          terrain += t.scrambling();
        if (r.glacierOrMoraine())    terrain += t.glacierOrMoraine();
        if (r.roughOrNoTrail())      terrain += t.roughOrNoTrail();
        if (r.campingRequired())     terrain += t.camping();
        terrain = Math.min(t.cap(), terrain);

        int score = Math.min(100, distance + gain + steepness + altitude + terrain);
        DifficultyLevel pointsGrade = fromScore(score);

        // 2) Floors
        if (alt >= 3500)  floors.add(floor(DifficultyLevel.MODERATE,  "Reaches 3,500 m or higher"));
        if (alt >= 4000)  floors.add(floor(DifficultyLevel.HARD,      "Reaches 4,000 m or higher (very high altitude)"));
        if (alt >= 5000)  floors.add(floor(DifficultyLevel.VERY_HARD, "Reaches 5,000 m or higher"));
        if (pass >= 5000) floors.add(floor(DifficultyLevel.VERY_HARD, "Crosses a high pass (" + pass + " m)"));
        if (r.campingRequired() && alt >= 4000)
            floors.add(floor(DifficultyLevel.VERY_HARD, "Camping needed above 4,000 m"));
        if (r.narrowOrExposed()) floors.add(floor(DifficultyLevel.MODERATE, "Narrow or exposed path"));
        if (r.scrambling())      floors.add(floor(DifficultyLevel.HARD,     "Hands needed to scramble"));
        if (r.technicalGear())   floors.add(floor(DifficultyLevel.EXPERT,   "Crampons, ice axe or rope needed"));

        DifficultyLevel grade = pointsGrade;
        for (var f : floors) grade = DifficultyLevel.max(grade, f.grade());

        // 3) Category: HIKE / TREK / PEAK_CLIMBING
        var c = rules.category();
        RouteCategory category = (r.distanceKm() <= c.hikeMaxDistanceKm()
                && r.elevationGainM() <= c.hikeMaxGainM()
                && !r.campingRequired())
                ? RouteCategory.HIKE : RouteCategory.TREK;

        if (r.peakClimb()) {
            category = RouteCategory.PEAK_CLIMBING;
            grade = DifficultyLevel.EXPERT;
            reasons.add("Summit climb" + (r.peakAlpineGrade() != null
                    ? " (alpine grade " + r.peakAlpineGrade().label() + ")" : ""));
        }

        // 4) Reasons
        reasons.add("Distance %s km (+%d)".formatted(fmt(r.distanceKm()), distance));
        reasons.add("Total climbing %s m (+%d)".formatted(fmt(r.elevationGainM()), gain));
        reasons.add("Steepness %d m per km (+%d)".formatted(Math.round(mPerKm), steepness));
        reasons.add("Highest point %s m (+%d)".formatted(fmt(alt), altitude));
        if (terrain > 0) reasons.add("Terrain (+" + terrain + ")");
        floors.forEach(f -> reasons.add("At least %s: %s".formatted(f.grade().label(), f.why())));

        // 5) Warnings (never change grade)
        if (alt >= 3000) warnings.add("Altitude sickness risk. Go up slowly and plan rest days.");
        if (pass >= 4000) warnings.add("High pass: check snow and weather before crossing.");
        if (r.landslideOrRockfall()) warnings.add("Landslide risk is higher in the monsoon (June–September).");

        return new GradeResult(grade, grade.level(), category, score,
                new GradeResult.Breakdown(distance, gain, steepness, altitude, terrain),
                pointsGrade, floors, reasons, warnings, r.peakAlpineGrade(), rules.rulesVersion());
    }

    private DifficultyLevel fromScore(int s) {
        var c = rules.gradeCuts();
        if (s >= c.expert())   return DifficultyLevel.EXPERT;
        if (s >= c.veryHard()) return DifficultyLevel.VERY_HARD;
        if (s >= c.hard())     return DifficultyLevel.HARD;
        if (s >= c.moderate()) return DifficultyLevel.MODERATE;
        return DifficultyLevel.EASY;
    }

    private static GradeResult.Floor floor(DifficultyLevel g, String why) { return new GradeResult.Floor(g, why); }
    private static String fmt(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d); }

}

