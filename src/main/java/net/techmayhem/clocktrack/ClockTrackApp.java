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
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.sample.SampleDataLoader;
import net.techmayhem.clocktrack.ui.EntitySection;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import net.techmayhem.clocktrack.ui.player.PlayerOverview;
import net.techmayhem.clocktrack.ui.script.ScriptsOverview;
import net.techmayhem.clocktrack.ui.session.SessionsOverview;

/// The entry point of ClockTrack. Opens the main window with one tab each for sessions, players, and
/// scripts.
///
/// On the first start, the user is offered sample data to try the app with.
public class ClockTrackApp extends Application {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 750;

    /// Sets up the main window and shows it.
    ///
    /// @param stage The primary stage provided by JavaFX
    /// @throws FileNotFoundException If the app icon is missing from the resources
    @Override
    public void start(Stage stage) throws FileNotFoundException {
        Dialogs.setPrimaryStage(stage);
        Thread.setDefaultUncaughtExceptionHandler(ClockTrackApp::handleUncaughtException);

        I18n.setLocale(Locale.getDefault());
        Roles.loadLocale(Locale.getDefault());

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

    /// Asks the user whether they want sample data loaded into the new database.
    private static void offerSampleData() {
        Dialogs.showConfirmDialog(
                localize("sampleDataDialog.title"),
                localize("sampleDataDialog.message"),
                localize("sampleDataDialog.accept"),
                localize("sampleDataDialog.cancel"),
                ClockTrackApp::buildSampleData);
    }

    /// Loads the sample data in the language of the locale.
    private static void buildSampleData() {
        SampleDataLoader.load(Database.getInstance(), Locale.getDefault());
    }

    /// Opens the app icon from the resources.
    ///
    /// @return A stream of the app icon
    /// @throws FileNotFoundException If the icon is missing from the resources
    private InputStream getIcon() throws FileNotFoundException {
        String iconPath = "/icons/app-256.png";
        InputStream icon = getClass().getResourceAsStream(iconPath);
        if (icon == null) throw new FileNotFoundException("Failed to find app icon " + iconPath);
        return icon;
    }

    /// Shows the error to the user in a dialog. The app closes afterward, unless it is a recoverable
    /// [DatabaseException]. Can be called from any thread.
    ///
    /// @param thread The thread the error was thrown on
    /// @param throwable The error to show
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
