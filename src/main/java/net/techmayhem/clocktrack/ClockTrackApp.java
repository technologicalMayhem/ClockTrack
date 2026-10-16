package net.techmayhem.clocktrack;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.database.DatabaseException;
import net.techmayhem.clocktrack.sample.SampleDataLoader;
import net.techmayhem.clocktrack.ui.EntitySection;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import net.techmayhem.clocktrack.ui.player.PlayerOverview;
import net.techmayhem.clocktrack.ui.script.ScriptsOverview;
import net.techmayhem.clocktrack.ui.session.SessionsOverview;

public class ClockTrackApp extends Application {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 750;

    @Override
    public void start(Stage stage) throws FileNotFoundException {
        Dialogs.setPrimaryStage(stage);
        Thread.setDefaultUncaughtExceptionHandler(ClockTrackApp::handleUncaughtException);

        I18n.setLocale(Locale.getDefault());

        stage.getIcons().add(new Image(getIcon()));
        stage.setTitle(localize("appTitle"));

        if (!Database.exists()) offerSampleData();

        TabPane tabPane = new TabPane();
        Scene scene = new Scene(tabPane, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.show();

        try {
            Tab sessions = new EntitySection(SessionsOverview::new).getTab();
            Tab players = new EntitySection(PlayerOverview::new).getTab();
            Tab scripts = new EntitySection(ScriptsOverview::new).getTab();
            sessions.setClosable(false);
            players.setClosable(false);
            scripts.setClosable(false);
            tabPane.getTabs().addAll(sessions, players, scripts);
        } catch (DatabaseException e) {
            handleUncaughtException(Thread.currentThread(), e);
        }
    }

    private static void offerSampleData() {
        Dialogs.showConfirmDialog(
                localize("sampleDataDialog.title"),
                localize("sampleDataDialog.message"),
                localize("sampleDataDialog.accept"),
                localize("sampleDataDialog.cancel"),
                ClockTrackApp::buildSampleData);
    }

    private static void buildSampleData() {
        SampleDataLoader.load(Database.getInstance(), Locale.getDefault());
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
