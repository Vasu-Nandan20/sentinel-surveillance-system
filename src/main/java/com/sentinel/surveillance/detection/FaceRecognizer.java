package com.sentinel.surveillance.detection;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.ThreatLevel;

import java.util.*;

/**
 * Face Recognition Module - Watchlist Matching System
 * 
 * How it works (simulation):
 * - Maintains a watchlist of known suspects with threat profiles
 * - Simulates Haar cascade-style face detection at random intervals
 * - When a "face" is detected, compares against watchlist
 * - Generates confidence scores (mock: 70-95% range)
 * - Only triggers alerts when confidence exceeds threshold (75%)
 * 
 * In a real system, this would use OpenCV's CascadeClassifier with
 * haarcascade_frontalface_default.xml for detection, then LBPH/Eigenface
 * recognizer for identification against enrolled face embeddings.
 */
public class FaceRecognizer {

    /** Watchlist entry representing a known suspect */
    public static class WatchlistEntry {
        public final String id;
        public final String name;
        public final String alias;
        public final ThreatLevel threatLevel;
        public final String description;
        public String lastSeen;
        public double lastConfidence;

        public WatchlistEntry(String id, String name, String alias,
                              ThreatLevel threatLevel, String description) {
            this.id = id;
            this.name = name;
            this.alias = alias;
            this.threatLevel = threatLevel;
            this.description = description;
            this.lastSeen = "Never";
            this.lastConfidence = 0;
        }
    }

    // Watchlist database
    private final List<WatchlistEntry> watchlist = new ArrayList<>();

    // Detection threshold (only alert if confidence > this value)
    private double confidenceThreshold = 75.0;

    // Cooldown to avoid rapid re-detection of same face
    private final Map<String, Long> lastDetectionTime = new HashMap<>();
    private static final long DETECTION_COOLDOWN_MS = 15000; // 15 seconds

    private final Random random = new Random();

    public FaceRecognizer() {
        // Initialize dummy watchlist with 3 suspects
        watchlist.add(new WatchlistEntry("WL-001", "John Doe", "The Ghost",
                ThreatLevel.HIGH,
                "Suspected arms dealer, last seen in border region. Armed and dangerous."));

        watchlist.add(new WatchlistEntry("WL-002", "Jane Smith", "Shadow",
                ThreatLevel.CRITICAL,
                "Known operative, multiple warrants. High-value target."));

        watchlist.add(new WatchlistEntry("WL-003", "Unknown Intruder", "Phantom",
                ThreatLevel.MEDIUM,
                "Unidentified individual spotted in restricted areas multiple times."));

        watchlist.add(new WatchlistEntry("WL-004", "Viktor Petrov", "Iron Wolf",
                ThreatLevel.CRITICAL,
                "International fugitive. Approach with extreme caution."));

        watchlist.add(new WatchlistEntry("WL-005", "Maria Chen", "Viper",
                ThreatLevel.HIGH,
                "Cyber warfare specialist. Intelligence priority target."));
    }

    /**
     * Simulates face detection and recognition on a camera feed.
     * Has a random chance of "detecting" a face and matching it.
     * 
     * @param cameraId     Camera being analyzed
     * @param personCount  Number of "person" entities in the frame
     * @param canvasWidth  For coordinate normalization
     * @param canvasHeight For coordinate normalization
     * @return Alert if a watchlist match is found, null otherwise
     */
    public Alert analyze(String cameraId, int personCount,
                         double canvasWidth, double canvasHeight) {
        if (personCount == 0) return null;

        long now = System.currentTimeMillis();

        // Random chance of face detection (simulates detection frequency)
        // Higher person count = higher detection probability
        double detectionChance = 0.02 * personCount; // 2% per person per frame
        if (random.nextDouble() > detectionChance) return null;

        // Pick a random suspect from watchlist
        WatchlistEntry suspect = watchlist.get(random.nextInt(watchlist.size()));

        // Check cooldown for this suspect
        Long lastTime = lastDetectionTime.get(suspect.id);
        if (lastTime != null && (now - lastTime) < DETECTION_COOLDOWN_MS) {
            return null;
        }

        // Generate confidence score (70-95% range, simulating real recognition)
        double confidence = 70.0 + random.nextDouble() * 25.0;

        // Only alert if above threshold
        if (confidence < confidenceThreshold) {
            System.out.printf("[FaceRecognizer] Low confidence match: %s (%.1f%%) - below threshold%n",
                    suspect.name, confidence);
            return null;
        }

        // Update watchlist entry
        suspect.lastSeen = cameraId + " at " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        suspect.lastConfidence = confidence;
        lastDetectionTime.put(suspect.id, now);

        // Determine threat level based on confidence and suspect profile
        ThreatLevel level = suspect.threatLevel;
        if (confidence > 90.0) {
            // High confidence match escalates threat
            level = ThreatLevel.values()[Math.min(level.ordinal() + 1, ThreatLevel.CRITICAL.ordinal())];
        }

        // Generate random face position on canvas
        double faceX = 50 + random.nextDouble() * (canvasWidth - 100);
        double faceY = 30 + random.nextDouble() * (canvasHeight - 80);

        System.out.printf("[FaceRecognizer] MATCH: %s (%s) - Confidence: %.1f%% at %s%n",
                suspect.name, suspect.alias, confidence, cameraId);

        return new Alert(
                cameraId,
                "FACE_MATCH",
                level,
                String.format("Watchlist match: %s (%s) - %s",
                        suspect.name, suspect.alias, suspect.description),
                confidence,
                faceX / canvasWidth * 300,
                faceY / canvasHeight * 300
        );
    }

    /** Get the full watchlist for UI display */
    public List<WatchlistEntry> getWatchlist() {
        return Collections.unmodifiableList(watchlist);
    }

    /** Add a new entry to the watchlist */
    public void addToWatchlist(WatchlistEntry entry) {
        watchlist.add(entry);
        System.out.println("[FaceRecognizer] Added to watchlist: " + entry.name);
    }

    /** Remove from watchlist by ID */
    public boolean removeFromWatchlist(String id) {
        return watchlist.removeIf(e -> e.id.equals(id));
    }

    public void setConfidenceThreshold(double threshold) {
        this.confidenceThreshold = threshold;
    }
}
