package com.esprit.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.esprit.controllers.Back.AdminController;
import com.esprit.controllers.Back.PatientController;
import com.esprit.main.App;
import com.esprit.models.User;
import com.esprit.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {
    @FXML private VBox buttonsContainer;
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        try {
            List<User> users = userService.getAllUsers();
            createUserButtons(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createUserButtons(List<User> users) {
        for (User user : users) {
            Button btn = new Button(user.getUsername());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

            btn.setOnAction(e -> {
                try {
                    loadUserInterface(user);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            buttonsContainer.getChildren().add(btn);
        }
    }

    private void loadUserInterface(User user) throws IOException {
        Stage stage = (Stage) buttonsContainer.getScene().getWindow();
        String fxmlFile = user.getRole().equals("admin")
                ? "/views/Back/AdminInterface.fxml"
                : "/views/Back/PatientInterface.fxml";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        // Set user in the next controller
        if (user.getRole().equals("admin")) {
            ((AdminController)loader.getController()).setUser(user);
        } else {
            ((PatientController)loader.getController()).setUser(user);
        }

        Scene scene = new Scene(root, App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        stage.setScene(scene);
    }
}