package net.techmayhem.clocktrack.screens.sessions;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import net.techmayhem.clocktrack.models.Session;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;

import java.time.LocalDate;
import java.util.function.Consumer;

public class SessionsOverview extends Overview {
    private final StackPane root;

    public SessionsOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
        root = new StackPane();
        Button button = new Button("Open session");
        button.setOnAction(_ -> editSession());
        root.getChildren().add(button);
    }

    @Override
    protected String getSectionName() {
        return "Sessions";
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void editSession() {
        Session session = new Session(LocalDate.EPOCH, 0, true, 1, null);
        createTabCallback.accept(new SessionView(session));
    }
}
