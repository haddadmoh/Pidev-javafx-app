package com.esprit.services;

import com.esprit.models.Comment;
import com.esprit.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService implements IService<Comment> {
    private Connection conn = DatabaseConnection.getInstance().getConnection();

    @Override
    public void add(Comment comment) throws SQLException {
        String sql = "INSERT INTO comment (author_id, post_id, content, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getAuthorId());
            stmt.setInt(2, comment.getPostId());
            stmt.setString(3, comment.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(comment.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    comment.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Comment comment) throws SQLException {
    }

    @Override
    public void delete(Comment comment) throws SQLException {
        String sql = "DELETE FROM comment WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, comment.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting comment failed, no rows affected.");
            }
        }
    }

    @Override
    public List<Comment> getAll() throws SQLException {
        return null;
    }

    // Additional useful methods
    public List<Comment> getCommentsByPost(int postId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comment WHERE post_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Comment comment = new Comment(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getInt("post_id"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    comments.add(comment);
                }
            }
        }
        return comments;
    }

    // In CommentService
    public Comment getById(int id) throws SQLException {
        String sql = "SELECT * FROM comment WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Comment(
                            rs.getInt("id"),
                            rs.getInt("author_id"),
                            rs.getInt("post_id"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        throw new SQLException("Comment not found with ID: " + id);
    }
}