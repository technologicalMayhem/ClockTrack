package net.techmayhem.clocktrack.ui.player;

import java.util.ArrayList;
import java.util.List;
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
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.component.ErrorSummary;
import org.jspecify.annotations.Nullable;

public class PlayerEditor extends Screen {
    @Nullable private final FromDb<Player> playerFromDb;

    private final String name;
    private final VBox root;
    private final TextField nameField;
    private final TextArea notesArea;
    private final ErrorSummary errorSummary;

    public PlayerEditor(@Nullable FromDb<Player> player) {
        playerFromDb = player;
        if (player != null) {
            name = player.model().name();
        } else {
            name = "New Player";
        }

        nameField = new TextField();
        notesArea = new TextArea();
        VBox.setVgrow(notesArea, Priority.ALWAYS);

        errorSummary = new ErrorSummary(this::validate, nameField.textProperty());

        Button submitButton = new Button("Submit");
        submitButton.setDefaultButton(true);
        submitButton.setOnAction(_ -> submit());
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        VBox nameRow = new VBox(new Label("Name"), nameField);

        root = new VBox(Layout.SPACING, nameRow, new Label("Notes"), notesArea, errorSummary, submitButton);
        root.setPadding(new Insets(Layout.PADDING));

        if (player != null) {
            Player model = player.model();
            nameField.setText(model.name().toString());
            if (model.notes() != null) notesArea.setText(model.notes());
        }
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (nameField.getText().isBlank()) {
            errors.add("Name is required");
        }

        return errors;
    }

    private void submit() {
        if (errorSummary.hasErrors().get()) return;
        Database db = Database.getInstance();
        String notes = notesArea.getText();
        Player model = new Player(nameField.getText().strip(), notes.isBlank() ? null : notes);
        if (playerFromDb != null) {
            db.updatePlayer(playerFromDb.with(model));
        } else {
            db.insertPlayer(model);
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
