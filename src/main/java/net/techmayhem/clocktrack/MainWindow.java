package net.techmayhem.clocktrack;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.dialog.Dialogs;
import net.techmayhem.clocktrack.screens.TabbedEntityEditor;
import net.techmayhem.clocktrack.screens.people.PeopleOverview;
import net.techmayhem.clocktrack.screens.scripts.ScriptsOverview;
import net.techmayhem.clocktrack.screens.sessions.SessionsOverview;
import org.jspecify.annotations.Nullable;
import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainWindow extends Application {
    @Nullable
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws FileNotFoundException {
        primaryStage = stage;
        Thread.setDefaultUncaughtExceptionHandler(MainWindow::handleUncaughtException);

        stage.getIcons().add(new Image(getIcon("/icons/app-256.png")));
        stage.setTitle("ClockTrack");

        TabPane tabPane = new TabPane();
        double ratio = 3.0 / 4.0;
        int width = 1000;
        Scene scene = new Scene(tabPane, width, width * ratio);
        stage.setScene(scene);
        stage.show();

        try {
            Tab sessions = new TabbedEntityEditor(SessionsOverview::new).buildTab();
            Tab people = new TabbedEntityEditor(PeopleOverview::new).buildTab();
            Tab scripts = new TabbedEntityEditor(ScriptsOverview::new).buildTab();
            sessions.setClosable(false);
            people.setClosable(false);
            scripts.setClosable(false);
            tabPane.getTabs().addAll(sessions, people, scripts);
        } catch (DatabaseException e) {
            handleUncaughtException(Thread.currentThread(), e);
        }
    }

    private InputStream getIcon(String name) throws FileNotFoundException {
        InputStream icon = getClass().getResourceAsStream(name);
        if (icon == null) throw new FileNotFoundException("Failed to find app icon " + name);
        return icon;
    }

    public static Stage getPrimaryStage() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary Stage not initialized");
        }
        return primaryStage;
    }

    private static void handleUncaughtException(Thread thread, Throwable throwable) {
        Logger.error(throwable, "Unhandled exception on {}", thread.getName());

        Runnable showDialog = () -> {
            if (throwable instanceof DatabaseException dbEx) {
                Dialogs.showErrorDialog(dbEx, !dbEx.isRecoverable());
            } else {
                Dialogs.showErrorDialog(throwable, true);
            }
        };

        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            Platform.runLater(showDialog);
        }
    }
}
