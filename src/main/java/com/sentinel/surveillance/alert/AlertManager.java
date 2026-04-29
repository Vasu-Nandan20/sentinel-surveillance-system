package com.sentinel.surveillance.alert;

import com.sentinel.surveillance.database.CSVLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Central alert management hub for the SENTINEL system.
 * 
 * Responsibilities:
 * - Receives alerts from all detection modules (thread-safe)
 * - Maintains an observable list for JavaFX UI binding
 * - Dispatches to CSV logger, email/SMS simulators
 * - Tracks threat escalation (multiple alerts → higher threat)
 * - Provides alert statistics
 * 
 * Thread safety: Detection threads call addAlert() which queues events.
 * The JavaFX thread processes the queue and updates the observable list.
 */
public class AlertManager {
    private static AlertManager instance;

    private final ObservableList<Alert> alerts = FXCollections.observableArrayList();
    private final ConcurrentLinkedQueue<Alert> pendingAlerts = new ConcurrentLinkedQueue<>();
    private final CSVLogger csvLogger;

    // Statistics
    private final AtomicInteger totalAlerts = new AtomicInteger(0);
    private final AtomicInteger criticalCount = new AtomicInteger(0);
    private final AtomicInteger highCount = new AtomicInteger(0);

    // Callback for system log messages
    private Consumer<String> logCallback;

    // Current overall threat level (highest active)
    private volatile ThreatLevel currentThreatLevel = ThreatLevel.LOW;

    private AlertManager() {
        this.csvLogger = new CSVLogger();
    }

    /** Singleton access */
    public static synchronized AlertManager getInstance() {
        if (instance == null) {
            instance = new AlertManager();
        }
        return instance;
    }

    /**
     * Adds a new alert from any thread (thread-safe).
     * The alert is queued and will be processed on the JavaFX thread.
     */
    public void addAlert(Alert alert) {
        pendingAlerts.offer(alert);
        totalAlerts.incrementAndGet();

        // Update statistics
        if (alert.getThreatLevel() == ThreatLevel.CRITICAL) criticalCount.incrementAndGet();
        if (alert.getThreatLevel() == ThreatLevel.HIGH) highCount.incrementAndGet();

        // Log to CSV (thread-safe)
        csvLogger.logAlert(alert);

        // Simulate email/SMS for HIGH and CRITICAL
        if (alert.getThreatLevel() == ThreatLevel.HIGH || alert.getThreatLevel() == ThreatLevel.CRITICAL) {
            simulateEmailNotification(alert);
            simulateSMSNotification(alert);
        }

        // Update overall threat level
        updateThreatLevel(alert.getThreatLevel());

        // Process on JavaFX thread
        Platform.runLater(this::processPendingAlerts);
    }

    /**
     * Processes all pending alerts on the JavaFX thread.
     * Transfers from the concurrent queue to the observable list.
     */
    private void processPendingAlerts() {
        Alert alert;
        while ((alert = pendingAlerts.poll()) != null) {
            alerts.add(0, alert); // Add to top of list
            if (logCallback != null) {
                logCallback.accept(alert.toLogString());
            }
        }
    }

    /**
     * Updates the overall system threat level.
     * Escalates based on recent alert severity.
     */
    private void updateThreatLevel(ThreatLevel newLevel) {
        if (newLevel.ordinal() > currentThreatLevel.ordinal()) {
            currentThreatLevel = newLevel;
        }
        // Check for escalation: 3+ HIGH alerts → CRITICAL
        if (highCount.get() >= 3 && currentThreatLevel != ThreatLevel.CRITICAL) {
            currentThreatLevel = ThreatLevel.CRITICAL;
            log("[ESCALATION] Multiple HIGH threats detected → System elevated to CRITICAL");
        }
    }

    /**
     * Simulates sending an email notification for high-priority alerts.
     */
    private void simulateEmailNotification(Alert alert) {
        String msg = String.format(
                "[EMAIL ALERT] To: security-ops@sentinel.mil | Subject: %s THREAT - %s | " +
                "Camera: %s | %s | Confidence: %.0f%%",
                alert.getThreatLevel().getLabel(), alert.getType(),
                alert.getCameraId(), alert.getDescription(), alert.getConfidence());
        System.out.println(msg);
        log(msg);
    }

    /**
     * Simulates sending an SMS notification for critical alerts.
     */
    private void simulateSMSNotification(Alert alert) {
        String msg = String.format(
                "[SMS ALERT] To: +1-555-SENTINEL | %s: %s at %s - %s",
                alert.getThreatLevel().getLabel(), alert.getType(),
                alert.getCameraId(), alert.getDescription());
        System.out.println(msg);
        log(msg);
    }

    /** Log a system message via the callback */
    private void log(String message) {
        if (logCallback != null) {
            Platform.runLater(() -> logCallback.accept(message));
        }
    }

    // === Accessors ===

    /** Observable list for JavaFX binding */
    public ObservableList<Alert> getAlerts() { return alerts; }

    /** Current system-wide threat level */
    public ThreatLevel getCurrentThreatLevel() { return currentThreatLevel; }

    /** Total alert count */
    public int getTotalAlertCount() { return totalAlerts.get(); }
    public int getCriticalCount() { return criticalCount.get(); }
    public int getHighCount() { return highCount.get(); }

    /** Set callback for system log messages */
    public void setLogCallback(Consumer<String> callback) { this.logCallback = callback; }

    /** Get the most recent alert, or null */
    public Alert getLastAlert() {
        return alerts.isEmpty() ? null : alerts.get(0);
    }

    /** Reset threat level (e.g., after acknowledgment) */
    public void resetThreatLevel() {
        currentThreatLevel = ThreatLevel.LOW;
    }

    /** Get the CSV logger for report generation */
    public CSVLogger getCsvLogger() { return csvLogger; }
}
