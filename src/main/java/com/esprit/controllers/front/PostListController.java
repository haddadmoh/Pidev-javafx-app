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
    @FXML private Label postsCountLabel;
    @FXML private ComboBox<PostCategory> categoryFilterComboBox;
    @FXML private ComboBox<String> dateSortComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;

    private final PostService postService = new PostService();
    private final UserService userService = new UserService();
    private final PostCategoryService categoryService = new PostCategoryService();
    private List<Post> allPosts;
    private List<PostCategory> allCategories;
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

    private void displayPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        postsCountLabel.setText(String.valueOf(posts.size()) + " posts");
        postsCountLabel.getStyleClass().setAll("count-label");

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

    private void showMessage(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        postsContainer.getChildren().add(label);
    }
}