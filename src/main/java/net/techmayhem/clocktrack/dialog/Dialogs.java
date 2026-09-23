package net.techmayhem.clocktrack.dialog;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.MainWindow;

import java.util.function.Consumer;

public class Dialogs {

    private static Stage createStage(String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(MainWindow.getPrimaryStage());
        return stage;
    }

    private static VBox createVBox() {
        VBox vBox = new VBox(5);
        vBox.setPadding(new Insets(10));
        return vBox;
    }

    public static void showTextDialog(String title, String prompt, String defaultValue, String accept, String cancel, Consumer<String> callback) {
        Stage stage = createStage(title);

        Label label = new Label(prompt);
        TextField textInput = new TextField(defaultValue);
        HBox buttonsRow = new HBox();
        Button acceptButton = new Button(accept);
        acceptButton.setDefaultButton(true);
        acceptButton.setOnAction(_ -> {
            callback.accept(textInput.getText());
            stage.close();
        });
        acceptButton.setDisable(true);
        textInput.setOnKeyTyped(_ -> acceptButton.setDisable(textInput.getText().isEmpty()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button cancelButton = new Button(cancel);
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(_ -> stage.close());
        buttonsRow.getChildren().addAll(acceptButton, spacer, cancelButton);

        VBox vBox = createVBox();
        vBox.getChildren().addAll(label, textInput, buttonsRow);

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
        stage.show();
    }

    public static void showConfirmDialog(String title, String prompt, String accept, String cancel, Runnable callback) {
        Stage stage = createStage(title);

        Label label = new Label(prompt);
        HBox buttonsRow = new HBox();
        Button acceptButton = new Button(accept);
        acceptButton.setDefaultButton(true);
        acceptButton.setOnAction(_ -> {
            callback.run();
            stage.close();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button cancelButton = new Button(cancel);
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(_ -> stage.close());
        buttonsRow.getChildren().addAll(acceptButton, spacer, cancelButton);

        VBox vBox = createVBox();
        vBox.getChildren().addAll(label, buttonsRow);

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
        stage.show();
    }
}
