package com.sentinel.surveillance.database;

import com.sentinel.surveillance.alert.Alert;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe CSV logger for persisting alert events to disk.
 * Automatically creates the log file with headers on first write.
 * All write operations are synchronized via ReentrantLock.
 */
public class CSVLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + "/alerts_log.csv";
    private final ReentrantLock writeLock = new ReentrantLock();
    private boolean headerWritten = false;

    public CSVLogger() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            // Check if file exists and has content (header already written)
            File f = new File(LOG_FILE);
            if (f.exists() && f.length() > 0) {
                headerWritten = true;
            }
        } catch (IOException e) {
            System.err.println("[CSVLogger] Failed to create log directory: " + e.getMessage());
        }
    }

    /**
     * Appends an alert event to the CSV log file.
     * Thread-safe: uses ReentrantLock to serialize writes.
     */
    public void logAlert(Alert alert) {
        writeLock.lock();
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(LOG_FILE, true))) {
            // Write header on first entry
            if (!headerWritten) {
                writer.write(Alert.csvHeader());
                writer.newLine();
                headerWritten = true;
            }
            writer.write(alert.toCSV());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("[CSVLogger] Failed to write alert: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Reads all alerts from the CSV log file.
     * Returns raw CSV lines (excluding header).
     */
    public List<String> readAllAlerts() {
        List<String> lines = new ArrayList<>();
        File f = new File(LOG_FILE);
        if (!f.exists()) return lines;

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            boolean skipHeader = true;
            while ((line = reader.readLine()) != null) {
                if (skipHeader) { skipHeader = false; continue; }
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[CSVLogger] Failed to read alerts: " + e.getMessage());
        }
        return lines;
    }

    /**
     * Returns the number of logged alerts.
     */
    public int getAlertCount() {
        return readAllAlerts().size();
    }

    /**
     * Clears the log file (keeps header).
     */
    public void clearLog() {
        writeLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, false))) {
            writer.write(Alert.csvHeader());
            writer.newLine();
            headerWritten = true;
        } catch (IOException e) {
            System.err.println("[CSVLogger] Failed to clear log: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }
}
