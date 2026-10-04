package net.techmayhem.clocktrack.ui.script;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import org.jspecify.annotations.Nullable;

public class ScriptEditor extends Screen {
    @Nullable private final FromDb<Script> scriptFromDb;

    private final String name;
    private final VBox root;
    private final TextField scriptName;
    private final TextArea scriptJson;

    public ScriptEditor(@Nullable FromDb<Script> script) {
        scriptFromDb = script;
        if (script != null) {
            name = script.model().name();
        } else {
            name = "New script";
        }

        scriptName = new TextField();
        scriptJson = new TextArea();
        VBox.setVgrow(scriptJson, Priority.ALWAYS);

        Button submitButton = new Button("Submit");
        submitButton.setDefaultButton(true);
        submitButton.setOnAction(_ -> submit());
        submitButton
                .disableProperty()
                .bind(Bindings.createBooleanBinding(this::cannotSubmit, scriptName.textProperty()));

        VBox nameRow = new VBox(new Label("Name"), scriptName);

        root = new VBox(Layout.SPACING, nameRow, new Label("JSON"), scriptJson, submitButton);
        root.setPadding(new Insets(Layout.PADDING));

        if (script != null) {
            Script model = script.model();
            scriptName.setText(model.name());
            if (model.json() != null) scriptJson.setText(model.json());
        }
    }

    private boolean cannotSubmit() {
        return scriptName.getText().isBlank();
    }

    private void submit() {
        if (cannotSubmit()) return;
        Database db = Database.getInstance();
        String json = scriptJson.getText();
        Script model = new Script(scriptName.getText(), json.isBlank() ? null : json);
        if (scriptFromDb != null) {
            db.updateScript(scriptFromDb.with(model));
        } else {
            db.insertScript(model);
        }
        closeTab();
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
