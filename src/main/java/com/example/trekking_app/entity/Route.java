package com.example.trekking_app.entity;

import com.example.trekking_app.dto.grade.GradeResult;
import com.example.trekking_app.dto.grade.TerrainProfile;
import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.model.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="routes",
indexes = {
        @Index(name="idx_routes_name",columnList = "name"),
        @Index(name="idx_routes_user" , columnList = "user_id")
},
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","destination_id"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Route extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Route name is required")
    @Size(max = 500, message = "Route name must not exceed 500 characters")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id" , nullable = false)
    private Destination destination;

    @Size(max = 20000, message = "Description must not exceed 20000 characters")
    private String description;

    @Column(name = "path",columnDefinition = "geometry(LineString, 4326)")
    private LineString path;

    @Column(name="max_elevation")
    @Builder.Default
    private Double maxElevation=0.0;

    @Column(name = "min_elevation")
    @Builder.Default
    private Double minElevation=0.0;

    @Column(name="estimated_days")
    private Integer estimatedDays;

    @Column(name = "approx_days")
    private List<Integer> approxDays;
    @Column(name = "approx_hours")
    private List<Integer> approxHours;
    @Column(name = "important_keywords")
    private List<String> importantKeywords;
    @Column(name = "included_regions")
    private List<String> includedRegions;

    @Column(name="is_published")
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name="is_locked")
    @Builder.Default
    private Boolean isLocked=false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Size(max = 2000)
    @Column(name = "verification_remarks", length = 2000)
    private String verificationRemarks;

    @Column
    @Builder.Default
    private Double distanceInKm=0.0;

    @Enumerated(EnumType.STRING)
    @Column(name="route_status")
    private RouteStatus routeStatus = RouteStatus.IDLE;

    @Column(name="difficulty_level",length=20)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;


    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    @OrderBy("global_sequence ASC")
    private List<TrackPoint> trackPoints = new ArrayList<>();

    @OneToMany(mappedBy = "route",cascade = CascadeType.ALL, orphanRemoval = true , fetch = FetchType.LAZY)
    @OrderBy("order_index ASC")
    private List<GpxSegment> gpxSegments = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<TrailSegment> trailSegments = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<Accommodation> accommodations = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<WayPoint> waypoints = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<RecentlyViewed> recentlyViewed = new ArrayList<>();


    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<POI> pois = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<DangerZone> dangerZones = new ArrayList<>();

   @OneToMany(mappedBy = "route" , cascade = CascadeType.ALL , orphanRemoval = true )
   private List<OfflineRegion> offlineRegions = new ArrayList<>();

    @Column(name = "elevation_gain_m")
    private Double elevationGainM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "terrain", columnDefinition = "jsonb")
    @Builder.Default
    private TerrainProfile terrain = new TerrainProfile();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_result", columnDefinition = "jsonb")
    private GradeResult difficultyResult;

    @Column(name="recorded_at")
    private LocalDateTime recordedAt;

public Boolean getIsPublished()
{
    return isPublished==null || isPublished==true ? true : false;
}
public Boolean getIsLocked(){return isLocked==null || isLocked==false ? false : true;}

    public Double getElevationGainM()
    {
        if(elevationGainM==null)
        return 0.0 ;
    else return elevationGainM;
    }

public VerificationStatus getVerificationStatus()
{
    if(verificationStatus==null)
        return VerificationStatus.UNVERIFIED;
    else
       return verificationStatus;

}

}
