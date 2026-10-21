package net.techmayhem.clocktrack.ui.script;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.gamedata.*;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.component.ErrorSummary;
import org.jspecify.annotations.Nullable;

/// A screen for creating a new script or editing an existing one.
///
/// The name of the script and valid JSON are required.
public class ScriptEditor extends Screen {
    @Nullable private final FromDb<Script> scriptFromDb;

    private final String name;
    private final VBox root;
    private final TextField scriptName;
    private final TextArea scriptJson;
    private final ErrorSummary errorSummary;

    private @Nullable String autoFilledName;

    /// Creates an editor for the given script, with its fields filled in. Without a script, the fields start empty.
    ///
    /// @param script The script to edit, or null to create a new one
    /// @param lockJson If true, the JSON can not be edited. Used when a script is in use
    public ScriptEditor(@Nullable FromDb<Script> script, boolean lockJson) {
        scriptFromDb = script;
        if (script != null) {
            name = script.model().name();
        } else {
            name = localize("scriptEditor.title.new");
        }

        scriptName = new TextField();
        scriptJson = new ScriptArea();
        VBox.setVgrow(scriptJson, Priority.ALWAYS);

        errorSummary = new ErrorSummary(this::validate, scriptName.textProperty(), scriptJson.textProperty());

        scriptJson.textProperty().addListener((_, _, json) -> {
            try {
                ParsedScript parsed = ScriptParser.parse(json, Roles.getInstance());
                fillNameFromScript(parsed);
            } catch (ScriptFormatException _) {
            }
        });

        Button submitButton = new Button(localize("generic.submit"));
        submitButton.setDefaultButton(true);
        submitButton.setOnAction(_ -> submit());
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        VBox nameRow = new VBox(new Label(localize("scriptEditor.label.name")), scriptName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label jsonTextLabel = new Label(localize("scriptEditor.label.json"));
        HBox scriptHeader = new HBox(jsonTextLabel, spacer);
        if (lockJson) {
            scriptJson.setEditable(false);
            Label lockedLabel = new Label(localize("scriptEditor.label.lockedJson"));
            scriptHeader.getChildren().add(lockedLabel);
        } else {
            Button formatButton = new Button(localize("scriptEditor.format"));
            formatButton.setOnAction(_ -> formatJson());
            scriptHeader.getChildren().add(formatButton);
        }

        root = new VBox(Layout.SPACING, nameRow, scriptHeader, scriptJson, errorSummary, submitButton);
        root.setPadding(new Insets(Layout.PADDING));

        if (script != null) {
            Script model = script.model();
            scriptName.setText(model.name());
            scriptJson.setText(model.json());
        }
    }

    private static String describe(ScriptProblem problem) {
        int entry = problem.entry();
        String detail = problem.detail();
        return switch (problem.type()) {
            case SYNTAX ->
                detail.isEmpty()
                        ? localize("scriptEditor.inspection.syntax")
                        : localize("scriptEditor.inspection.syntaxAtLine", detail);
            case NOT_AN_ARRAY -> localize("scriptEditor.inspection.notAnArray");
            case INVALID_ENTRY -> localize("scriptEditor.inspection.invalidEntry", entry, detail);
            case MISSING_FIELD -> localize("scriptEditor.inspection.missingField", entry, detail);
            case UNKNOWN_ROLE -> localize("scriptEditor.inspection.unknownRole", entry, detail);
            case INVALID_ID -> localize("scriptEditor.inspection.invalidId", entry, detail);
            case INVALID_TEAM -> localize("scriptEditor.inspection.invalidTeam", entry, detail);
            case DUPLICATE_ID -> localize("scriptEditor.inspection.duplicateId", entry, detail);
        };
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (scriptName.getText().isBlank()) {
            errors.add(localize("scriptEditor.inspection.name"));
        }

        String scriptJsonText = scriptJson.getText();
        if (scriptJsonText.isBlank()) {
            errors.add(localize("scriptEditor.inspection.json"));
        } else {
            try {
                ScriptParser.parse(scriptJsonText, Roles.getInstance());
            } catch (ScriptFormatException e) {
                errors.addAll(e.problems().stream().map(ScriptEditor::describe).toList());
            }
        }

        return errors;
    }

    private void formatJson() {
        String jsonText = scriptJson.getText();
        if (jsonText.isBlank()) return;
        try {
            String formatted = ScriptParser.format(jsonText);
            scriptJson.replaceText(0, jsonText.length(), formatted);
        } catch (ScriptFormatException _) {
        }
    }

    private void fillNameFromScript(ParsedScript parsed) {
        String metaName = parsed.name();
        if (metaName == null || metaName.isBlank()) return;

        String current = scriptName.getText();
        boolean untouched = current.isBlank() || current.equals(autoFilledName);
        if (!untouched) return;

        autoFilledName = metaName.strip();
        scriptName.setText(autoFilledName);
    }

    private void submit() {
        if (errorSummary.hasErrors().get()) return;
        Database db = Database.getInstance();
        Script model =
                new Script(scriptName.getText().strip(), scriptJson.getText().strip());
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

    private static class ScriptArea extends TextArea {
        @Override
        public void paste() {
            if (!isEditable()) return;
            super.paste();
            try {
                String formatted = ScriptParser.format(getText());
                replaceText(0, getLength(), formatted);
                positionCaret(0);
            } catch (ScriptFormatException _) {
            }
        }
    }
}
