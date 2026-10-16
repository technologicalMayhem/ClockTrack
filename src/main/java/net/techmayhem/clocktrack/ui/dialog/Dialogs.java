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

/// Creates and shows the dialogs of the app.
///
/// All dialogs are modal and owned by the primary stage, so the primary stage has to be set with
/// [#setPrimaryStage(Stage)] before the first dialog is created.
public class Dialogs {
    private static final int STACK_TRACE_ROWS = 15;
    private static final int EXIT_CODE_FATAL = 1;
    /// The maximum width of a dialog.
    public static final double DIALOG_MAX_WIDTH = 500.0;

    @Nullable private static Stage primaryStage;

    /// Sets the stage that owns all dialogs.
    ///
    /// @param stage The primary stage of the app
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /// Creates the stage for a dialog. The stage is modal, cannot be resized, and uses the icons of the primary stage.
    ///
    /// @param title The title of the dialog
    /// @return The stage of the dialog
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

    /// Creates a vertical box with the spacing and padding used by all dialogs.
    ///
    /// @return The box
    public static VBox createVBox() {
        VBox vBox = new VBox(Layout.SPACING);
        vBox.setPadding(new Insets(Layout.PADDING));
        return vBox;
    }

    /// Shows a dialog that asks the user to confirm an action, and waits until it is closed.
    ///
    /// @param title The title of the dialog
    /// @param prompt The question to ask the user
    /// @param accept The text of the accept button
    /// @param cancel The text of the cancel button
    /// @param callback The action to run if the user accepts
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
        stage.showAndWait();
    }

    /// Shows a dialog with a message, and waits until it is closed.
    ///
    /// @param title The title of the dialog
    /// @param text The message to show
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

    /// Shows a dialog with an error and its stack trace, and waits until it is closed.
    ///
    /// The stack trace is collapsed at first, so the dialog stays readable for users who are not interested in it.
    /// If the error is fatal, the app exits when the user confirms the dialog.
    ///
    /// @param error The error to show
    /// @param isFatal Whether the app exits after the dialog
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
