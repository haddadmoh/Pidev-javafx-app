package com.esprit.controllers.front;

import com.esprit.models.Conversation;
import com.esprit.models.User;
import com.esprit.services.PostService;
import com.esprit.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import com.esprit.services.FirebaseService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ConversationsListController {
    @FXML private ListView<Conversation> convListView;
    private User currentUser;
    private final PostService postService = new PostService();
    private final UserService userService = new UserService();

    // This method will be called by JavaFX when the FXML is loaded
    @FXML
    public void initialize() {
        setupListView();
        // Don't load conversations here - we'll do it after currentUser is set
    }

    // Call this method when setting the controller
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            // Now that we have a user, load conversations
            loadConversations();
        }
    }

    private void setupListView() {
        // Setup cell factory and click handler without using currentUser
        convListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation convo, boolean empty) {
                super.updateItem(convo, empty);
                if (empty || convo == null) {
                    setText(null);
                } else {
                    // Check if currentUser is available before using it
                    if (currentUser != null) {
                        int other = convo.postOwnerId == currentUser.getId()
                                ? convo.interestedUserId
                                : convo.postOwnerId;
                        try {
                            setText("Chat with User " + userService.getUsernameById(other) + " about post " + postService.getPostNameById(convo.postId));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // Fallback if currentUser is null
                        setText("Conversation: " + convo.conversationId);
                    }
                }
            }
        });

        convListView.setOnMouseClicked(evt -> {
            Conversation convo = convListView.getSelectionModel().getSelectedItem();
            if (convo != null) openChat(convo.conversationId);
        });
    }

    private void loadConversations() {
        if (currentUser == null) {
            System.err.println("Cannot load conversations: currentUser is null");
            return;
        }

        int userId = currentUser.getId();
        System.out.println("Loading conversations for user ID: " + userId);

        new Thread(() -> {
            try {
                List<Conversation> convos = FirebaseService.getConversationsForUser(userId);
                Platform.runLater(() -> {
                    convListView.getItems().setAll(convos);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // Handle error in UI
                    System.err.println("Failed to load conversations: " + e.getMessage());
                });
            }
        }).start();
    }

    private void openChat(String conversationId) {
        System.out.println("Opening chat for conversation: " + conversationId);

        Platform.runLater(() -> {
            try {
                // Load the ChatWindow FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Front/ChatWindow.fxml"));
                Parent root = loader.load();

                // Get the controller and set up the conversation
                ChatWindowController controller = loader.getController();
                controller.setConversation(conversationId, currentUser);

                // Set up the stage
                Stage stage = new Stage();
                stage.setTitle("Chat");
                stage.setScene(new Scene(root));

                // Handle window close to clean up resources
                stage.setOnCloseRequest(event -> controller.onWindowClose());

                // Show the window
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to open chat window: " + e.getMessage());
            }
        });
    }

    // Method to manually refresh conversations
    public void refreshConversations() {
        if (currentUser != null) {
            loadConversations();
        }
    }
}