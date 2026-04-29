package com.sentinel.surveillance.detection;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.ThreatLevel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulated YOLO Object Detection Module.
 * Detects: weapons, drones, abandoned bags. Tracks object persistence.
 */
public class ObjectDetector {

    public static class DetectedObject {
        public final String className;
        public final double confidence;
        public final double x, y, width, height;
        public final long firstSeenTime;
        public long lastSeenTime;
        public final String id;

        public DetectedObject(String className, double confidence,
                              double x, double y, double w, double h) {
            this.className = className; this.confidence = confidence;
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.firstSeenTime = System.currentTimeMillis();
            this.lastSeenTime = this.firstSeenTime;
            this.id = UUID.randomUUID().toString().substring(0, 6);
        }
    }

    private static final String[] WEAPONS = {"Handgun", "Rifle", "Knife"};
    private static final String[] ITEMS = {"Backpack", "Suitcase", "Package"};
    private static final String[] AERIALS = {"Drone", "UAV"};
    private final ConcurrentHashMap<String, DetectedObject> trackedObjects = new ConcurrentHashMap<>();
    private static final long ABANDONED_THRESHOLD_MS = 30000;
    private final Map<String, Long> lastAlertTime = new HashMap<>();
    private static final long COOLDOWN_MS = 8000;
    private final Random random = new Random();

    public List<Alert> analyze(String cameraId, double cw, double ch) {
        List<Alert> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();
        Long last = lastAlertTime.get(cameraId);
        if (last != null && (now - last) < COOLDOWN_MS) return alerts;

        // Weapon detection (0.5% chance per analysis cycle)
        if (random.nextDouble() < 0.005) {
            String w = WEAPONS[random.nextInt(WEAPONS.length)];
            double conf = 78.0 + random.nextDouble() * 18;
            double x = 50 + random.nextDouble() * (cw - 100);
            double y = 50 + random.nextDouble() * (ch - 100);
            alerts.add(new Alert(cameraId, "WEAPON",
                    conf > 90 ? ThreatLevel.CRITICAL : ThreatLevel.HIGH,
                    String.format("YOLO: %s detected (%.1f%%)", w, conf),
                    conf, x / cw * 300, y / ch * 300));
            lastAlertTime.put(cameraId, now);
        }

        // Drone detection (0.3% chance)
        if (random.nextDouble() < 0.003) {
            String d = AERIALS[random.nextInt(AERIALS.length)];
            double conf = 80 + random.nextDouble() * 15;
            double x = random.nextDouble() * cw;
            double y = random.nextDouble() * ch * 0.4;
            alerts.add(new Alert(cameraId, "DRONE", ThreatLevel.HIGH,
                    String.format("Aerial: %s detected (%.1f%%)", d, conf),
                    conf, x / cw * 300, y / ch * 300));
            lastAlertTime.put(cameraId, now);
        }

        // Abandoned item (1% chance)
        if (random.nextDouble() < 0.01) {
            String item = ITEMS[random.nextInt(ITEMS.length)];
            double x = 80 + random.nextDouble() * (cw - 160);
            double y = 100 + random.nextDouble() * (ch - 200);
            double conf = 75 + random.nextDouble() * 20;
            String key = cameraId + "_" + Math.round(x / 50) + "_" + Math.round(y / 50);
            DetectedObject existing = trackedObjects.get(key);
            if (existing != null) {
                existing.lastSeenTime = now;
                long dur = now - existing.firstSeenTime;
                if (dur > ABANDONED_THRESHOLD_MS) {
                    alerts.add(new Alert(cameraId, "ABANDONED_OBJECT", ThreatLevel.MEDIUM,
                            String.format("Abandoned %s - stationary %ds", existing.className, dur / 1000),
                            conf, x / cw * 300, y / ch * 300));
                    trackedObjects.remove(key);
                    lastAlertTime.put(cameraId, now);
                }
            } else {
                trackedObjects.put(key, new DetectedObject(item, conf, x, y, 35, 25));
            }
        }

        trackedObjects.entrySet().removeIf(e -> (now - e.getValue().lastSeenTime) > 60000);
        return alerts;
    }

    public Alert simulateWeaponDetection(String cameraId, double cw, double ch) {
        String w = WEAPONS[random.nextInt(WEAPONS.length)];
        double conf = 88 + random.nextDouble() * 10;
        double x = 100 + random.nextDouble() * (cw - 200);
        double y = 80 + random.nextDouble() * (ch - 160);
        return new Alert(cameraId, "WEAPON", ThreatLevel.CRITICAL,
                String.format("CONFIRMED: %s detected by YOLO (%.1f%%) - IMMEDIATE RESPONSE", w, conf),
                conf, x / cw * 300, y / ch * 300);
    }

    public Collection<DetectedObject> getTrackedObjects() { return trackedObjects.values(); }
}
