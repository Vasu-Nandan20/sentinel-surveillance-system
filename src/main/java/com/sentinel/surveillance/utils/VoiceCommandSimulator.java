package com.sentinel.surveillance.utils;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.AlertManager;
import java.util.function.Consumer;

/**
 * Voice Command Simulator - processes text commands as if spoken.
 * Maps keyboard shortcuts and text input to system actions.
 * 
 * Supported commands:
 *   "show last alert"      / Ctrl+L  → Display most recent alert
 *   "show critical alerts" / Ctrl+A  → Filter to CRITICAL alerts
 *   "generate report"      / Ctrl+R  → Generate PDF report
 *   "system status"                   → Show system health
 *   "show camera [name]"             → Switch to camera feed
 *   "activate all cameras"           → Enable all camera feeds
 *   "help"                            → List available commands
 */
public class VoiceCommandSimulator {

    private Consumer<String> outputCallback;   // Displays response in UI
    private Runnable reportAction;             // Triggers report generation
    private Consumer<String> cameraSwitch;     // Switches camera feed

    public VoiceCommandSimulator() {}

    public void setOutputCallback(Consumer<String> callback) { this.outputCallback = callback; }
    public void setReportAction(Runnable action) { this.reportAction = action; }
    public void setCameraSwitch(Consumer<String> action) { this.cameraSwitch = action; }

    /**
     * Processes a text command and executes the corresponding action.
     * @param command Raw command string (case-insensitive)
     * @return Response text to display
     */
    public String processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "[VOICE] No command received.";
        }

        String cmd = command.trim().toLowerCase();
        AlertManager am = AlertManager.getInstance();

        // === SHOW LAST ALERT ===
        if (cmd.contains("last alert") || cmd.equals("ctrl+l")) {
            Alert last = am.getLastAlert();
            if (last != null) {
                String response = String.format("[VOICE] Last Alert:\n  [%s] %s\n  Camera: %s | Type: %s\n  Confidence: %.0f%%\n  %s",
                        last.getThreatLevel().getLabel(), last.getFormattedTimestamp(),
                        last.getCameraId(), last.getType(),
                        last.getConfidence(), last.getDescription());
                output(response);
                return response;
            } else {
                output("[VOICE] No alerts recorded yet.");
                return "[VOICE] No alerts recorded.";
            }
        }

        // === SHOW CRITICAL ALERTS ===
        if (cmd.contains("critical") || cmd.equals("ctrl+a")) {
            long critCount = am.getAlerts().stream()
                    .filter(a -> a.getThreatLevel() == com.sentinel.surveillance.alert.ThreatLevel.CRITICAL)
                    .count();
            String response = String.format("[VOICE] %d CRITICAL alerts found.", critCount);
            am.getAlerts().stream()
                    .filter(a -> a.getThreatLevel() == com.sentinel.surveillance.alert.ThreatLevel.CRITICAL)
                    .limit(5)
                    .forEach(a -> output("  → " + a.toLogString()));
            output(response);
            return response;
        }

        // === GENERATE REPORT ===
        if (cmd.contains("report") || cmd.contains("generate") || cmd.equals("ctrl+r")) {
            if (reportAction != null) {
                reportAction.run();
                output("[VOICE] Report generation initiated.");
                return "[VOICE] Generating report...";
            }
            return "[VOICE] Report generator not available.";
        }

        // === SYSTEM STATUS ===
        if (cmd.contains("status") || cmd.contains("health")) {
            String response = String.format(
                    "[VOICE] System Status:\n  Threat Level: %s\n  Total Alerts: %d\n  Critical: %d | High: %d\n  All cameras: ONLINE",
                    am.getCurrentThreatLevel().getLabel(),
                    am.getTotalAlertCount(),
                    am.getCriticalCount(), am.getHighCount());
            output(response);
            return response;
        }

        // === SHOW CAMERA ===
        if (cmd.contains("camera") || cmd.contains("show cam")) {
            String camName = null;
            if (cmd.contains("gate")) camName = "Gate 1";
            else if (cmd.contains("perimeter")) camName = "Perimeter East";
            else if (cmd.contains("watchtower") || cmd.contains("tower")) camName = "Watchtower 2";

            if (camName != null && cameraSwitch != null) {
                cameraSwitch.accept(camName);
                output("[VOICE] Switching to " + camName);
                return "[VOICE] Camera switched to " + camName;
            }
            output("[VOICE] Camera not found. Available: Gate 1, Perimeter East, Watchtower 2");
            return "[VOICE] Camera not found.";
        }

        // === ACTIVATE ALL ===
        if (cmd.contains("activate all")) {
            output("[VOICE] All cameras activated. Monitoring resumed.");
            return "[VOICE] All cameras activated.";
        }

        // === HELP ===
        if (cmd.contains("help")) {
            String help = "[VOICE] Available Commands:\n" +
                    "  • 'show last alert' (Ctrl+L)\n" +
                    "  • 'show critical alerts' (Ctrl+A)\n" +
                    "  • 'generate report' (Ctrl+R)\n" +
                    "  • 'system status'\n" +
                    "  • 'show camera [gate/perimeter/watchtower]'\n" +
                    "  • 'activate all cameras'\n" +
                    "  • 'help'";
            output(help);
            return help;
        }

        output("[VOICE] Unrecognized command: '" + command + "'. Type 'help' for commands.");
        return "[VOICE] Unknown command.";
    }

    private void output(String msg) {
        if (outputCallback != null) outputCallback.accept(msg);
    }
}
