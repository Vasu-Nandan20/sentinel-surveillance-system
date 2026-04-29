package com.sentinel.surveillance.controller;

import com.sentinel.surveillance.alert.Alert;
import com.sentinel.surveillance.alert.ThreatLevel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Custom ListCell renderer for the alert dashboard panel.
 * Displays alerts with color-coded severity, timestamp, and details.
 */
public class AlertController extends ListCell<Alert> {

    @Override
    protected void updateItem(Alert alert, boolean empty) {
        super.updateItem(alert, empty);

        if (empty || alert == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        // Main container
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(6, 8, 6, 8));

        // Severity indicator (colored dot)
        Circle indicator = new Circle(6);
        indicator.setFill(Color.web(alert.getThreatLevel().getColor()));
        if (alert.getThreatLevel() == ThreatLevel.CRITICAL) {
            indicator.setStroke(Color.web("#ffffff", 0.5));
            indicator.setStrokeWidth(2);
        }

        // Alert info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Top row: type + confidence
        HBox topRow = new HBox(8);
        Label typeLabel = new Label(alert.getType());
        typeLabel.setStyle("-fx-text-fill: " + alert.getThreatLevel().getColor() +
                "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-font-family: 'Consolas';");

        Label confLabel = new Label(String.format("%.0f%%", alert.getConfidence()));
        confLabel.setStyle("-fx-text-fill: #8899aa; -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        Label levelBadge = new Label(alert.getThreatLevel().getLabel());
        levelBadge.getStyleClass().add(alert.getThreatLevel().getStyleClass());
        levelBadge.setStyle(levelBadge.getStyle() + "; -fx-font-size: 9px; -fx-padding: 1 6 1 6;");

        topRow.getChildren().addAll(typeLabel, levelBadge, confLabel);

        // Description
        Label descLabel = new Label(alert.getDescription());
        descLabel.setStyle("-fx-text-fill: #c8d6e5; -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(280);

        // Bottom row: camera + timestamp
        HBox bottomRow = new HBox(8);
        Label camLabel = new Label("📹 " + alert.getCameraId());
        camLabel.setStyle("-fx-text-fill: #00b4d8; -fx-font-size: 10px;");

        Label timeLabel = new Label(alert.getFormattedTimestamp());
        timeLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 10px; -fx-font-family: 'Consolas';");

        bottomRow.getChildren().addAll(camLabel, timeLabel);

        info.getChildren().addAll(topRow, descLabel, bottomRow);

        container.getChildren().addAll(indicator, info);

        // Apply severity background style
        container.setStyle(getSeverityStyle(alert.getThreatLevel()));

        setGraphic(container);
        setText(null);
        setStyle("-fx-background-color: transparent; -fx-padding: 2;");
    }

    private String getSeverityStyle(ThreatLevel level) {
        return switch (level) {
            case LOW -> "-fx-background-color: #0a2e1a; -fx-border-color: #00ff88; " +
                    "-fx-border-width: 0 0 0 3; -fx-background-radius: 4;";
            case MEDIUM -> "-fx-background-color: #2e2a0a; -fx-border-color: #ffc107; " +
                    "-fx-border-width: 0 0 0 3; -fx-background-radius: 4;";
            case HIGH -> "-fx-background-color: #2e1a0a; -fx-border-color: #ff9800; " +
                    "-fx-border-width: 0 0 0 3; -fx-background-radius: 4;";
            case CRITICAL -> "-fx-background-color: #3d0a0a; -fx-border-color: #e94560; " +
                    "-fx-border-width: 0 0 0 3; -fx-background-radius: 4;";
        };
    }
}
