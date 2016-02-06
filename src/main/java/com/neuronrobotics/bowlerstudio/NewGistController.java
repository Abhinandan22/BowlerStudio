package com.neuronrobotics.bowlerstudio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by Ryan Benasutti on 2/5/2016.
 */

public class NewGistController extends Application
{
    @FXML
    public TextField filenameField, descriptionField;

    @FXML
    public Button addAsPublicButton, addAsPrivateButton, cancelButton;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Platform.runLater(() -> {
            primaryStage.setTitle("Create new Gist");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("createNewGist.fxml"));
            Parent root;
            try
            {
                root = loader.load();
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.initModality(Modality.WINDOW_MODAL);
                primaryStage.setResizable(false);
                primaryStage.show();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void onAddAsPublic(ActionEvent actionEvent)
    {
        Platform.runLater(() -> {
            new Thread(() -> {
                GistHelper.createNewGist(filenameField.getText(), descriptionField.getText(), true);
            }).start();

            Stage stage = (Stage)addAsPublicButton.getScene().getWindow();
            stage.close();
        });
    }

    @FXML
    public void onAddAsPrivate(ActionEvent actionEvent)
    {
        Platform.runLater(() -> {
            new Thread(() -> {
                GistHelper.createNewGist(filenameField.getText(), descriptionField.getText(), false);
            }).start();

            Stage stage = (Stage)addAsPrivateButton.getScene().getWindow();
            stage.close();
        });
    }

    @FXML
    public void onCancel(ActionEvent actionEvent)
    {
        Platform.runLater(() -> {
            Stage stage = (Stage)cancelButton.getScene().getWindow();
            stage.close();
        });
    }
}
