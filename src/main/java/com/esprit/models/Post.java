package com.esprit.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Post {
    private int id;
    private int categoryId;
    private int authorId;
    private String title;
    private String description;
    private String type;
    private String image;
    private LocalDateTime createdAt;
    private boolean enabled;

    // Constructor for new posts
    public Post(int categoryId, int authorId, String title, String description,
                String type, String image, boolean enabled) {
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.createdAt = LocalDateTime.now();
        this.enabled = enabled;
    }

    // Constructor for existing posts from DB
    public Post(int id, int categoryId, int authorId, String title,
                String description, String type, String image,
                boolean enabled) {
        this.id = id;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.enabled = enabled;
    }

    public Post(int id, int categoryId, int authorId, String title,
                String description, String type, String image,
                LocalDateTime createdAt, boolean enabled) {
        this.id = id;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.image = image;
        this.createdAt = createdAt;
        this.enabled = enabled;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // Formatted date display
    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
    }

    @Override
    public String toString() {
        return String.format("Post [id=%d, title='%s', categoryId=%d, authorId=%s, description='%s', type='%s', image='%s']",
                id, title, categoryId, authorId, description, type, image);
    }
}