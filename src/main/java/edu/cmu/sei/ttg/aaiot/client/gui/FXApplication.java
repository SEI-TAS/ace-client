package edu.cmu.sei.ttg.aaiot.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class FXApplication extends Application
{
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Main.fxml"));

        Scene scene = new Scene(root, 500, 300);
        primaryStage.setTitle("ACE Client");
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
