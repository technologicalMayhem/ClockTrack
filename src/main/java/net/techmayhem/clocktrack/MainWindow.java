package net.techmayhem.clocktrack;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.screens.TabbedEntityEditor;
import net.techmayhem.clocktrack.screens.people.PeopleOverview;
import net.techmayhem.clocktrack.screens.scripts.ScriptsOverview;
import net.techmayhem.clocktrack.screens.sessions.SessionsOverview;

public class MainWindow extends Application {
    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        Tab sessions = new TabbedEntityEditor(SessionsOverview::new).buildTab();
        Tab people = new TabbedEntityEditor(PeopleOverview::new).buildTab();
        Tab scripts = new TabbedEntityEditor(ScriptsOverview::new).buildTab();
        sessions.setClosable(false);
        people.setClosable(false);
        scripts.setClosable(false);
        tabPane.getTabs().addAll(sessions, people, scripts);
        double ratio = 3.0 / 4.0;
        int width = 1000;
        Scene scene = new Scene(tabPane, width, width * ratio);
        stage.setScene(scene);
        stage.show();
    }
}
