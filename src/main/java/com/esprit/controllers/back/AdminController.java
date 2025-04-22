package com.esprit.controllers.back;

import com.esprit.main.App;
import com.esprit.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class AdminController {
    @FXML private BorderPane adminRoot;
    @FXML private Label adminNameLabel;
    @FXML private VBox submenu; // Add this reference

    private User user;

    public void setUser(User user) {
        this.user = user;
        adminNameLabel.setText(user.getUsername());
    }

    private boolean isSubmenuVisible = false;

    public void initialize() {
        // Hide submenu by default
        submenu.setVisible(false);
        submenu.setManaged(false); // This makes it not take up space when hidden
    }

    @FXML
    private void handlePostsManagement() {
        isSubmenuVisible = !isSubmenuVisible; // Toggle visibility
        submenu.setVisible(isSubmenuVisible);
        submenu.setManaged(isSubmenuVisible);
    }

    @FXML
    private void handleAddCategory() {
        loadContent("/views/Back/AddCategory.fxml");
    }

    @FXML
    private void handleViewCategories() {
        loadContent("/views/Back/ViewCategories.fxml");
    }

    @FXML
    private void handleViewPosts() {
        loadContent("/views/Back/ViewPosts.fxml");
    }

    @FXML
    private void handleProductsManagement() {
        System.out.println("Products Management clicked");
    }

    @FXML
    private void handleEventsManagement() {
        System.out.println("Events Management clicked");
    }

    @FXML
    private void handleAccountSettings() {
        System.out.println("Account Settings clicked");
    }

    @FXML
    private void handleLogout() throws Exception {
        // Return to log in screen
        Stage stage = (Stage) adminRoot.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(root, App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/Back/styles.css").toExternalForm());
        stage.setScene(scene);
    }


    // Method to load content into the center area
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Set the loaded content to the center of BorderPane
            adminRoot.setCenter(content);

        } catch (IOException e) {
            e.printStackTrace();
            // Handle error (maybe show an alert)
        }
    }
}