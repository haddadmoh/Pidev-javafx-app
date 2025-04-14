package com.esprit.controllers.Back;

import com.esprit.main.App;
import com.esprit.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class PatientController {
    @FXML private Label welcomeLabel;
    @FXML private Button logoutBtn;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        updateUI();
    }

    private void updateUI() {
        welcomeLabel.setText("Welcome, " + currentUser.getUsername());
        // Update other UI elements based on currentUser
    }

    @FXML
    private void handleLogout() throws Exception {
        // Return to log in screen
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(root, App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        stage.setScene(scene);
    }
}