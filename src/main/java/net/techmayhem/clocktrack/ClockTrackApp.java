package net.techmayhem.clocktrack;

import java.io.FileNotFoundException;
import java.io.InputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.database.DatabaseException;
import net.techmayhem.clocktrack.ui.EntitySection;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import net.techmayhem.clocktrack.ui.person.PeopleOverview;
import net.techmayhem.clocktrack.ui.script.ScriptsOverview;
import net.techmayhem.clocktrack.ui.session.SessionsOverview;

public class ClockTrackApp extends Application {
    @Override
    public void start(Stage stage) throws FileNotFoundException {
        Dialogs.setPrimaryStage(stage);
        Thread.setDefaultUncaughtExceptionHandler(ClockTrackApp::handleUncaughtException);

        stage.getIcons().add(new Image(getIcon()));
        stage.setTitle("ClockTrack");

        TabPane tabPane = new TabPane();
        double ratio = 3.0 / 4.0;
        int width = 1000;
        Scene scene = new Scene(tabPane, width, width * ratio);
        stage.setScene(scene);
        stage.show();

        try {
            Tab sessions = new EntitySection(SessionsOverview::new).getTab();
            Tab people = new EntitySection(PeopleOverview::new).getTab();
            Tab scripts = new EntitySection(ScriptsOverview::new).getTab();
            sessions.setClosable(false);
            people.setClosable(false);
            scripts.setClosable(false);
            tabPane.getTabs().addAll(sessions, people, scripts);
        } catch (DatabaseException e) {
            handleUncaughtException(Thread.currentThread(), e);
        }
    }

    private InputStream getIcon() throws FileNotFoundException {
        String iconPath = "/icons/app-256.png";
        InputStream icon = getClass().getResourceAsStream(iconPath);
        if (icon == null) throw new FileNotFoundException("Failed to find app icon " + iconPath);
        return icon;
    }

    private static void handleUncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace();

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
