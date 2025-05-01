package com.esprit.services;

import com.esprit.models.Reactions;
import com.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactionsService {
    private Connection connection;

    public ReactionsService() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    // Add a new reaction
    public void addReaction(Reactions reaction) throws SQLException {
        // First, check if the user has already reacted to this post
        String checkQuery = "SELECT id, type FROM reactions WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, reaction.getPostId());
            checkStmt.setInt(2, reaction.getUserId());

            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // User already reacted - update the reaction type
                int existingId = rs.getInt("id");
                String existingType = rs.getString("type");

                // If clicking the same reaction type, remove it (toggle behavior)
                if (existingType.equals(reaction.getType())) {
                    deleteReaction(existingId);
                } else {
                    // Change reaction type
                    String updateQuery = "UPDATE reactions SET type = ?, created_at = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, reaction.getType());
                        updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setInt(3, existingId);
                        updateStmt.executeUpdate();
                    }
                }
            } else {
                // New reaction - insert it
                String insertQuery = "INSERT INTO reactions (post_id, user_id, type, created_at) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setInt(1, reaction.getPostId());
                    insertStmt.setInt(2, reaction.getUserId());
                    insertStmt.setString(3, reaction.getType());
                    insertStmt.setTimestamp(4, Timestamp.valueOf(reaction.getCreatedAt()));
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    // Delete a reaction
    public void deleteReaction(int reactionId) throws SQLException {
        String query = "DELETE FROM reactions WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, reactionId);
            stmt.executeUpdate();
        }
    }

    // Get reaction counts for a post
    public Map<String, Integer> getReactionCountsForPost(int postId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        // Initialize with 0 counts for all reaction types
        counts.put("LIKE", 0);
        counts.put("LOVE", 0);
        counts.put("SAD", 0);

        String query = "SELECT type, COUNT(*) as count FROM reactions WHERE post_id = ? GROUP BY type";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                int count = rs.getInt("count");
                counts.put(type, count);
            }
        }

        return counts;
    }

    // Get user's reaction to a post (if any)
    public String getUserReactionToPost(int postId, int userId) throws SQLException {
        String query = "SELECT type FROM reactions WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, postId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("type");
            }
        }

        return null; // No reaction from this user
    }

    // Get all reactions for a post
    public List<Reactions> getReactionsForPost(int postId) throws SQLException {
        List<Reactions> reactions = new ArrayList<>();
        String query = "SELECT * FROM reactions WHERE post_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Reactions reaction = new Reactions();
                reaction.setId(rs.getInt("id"));
                reaction.setPostId(rs.getInt("post_id"));
                reaction.setUserId(rs.getInt("user_id"));
                reaction.setType(rs.getString("type"));
                reaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                reactions.add(reaction);
            }
        }

        return reactions;
    }
}