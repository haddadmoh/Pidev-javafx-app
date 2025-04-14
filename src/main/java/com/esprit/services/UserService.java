package com.esprit.services;

import com.esprit.models.User;
import com.esprit.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection conn = DatabaseConnection.getInstance().getConnection();

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT username, role FROM user";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getString("username"),
                        rs.getString("role")
                ));
            }
        }
        return users;
    }
}