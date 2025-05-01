package com.esprit.utils;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WindowManager {

    /**
     * Changes the scene while preserving the exact current window dimensions
     *
     * @param stage Current stage
     * @param root New root node to display
     * @param cssPath CSS stylesheet path (optional, can be null)
     */
    public static void changeScene(Stage stage, Parent root, String cssPath) {
        // Save ALL current window properties
        final boolean wasMaximized = stage.isMaximized();
        final double stageX = stage.getX();
        final double stageY = stage.getY();
        final double stageWidth = stage.getWidth();
        final double stageHeight = stage.getHeight();

        // Create new scene that preserves content size
        Scene scene = new Scene(root);

        // Add stylesheet if provided
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        // Apply the scene
        stage.setScene(scene);

        // Restore exact position and size
        stage.setX(stageX);
        stage.setY(stageY);
        stage.setWidth(stageWidth);
        stage.setHeight(stageHeight);

        // Ensure maximized state is preserved
        Platform.runLater(() -> {
            stage.setMaximized(wasMaximized);
        });
    }
}