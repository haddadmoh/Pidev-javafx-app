package com.esprit.services;

import com.esprit.models.Post;
import com.esprit.models.User;
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
                        rs.getBoolean("enabled"),
                        rs.getString("status")
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
                            rs.getBoolean("enabled"),
                            rs.getString("status")
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
                    rs.getBoolean("enabled"),
                    rs.getString("status")
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

    public void updatePostStatus(int postId, String status, int reservedById) throws SQLException {
        String query = "UPDATE post SET status = ?, reserved_by_id = ?, reservation_date = NOW() WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setString(1, status);

            if (reservedById > 0) {
                statement.setInt(2, reservedById);
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }

            statement.setInt(3, postId);
            statement.executeUpdate();
        }
    }

    // Method to complete a post
    public void completePost(int postId) throws SQLException {
        String query = "UPDATE post SET status = 'COMPLETED' WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setInt(1, postId);
            statement.executeUpdate();
        }
    }

    // Method to cancel a reservation
    public void cancelReservation(int postId) throws SQLException {
        String query = "UPDATE post SET status = 'ACTIVE', reserved_by_id = NULL, reservation_date = NULL WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setInt(1, postId);
            statement.executeUpdate();
        }
    }

    public List<Post> getUserReservations(int userId) throws SQLException {
        List<Post> reservations = new ArrayList<>();
        String query = "SELECT * FROM post WHERE (author_id = ? OR reserved_by_id = ?) AND status = 'RESERVED'";

        try (PreparedStatement statement = conn.prepareStatement(query)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(mapResultSetToPost(resultSet));
                }
            }
        }
        return reservations;
    }

    // Helper method to map ResultSet to User
    private Post mapResultSetToPost(ResultSet resultSet) throws SQLException {
        Post post = new Post();
        post.setId(resultSet.getInt("id"));
        post.setCategoryId(resultSet.getInt("category_id"));
        post.setAuthorId(resultSet.getInt("author_id"));
        post.setTitle(resultSet.getString("title"));
        post.setDescription(resultSet.getString("description"));
        post.setType(resultSet.getString("type"));
        post.setImage(resultSet.getString("image"));
        post.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        post.setEnabled(resultSet.getBoolean("enabled"));
        post.setStatus(resultSet.getString("status"));
        return post;
    }

}