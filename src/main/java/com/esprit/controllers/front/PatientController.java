package com.esprit.controllers.front;

import com.esprit.main.App;
import com.esprit.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PatientController {

    @FXML private Label usernameLabel;
    @FXML private Button logoutBtn;
    @FXML private StackPane contentPane;


    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        updateUI();
    }

    private void updateUI() {
        usernameLabel.setText(currentUser.getUsername());
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

    @FXML
    private void showCreatePostForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/CreateFormPost.fxml"));
            Parent form = loader.load();

            // Pass current user ID to the form controller
            CreatePostController controller = loader.getController();
            controller.setCurrentUserId(currentUser.getId());

            // Create a new stage for the form
            Stage formStage = new Stage();
            formStage.setTitle("Create New Post");
            formStage.setScene(new Scene(form));
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.showAndWait();



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showPostsList() {
        loadContent("/views/Front/PostList.fxml");
    }

    @FXML
    private void handleProductsClick() {
        System.out.println("products clicked");
    }

    @FXML
    private void handleEventsClick() {
        System.out.println("events clicked");
    }

    // Helper method to load content into the main area
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Clear existing content and add new content
            contentPane.getChildren().setAll(content);

            PostListController controller = loader.getController();
            controller.setCurrentUser(currentUser);

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to error message
            contentPane.getChildren().setAll(new Label("Failed to load: " + fxmlPath));
        }
    }


}