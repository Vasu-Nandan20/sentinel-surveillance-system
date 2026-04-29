package com.sentinel.surveillance.controller;

import com.sentinel.surveillance.database.UserAuth;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.function.BiConsumer;

/**
 * Login screen controller with secure authentication and animated UI.
 * Supports Admin and Operator roles with visual branding.
 */
public class LoginController {

    private final BiConsumer<String, String> onLoginSuccess; // (username, role)
    private Label statusLabel;

    public LoginController(BiConsumer<String, String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    /**
     * Creates the login scene with dark defense theme.
     */
    public Scene createScene() {
        // Root container
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0a0e17;");

        // Animated background dots
        Pane bgPane = new Pane();
        bgPane.setMouseTransparent(true);
        for (int i = 0; i < 30; i++) {
            Circle dot = new Circle(1 + Math.random() * 2);
            dot.setFill(Color.web("#00b4d8", 0.1 + Math.random() * 0.15));
            dot.setCenterX(Math.random() * 1400);
            dot.setCenterY(Math.random() * 900);

            TranslateTransition tt = new TranslateTransition(
                    Duration.seconds(8 + Math.random() * 12), dot);
            tt.setByY(-100 - Math.random() * 200);
            tt.setByX(-50 + Math.random() * 100);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.play();

            FadeTransition ft = new FadeTransition(Duration.seconds(3 + Math.random() * 4), dot);
            ft.setFromValue(0.1);
            ft.setToValue(0.3);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();

            bgPane.getChildren().add(dot);
        }

        // Login box
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(50, 50, 50, 50));
        loginBox.setMaxWidth(420);
        loginBox.setMaxHeight(520);
        loginBox.setStyle(
                "-fx-background-color: rgba(17,24,39,0.95);" +
                "-fx-border-color: #1e3a5f;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;");
        loginBox.setEffect(new DropShadow(30, Color.web("#00b4d8", 0.15)));

        // Shield icon (ASCII art style)
        Label shieldIcon = new Label("🛡");
        shieldIcon.setFont(Font.font(48));
        shieldIcon.setStyle("-fx-text-fill: #00b4d8;");

        // Title
        Label titleLabel = new Label("SENTINEL");
        titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 32));
        titleLabel.setStyle("-fx-text-fill: #00ff88;");

        Label subtitleLabel = new Label("REAL-TIME SURVEILLANCE SYSTEM");
        subtitleLabel.setFont(Font.font("Consolas", 12));
        subtitleLabel.setStyle("-fx-text-fill: #4a5568;");

        Label versionLabel = new Label("v1.0.0 | DEFENSE GRADE");
        versionLabel.setFont(Font.font("Consolas", 10));
        versionLabel.setStyle("-fx-text-fill: #2d3748;");

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e3a5f;");

        // Access level label
        Label accessLabel = new Label("SECURE ACCESS REQUIRED");
        accessLabel.setFont(Font.font("Consolas", 11));
        accessLabel.setStyle("-fx-text-fill: #e94560;");

        // Username field
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefHeight(40);
        usernameField.setStyle(
                "-fx-background-color: #0d1117; -fx-text-fill: #e0e0e0;" +
                "-fx-border-color: #1e3a5f; -fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-padding: 8 12; -fx-font-family: 'Consolas'; -fx-font-size: 13px;" +
                "-fx-prompt-text-fill: #4a5568;");

        // Password field
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(40);
        passwordField.setStyle(usernameField.getStyle());

        // Login button
        Button loginButton = new Button("▶  AUTHENTICATE");
        loginButton.setPrefHeight(42);
        loginButton.setPrefWidth(300);
        final String btnNormal =
                "-fx-background-color: linear-gradient(to right, #0f3460, #1e3a5f);" +
                "-fx-text-fill: #00b4d8; -fx-border-color: #00b4d8; -fx-border-width: 1;" +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Consolas';";
        final String btnHover =
                "-fx-background-color: linear-gradient(to right, #1e3a5f, #2a5a8f);" +
                "-fx-text-fill: #ffffff; -fx-border-color: #00b4d8; -fx-border-width: 1;" +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-family: 'Consolas';";
        loginButton.setStyle(btnNormal);
        loginButton.setOnMouseEntered(e -> loginButton.setStyle(btnHover));
        loginButton.setOnMouseExited(e -> loginButton.setStyle(btnNormal));

        // Status label for errors
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Consolas", 11));
        statusLabel.setStyle("-fx-text-fill: #e94560;");

        // Credentials hint
        Label hintLabel = new Label("Credentials: admin/admin or operator/operator");
        hintLabel.setFont(Font.font("Consolas", 9));
        hintLabel.setStyle("-fx-text-fill: #2d3748;");

        // Login action
        Runnable loginAction = () -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("⚠ All fields are required");
                shakeNode(loginBox);
                return;
            }

            if (UserAuth.authenticate(user, pass)) {
                statusLabel.setStyle("-fx-text-fill: #00ff88;");
                statusLabel.setText("✓ Access granted. Loading system...");

                // Fade out and transition
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e ->
                        onLoginSuccess.accept(user, UserAuth.getCurrentRole().name()));
                fadeOut.play();
            } else {
                statusLabel.setText("✗ ACCESS DENIED - Invalid credentials");
                shakeNode(loginBox);
                passwordField.clear();
            }
        };

        loginButton.setOnAction(e -> loginAction.run());
        passwordField.setOnAction(e -> loginAction.run());

        loginBox.getChildren().addAll(
                shieldIcon, titleLabel, subtitleLabel, versionLabel,
                sep, accessLabel,
                usernameField, passwordField,
                loginButton, statusLabel, hintLabel
        );

        root.getChildren().addAll(bgPane, loginBox);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    /** Shake animation for invalid login attempts */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }
}
