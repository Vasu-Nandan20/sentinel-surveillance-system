package com.sentinel.surveillance.utils;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.AlertManager;
import com.sentinel.surveillance.alert.ThreatLevel;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates professional PDF incident reports using OpenPDF.
 * Reports include executive summary, timeline, and threat breakdown.
 */
public class ReportGenerator {

    private static final String REPORT_DIR = "reports";
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public ReportGenerator() {
        try { Files.createDirectories(Paths.get(REPORT_DIR)); }
        catch (IOException e) { System.err.println("[ReportGen] Dir error: " + e.getMessage()); }
    }

    /**
     * Generates a full PDF incident report from current alert data.
     * @return The file path of the generated report
     */
    public String generateReport() {
        String filename = REPORT_DIR + "/SENTINEL_Report_" + LocalDateTime.now().format(TS_FMT) + ".pdf";
        AlertManager am = AlertManager.getInstance();
        List<Alert> alerts = am.getAlerts();

        try {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
            doc.open();

            // === HEADER ===
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(0, 180, 216));
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(150, 150, 150));
            Font headingFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 255, 136));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(50, 50, 50));
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(50, 50, 50));
            Font criticalFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(233, 69, 96));
            Font highFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(255, 152, 0));

            Paragraph title = new Paragraph("SENTINEL SURVEILLANCE REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph classification = new Paragraph("CLASSIFICATION: CONFIDENTIAL", subtitleFont);
            classification.setAlignment(Element.ALIGN_CENTER);
            doc.add(classification);

            Paragraph timestamp = new Paragraph(
                    "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    subtitleFont);
            timestamp.setAlignment(Element.ALIGN_CENTER);
            doc.add(timestamp);

            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("————————————————————————————————————————", subtitleFont));
            doc.add(new Paragraph("\n"));

            // === EXECUTIVE SUMMARY ===
            doc.add(new Paragraph("1. EXECUTIVE SUMMARY", headingFont));
            doc.add(new Paragraph("\n"));

            Map<ThreatLevel, Long> bySeverity = alerts.stream()
                    .collect(Collectors.groupingBy(Alert::getThreatLevel, Collectors.counting()));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(60);
            summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            addTableRow(summaryTable, "Total Alerts", String.valueOf(alerts.size()), boldFont, normalFont);
            addTableRow(summaryTable, "CRITICAL", String.valueOf(bySeverity.getOrDefault(ThreatLevel.CRITICAL, 0L)), boldFont, criticalFont);
            addTableRow(summaryTable, "HIGH", String.valueOf(bySeverity.getOrDefault(ThreatLevel.HIGH, 0L)), boldFont, highFont);
            addTableRow(summaryTable, "MEDIUM", String.valueOf(bySeverity.getOrDefault(ThreatLevel.MEDIUM, 0L)), boldFont, normalFont);
            addTableRow(summaryTable, "LOW", String.valueOf(bySeverity.getOrDefault(ThreatLevel.LOW, 0L)), boldFont, normalFont);
            addTableRow(summaryTable, "System Threat Level", am.getCurrentThreatLevel().getLabel(), boldFont, criticalFont);
            doc.add(summaryTable);

            doc.add(new Paragraph("\n"));

            // === ALERT TIMELINE ===
            doc.add(new Paragraph("2. ALERT TIMELINE", headingFont));
            doc.add(new Paragraph("\n"));

            PdfPTable alertTable = new PdfPTable(5);
            alertTable.setWidthPercentage(100);
            alertTable.setWidths(new float[]{2f, 1.5f, 1.2f, 1f, 3f});

            // Header row
            String[] headers = {"Timestamp", "Camera", "Type", "Level", "Description"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(new Color(15, 52, 96));
                cell.setPadding(5);
                alertTable.addCell(cell);
            }

            // Data rows (max 50 most recent)
            int count = 0;
            for (Alert alert : alerts) {
                if (count++ >= 50) break;
                Font levelFont = alert.getThreatLevel() == ThreatLevel.CRITICAL ? criticalFont :
                                 alert.getThreatLevel() == ThreatLevel.HIGH ? highFont : normalFont;

                alertTable.addCell(new Phrase(alert.getFormattedTimestamp(), normalFont));
                alertTable.addCell(new Phrase(alert.getCameraId(), normalFont));
                alertTable.addCell(new Phrase(alert.getType(), normalFont));
                alertTable.addCell(new Phrase(alert.getThreatLevel().getLabel(), levelFont));

                String desc = alert.getDescription();
                if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
                alertTable.addCell(new Phrase(desc, normalFont));
            }
            doc.add(alertTable);

            doc.add(new Paragraph("\n"));

            // === CAMERA BREAKDOWN ===
            doc.add(new Paragraph("3. CAMERA-WISE BREAKDOWN", headingFont));
            doc.add(new Paragraph("\n"));

            Map<String, Long> byCamera = alerts.stream()
                    .collect(Collectors.groupingBy(Alert::getCameraId, Collectors.counting()));
            for (Map.Entry<String, Long> entry : byCamera.entrySet()) {
                doc.add(new Paragraph(String.format("  • %s: %d alerts", entry.getKey(), entry.getValue()), normalFont));
            }

            doc.add(new Paragraph("\n\n"));

            // === FOOTER ===
            doc.add(new Paragraph("————————————————————————————————————————", subtitleFont));
            doc.add(new Paragraph("SENTINEL Surveillance System v1.0 | Report ID: RPT-" +
                    System.currentTimeMillis() % 100000, subtitleFont));
            doc.add(new Paragraph("This report is auto-generated. Handle according to classification level.", subtitleFont));

            doc.close();
            System.out.println("[ReportGen] Report generated: " + filename);
            return filename;

        } catch (Exception e) {
            System.err.println("[ReportGen] Error generating report: " + e.getMessage());
            e.printStackTrace();
            return generateTextFallback(alerts);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font lf, Font vf) {
        table.addCell(new Phrase(label, lf));
        table.addCell(new Phrase(value, vf));
    }

    /** Fallback: generate a text report if PDF library fails */
    private String generateTextFallback(List<Alert> alerts) {
        String filename = REPORT_DIR + "/SENTINEL_Report_" + LocalDateTime.now().format(TS_FMT) + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("═══════════════════════════════════════════════");
            pw.println("         SENTINEL SURVEILLANCE REPORT          ");
            pw.println("═══════════════════════════════════════════════");
            pw.println("Generated: " + LocalDateTime.now());
            pw.println("Total Alerts: " + alerts.size());
            pw.println("───────────────────────────────────────────────");
            for (Alert a : alerts) {
                pw.println(a.toLogString());
            }
            pw.println("═══════════════════════════════════════════════");
        } catch (IOException e) {
            System.err.println("[ReportGen] Text fallback failed: " + e.getMessage());
        }
        return filename;
    }
}
