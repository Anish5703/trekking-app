package com.example.trekking_app.model;
/**
 * Severity levels for danger zone and proximity alerts.
 * CRITICAL = immediate life-threatening danger (landslide, avalanche, flash flood)
 * HIGH = significant risk (unstable terrain, wild animals, no-go zone)
 * MEDIUM = caution required (slippery trail, narrow path, river crossing)
 * LOW = advisory (altitude sickness risk, weather advisory, poor network)
 */
public enum AlertSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
