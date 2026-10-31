package com.example.trekking_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.offline")
public class GeoOfflineProperties {
    private boolean enabled = true;
    private double defaultCorridorMeters = 5000;
    private double minCorridorMeters = 250;
    private double maxCorridorMeters = 20000;
    private double defaultSimplifyMeters = 75;
    private double maxSimplifyMeters = 1000;
    private long maxPackageBytes = 10L * 1024 * 1024;
    private int queryTimeoutSeconds = 30;
    private String routeTable = "routes";
    private String routeIdColumn = "id";
    private String routeGeometryColumn = "path";
    private String routePublishedColumn = "is_published";
    private double maxRouteLengthKm = 600;
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public double getDefaultCorridorMeters(){return defaultCorridorMeters;} public void setDefaultCorridorMeters(double v){defaultCorridorMeters=v;}
    public double getMinCorridorMeters(){return minCorridorMeters;} public void setMinCorridorMeters(double v){minCorridorMeters=v;}
    public double getMaxCorridorMeters(){return maxCorridorMeters;} public void setMaxCorridorMeters(double v){maxCorridorMeters=v;}
    public double getDefaultSimplifyMeters(){return defaultSimplifyMeters;} public void setDefaultSimplifyMeters(double v){defaultSimplifyMeters=v;}
    public double getMaxSimplifyMeters(){return maxSimplifyMeters;} public void setMaxSimplifyMeters(double v){maxSimplifyMeters=v;}
    public long getMaxPackageBytes(){return maxPackageBytes;} public void setMaxPackageBytes(long v){maxPackageBytes=v;}
    public int getQueryTimeoutSeconds(){return queryTimeoutSeconds;} public void setQueryTimeoutSeconds(int v){queryTimeoutSeconds=v;}
    public String getRouteTable(){return routeTable;} public void setRouteTable(String v){routeTable=v;}
    public String getRouteIdColumn(){return routeIdColumn;} public void setRouteIdColumn(String v){routeIdColumn=v;}
    public String getRouteGeometryColumn(){return routeGeometryColumn;} public void setRouteGeometryColumn(String v){routeGeometryColumn=v;}
    public String getRoutePublishedColumn(){return routePublishedColumn;} public void setRoutePublishedColumn(String v){routePublishedColumn=v;}
    public double getMaxRouteLengthKm(){return maxRouteLengthKm;} public void setMaxRouteLengthKm(double v){maxRouteLengthKm=v;}
}
