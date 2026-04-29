package com.sentinel.surveillance.alert;

/**
 * Threat severity classification for the SENTINEL surveillance system.
 * Each level maps to specific detection scenarios and UI color coding.
 */
public enum ThreatLevel {
    LOW("LOW", "#00ff88", "Minor activity detected"),
    MEDIUM("MEDIUM", "#ffc107", "Suspicious activity requires attention"),
    HIGH("HIGH", "#ff9800", "Confirmed threat - immediate response needed"),
    CRITICAL("CRITICAL", "#e94560", "Maximum threat - all units respond");

    private final String label;
    private final String color;
    private final String description;

    ThreatLevel(String label, String color, String description) {
        this.label = label;
        this.color = color;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
    public String getDescription() { return description; }

    /**
     * Returns the CSS style class name for this threat level.
     */
    public String getStyleClass() {
        return "threat-" + label.toLowerCase();
    }

    /**
     * Returns the alert panel CSS style class for this threat level.
     */
    public String getAlertStyleClass() {
        return "alert-" + label.toLowerCase();
    }
}
