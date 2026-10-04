package net.techmayhem.clocktrack.ui.dialog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.ui.Layout;
import org.jspecify.annotations.Nullable;

public class Dialogs {
    private static final int STACK_TRACE_ROWS = 15;
    private static final int EXIT_CODE_FATAL = 1;

    @Nullable private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage createStage(String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (primaryStage == null) throw new IllegalStateException("Primary stage is not set");
        stage.initOwner(primaryStage);
        stage.getIcons().setAll(primaryStage.getIcons());
        return stage;
    }

    public static VBox createVBox() {
        VBox vBox = new VBox(Layout.SPACING);
        vBox.setPadding(new Insets(Layout.PADDING));
        return vBox;
    }

    public static void showTextDialog(
            String title, String prompt, String defaultValue, String accept, String cancel, Consumer<String> callback) {
        Stage stage = createStage(title);

        Label label = new Label(prompt);
        TextField textInput = new TextField(defaultValue);
        HBox buttonsRow = new HBox();
        Button acceptButton = new Button(accept);
        acceptButton.setDefaultButton(true);
        acceptButton.disableProperty().bind(textInput.textProperty().isEmpty());
        acceptButton.setOnAction(_ -> {
            callback.accept(textInput.getText());
            stage.close();
        });
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

    public static void showErrorDialog(Throwable error, boolean isFatal) {
        Stage stage = createStage("An error occurred");

        Text header = new Text("An error occurred:");
        Text errorText = new Text(error.getMessage());
        Text footer =
                new Text(isFatal ? "The application cannot recover from this and will exit." : "Press ok to continue.");

        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        TextArea stackTraceArea = new TextArea(sw.toString());
        stackTraceArea.setEditable(false);
        stackTraceArea.setWrapText(false);
        stackTraceArea.setPrefRowCount(STACK_TRACE_ROWS);

        TitledPane collapseStackTrack = new TitledPane("Stack trace", stackTraceArea);
        collapseStackTrack.setExpanded(false);
        collapseStackTrack.setAnimated(false);
        collapseStackTrack.expandedProperty().addListener((_, _, _) -> Platform.runLater(stage::sizeToScene));

        Button button = new Button("Ok");
        button.setDefaultButton(true);
        button.setOnAction(_ -> {
            if (isFatal) System.exit(EXIT_CODE_FATAL);
            stage.close();
        });

        VBox vBox = createVBox();
        vBox.getChildren().addAll(header, errorText, collapseStackTrack, footer, button);

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
        stage.showAndWait();
    }
}
