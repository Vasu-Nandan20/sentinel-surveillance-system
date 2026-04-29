package com.sentinel.surveillance.controller;

import com.sentinel.surveillance.alert.*;
import com.sentinel.surveillance.database.UserAuth;
import com.sentinel.surveillance.detection.*;
import com.sentinel.surveillance.simulation.*;
import com.sentinel.surveillance.ui.MapView;
import com.sentinel.surveillance.utils.*;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class MainController {
    private final Stage stage;
    private final String username, role;
    private Canvas videoCanvas;
    private ListView<Alert> alertListView;
    private TextArea logArea;
    private Label threatLevelLabel, clockLabel, statusLabel;
    private ComboBox<String> cameraSelector;
    private MapView mapView;
    private AlertManager alertManager;
    private CameraSimulator cameraSim;
    private ThreatSimulator threatSim;
    private MotionDetector motionDetector;
    private FaceRecognizer faceRecognizer;
    private ObjectDetector objectDetector;
    private BehaviorAnalyzer behaviorAnalyzer;
    private VoiceCommandSimulator voiceCmd;
    private ReportGenerator reportGen;
    private AnimationTimer videoAnimator;
    private ScheduledExecutorService detectionExecutor;
    private boolean drawingZone = false;
    private List<double[]> currentZonePoints = new ArrayList<>();
    private long frameCount = 0;
    private volatile String currentCameraName = "Gate 1"; // Thread-safe camera name
    private static final double VW = 640, VH = 400;

    public MainController(Stage stage, String username, String role) {
        this.stage = stage; this.username = username; this.role = role;
        alertManager = AlertManager.getInstance();
        cameraSim = new CameraSimulator(VW, VH);
        motionDetector = new MotionDetector();
        faceRecognizer = new FaceRecognizer();
        objectDetector = new ObjectDetector();
        behaviorAnalyzer = new BehaviorAnalyzer();
        voiceCmd = new VoiceCommandSimulator();
        reportGen = new ReportGenerator();
        threatSim = new ThreatSimulator(alertManager);
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0a0e17;");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setRight(buildRightPanel());
        root.setBottom(buildStatusBar());
        Scene scene = new Scene(root, 1400, 900);
        try {
            String css = getClass().getResource("/css/DarkTheme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) { System.err.println("CSS not found, using inline styles"); }
        setupKeyboardShortcuts(scene);
        setupAlertListener();
        return scene;
    }

    private HBox buildHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color:linear-gradient(to right,#0a0e17,#0f1b2d,#0a0e17);-fx-border-color:#1e3a5f;-fx-border-width:0 0 1 0;");
        Label logo = new Label("🛡 SENTINEL");
        logo.setFont(Font.font("Consolas", 22)); logo.setStyle("-fx-text-fill:#00ff88;-fx-font-weight:bold;");
        Label sub = new Label("SURVEILLANCE SYSTEM");
        sub.setFont(Font.font("Consolas", 11)); sub.setStyle("-fx-text-fill:#4a5568;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        threatLevelLabel = new Label("LOW");
        threatLevelLabel.setStyle("-fx-background-color:#00ff88;-fx-text-fill:#000;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:4;-fx-font-family:'Consolas';");
        clockLabel = new Label(); clockLabel.setFont(Font.font("Consolas", 14)); clockLabel.setStyle("-fx-text-fill:#00ff88;");
        Label userLabel = new Label("▸ " + username.toUpperCase() + " [" + role + "]");
        userLabel.setFont(Font.font("Consolas", 11)); userLabel.setStyle("-fx-text-fill:#00b4d8;");
        header.getChildren().addAll(logo, sub, spacer, new Label("THREAT:") {{ setStyle("-fx-text-fill:#8899aa;-fx-font-family:'Consolas';"); }}, threatLevelLabel, clockLabel, userLabel);
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            updateThreatDisplay();
        }));
        clock.setCycleCount(Timeline.INDEFINITE); clock.play();
        return header;
    }

    private SplitPane buildCenter() {
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color:#0a0e17;");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(buildVideoTab(), buildMapTab(), buildWatchlistTab());
        VBox logBox = buildLogPanel();
        split.getItems().addAll(tabs, logBox);
        split.setDividerPositions(0.65);
        return split;
    }

    private Tab buildVideoTab() {
        Tab tab = new Tab("📹 LIVE FEED");
        VBox container = new VBox(8);
        container.setPadding(new Insets(8)); container.setStyle("-fx-background-color:#111827;");
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);
        cameraSelector = new ComboBox<>();
        cameraSelector.getItems().addAll("Gate 1", "Perimeter East", "Watchtower 2");
        cameraSelector.setValue("Gate 1");
        cameraSelector.setStyle("-fx-background-color:#16213e;-fx-text-fill:#c8d6e5;-fx-border-color:#0f3460;-fx-font-family:'Consolas';");
        Button drawZoneBtn = new Button("🔲 Draw Zone");
        drawZoneBtn.setStyle("-fx-background-color:#16213e;-fx-text-fill:#e94560;-fx-border-color:#e94560;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;");
        drawZoneBtn.setOnAction(e -> {
            drawingZone = !drawingZone;
            drawZoneBtn.setText(drawingZone ? "✓ Finish Zone" : "🔲 Draw Zone");
            if (!drawingZone) {
                // Save the zone BEFORE clearing points
                if (currentZonePoints.size() >= 3) {
                    motionDetector.addRestrictedZone(cameraSelector.getValue(), new ArrayList<>(currentZonePoints));
                    appendLog("[ZONE] Restricted zone added to " + cameraSelector.getValue() + " (" + currentZonePoints.size() + " points)");
                } else if (!currentZonePoints.isEmpty()) {
                    appendLog("[ZONE] Need at least 3 points to create a zone (had " + currentZonePoints.size() + ")");
                }
                currentZonePoints.clear();
            } else {
                currentZonePoints.clear();
                appendLog("[ZONE] Click on video to place zone points. Click 'Finish Zone' when done.");
            }
        });
        Button clearZoneBtn = new Button("Clear Zones");
        clearZoneBtn.setStyle("-fx-background-color:#16213e;-fx-text-fill:#8899aa;-fx-border-color:#1e3a5f;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;");
        clearZoneBtn.setOnAction(e -> { motionDetector.clearZones(cameraSelector.getValue()); appendLog("[ZONE] Zones cleared"); });
        Button simBtn = new Button("⚡ Simulate Threat");
        simBtn.setStyle("-fx-background-color:#3d0a0a;-fx-text-fill:#ff6b6b;-fx-border-color:#e94560;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;-fx-font-weight:bold;");
        simBtn.setOnAction(e -> threatSim.triggerManualThreat());
        Button reportBtn = new Button("📄 Report");
        reportBtn.setStyle("-fx-background-color:#0a3d1a;-fx-text-fill:#00ff88;-fx-border-color:#00ff88;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;");
        reportBtn.setOnAction(e -> { String f = reportGen.generateReport(); appendLog("[REPORT] Generated: " + f); });
        reportBtn.setVisible("ADMIN".equals(role));
        controls.getChildren().addAll(new Label("Camera:") {{ setStyle("-fx-text-fill:#8899aa;"); }}, cameraSelector, drawZoneBtn, clearZoneBtn, simBtn, reportBtn);
        videoCanvas = new Canvas(VW, VH);
        videoCanvas.setOnMouseClicked(e -> { if (drawingZone) { currentZonePoints.add(new double[]{e.getX(), e.getY()}); } });
        StackPane videoWrapper = new StackPane(videoCanvas);
        videoWrapper.setStyle("-fx-background-color:#000;-fx-border-color:#0f3460;-fx-border-width:2;-fx-border-radius:4;");
        VBox.setVgrow(videoWrapper, Priority.ALWAYS);
        container.getChildren().addAll(controls, videoWrapper);
        tab.setContent(container);
        return tab;
    }

    private Tab buildMapTab() {
        Tab tab = new Tab("🗺 MAP");
        VBox box = new VBox(8); box.setPadding(new Insets(8)); box.setStyle("-fx-background-color:#111827;");
        mapView = new MapView(300, 300, cameraSim);
        Label mapTitle = new Label("TACTICAL MAP - FACILITY OVERVIEW");
        mapTitle.setStyle("-fx-text-fill:#00b4d8;-fx-font-family:'Consolas';-fx-font-size:12px;");
        box.getChildren().addAll(mapTitle, mapView);
        tab.setContent(box);
        return tab;
    }

    private Tab buildWatchlistTab() {
        Tab tab = new Tab("👤 WATCHLIST");
        VBox box = new VBox(8); box.setPadding(new Insets(10)); box.setStyle("-fx-background-color:#111827;");
        Label title = new Label("FACE RECOGNITION WATCHLIST");
        title.setStyle("-fx-text-fill:#e94560;-fx-font-family:'Consolas';-fx-font-weight:bold;");
        for (FaceRecognizer.WatchlistEntry entry : faceRecognizer.getWatchlist()) {
            HBox card = new HBox(12); card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color:#16213e;-fx-border-color:#1e3a5f;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;");
            Label avatar = new Label("👤");avatar.setFont(Font.font(28));
            VBox info = new VBox(3);
            Label name = new Label(entry.name + " (" + entry.alias + ")");
            name.setStyle("-fx-text-fill:#c8d6e5;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            Label threat = new Label("Threat: " + entry.threatLevel.getLabel());
            threat.setStyle("-fx-text-fill:" + entry.threatLevel.getColor() + ";-fx-font-size:11px;");
            Label desc = new Label(entry.description);
            desc.setStyle("-fx-text-fill:#8899aa;-fx-font-size:10px;"); desc.setWrapText(true); desc.setMaxWidth(400);
            Label lastSeen = new Label("Last seen: " + entry.lastSeen);
            lastSeen.setStyle("-fx-text-fill:#4a5568;-fx-font-size:10px;-fx-font-family:'Consolas';");
            info.getChildren().addAll(name, threat, desc, lastSeen);
            card.getChildren().addAll(avatar, info);
            box.getChildren().add(card);
        }
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true); sp.setStyle("-fx-background-color:#111827;-fx-background:#111827;");
        tab.setContent(sp);
        return tab;
    }

    private VBox buildRightPanel() {
        VBox right = new VBox(8);
        right.setPrefWidth(360); right.setPadding(new Insets(8));
        right.setStyle("-fx-background-color:#111827;-fx-border-color:#1e3a5f;-fx-border-width:0 0 0 1;");
        Label alertTitle = new Label("⚠ ALERT DASHBOARD");
        alertTitle.setStyle("-fx-text-fill:#e94560;-fx-font-weight:bold;-fx-font-family:'Consolas';-fx-font-size:14px;");
        alertListView = new ListView<>(alertManager.getAlerts());
        alertListView.setCellFactory(lv -> new AlertController());
        alertListView.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1e3a5f;");
        VBox.setVgrow(alertListView, Priority.ALWAYS);
        // Voice command bar
        Label vcLabel = new Label("🎤 COMMAND TERMINAL");
        vcLabel.setStyle("-fx-text-fill:#00ff88;-fx-font-family:'Consolas';-fx-font-size:11px;");
        TextField cmdField = new TextField();
        cmdField.setPromptText("Type command... (try 'help')");
        cmdField.setStyle("-fx-background-color:#0a0e17;-fx-text-fill:#00ff88;-fx-border-color:#00ff88;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-family:'Consolas';-fx-prompt-text-fill:#2d5a3d;");
        TextArea cmdOutput = new TextArea();
        cmdOutput.setPrefHeight(80); cmdOutput.setEditable(false); cmdOutput.setWrapText(true);
        cmdOutput.setStyle("-fx-background-color:#0a0e17;-fx-text-fill:#00ff88;-fx-control-inner-background:#0a0e17;-fx-font-family:'Consolas';-fx-font-size:10px;-fx-border-color:#1e3a5f;");
        voiceCmd.setOutputCallback(msg -> Platform.runLater(() -> cmdOutput.appendText(msg + "\n")));
        voiceCmd.setReportAction(() -> { String f = reportGen.generateReport(); appendLog("[REPORT] " + f); });
        voiceCmd.setCameraSwitch(cam -> Platform.runLater(() -> cameraSelector.setValue(cam)));
        cmdField.setOnAction(e -> { voiceCmd.processCommand(cmdField.getText()); cmdField.clear(); });
        right.getChildren().addAll(alertTitle, alertListView, vcLabel, cmdField, cmdOutput);
        return right;
    }

    private VBox buildLogPanel() {
        VBox box = new VBox(6); box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color:#111827;");
        Label title = new Label("📋 SYSTEM LOG");
        title.setStyle("-fx-text-fill:#00b4d8;-fx-font-family:'Consolas';-fx-font-weight:bold;");
        logArea = new TextArea();
        logArea.setEditable(false); logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background:#0d1117;-fx-text-fill:#8899aa;-fx-font-family:'Consolas';-fx-font-size:11px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        alertManager.setLogCallback(this::appendLog);
        appendLog("[SYSTEM] SENTINEL Surveillance System initialized");
        appendLog("[SYSTEM] User " + username + " logged in as " + role);
        appendLog("[SYSTEM] 3 cameras online: Gate 1, Perimeter East, Watchtower 2");
        box.getChildren().addAll(title, logArea);
        return box;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 15, 6, 15));
        bar.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1e3a5f;-fx-border-width:1 0 0 0;");
        statusLabel = new Label("Active Cameras: 3 | Alerts: 0 | System: ONLINE");
        statusLabel.setStyle("-fx-text-fill:#4a5568;-fx-font-family:'Consolas';-fx-font-size:11px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ver = new Label("SENTINEL v1.0.0 | DEFENSE GRADE | CLASSIFICATION: CONFIDENTIAL");
        ver.setStyle("-fx-text-fill:#2d3748;-fx-font-family:'Consolas';-fx-font-size:10px;");
        bar.getChildren().addAll(statusLabel, spacer, ver);
        return bar;
    }

    public void start() {
        threatSim.start();
        appendLog("[SYSTEM] Threat simulation engine started");
        detectionExecutor = Executors.newScheduledThreadPool(2, r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        detectionExecutor.scheduleAtFixedRate(this::runDetection, 2, 3, TimeUnit.SECONDS);
        videoAnimator = new AnimationTimer() {
            @Override public void handle(long now) {
                frameCount++;
                cameraSim.update();
                renderVideoFeed();
                if (frameCount % 120 == 0 && mapView != null) mapView.render();
                if (frameCount % 60 == 0) updateStatus();
            }
        };
        videoAnimator.start();
        appendLog("[SYSTEM] Video feed and detection engines started");
    }

    private void renderVideoFeed() {
        GraphicsContext gc = videoCanvas.getGraphicsContext2D();
        String cam = cameraSelector.getValue();
        currentCameraName = cam; // Sync for detection thread
        CameraSimulator.Camera camera = cameraSim.getCamera(cam);
        // Background
        gc.setFill(Color.web("#0d1117")); gc.fillRect(0, 0, VW, VH);
        // Grid overlay
        gc.setStroke(Color.web("#1e3a5f", 0.15)); gc.setLineWidth(0.5);
        for (double x = 0; x < VW; x += 40) gc.strokeLine(x, 0, x, VH);
        for (double y = 0; y < VH; y += 40) gc.strokeLine(0, y, VW, y);
        // Ground plane
        gc.setFill(Color.web("#111827")); gc.fillRect(0, VH * 0.7, VW, VH * 0.3);
        gc.setStroke(Color.web("#1e3a5f", 0.3)); gc.strokeLine(0, VH * 0.7, VW, VH * 0.7);
        // Draw entities
        for (CameraSimulator.Entity e : camera.entities) {
            if ("person".equals(e.type)) {
                gc.setFill(Color.web("#00b4d8", 0.8));
                gc.fillRect(e.x - e.width / 2, e.y - e.height, e.width, e.height);
                gc.setFill(Color.web("#00b4d8")); gc.fillOval(e.x - 5, e.y - e.height - 10, 10, 10);
                gc.setStroke(Color.web("#00ff88", 0.4)); gc.setLineWidth(1);
                gc.strokeRect(e.x - e.width / 2 - 4, e.y - e.height - 14, e.width + 8, e.height + 18);
            } else if ("vehicle".equals(e.type)) {
                gc.setFill(Color.web("#ff9800", 0.7));
                gc.fillRect(e.x, e.y, e.width, e.height);
                gc.setFill(Color.web("#ffc107", 0.5)); gc.fillRect(e.x + 8, e.y + 3, 15, 10);
                gc.setStroke(Color.web("#ff9800", 0.5)); gc.setLineWidth(1);
                gc.strokeRect(e.x - 3, e.y - 3, e.width + 6, e.height + 6);
            }
        }
        // Draw restricted zones
        for (List<double[]> zone : motionDetector.getZones(cam)) {
            if (zone.size() < 2) continue;
            gc.setFill(Color.web("#e94560", 0.15)); gc.setStroke(Color.web("#e94560", 0.7)); gc.setLineWidth(2);
            double[] xs = zone.stream().mapToDouble(p -> p[0]).toArray();
            double[] ys = zone.stream().mapToDouble(p -> p[1]).toArray();
            gc.fillPolygon(xs, ys, zone.size());
            gc.strokePolygon(xs, ys, zone.size());
            gc.setFill(Color.web("#e94560")); gc.setFont(Font.font("Consolas", 10));
            gc.fillText("RESTRICTED", xs[0], ys[0] - 5);
        }
        // Draw zone-in-progress
        if (drawingZone && !currentZonePoints.isEmpty()) {
            gc.setFill(Color.web("#ffc107", 0.3)); gc.setStroke(Color.web("#ffc107")); gc.setLineWidth(1.5);
            for (double[] p : currentZonePoints) gc.fillOval(p[0] - 4, p[1] - 4, 8, 8);
            if (currentZonePoints.size() > 1) {
                for (int i = 0; i < currentZonePoints.size() - 1; i++) {
                    gc.strokeLine(currentZonePoints.get(i)[0], currentZonePoints.get(i)[1],
                            currentZonePoints.get(i + 1)[0], currentZonePoints.get(i + 1)[1]);
                }
            }
        }
        // Scan line effect
        double scanY = (frameCount * 2) % VH;
        gc.setStroke(Color.web("#00ff88", 0.06)); gc.setLineWidth(1);
        gc.strokeLine(0, scanY, VW, scanY);
        // Camera info overlay
        gc.setFill(Color.web("#0a0e17", 0.7)); gc.fillRect(0, 0, VW, 28);
        gc.setFont(Font.font("Consolas", 12)); gc.setFill(Color.web("#00ff88"));
        gc.fillText("● REC  " + camera.name + "  [" + camera.id + "]", 10, 18);
        gc.setFill(Color.web("#8899aa")); gc.setFont(Font.font("Consolas", 11));
        gc.fillText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SS")), VW - 100, 18);
        // Bottom bar
        gc.setFill(Color.web("#0a0e17", 0.7)); gc.fillRect(0, VH - 22, VW, 22);
        gc.setFill(Color.web("#4a5568")); gc.setFont(Font.font("Consolas", 10));
        gc.fillText(String.format("FPS: 60 | Entities: %d | Zones: %d | Frame: %d",
                camera.entities.size(), motionDetector.getZones(cam).size(), frameCount), 10, VH - 7);
    }

    private void runDetection() {
        try {
            String cam = currentCameraName; // Use cached value (thread-safe)
            List<double[]> positions = cameraSim.getEntityPositions(cam);
            // Motion detection
            List<Alert> motionAlerts = motionDetector.analyze(cam, positions, VW, VH);
            motionAlerts.forEach(alertManager::addAlert);
            // Face recognition
            int personCount = (int) cameraSim.getCamera(cam).entities.stream()
                    .filter(e -> "person".equals(e.type)).count();
            Alert faceAlert = faceRecognizer.analyze(cam, personCount, VW, VH);
            if (faceAlert != null) alertManager.addAlert(faceAlert);
            // Object detection
            List<Alert> objAlerts = objectDetector.analyze(cam, VW, VH);
            objAlerts.forEach(alertManager::addAlert);
            // Behavior analysis
            List<Alert> behAlerts = behaviorAnalyzer.analyze(cam, positions, VW, VH);
            behAlerts.forEach(alertManager::addAlert);
        } catch (Exception e) {
            System.err.println("[Detection] Error: " + e.getMessage());
        }
    }

    private void updateThreatDisplay() {
        ThreatLevel level = alertManager.getCurrentThreatLevel();
        threatLevelLabel.setText(level.getLabel());
        String style = switch (level) {
            case LOW -> "-fx-background-color:#00ff88;-fx-text-fill:#000;";
            case MEDIUM -> "-fx-background-color:#ffc107;-fx-text-fill:#000;";
            case HIGH -> "-fx-background-color:#ff9800;-fx-text-fill:#000;";
            case CRITICAL -> "-fx-background-color:#e94560;-fx-text-fill:#fff;";
        };
        threatLevelLabel.setStyle(style + "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:4;-fx-font-family:'Consolas';");
    }

    private void updateStatus() {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(String.format("Active Cameras: 3 | Alerts: %d | Critical: %d | System: ONLINE",
                        alertManager.getTotalAlertCount(), alertManager.getCriticalCount()));
            }
        });
    }

    private void setupAlertListener() {
        alertManager.getAlerts().addListener((ListChangeListener<Alert>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Alert a : c.getAddedSubList()) {
                        if (a.getThreatLevel() == ThreatLevel.CRITICAL || a.getThreatLevel() == ThreatLevel.HIGH) {
                            updateThreatDisplay();
                        }
                    }
                }
            }
        });
    }

    private void setupKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.L) voiceCmd.processCommand("show last alert");
            else if (e.isControlDown() && e.getCode() == KeyCode.R) {
                String f = reportGen.generateReport(); appendLog("[REPORT] " + f);
            }
            else if (e.isControlDown() && e.getCode() == KeyCode.A) voiceCmd.processCommand("show critical alerts");
        });
    }

    private void appendLog(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText("[" + ts + "] " + msg + "\n");
            }
        });
    }

    public void stop() {
        if (videoAnimator != null) videoAnimator.stop();
        if (threatSim != null) threatSim.stop();
        if (detectionExecutor != null) detectionExecutor.shutdownNow();
    }
}
