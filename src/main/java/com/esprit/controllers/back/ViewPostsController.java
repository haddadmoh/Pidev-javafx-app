package com.esprit.controllers.back;

import com.esprit.models.Post;
import com.esprit.models.PostCategory;
import com.esprit.services.PostService;
import com.esprit.services.PostCategoryService;
import com.esprit.services.UserService;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewPostsController {
    @FXML private VBox postsList;
    @FXML private Label postsCountLabel;
    @FXML private ComboBox<PostCategory> categoryFilterComboBox;
    @FXML private ComboBox<String> dateSortComboBox;
    // Add this new field
    @FXML private ComboBox<String> typeFilterComboBox;

    private final PostService postService = new PostService();
    private final PostCategoryService categoryService = new PostCategoryService();
    private final UserService userService = new UserService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private List<Post> allPosts;
    private List<PostCategory> allCategories;

    @FXML
    public void initialize() {
        setupFilters();
        loadPosts();
    }

    private void setupFilters() {
        try {

            // Initialize type filter
            typeFilterComboBox.getSelectionModel().selectFirst();
            typeFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Load all categories for filtering
            allCategories = categoryService.getAll();
            categoryFilterComboBox.setItems(FXCollections.observableArrayList(allCategories));
            categoryFilterComboBox.getSelectionModel().selectFirst(); // Select "All" option

            // Set cell factory to show category names
            categoryFilterComboBox.setCellFactory(param -> new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            // Set button cell to show selected category name
            categoryFilterComboBox.setButtonCell(new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Filter by category..." : item.getName());
                }
            });

            // Initialize date sort options
            dateSortComboBox.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First"
            ));
            dateSortComboBox.getSelectionModel().selectFirst();

            // Set up listeners
            categoryFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            dateSortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    filterPosts();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize filters: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadPosts() {
        try {
            allPosts = postService.getAll();
            filterPosts();
        } catch (Exception e) {
            showMessage("Error loading posts: " + e.getMessage(), "error-message");
            e.printStackTrace();
        }
    }

    private void filterPosts() throws SQLException {
        List<Post> filtered = allPosts;

        // Apply type filter
        String selectedType = typeFilterComboBox.getValue();
        if (selectedType != null && !selectedType.equals("All Types")) {
            filtered = filtered.stream()
                    .filter(post -> post.getType().equalsIgnoreCase(selectedType))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        PostCategory selectedCategory = categoryFilterComboBox.getValue();
        if (selectedCategory != null) {
            filtered = filtered.stream()
                    .filter(post -> post.getCategoryId() == selectedCategory.getId())
                    .collect(Collectors.toList());
        }

        // Apply date sorting
        String sortOption = dateSortComboBox.getValue();
        if (sortOption != null) {
            filtered.sort(sortOption.equals("Newest First")
                    ? Comparator.comparing(Post::getCreatedAt).reversed()
                    : Comparator.comparing(Post::getCreatedAt));
        }

        displayPosts(filtered);
    }

    private void displayPosts(List<Post> posts) throws SQLException {
        postsList.getChildren().clear();
        postsCountLabel.setText(String.valueOf(posts.size()) + " posts");
        postsCountLabel.getStyleClass().setAll("count-label");

        if (posts.isEmpty()) {
            showMessage("No posts found matching your criteria", "empty-message");
            return;
        }

        for (Post post : posts) {
            postsList.getChildren().add(createPostCard(post));
        }
    }

    private VBox createPostCard(Post post) throws SQLException {
        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER); // Center all content

        // Image container (will be empty if no image)
        StackPane imageContainer = new StackPane();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.getStyleClass().add("image-container");

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(new File(post.getImage()).toURI().toString()));
                imageView.setFitWidth(300);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true); // Enable smooth scaling
                imageView.getStyleClass().add("post-image");

                // Add subtle loading animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), imageView);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                imageContainer.getChildren().add(imageView);
                fadeIn.play(); // Smooth appearance
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
                // Don't show any error message - just skip the image
            }
        }
        // Only add image container if it has content
        if (!imageContainer.getChildren().isEmpty()) {
            card.getChildren().add(imageContainer);
        }

        // Top row with title and type
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getStyleClass().add("post-header");

        Label titleLabel = new Label(post.getTitle());
        titleLabel.getStyleClass().add("post-title");

        Label typeLabel = new Label(post.getType());
        typeLabel.getStyleClass().add("post-type-" + post.getType().toLowerCase());

        topRow.getChildren().addAll(titleLabel, typeLabel);

        // Second row with category and author
        HBox metaRow = new HBox(10);
        metaRow.getStyleClass().add("post-meta-row");

        String categoryName = allCategories.stream()
                .filter(c -> c.getId() == post.getCategoryId())
                .findFirst()
                .map(PostCategory::getName)
                .orElse("Unknown Category");

        Label categoryLabel = new Label(categoryName);
        categoryLabel.getStyleClass().add("post-category");

        Label authorLabel = new Label("Posted by: " + userService.getUsernameById(post.getAuthorId()));
        authorLabel.getStyleClass().add("post-author");

        metaRow.getChildren().addAll(categoryLabel, authorLabel);

        // Description
        TextFlow descriptionFlow = new TextFlow(new Text(post.getDescription()));
        descriptionFlow.getStyleClass().add("post-description");

        // Date row
        HBox dateRow = new HBox();
        dateRow.getStyleClass().add("post-bottom-row");

        Label dateLabel = new Label("Posted: " + post.getCreatedAt().format(dateFormatter));
        dateLabel.getStyleClass().add("post-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        dateRow.getChildren().addAll(dateLabel, spacer);

        // Action buttons
        HBox actionRow = new HBox();
        actionRow.getStyleClass().add("post-actions");

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("delete-btn");
        deleteBtn.setOnAction(e -> handleDeletePost(post));

        actionRow.getChildren().addAll(spacer, deleteBtn);

        // Add all components to card
        card.getChildren().addAll(topRow, metaRow, descriptionFlow, dateRow, actionRow);

        return card;
    }

    private void handleDeletePost(Post post) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Post");
        confirmAlert.setContentText("Are you sure you want to delete '" + post.getTitle() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.delete(post);
                showAlert("Success", "Post deleted successfully", Alert.AlertType.INFORMATION);
                loadPosts(); // Refresh the list
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete post: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadPosts();
    }

    private void showMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        postsList.getChildren().add(label);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}