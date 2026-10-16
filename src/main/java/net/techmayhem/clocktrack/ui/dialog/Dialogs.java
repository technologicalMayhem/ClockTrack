package net.techmayhem.clocktrack.ui.dialog;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    public static final double DIALOG_MAX_WIDTH = 500.0;

    @Nullable private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage createStage(String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setMaxWidth(DIALOG_MAX_WIDTH);
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

    public static void showConfirmDialog(String title, String prompt, String accept, String cancel, Runnable callback) {
        Stage stage = createStage(title);

        Text promptText = new Text(prompt);
        promptText.setWrappingWidth(DIALOG_MAX_WIDTH - Layout.PADDING * 2);
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
        vBox.getChildren().addAll(promptText, buttonsRow);

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
        stage.show();
    }

    public static void showMessageDialog(String title, String text) {
        Stage stage = createStage(title);

        Text textElement = new Text(text);
        textElement.setWrappingWidth(DIALOG_MAX_WIDTH - Layout.PADDING * 2);
        Button acceptButton = new Button(localize("generic.ok"));
        acceptButton.setDefaultButton(true);
        acceptButton.setOnAction(_ -> stage.close());

        VBox vBox = createVBox();
        vBox.getChildren().addAll(textElement, acceptButton);

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
        stage.showAndWait();
    }

    public static void showErrorDialog(Throwable error, boolean isFatal) {
        Stage stage = createStage(localize("dialogs.error.title"));

        Text header = new Text(localize("dialogs.error.header"));
        Text errorText = new Text(error.getMessage());
        errorText.setWrappingWidth(DIALOG_MAX_WIDTH - Layout.PADDING * 2);
        Text footer = new Text(isFatal ? localize("dialogs.error.fatal") : localize("dialogs.error.nonFatal"));

        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        TextArea stackTraceArea = new TextArea(sw.toString());
        stackTraceArea.setEditable(false);
        stackTraceArea.setWrapText(false);
        stackTraceArea.setPrefRowCount(STACK_TRACE_ROWS);

        TitledPane collapseStackTrack = new TitledPane(localize("dialogs.error.stackTrace"), stackTraceArea);
        collapseStackTrack.setExpanded(false);
        collapseStackTrack.setAnimated(false);
        collapseStackTrack.expandedProperty().addListener((_, _, _) -> Platform.runLater(stage::sizeToScene));

        Button button = new Button(localize("generic.ok"));
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
