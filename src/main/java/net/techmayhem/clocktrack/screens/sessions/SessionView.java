package net.techmayhem.clocktrack.screens.sessions;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.models.Session;
import net.techmayhem.clocktrack.screens.Screen;

public class SessionView extends Screen {
    private final StackPane root;
    private final Session session;
    private final String name;

    SessionView(Session session) {
        this.session = session;
        root = new StackPane();
        root.getChildren().add(new Text("Hello, I happened on " + session.getDate() + "."));
        name = "View: " + session.getDate();
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected String getName() {
        return name;
    }
}
