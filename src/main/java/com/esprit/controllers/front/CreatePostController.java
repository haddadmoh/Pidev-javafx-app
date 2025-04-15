package com.esprit.controllers.front;

import com.esprit.models.Post;
import com.esprit.services.PostCategoryService;
import com.esprit.services.PostService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.esprit.models.PostCategory;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePostController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label imageNameLabel;
    @FXML private Button browseButton;
    @FXML private Button submitButton;

    private File selectedImageFile;
    private int currentUserId;
    private PostCategoryService postcategoryService;

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    @FXML
    public void initialize() {
        postcategoryService = new PostCategoryService();

        // Initialize type options
        typeComboBox.getItems().addAll("Offre", "Demande");

        // Load categories from database
        try {
            List<PostCategory> categories = postcategoryService.getAll();
            for (PostCategory category : categories) {
                categoryComboBox.getItems().add(category.getName());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to load categories: " + e.getMessage());
            e.printStackTrace();
        }

        // Disable submit button until required fields are filled
        submitButton.disableProperty().bind(
                titleField.textProperty().isEmpty()
                        .or(descriptionField.textProperty().isEmpty())
                        .or(typeComboBox.valueProperty().isNull())
                        .or(categoryComboBox.valueProperty().isNull())
        );
    }

    @FXML
    private void handleImageBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Post Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedImageFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedImageFile != null) {
            imageNameLabel.setText(selectedImageFile.getName());
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            // Create new post object
            Post newPost = new Post();
            newPost.setTitle(titleField.getText());
            newPost.setDescription(descriptionField.getText());
            newPost.setType(typeComboBox.getValue());
            newPost.setCategoryId(postcategoryService.getCategoryIdByName(categoryComboBox.getValue()));
            newPost.setAuthorId(currentUserId);
            newPost.setImage(selectedImageFile != null ? selectedImageFile.getAbsolutePath() : null);
            newPost.setCreatedAt(LocalDateTime.now());
            newPost.setEnabled(true);

            // Save to database
            PostService postService = new PostService();
            postService.add(newPost);

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success", "Post created successfully!");

            // Close the form
            ((Stage) submitButton.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) submitButton.getScene().getWindow()).close();
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}