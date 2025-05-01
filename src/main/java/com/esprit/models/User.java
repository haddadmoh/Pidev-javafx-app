package com.esprit.models;

public class User {
    private int id;
    private String username;
    private String role;
    private String email;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }
    public User(int id, String username, String role, String email) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public User() {
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) {this.username = username;}
    public String getRole() { return role; }
    public void setRole(String role) {this.role = role;}
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}