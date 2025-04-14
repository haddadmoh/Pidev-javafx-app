package com.esprit.services;

import com.esprit.models.PostCategory;
import com.esprit.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostCategoryService implements IService<PostCategory> {
    private Connection conn = DatabaseConnection.getInstance().getConnection();

    @Override
    public void add(PostCategory category) throws SQLException {
        String sql = "INSERT INTO post_category (name, description, created_at) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(category.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(PostCategory category) throws Exception {
        String sql = "UPDATE post_category SET name = ?, description = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setInt(3, category.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating category failed, no rows affected.");
            }
        }

    }

    @Override
    public void delete(PostCategory category) throws SQLException {
        // Check if category has posts first
        if (hasPostsInCategory(category.getId())) {
            throw new SQLException("Cannot delete category '" + category.getName() + "' - it contains posts. " +
                    "Please delete or move the posts first.");
        }

        String sql = "DELETE FROM post_category WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, category.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Category not found or could not be deleted.");
            }
        }
    }

    private boolean hasPostsInCategory(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM post WHERE category_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<PostCategory> getAll() throws Exception {
        List<PostCategory> categories = new ArrayList<>();
        String sql = "SELECT * FROM post_category ORDER BY created_at DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                PostCategory category = new PostCategory(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                categories.add(category);
            }
        }
        return categories;
    }

    public boolean categoryExists(String categoryName) throws Exception {
        try {
            // Query the database to check if a category with this name exists
            String query = "SELECT COUNT(*) FROM post_category WHERE name = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, categoryName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public PostCategory getById(int id) throws SQLException {
        String sql = "SELECT * FROM post_category WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PostCategory(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        throw new SQLException("Category not found with ID: " + id);
    }
}