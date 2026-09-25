package net.techmayhem.clocktrack.screens.sessions;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.Database;
import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Person;
import net.techmayhem.clocktrack.models.Script;
import net.techmayhem.clocktrack.models.Session;
import net.techmayhem.clocktrack.screens.Screen;
import org.jspecify.annotations.Nullable;

public class SessionEdit extends Screen {
    private final VBox root;
    @Nullable
    private final FromDb<Session> session;

    private final DatePicker datePicker;
    private final ChoiceBox<FromDb<Person>> storyteller;
    private final RadioButton goodWon;
    private final RadioButton evilWon;
    private final ChoiceBox<FromDb<Script>> script;
    private final TextArea note;

    public SessionEdit(@Nullable FromDb<Session> session) {
        root = new VBox(5.0);
        root.setPadding(new Insets(10));
        this.session = session;

        Database db = Database.getInstance();

        datePicker = new DatePicker();

        storyteller = new ChoiceBox<>();
        storyteller.getItems().addAll(db.getAllPersons());

        ToggleGroup winnerGroup = new ToggleGroup();
        goodWon = new RadioButton("Good");
        goodWon.setToggleGroup(winnerGroup);
        evilWon = new RadioButton("Evil");
        evilWon.setToggleGroup(winnerGroup);

        script = new ChoiceBox<>();
        script.getItems().addAll(db.getAllScripts());

        note = new TextArea();

        GridPane grid = new GridPane(5.0, 5.0);
        grid.addRow(0, new Label("Date"), datePicker);
        grid.addRow(1, new Label("Storyteller"), storyteller);
        grid.addRow(2, new Label("Winner"), new HBox(5.0, goodWon, evilWon));
        grid.addRow(3, new Label("Script"), script);

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(_ -> submit());
        submitButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::cannotSubmit,
                datePicker.valueProperty(),
                storyteller.valueProperty(),
                winnerGroup.selectedToggleProperty(),
                script.valueProperty()
        ));

        root.getChildren().addAll(grid, new Label("Notes"), note, submitButton);

        if (session != null) {
            Session model = session.model();
            datePicker.setValue(model.date());
            storyteller.setValue(db.getPerson(model.storyteller()));
            if (model.goodWon()) {
                goodWon.setSelected(true);
            } else {
                evilWon.setSelected(true);
            }
            script.setValue(db.getScript(model.script()));
            if (model.note() != null) note.setText(model.note());
        }
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected String getName() {
        if (session == null) {
            return "Creating Session";
        }
        return "Editing " + session;
    }

    private boolean cannotSubmit() {
        return datePicker.getValue() == null || storyteller.getSelectionModel().isEmpty() || (!goodWon.isSelected() && !evilWon.isSelected()) || script.getSelectionModel().isEmpty();
    }

    private void submit() {
        if (cannotSubmit()) return;
        Database db = Database.getInstance();
        String text = note.getText();
        Session newSession = new Session(datePicker.getValue(), storyteller.getValue().id(), goodWon.isSelected(), script.getValue().id(), text.isEmpty() ? null : text);
        if (session == null) {
            db.insertSession(newSession);
        } else {
            db.updateSession(session.with(newSession));
        }
        closeTab();
    }
}
