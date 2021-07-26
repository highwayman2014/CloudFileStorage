package com.shepa.filestorage.client;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientStageStarter extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientStage.fxml"));
        Parent root = loader.load();
        ClientStageController clientStageController = loader.getController();
        primaryStage.setTitle("Cloud file storage");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            clientStageController.exitAction(new ActionEvent());
        });
    }

    public static void start(){
        launch();
    }

}
