package net.techmayhem.clocktrack.screens.scripts;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import net.techmayhem.clocktrack.models.Script;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;

import java.util.function.Consumer;

public class ScriptsOverview extends Overview {
    private final StackPane root;

    public ScriptsOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
        root = new StackPane();
        Button button = new Button("Open script");
        button.setOnAction(_ -> editPerson());
        root.getChildren().add(button);
    }

    @Override
    protected String getSectionName() {
        return "Scripts";
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void editPerson() {
        Script script = new Script("Trouble Brewing", null);
        createTabCallback.accept(new ScriptView(script));
    }
}
