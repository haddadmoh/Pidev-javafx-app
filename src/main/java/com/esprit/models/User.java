package com.esprit.models;

public class User {
    private String username;
    private String role; // "patient1", "patient2", or "admin"

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getRole() { return role; }
}