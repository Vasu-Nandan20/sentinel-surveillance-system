package com.sentinel.surveillance.alert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Immutable data model representing a single surveillance alert event.
 * Contains all metadata needed for logging, display, and reporting.
 */
public class Alert {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final LocalDateTime timestamp;
    private final String cameraId;
    private final String type;           // e.g., "MOTION", "FACE_MATCH", "WEAPON", "LOITERING"
    private final ThreatLevel threatLevel;
    private final String description;
    private final double confidence;     // 0.0 - 100.0
    private final double locationX;      // Map coordinate X
    private final double locationY;      // Map coordinate Y
    private boolean acknowledged;

    public Alert(String cameraId, String type, ThreatLevel threatLevel,
                 String description, double confidence, double locationX, double locationY) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = LocalDateTime.now();
        this.cameraId = cameraId;
        this.type = type;
        this.threatLevel = threatLevel;
        this.description = description;
        this.confidence = confidence;
        this.locationX = locationX;
        this.locationY = locationY;
        this.acknowledged = false;
    }

    // === Getters ===
    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFormattedTimestamp() { return timestamp.format(FORMATTER); }
    public String getCameraId() { return cameraId; }
    public String getType() { return type; }
    public ThreatLevel getThreatLevel() { return threatLevel; }
    public String getDescription() { return description; }
    public double getConfidence() { return confidence; }
    public double getLocationX() { return locationX; }
    public double getLocationY() { return locationY; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    /**
     * Returns a formatted single-line summary for the alert log panel.
     */
    public String toLogString() {
        return String.format("[%s] [%s] [%s] %s - %s (%.0f%% confidence)",
                getFormattedTimestamp(), threatLevel.getLabel(), cameraId, type, description, confidence);
    }

    /**
     * Returns CSV-formatted string for file logging.
     */
    public String toCSV() {
        return String.format("%s,%s,%s,%s,%s,\"%s\",%.1f,%.1f,%.1f",
                id, getFormattedTimestamp(), cameraId, type, threatLevel.getLabel(),
                description, confidence, locationX, locationY);
    }

    /**
     * CSV header row.
     */
    public static String csvHeader() {
        return "AlertID,Timestamp,Camera,Type,ThreatLevel,Description,Confidence,LocationX,LocationY";
    }

    @Override
    public String toString() {
        return toLogString();
    }
}
