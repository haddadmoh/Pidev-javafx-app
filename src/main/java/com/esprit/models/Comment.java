package com.esprit.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Comment {
    private int id;
    private int authorId;
    private int postId;
    private String content;
    private LocalDateTime createdAt;

    // Constructor for new comments
    public Comment(int authorId, int postId, String content) {
        this.authorId = authorId;
        this.postId = postId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for existing comments
    public Comment(int id, int authorId, int postId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.postId = postId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Comment(int id, int authorId, int postId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.postId = postId;
        this.content = content;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Formatted date display
    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
    }

    @Override
    public String toString() {

        return String.format("Comment [id=%d, postId=%d, authorId=%s, content='%s']",
                id, postId, authorId, content.substring(0, Math.min(content.length(), 20)) + "...");
    }
}