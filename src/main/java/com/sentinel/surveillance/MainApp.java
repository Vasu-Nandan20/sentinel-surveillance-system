package com.sentinel.surveillance;

import com.sentinel.surveillance.controller.LoginController;
import com.sentinel.surveillance.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * SENTINEL - Real-Time Intelligent Surveillance System
 * Main application entry point. Launches login, then transitions to dashboard.
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private MainController mainController;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("SENTINEL - Real-Time Surveillance System");
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);

        // Show login screen first
        LoginController login = new LoginController(this::onLoginSuccess);
        stage.setScene(login.createScene());
        stage.show();

        System.out.println("============================================");
        System.out.println(" SENTINEL Surveillance System v1.0.0");
        System.out.println(" Defense-Grade Real-Time Monitoring");
        System.out.println("============================================");
        System.out.println(" Login: admin/admin or operator/operator");
        System.out.println("============================================");
    }

    private void onLoginSuccess(String username, String role) {
        mainController = new MainController(primaryStage, username, role);
        primaryStage.setScene(mainController.createScene());
        mainController.start();
    }

    @Override
    public void stop() {
        if (mainController != null) mainController.stop();
        System.out.println("[SENTINEL] System shutdown complete.");
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
