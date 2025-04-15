package com.esprit.models;

public class User {
    private int id;
    private String username;
    private String role;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }
    public User(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public int getId() {return id;}
}