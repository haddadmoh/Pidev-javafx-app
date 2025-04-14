package com.esprit.controllers.back;

import com.esprit.models.PostCategory;
import com.esprit.services.PostCategoryService;
import com.esprit.services.PostService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ViewCategoriesController {
    @FXML private VBox categoriesList;


    private final PostCategoryService categoryService = new PostCategoryService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        loadCategories();
    }

    private void loadCategories() {
        categoriesList.getChildren().clear();

        try {
            List<PostCategory> categories = categoryService.getAll();

            if (categories.isEmpty()) {
                showMessage("No categories found", "empty-message");
                return;
            }

            for (PostCategory category : categories) {
                categoriesList.getChildren().add(createCategoryCard(category));
            }

        } catch (Exception e) {
            showMessage("Error loading categories: " + e.getMessage(), "error-message");
            e.printStackTrace();
        }
    }

    private VBox createCategoryCard(PostCategory category) {
        VBox card = new VBox();
        card.getStyleClass().add("category-card");

        // Top row with name and post count
        HBox topRow = new HBox(10);
        topRow.getStyleClass().add("category-header");

        Label nameLabel = new Label(category.getName());
        nameLabel.getStyleClass().add("category-name");

        // Description
        TextFlow descriptionFlow = new TextFlow(new Text(category.getDescription()));
        descriptionFlow.getStyleClass().add("category-description");

        // Bottom row with date and buttons
        HBox bottomRow = createBottomRow(category);

        card.getChildren().addAll(topRow, descriptionFlow, bottomRow);
        return card;
    }

    private HBox createBottomRow(PostCategory category) {
        HBox row = new HBox(10);
        row.getStyleClass().add("category-bottom-row");

        Label dateLabel = new Label("Created: " + category.getCreatedAt().format(dateFormatter));
        dateLabel.getStyleClass().add("category-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(
                createButton("Edit", "edit-btn", () -> handleEditCategory(category)),
                createButton("Delete", "delete-btn", () -> handleDeleteCategory(category))
        );

        row.getChildren().addAll(dateLabel, spacer, buttonBox);
        return row;
    }

    /**
     * Helper method to create styled buttons consistently.
     * @param text The text to display on the button
     * @param styleClass The CSS class to apply for styling
     * @param action The action to perform when the button is clicked
     * @return A fully configured Button instance
     */
    private Button createButton(String text, String styleClass, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);  // Apply styling

        // Set up the click handler - converts ActionEvent to parameterless Runnable
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * Displays a message in the categories list area.
     * Used for showing status messages like errors or empty states.
     * @param text The message text to display
     * @param styleClass The CSS class to apply for styling
     */
    private void showMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        categoriesList.getChildren().add(label);
    }

    private void handleEditCategory(PostCategory category) {
        System.out.println("edit category");
    }

    private void handleDeleteCategory(PostCategory category) {
        System.out.println("delete category");
    }

    /**
     * Handles the refresh action triggered by the UI button.
     * Simply reloads all categories from the database.
     */
    @FXML
    private void handleRefresh() {
        loadCategories();  // Re-fetch and re-display all categories
    }
}