package com.sentinel.surveillance.ui;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.AlertManager;
import com.sentinel.surveillance.simulation.CameraSimulator;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;

/**
 * Interactive map visualization showing camera locations and threat markers.
 * Renders a tactical grid-based facility map on a JavaFX Canvas.
 */
public class MapView extends Canvas {

    private final CameraSimulator cameraSimulator;
    private final AlertManager alertManager;
    private String selectedCamera = null;

    public MapView(double width, double height, CameraSimulator cameraSimulator) {
        super(width, height);
        this.cameraSimulator = cameraSimulator;
        this.alertManager = AlertManager.getInstance();

        // Click handler: select camera by clicking on its dot
        setOnMouseClicked(e -> {
            for (CameraSimulator.Camera cam : cameraSimulator.getCameras()) {
                double dx = e.getX() - cam.mapX;
                double dy = e.getY() - cam.mapY;
                if (Math.sqrt(dx * dx + dy * dy) < 20) {
                    selectedCamera = cam.name;
                    render();
                    return;
                }
            }
            selectedCamera = null;
            render();
        });
    }

    /** Renders the complete map visualization */
    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();

        // Background
        gc.setFill(Color.web("#0a0e17"));
        gc.fillRect(0, 0, w, h);

        // Grid lines
        gc.setStroke(Color.web("#1e3a5f", 0.3));
        gc.setLineWidth(0.5);
        for (double x = 0; x < w; x += 30) gc.strokeLine(x, 0, x, h);
        for (double y = 0; y < h; y += 30) gc.strokeLine(0, y, w, y);

        // Facility outline (simplified base perimeter)
        gc.setStroke(Color.web("#0f3460"));
        gc.setLineWidth(2);
        gc.strokeRect(30, 30, w - 60, h - 60);
        gc.setStroke(Color.web("#1e3a5f"));
        gc.setLineWidth(1);
        gc.strokeRect(60, 80, 100, 60);  // Building 1
        gc.strokeRect(180, 60, 80, 80);  // Building 2
        gc.strokeRect(100, 180, 120, 50); // Building 3

        // Labels
        gc.setFill(Color.web("#4a5568"));
        gc.setFont(Font.font("Consolas", 9));
        gc.fillText("BLDG-A", 85, 115);
        gc.fillText("BLDG-B", 200, 105);
        gc.fillText("BLDG-C", 135, 210);
        gc.fillText("SENTINEL TACTICAL MAP", 75, 22);

        // Draw threat markers from recent alerts
        List<Alert> alerts = alertManager.getAlerts();
        int shown = 0;
        for (Alert alert : alerts) {
            if (shown >= 15) break;
            double ax = alert.getLocationX();
            double ay = alert.getLocationY();
            if (ax < 5 || ay < 5 || ax > w - 5 || ay > h - 5) continue;

            Color markerColor = Color.web(alert.getThreatLevel().getColor(), 0.6);
            gc.setFill(markerColor);

            // Pulsing ring effect (based on threat level)
            double ringSize = 8 + alert.getThreatLevel().ordinal() * 3;
            gc.setStroke(markerColor);
            gc.setLineWidth(1);
            gc.strokeOval(ax - ringSize, ay - ringSize, ringSize * 2, ringSize * 2);

            // Inner dot
            gc.fillOval(ax - 3, ay - 3, 6, 6);
            shown++;
        }

        // Draw camera positions
        for (CameraSimulator.Camera cam : cameraSimulator.getCameras()) {
            boolean isSelected = cam.name.equals(selectedCamera);

            // Camera icon (triangle + circle)
            gc.setFill(isSelected ? Color.web("#00ff88") : Color.web("#00b4d8"));
            gc.fillOval(cam.mapX - 8, cam.mapY - 8, 16, 16);

            // Selection ring
            if (isSelected) {
                gc.setStroke(Color.web("#00ff88"));
                gc.setLineWidth(2);
                gc.strokeOval(cam.mapX - 14, cam.mapY - 14, 28, 28);
            }

            // Camera label
            gc.setFill(Color.web("#c8d6e5"));
            gc.setFont(Font.font("Consolas", 10));
            gc.fillText(cam.name, cam.mapX - 20, cam.mapY + 24);

            // Status indicator
            gc.setFill(cam.active ? Color.web("#00ff88") : Color.web("#e94560"));
            gc.fillOval(cam.mapX + 10, cam.mapY - 12, 6, 6);
        }

        // Compass rose
        gc.setFill(Color.web("#4a5568"));
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText("N", w - 25, 20);
        gc.fillText("S", w - 25, h - 8);
        gc.fillText("E", w - 15, h / 2);
        gc.fillText("W", 5, h / 2);

        // Scale bar
        gc.setStroke(Color.web("#4a5568"));
        gc.setLineWidth(1);
        gc.strokeLine(20, h - 15, 70, h - 15);
        gc.fillText("50m", 35, h - 18);
    }

    public String getSelectedCamera() { return selectedCamera; }
    public void setSelectedCamera(String cam) { this.selectedCamera = cam; render(); }
}
