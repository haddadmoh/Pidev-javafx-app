package com.esprit.controllers.front;

import com.esprit.models.User;
import com.esprit.utils.WindowManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;


import java.io.IOException;

public class PatientController {

    @FXML private Button messagesBtn;
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
    private void handleLogout(ActionEvent event) throws Exception {
        // Get the stage
        Stage stage;
        if (event.getSource() instanceof MenuItem menuItem) {
            stage = (Stage) menuItem.getParentPopup().getOwnerNode().getScene().getWindow();
        } else {
            stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        }

        // Load the login view
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));

        // Use the WindowManager to change scene
        WindowManager.changeScene(
                stage,
                root,
                getClass().getResource("/styles/Back/styles.css").toExternalForm()
        );
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

    @FXML
    public void handleMessages() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/ConversationsList.fxml"));
            Parent content = loader.load();

            // Clear existing content and add new content
            contentPane.getChildren().setAll(content);

            // Pass current user ID to the form controller
            ConversationsListController controller = loader.getController();
            controller.setCurrentUser(currentUser);

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to error message
            contentPane.getChildren().setAll(new Label("Failed to load: " + "/views/Front/ConversationsList.fxml"));
        }
    }

    // Helper method to load content into the main area
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Clear existing content and add new content
            contentPane.getChildren().setAll(content);

            // Only set the current user if the controller is PostListController
            if (loader.getController() instanceof PostListController) {
                PostListController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to error message
            contentPane.getChildren().setAll(new Label("Failed to load: " + fxmlPath));
        }
    }
}