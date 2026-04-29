package com.sentinel.surveillance.detection;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.ThreatLevel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Behavior Analysis Module
 * Detects: loitering (>10s same area), running (rapid movement), crowd formation (>3 people close)
 */
public class BehaviorAnalyzer {

    private static class PersonTrack {
        double x, y;
        long firstSeen;
        long lastUpdate;
        List<double[]> positionHistory = new ArrayList<>();
    }

    private final ConcurrentHashMap<String, PersonTrack> tracks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlertTime = new HashMap<>();
    private static final long ALERT_COOLDOWN_MS = 10000;
    private static final long LOITER_THRESHOLD_MS = 10000;
    private static final double LOITER_RADIUS = 50.0;
    private static final double RUNNING_SPEED_THRESHOLD = 15.0;
    private static final int CROWD_THRESHOLD = 3;
    private static final double CROWD_RADIUS = 100.0;
    private final Random random = new Random();

    public List<Alert> analyze(String cameraId, List<double[]> entities,
                                double canvasWidth, double canvasHeight) {
        List<Alert> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();

        Long lastAlert = lastAlertTime.get(cameraId);
        if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) return alerts;

        // Update person tracks
        for (int i = 0; i < entities.size(); i++) {
            double[] e = entities.get(i);
            String trackId = cameraId + "_person_" + i;
            PersonTrack track = tracks.computeIfAbsent(trackId, k -> {
                PersonTrack t = new PersonTrack();
                t.x = e[0]; t.y = e[1];
                t.firstSeen = now; t.lastUpdate = now;
                return t;
            });

            double dx = e[0] - track.x;
            double dy = e[1] - track.y;
            double speed = Math.sqrt(dx * dx + dy * dy);

            track.positionHistory.add(new double[]{e[0], e[1], (double) now});
            if (track.positionHistory.size() > 60) track.positionHistory.remove(0);

            // === Running Detection ===
            if (speed > RUNNING_SPEED_THRESHOLD) {
                alerts.add(new Alert(cameraId, "RUNNING",
                        ThreatLevel.MEDIUM,
                        String.format("Rapid movement detected - Person running (speed: %.0f px/frame)", speed),
                        75.0 + random.nextDouble() * 15,
                        e[0] / canvasWidth * 300, e[1] / canvasHeight * 300));
                lastAlertTime.put(cameraId, now);
            }

            // === Loitering Detection ===
            double distFromStart = Math.sqrt(
                    Math.pow(e[0] - track.x, 2) + Math.pow(e[1] - track.y, 2));
            long duration = now - track.firstSeen;

            if (distFromStart < LOITER_RADIUS && duration > LOITER_THRESHOLD_MS) {
                alerts.add(new Alert(cameraId, "LOITERING",
                        ThreatLevel.MEDIUM,
                        String.format("Person loitering for %d seconds in same area", duration / 1000),
                        70.0 + random.nextDouble() * 20,
                        e[0] / canvasWidth * 300, e[1] / canvasHeight * 300));
                track.firstSeen = now; // Reset to avoid repeated alerts
                lastAlertTime.put(cameraId, now);
            }

            track.x = e[0]; track.y = e[1]; track.lastUpdate = now;
        }

        // === Crowd Formation Detection ===
        if (entities.size() >= CROWD_THRESHOLD) {
            for (int i = 0; i < entities.size(); i++) {
                int nearby = 0;
                for (int j = 0; j < entities.size(); j++) {
                    if (i == j) continue;
                    double dist = Math.sqrt(
                            Math.pow(entities.get(i)[0] - entities.get(j)[0], 2) +
                            Math.pow(entities.get(i)[1] - entities.get(j)[1], 2));
                    if (dist < CROWD_RADIUS) nearby++;
                }
                if (nearby >= CROWD_THRESHOLD - 1) {
                    alerts.add(new Alert(cameraId, "CROWD",
                            ThreatLevel.MEDIUM,
                            String.format("Crowd formation: %d people gathered in small area",
                                    nearby + 1),
                            80.0 + random.nextDouble() * 15,
                            entities.get(i)[0] / canvasWidth * 300,
                            entities.get(i)[1] / canvasHeight * 300));
                    lastAlertTime.put(cameraId, now);
                    break;
                }
            }
        }

        // Cleanup old tracks
        tracks.entrySet().removeIf(e -> (now - e.getValue().lastUpdate) > 30000);

        return alerts;
    }
}
