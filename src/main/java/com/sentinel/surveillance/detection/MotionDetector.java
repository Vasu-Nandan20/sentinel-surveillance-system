package com.sentinel.surveillance.detection;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.ThreatLevel;

import java.util.*;

/**
 * Motion Detection Module - Frame Differencing Algorithm
 * 
 * How it works (simulation):
 * - Tracks simulated entity positions across frames
 * - Computes "motion level" based on entity movement deltas
 * - Checks if motion occurs within user-defined restricted zones
 * - Generates alerts when intrusion into restricted zones is detected
 * 
 * In a real system, this would compare pixel buffers between consecutive
 * frames, compute the absolute difference, threshold it, and find contours
 * around moving regions. We simulate this with entity position tracking.
 */
public class MotionDetector {

    // Restricted zones per camera: cameraId → list of polygon zones
    // Each zone is a list of [x,y] points defining the polygon
    private final Map<String, List<List<double[]>>> restrictedZones = new HashMap<>();

    // Previous entity positions per camera for frame differencing
    private final Map<String, List<double[]>> previousPositions = new HashMap<>();

    // Motion threshold (0.0 - 1.0): how much movement triggers an alert
    private double motionThreshold = 0.3;

    // Cooldown tracking to avoid alert spam
    private final Map<String, Long> lastAlertTime = new HashMap<>();
    private static final long ALERT_COOLDOWN_MS = 5000; // 5 second cooldown

    /**
     * Adds a restricted zone polygon for a specific camera.
     * @param cameraId Camera identifier
     * @param zonePoints List of [x, y] points defining the zone polygon
     */
    public void addRestrictedZone(String cameraId, List<double[]> zonePoints) {
        restrictedZones.computeIfAbsent(cameraId, k -> new ArrayList<>()).add(zonePoints);
        System.out.println("[MotionDetector] Added restricted zone to " + cameraId +
                " with " + zonePoints.size() + " points");
    }

    /**
     * Clears all restricted zones for a camera.
     */
    public void clearZones(String cameraId) {
        restrictedZones.remove(cameraId);
    }

    /**
     * Gets restricted zones for a camera.
     */
    public List<List<double[]>> getZones(String cameraId) {
        return restrictedZones.getOrDefault(cameraId, Collections.emptyList());
    }

    /**
     * Analyzes entity positions for motion detection.
     * Compares current positions with previous frame's positions.
     * 
     * @param cameraId     Camera being analyzed
     * @param entities     Current entity positions [[x,y,w,h,type], ...]
     * @param canvasWidth  Canvas width for coordinate normalization
     * @param canvasHeight Canvas height for coordinate normalization
     * @return List of alerts generated, or empty list
     */
    public List<Alert> analyze(String cameraId, List<double[]> entities,
                                double canvasWidth, double canvasHeight) {
        List<Alert> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Check cooldown
        Long lastAlert = lastAlertTime.get(cameraId);
        if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) {
            // Update previous positions and return
            previousPositions.put(cameraId, new ArrayList<>(entities));
            return alerts;
        }

        List<double[]> prevPositions = previousPositions.get(cameraId);

        if (prevPositions != null && !entities.isEmpty()) {
            // === Frame Differencing Simulation ===
            // Compare entity positions between frames
            double totalMotion = 0;
            int movingEntities = 0;

            for (double[] current : entities) {
                double minDist = Double.MAX_VALUE;
                for (double[] prev : prevPositions) {
                    double dist = Math.sqrt(Math.pow(current[0] - prev[0], 2) +
                                           Math.pow(current[1] - prev[1], 2));
                    minDist = Math.min(minDist, dist);
                }
                if (minDist > 5.0) { // Significant movement
                    totalMotion += minDist;
                    movingEntities++;

                    // Check if moving entity is inside a restricted zone
                    List<List<double[]>> zones = restrictedZones.getOrDefault(cameraId, Collections.emptyList());
                    for (int z = 0; z < zones.size(); z++) {
                        if (isPointInPolygon(current[0], current[1], zones.get(z))) {
                            Alert alert = new Alert(
                                    cameraId,
                                    "INTRUSION",
                                    ThreatLevel.HIGH,
                                    "Motion detected in Restricted Zone " + (z + 1) +
                                    " - Unauthorized intrusion",
                                    85.0 + new Random().nextDouble() * 10,
                                    current[0] / canvasWidth * 300,
                                    current[1] / canvasHeight * 300
                            );
                            alerts.add(alert);
                            lastAlertTime.put(cameraId, now);
                        }
                    }
                }
            }

            // General motion alert if significant movement detected
            double motionLevel = totalMotion / Math.max(1, canvasWidth);
            if (motionLevel > motionThreshold && movingEntities > 0) {
                Alert alert = new Alert(
                        cameraId,
                        "MOTION",
                        ThreatLevel.LOW,
                        String.format("Motion detected: %d moving objects, intensity %.0f%%",
                                movingEntities, motionLevel * 100),
                        70.0 + new Random().nextDouble() * 20,
                        entities.get(0)[0] / canvasWidth * 300,
                        entities.get(0)[1] / canvasHeight * 300
                );
                alerts.add(alert);
                lastAlertTime.put(cameraId, now);
            }
        }

        // Store current positions for next frame comparison
        previousPositions.put(cameraId, new ArrayList<>(entities));
        return alerts;
    }

    /**
     * Ray-casting algorithm to determine if a point is inside a polygon.
     * Used for restricted zone intersection checking.
     */
    public static boolean isPointInPolygon(double px, double py, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) return false;

        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];

            if (((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    public void setMotionThreshold(double threshold) {
        this.motionThreshold = threshold;
    }
}
