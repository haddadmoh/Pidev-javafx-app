package com.esprit.controllers.front;

import com.esprit.models.Conversation;
import com.esprit.models.Message;
import com.esprit.models.Post;
import com.esprit.models.User;
import com.esprit.services.FirebaseService;
import com.esprit.services.PostService;
import com.esprit.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatWindowController {
    @FXML private ListView<Message> messagesListView;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;
    @FXML private Button validateButton;
    @FXML private Label chatTitleLabel;
    @FXML private Label statusLabel;

    private String conversationId;
    private User currentUser;
    private Conversation conversation;
    private Timer refreshTimer;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    private final PostService postService = new PostService();
    private final UserService userService = new UserService();


    @FXML
    public void initialize() {
        // Configure message input to send on Enter (Shift+Enter for new line)
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                handleSendMessage();
                event.consume();
            }
        });

        // Custom cell factory for message display
        messagesListView.setCellFactory(param -> new MessageCell());
    }

    public void setConversation(String conversationId, User currentUser) {
        this.conversationId = conversationId;
        this.currentUser = currentUser;

        // Load the conversation details and messages
        loadConversation();

        // Setup automatic refresh every 5 seconds
        startAutoRefresh();
    }

    private void loadConversation() {
        try {
            // Get conversation details
            conversation = FirebaseService.getConversation(conversationId);
            if (conversation == null) {
                showError("Conversation not found");
                return;
            }

            // Set chat title with appropriate users
            int otherUserId = conversation.postOwnerId == currentUser.getId()
                    ? conversation.interestedUserId
                    : conversation.postOwnerId;
            chatTitleLabel.setText("Chat with User " +  userService.getUsernameById(otherUserId) + " about Post " + postService.getPostNameById(conversation.postId));

            // Set status
            updateStatusDisplay();

            // Load messages
            loadMessages();

            // Update UI based on conversation status
            updateUIForStatus();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load conversation: " + e.getMessage());
        }
    }

    private void loadMessages() {
        try {
            List<Message> messages = FirebaseService.getMessages(conversationId);
            Platform.runLater(() -> {
                messagesListView.getItems().clear();
                messagesListView.getItems().addAll(messages);
                // Scroll to bottom
                if (!messages.isEmpty()) {
                    messagesListView.scrollTo(messages.size() - 1);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load messages: " + e.getMessage());
        }
    }

    @FXML
    public void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || conversation == null || !"open".equals(conversation.status)) {
            return;
        }

        Message message = new Message();
        message.senderId = currentUser.getId();
        message.content = content;
        message.timestamp = System.currentTimeMillis();

        // Clear input first for better UX
        messageInput.clear();

        new Thread(() -> {
            try {
                FirebaseService.sendMessage(conversationId, message);
                // Load messages again to see the new message
                loadMessages();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Failed to send message: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleCancelConversation() {
        if (conversation == null || !"open".equals(conversation.status)) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Conversation");
        confirmation.setHeaderText("Are you sure you want to cancel this conversation?");
        confirmation.setContentText("This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Update only the status field
                    FirebaseService.updateConversationStatus(conversation.conversationId, "cancelled");
                    conversation.status = "cancelled"; // Update local object too
                    updateStatusDisplay();
                    updateUIForStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to cancel conversation: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void handleValidateConversation() {
        if (conversation == null || !"open".equals(conversation.status)) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Validate Conversation");
        confirmation.setHeaderText("Are you sure you want to validate this conversation?");
        confirmation.setContentText("This will mark the conversation as completed.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Update only the status field
                    FirebaseService.updateConversationStatus(conversation.conversationId, "validated");
                    conversation.status = "validated"; // Update local object too

                    //update the post status to completed when the convo is validated
                    postService.updateStatus(conversation.postId, "completed");

                    // Verify update worked
                    Post updatedPost = postService.getById(conversation.postId);
                    System.out.println("Post status after update: " + updatedPost.getStatus());

                    updateStatusDisplay();
                    updateUIForStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Failed to validate conversation: " + e.getMessage());
                }
            }
        });
    }

    private void updateStatusDisplay() {
        if (conversation != null) {
            Platform.runLater(() -> {
                statusLabel.setText("Status: " + conversation.status);

                // Update color based on status
                switch (conversation.status.toLowerCase()) {
                    case "open":
                        statusLabel.setTextFill(Color.web("#2e7d32")); // Green
                        break;
                    case "validated":
                        statusLabel.setTextFill(Color.web("#1565c0")); // Blue
                        break;
                    case "cancelled":
                        statusLabel.setTextFill(Color.web("#c62828")); // Red
                        break;
                    default:
                        statusLabel.setTextFill(Color.BLACK);
                        break;
                }
            });
        }
    }

    private void updateUIForStatus() {
        boolean isOpen = conversation != null && "open".equals(conversation.status);

        Platform.runLater(() -> {
            // Only allow sending messages if the conversation is open
            messageInput.setDisable(!isOpen);
            sendButton.setDisable(!isOpen);

            // Only show action buttons to the owner of the post
            boolean isPostOwner = conversation != null && conversation.postOwnerId == currentUser.getId();
            cancelButton.setVisible(isOpen && isPostOwner);
            validateButton.setVisible(isOpen && isPostOwner);

            if (!isOpen) {
                messageInput.setPromptText("This conversation is " +
                        (conversation != null ? conversation.status : "closed") +
                        ". New messages cannot be sent.");
            }
        });
    }

    private void startAutoRefresh() {
        // Cancel any existing timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        // Create new timer to refresh messages every 5 seconds
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Refresh conversation status
                    String status = FirebaseService.getConversationStatus(conversationId);
                    if (status != null && conversation != null && !status.equals(conversation.status)) {
                        conversation.status = status;
                        updateStatusDisplay();
                        updateUIForStatus();
                    }

                    // Refresh messages
                    loadMessages();
                } catch (Exception e) {
                    // Silently fail for auto-refresh
                    System.err.println("Auto-refresh failed: " + e.getMessage());
                }
            }
        }, 5000, 5000); // 5 seconds delay, then every 5 seconds
    }

    public void onWindowClose() {
        // Clean up timer when window closes
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Custom cell for messages display
    private class MessageCell extends ListCell<Message> {
        @Override
        protected void updateItem(Message message, boolean empty) {
            super.updateItem(message, empty);

            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            // Create message bubble
            HBox wrapper = new HBox();
            VBox bubble = new VBox();
            bubble.setPadding(new Insets(8, 12, 8, 12));
            bubble.setMaxWidth(300);

            // Header with user and time
            HBox header = new HBox();
            header.setSpacing(10);

            Label userLabel = new Label("User " + message.senderId);
            userLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

            Label timeLabel = new Label(timeFormat.format(new Date(message.timestamp)));
            timeLabel.setFont(Font.font("System", 10));
            timeLabel.setTextFill(Color.GRAY);

            header.getChildren().addAll(userLabel, timeLabel);

            // Message content
            Text contentText = new Text(message.content);
            contentText.setWrappingWidth(280);

            bubble.getChildren().addAll(header, contentText);

            // Style based on sender
            boolean isCurrentUser = currentUser != null && message.senderId == currentUser.getId();
            if (isCurrentUser) {
                bubble.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 15px;");
                wrapper.setAlignment(Pos.CENTER_RIGHT);
            } else {
                bubble.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 15px;");
                wrapper.setAlignment(Pos.CENTER_LEFT);
            }

            wrapper.getChildren().add(bubble);
            setGraphic(wrapper);
        }
    }
}