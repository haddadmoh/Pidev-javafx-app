package com.esprit.controllers.front;

import com.esprit.models.Post;
import com.esprit.models.PostCategory;
import com.esprit.models.User;
import com.esprit.services.PostCategoryService;
import com.esprit.services.PostService;
import com.esprit.services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostListController {
    @FXML private VBox postsContainer;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<PostCategory> categoryFilterCombo;
    @FXML private ComboBox<String> dateSortCombo;

    private final PostService postService = new PostService();
    private final UserService userService = new UserService();
    private final PostCategoryService categoryService = new PostCategoryService();
    private List<Post> allPosts;
    private User currentUser; // To track the logged-in user

    // Call this method when setting the controller
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            initialize();
        }
    }

    @FXML
    public void initialize() {
        setupFilters();
        loadPosts();
    }

    private void setupFilters() {
        try {
            // Initialize type filter
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Types", "Offre", "Demande"
            ));
            typeFilterCombo.getSelectionModel().selectFirst();

            // Initialize category filter
            List<PostCategory> categories = categoryService.getAll();
            categoryFilterCombo.setItems(FXCollections.observableArrayList(categories));
            categoryFilterCombo.setCellFactory(param -> new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            categoryFilterCombo.setButtonCell(new ListCell<PostCategory>() {
                @Override
                protected void updateItem(PostCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "All Categories" : item.getName());
                }
            });

            // Initialize date sort
            dateSortCombo.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First"
            ));
            dateSortCombo.getSelectionModel().selectFirst();

            // Set up filter change listeners
            typeFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            categoryFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            dateSortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        } catch (Exception e) {
            showError("Error initializing filters: " + e.getMessage());
        }
    }

    private void loadPosts() {
        try {
            allPosts = postService.getAll();
            applyFilters();
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        if (allPosts == null) return;

        List<Post> filteredPosts = allPosts.stream()
                // Filter by type
                .filter(post -> {
                    String selectedType = typeFilterCombo.getValue();
                    return selectedType == null || selectedType.equals("All Types") ||
                            post.getType().equalsIgnoreCase(selectedType);
                })
                // Filter by category
                .filter(post -> {
                    PostCategory selectedCategory = categoryFilterCombo.getValue();
                    return selectedCategory == null ||
                            post.getCategoryId() == selectedCategory.getId();
                })
                .collect(Collectors.toList());

        // Sort by date
        String sortOption = dateSortCombo.getValue();
        if (sortOption != null) {
            filteredPosts.sort(sortOption.equals("Newest First")
                    ? Comparator.comparing(Post::getCreatedAt).reversed()
                    : Comparator.comparing(Post::getCreatedAt));
        }

        displayPosts(filteredPosts);
    }

    private void displayPosts(List<Post> posts) {
        postsContainer.getChildren().clear();

        if (posts.isEmpty()) {
            Label noPostsLabel = new Label("No posts found matching your filters");
            noPostsLabel.getStyleClass().add("no-posts-label");
            postsContainer.getChildren().add(noPostsLabel);
            return;
        }

        for (Post post : posts) {
            try {
                String authorUsername = userService.getUsernameById(post.getAuthorId());
                String categoryName = categoryService.getCategoryNameById(post.getCategoryId());
                postsContainer.getChildren().add(
                        createPostCard(post, authorUsername, categoryName)
                );
            } catch (SQLException e) {
                System.err.println("Error loading post details: " + e.getMessage());
            }
        }
    }

    private Node createPostCard(Post post, String authorUsername, String categoryName) {
        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(15));
        card.setMaxWidth(680);

        // Header with title
        Label titleLabel = new Label(post.getTitle());
        titleLabel.getStyleClass().add("post-title");

        // Tags container (type + category)
        HBox tagsContainer = new HBox(8);
        tagsContainer.getStyleClass().add("post-tags");

        Label typeLabel = new Label(post.getType());
        typeLabel.getStyleClass().addAll("post-type", post.getType().toLowerCase());

        Label categoryLabel = new Label(categoryName);
        categoryLabel.getStyleClass().add("post-category");

        tagsContainer.getChildren().addAll(typeLabel, categoryLabel);

        // Header container
        VBox headerContainer = new VBox(8);
        headerContainer.getStyleClass().add("post-header");
        headerContainer.getChildren().addAll(titleLabel, tagsContainer);

        // Meta info
        Label metaLabel = new Label(String.format(
                "Posted by %s • %s",
                authorUsername,
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
        ));
        metaLabel.getStyleClass().add("post-meta");

        // Description
        Text description = new Text(post.getDescription());
        description.getStyleClass().add("post-description");
        description.setWrappingWidth(650);

        // Image container
        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("post-image-container");
        imageContainer.setAlignment(Pos.CENTER);

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image("file:" + post.getImage()));
                imageView.getStyleClass().add("post-image");
                imageView.setFitWidth(600);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageContainer.getChildren().add(imageView);
            } catch (Exception e) {
                Label errorLabel = new Label("[Image unavailable]");
                errorLabel.getStyleClass().add("post-meta");
                imageContainer.getChildren().add(errorLabel);
            }
        }

        // Action buttons (only show if current user is the author)
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.getStyleClass().add("post-actions");

        if (currentUser != null && post.getAuthorId() == currentUser.getId()) {
            Button editButton = new Button("Edit");
            editButton.getStyleClass().add("edit-button");
            editButton.setOnAction(e -> handleEditPost(post));

            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("delete-button");
            deleteButton.setOnAction(e -> handleDeletePost(post));

            actionButtons.getChildren().addAll(editButton, deleteButton);
        }

        card.getChildren().addAll(
                headerContainer,
                metaLabel,
                description,
                imageContainer,
                actionButtons
        );

        return card;
    }

    private void handleEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/EditFormPost.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the post to edit
            EditPostController controller = loader.getController();
            controller.setPostToEdit(post);
            controller.setPostListController(this); // For refreshing after edit

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Post");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            showAlert("Error", "Could not load edit form", Alert.AlertType.ERROR);
        }
    }

    private void handleDeletePost(Post post) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Post");
        confirmation.setContentText("Are you sure you want to delete this post?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.delete(post);
                loadPosts(); // Refresh the list
                showAlert("Success", "Post deleted successfully", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete post", Alert.AlertType.ERROR);
            }
        }
    }

    public void refreshPosts() {
        loadPosts();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        postsContainer.getChildren().clear();
        postsContainer.getChildren().add(new Label(message));
    }
}