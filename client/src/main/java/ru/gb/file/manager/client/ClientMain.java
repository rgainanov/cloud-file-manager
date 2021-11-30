package ru.gb.file.manager.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import lombok.Data;

@Data
public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        String css = this.getClass().getResource("/stylesheet.css").toExternalForm();
        Parent root = FXMLLoader.load(getClass().getResource("/layout.fxml"));
        primaryStage.setTitle("Cloud File Storage");
        Scene scene = new Scene(root, 1200, 600);
        scene.getStylesheets().add(css);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event ->
                System.exit(0));
    }


    public static void main(String[] args) {
        launch(args);
    }
}
