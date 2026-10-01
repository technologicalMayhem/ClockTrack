package net.techmayhem.clocktrack.screens.scripts;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.models.Script;
import net.techmayhem.clocktrack.screens.Screen;

public class ScriptView extends Screen {
    private final StackPane root;
    private final String name;

    ScriptView(Script script) {
        root = new StackPane();
        root.getChildren().add(new Text("Hello, I am " + script.name() + ".\nMy json is: " + script.json()));
        name = "View: " + script.name();
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
