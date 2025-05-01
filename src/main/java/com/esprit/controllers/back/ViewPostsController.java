package com.esprit.controllers.back;

import com.esprit.models.Post;
import com.esprit.models.PostCategory;
import com.esprit.services.PostService;
import com.esprit.services.PostCategoryService;
import com.esprit.services.ReactionsService;
import com.esprit.services.UserService;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.util.Map;
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
            typeFilterComboBox.setItems(FXCollections.observableArrayList(
                    "All Types", "Offre", "Demande"
            ));
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

            // Create "All Categories" option
            PostCategory allCategoriesOption = new PostCategory(-1, "All Categories");
            allCategories.add(0, allCategoriesOption); // Add at beginning

            categoryFilterComboBox.setItems(FXCollections.observableArrayList(allCategories));
            categoryFilterComboBox.getSelectionModel().selectFirst(); // Select "All Categories" by default

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

        // Apply category filter (skip if "All Categories" is selected)
        PostCategory selectedCategory = categoryFilterComboBox.getValue();
        if (selectedCategory != null && selectedCategory.getId() != -1) {
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
        card.setAlignment(Pos.TOP_CENTER);

        // Title Row - styled like category header
        HBox titleRow = new HBox(10);
        titleRow.getStyleClass().add("category-header");

        Label titleLabel = new Label(post.getTitle());
        titleLabel.getStyleClass().add("category-name");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(true);

        Label typeLabel = new Label(post.getType());
        // Keep your original type styling classes
        typeLabel.getStyleClass().add("post-type-" + post.getType().toLowerCase());

        titleRow.getChildren().addAll(titleLabel, typeLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Meta Info - styled like category date
        Label metaLabel = new Label(String.format("%s • %s • %s",
                userService.getUsernameById(post.getAuthorId()),
                post.getCreatedAt().format(dateFormatter),
                allCategories.stream()
                        .filter(c -> c.getId() == post.getCategoryId())
                        .findFirst()
                        .map(PostCategory::getName)
                        .orElse("Unknown Category")));
        metaLabel.getStyleClass().add("category-date");

        // Add reactions row
        HBox reactionsRow = createReactionsRow(post.getId());
        reactionsRow.getStyleClass().add("reactions-row");
        reactionsRow.setSpacing(10);
        reactionsRow.setAlignment(Pos.CENTER_LEFT);

        // Description - styled like category description but keeping your text flow
        TextFlow descriptionFlow = new TextFlow(new Text(post.getDescription()));
        descriptionFlow.getStyleClass().add("category-description");
        descriptionFlow.setMaxWidth(Double.MAX_VALUE);

        // Image Container - PRESERVING YOUR EXACT IMPLEMENTATION
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setVisible(false);
        imageContainer.setManaged(false);

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            try {
                ImageView imageView = new ImageView();
                Image image = new Image(new File(post.getImage()).toURI().toString());

                // Your original image sizing logic preserved exactly
                double maxWidth = 300;
                double maxHeight = 200;
                double ratio = Math.min(
                        maxWidth / image.getWidth(),
                        maxHeight / image.getHeight()
                );

                imageView.setImage(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(image.getWidth() * ratio);
                imageView.setFitHeight(image.getHeight() * ratio);
                imageView.setSmooth(true);

                imageContainer.getChildren().add(imageView);
            } catch (Exception e) {
                Label errorLabel = new Label("Image unavailable");
                errorLabel.getStyleClass().add("error-label");
                imageContainer.getChildren().add(errorLabel);
            }
        } else {
            Label noImageLabel = new Label("No image available");
            noImageLabel.getStyleClass().add("no-image-label");
            imageContainer.getChildren().add(noImageLabel);
        }

        // Bottom Row - styled like category actions but keeping your toggle button
        HBox bottomRow = new HBox();
        bottomRow.getStyleClass().add("action-buttons");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Keeping your original toggle button implementation
        Button toggleButton = new Button("More Details");
        toggleButton.getStyleClass().add("toggle-button");
        toggleButton.setOnAction(e -> {
            boolean visible = !imageContainer.isVisible();
            imageContainer.setVisible(visible);
            imageContainer.setManaged(visible);
            toggleButton.setText(visible ? "Hide Details" : "More Details");
        });

        // Delete button styled like category view
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("delete-btn");
        deleteBtn.setOnAction(e -> handleDeletePost(post));

        bottomRow.getChildren().addAll(spacer, toggleButton, deleteBtn);

        // Building card structure (similar to category view but preserving your order)
        card.getChildren().addAll(titleRow, reactionsRow, metaLabel, descriptionFlow, imageContainer, bottomRow);

        return card;
    }
    private HBox createReactionsRow(int postId) throws SQLException {
        HBox reactionsRow = new HBox(10);

        ReactionsService reactionsService = new ReactionsService();
        // Get reaction counts for this post
        Map<String, Integer> reactions = reactionsService.getReactionCountsForPost(postId);

        // Display each reaction type with its count
        for (Map.Entry<String, Integer> entry : reactions.entrySet()) {
            String reactionType = entry.getKey();
            int count = entry.getValue();

            // Create reaction display
            HBox reactionBox = new HBox(5);
            reactionBox.setAlignment(Pos.CENTER);
            reactionBox.getStyleClass().addAll("reaction-box", "reaction-" + reactionType.toLowerCase());

            // Add reaction icon (using a label as placeholder)
            Label iconLabel = new Label(getReactionEmoji(reactionType));
            iconLabel.getStyleClass().add("reaction-icon");

            // Add count
            Label countLabel = new Label(String.valueOf(count));
            countLabel.getStyleClass().add("reaction-count");

            reactionBox.getChildren().addAll(iconLabel, countLabel);
            reactionsRow.getChildren().add(reactionBox);
        }

        return reactionsRow;
    }

    // Helper method to get emoji for reaction types
    private String getReactionEmoji(String reactionType) {
        switch (reactionType) {
            case "LIKE": return "👍";
            case "LOVE": return "❤️";
            case "SAD": return "😢";
            default: return "?";
        }
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