package com.esprit.services;

import com.esprit.models.Post;
import com.esprit.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService implements IService<Post> {
    private Connection conn = DatabaseConnection.getInstance().getConnection();

    @Override
    public void add(Post post) throws SQLException {
        String sql = "INSERT INTO post (category_id, author_id, title, description, type, image, created_at, enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, post.getCategoryId());
            stmt.setInt( 2, post.getAuthorId());
            stmt.setString(3, post.getTitle());
            stmt.setString(4, post.getDescription());
            stmt.setString(5, post.getType());
            stmt.setString(6, post.getImage());
            stmt.setTimestamp(7, Timestamp.valueOf(post.getCreatedAt()));
            stmt.setBoolean(8, post.isEnabled());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    post.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Post post) throws SQLException {
        String sql = "UPDATE post SET category_id=?, author_id=?, title=?, description=?, " +
                "type=?, image=?, enabled=? WHERE id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, post.getCategoryId());
            stmt.setInt(2, post.getAuthorId());
            stmt.setString(3, post.getTitle());
            stmt.setString(4, post.getDescription());
            stmt.setString(5, post.getType());
            stmt.setString(6, post.getImage());
            stmt.setBoolean(7, post.isEnabled());
            stmt.setInt(8, post.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating post failed, no rows affected.");
            }
        }
    }

    @Override
    public void delete(Post post) throws SQLException {
        String sql = "DELETE FROM post WHERE id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, post.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting post failed, no rows affected.");
            }
        }
    }

    @Override
    public List<Post> getAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post ORDER BY created_at DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Post post = new Post(
                        rs.getInt("id"),
                        rs.getInt("category_id"),
                        rs.getInt("author_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("image"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("enabled")
                );
                posts.add(post);
            }
        }
        return posts;
    }

    public Post getById(int id) throws SQLException {
        String sql = "SELECT * FROM post WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Post(
                            rs.getInt("id"),
                            rs.getInt("category_id"),
                            rs.getInt("author_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("type"),
                            rs.getString("image"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("enabled")
                    );
                }
            }
        }
        throw new SQLException("Post not found with ID: " + id);
    }
    public List<Post> getVisiblePosts() throws SQLException {
        String sql = "SELECT * FROM post WHERE enabled = true ORDER BY created_at DESC";
        return getPostsFromQuery(sql);
    }

    public List<Post> getPostsByAuthor(int authorId) throws SQLException {
        String sql = "SELECT * FROM post WHERE author_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authorId);
            return getPostsFromResultSet(stmt.executeQuery());
        }
    }

    private List<Post> getPostsFromQuery(String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return getPostsFromResultSet(rs);
        }
    }

    private List<Post> getPostsFromResultSet(ResultSet rs) throws SQLException {
        List<Post> posts = new ArrayList<>();
        while (rs.next()) {
            posts.add(new Post(
                    rs.getInt("id"),
                    rs.getInt("category_id"),
                    rs.getInt("author_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("image"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getBoolean("enabled")
            ));
        }
        return posts;
    }

    public List<Post> getPostsByCategory(int categoryId) throws SQLException {
        String sql = "SELECT * FROM post WHERE category_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            return getPostsFromResultSet(stmt.executeQuery());
        }
    }

    public int getPostCountByCategory(int categoryId) {
        String query = "SELECT COUNT(*) FROM post WHERE category_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, categoryId);
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

}