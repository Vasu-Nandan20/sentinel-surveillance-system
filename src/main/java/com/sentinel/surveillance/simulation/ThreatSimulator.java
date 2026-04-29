package com.sentinel.surveillance.simulation;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.AlertManager;
import com.sentinel.surveillance.alert.ThreatLevel;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background threat simulator that generates random suspicious events.
 * Runs on a scheduled thread and periodically injects realistic alerts
 * to demonstrate the system capabilities.
 */
public class ThreatSimulator {

    private final AlertManager alertManager;
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private volatile boolean running = false;

    private static final String[] CAMERAS = {"Gate 1", "Perimeter East", "Watchtower 2"};

    private static final String[][] SCENARIOS = {
        {"MOTION", "LOW", "Unusual movement detected near fence line"},
        {"MOTION", "LOW", "Animal or debris movement detected"},
        {"MOTION", "MEDIUM", "Unauthorized movement in restricted sector"},
        {"FACE_MATCH", "HIGH", "Watchlist match: John Doe (The Ghost) identified"},
        {"FACE_MATCH", "CRITICAL", "HIGH-VALUE TARGET: Jane Smith (Shadow) confirmed"},
        {"WEAPON", "HIGH", "Possible firearm detected in subject's possession"},
        {"WEAPON", "CRITICAL", "CONFIRMED weapon - Rifle visible on thermal"},
        {"LOITERING", "MEDIUM", "Individual loitering near entry point for 45 seconds"},
        {"DRONE", "HIGH", "Unauthorized drone detected in restricted airspace"},
        {"CROWD", "MEDIUM", "Unusual gathering of 5+ individuals at checkpoint"},
        {"ABANDONED_OBJECT", "MEDIUM", "Unattended package left near building entrance"},
        {"INTRUSION", "HIGH", "Perimeter breach detected - Zone Alpha compromised"},
        {"RUNNING", "MEDIUM", "Subject fleeing from checkpoint at high speed"},
        {"VEHICLE", "LOW", "Unregistered vehicle approaching main gate"},
        {"SATELLITE", "LOW", "Land change detected in border sector 7"},
    };

    public ThreatSimulator(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    /** Start automatic background threat generation */
    public void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ThreatSimulator");
            t.setDaemon(true);
            return t;
        });

        // Generate a random threat every 10-25 seconds
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                generateRandomThreat();
            } catch (Exception e) {
                System.err.println("[ThreatSimulator] Error: " + e.getMessage());
            }
        }, 5, 15, TimeUnit.SECONDS);

        System.out.println("[ThreatSimulator] Background simulation started");
    }

    /** Stop background simulation */
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        System.out.println("[ThreatSimulator] Background simulation stopped");
    }

    /** Generate a single random threat event */
    public void generateRandomThreat() {
        String camera = CAMERAS[random.nextInt(CAMERAS.length)];
        String[] scenario = SCENARIOS[random.nextInt(SCENARIOS.length)];
        String type = scenario[0];
        ThreatLevel level = ThreatLevel.valueOf(scenario[1]);
        String desc = scenario[2];
        double confidence = 65 + random.nextDouble() * 30;
        double locX = 20 + random.nextDouble() * 260;
        double locY = 20 + random.nextDouble() * 260;

        Alert alert = new Alert(camera, type, level, desc, confidence, locX, locY);
        alertManager.addAlert(alert);
    }

    /** Manually trigger a high-severity threat (for demo button) */
    public void triggerManualThreat() {
        String camera = CAMERAS[random.nextInt(CAMERAS.length)];
        double locX = 50 + random.nextDouble() * 200;
        double locY = 50 + random.nextDouble() * 200;

        // Generate a dramatic multi-alert scenario
        alertManager.addAlert(new Alert(camera, "WEAPON", ThreatLevel.CRITICAL,
                "CONFIRMED: Armed intruder detected - Rifle identified by YOLO v8",
                94.5 + random.nextDouble() * 5, locX, locY));

        // Follow-up alerts using daemon threads (safe even if scheduler is null)
        Thread followUp1 = new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            alertManager.addAlert(new Alert(camera, "INTRUSION", ThreatLevel.CRITICAL,
                    "PERIMETER BREACH in Sector 4 - Multiple hostiles detected",
                    91.0 + random.nextDouble() * 8, locX + 20, locY + 10));
        });
        followUp1.setDaemon(true);
        followUp1.start();

        Thread followUp2 = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            alertManager.addAlert(new Alert(camera, "FACE_MATCH", ThreatLevel.HIGH,
                    "Watchlist match: Viktor Petrov (Iron Wolf) - International fugitive",
                    87.0 + random.nextDouble() * 10, locX + 5, locY - 15));
        });
        followUp2.setDaemon(true);
        followUp2.start();
    }

    public boolean isRunning() { return running; }
}
