package net.techmayhem.clocktrack.ui.session;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.model.Session;
import net.techmayhem.clocktrack.ui.Screen;

public class SessionView extends Screen {
    private final StackPane root;
    private final String name;

    SessionView(Session session) {
        root = new StackPane();
        root.getChildren().add(new Text("Hello, I happened on " + session.date() + "."));
        name = "View: " + session.date();
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
