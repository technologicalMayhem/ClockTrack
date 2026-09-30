package net.techmayhem.clocktrack.screens.sessions;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.techmayhem.clocktrack.Database;
import net.techmayhem.clocktrack.dialog.PersonSessionDialog;
import net.techmayhem.clocktrack.models.*;
import net.techmayhem.clocktrack.screens.DisplayConverter;
import net.techmayhem.clocktrack.screens.MaybeFromDb;
import net.techmayhem.clocktrack.screens.Screen;
import net.techmayhem.clocktrack.utils.ColumnDef;
import net.techmayhem.clocktrack.utils.TableHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SessionEdit extends Screen {
    private final VBox root;
    @Nullable
    private final FromDb<Session> session;
    private final List<MaybeFromDb<PersonSession>> personSessions;

    private final DatePicker datePicker;
    private final ChoiceBox<FromDb<Person>> storyteller;
    private final RadioButton goodWon;
    private final RadioButton evilWon;
    private final ChoiceBox<FromDb<Script>> script;
    private final TextArea note;
    private final TableView<MaybeFromDb<PersonSession>> personSessionTable;

    public SessionEdit(@Nullable FromDb<Session> session, @Nullable List<FromDb<PersonSession>> personSessions) {
        root = new VBox(5.0);
        root.setPadding(new Insets(10));
        this.session = session;
        if (personSessions != null) {
            this.personSessions = personSessions.stream().map(MaybeFromDb::of).collect(Collectors.toList());
        } else {
            this.personSessions = new ArrayList<>();
        }

        Database db = Database.getInstance();

        datePicker = new DatePicker();

        storyteller = new ChoiceBox<>();
        storyteller.getItems().addAll(db.getAllPersons());
        storyteller.setConverter(new DisplayConverter<>(p -> p.model().name()));

        ToggleGroup winnerGroup = new ToggleGroup();
        goodWon = new RadioButton("Good");
        goodWon.setToggleGroup(winnerGroup);
        evilWon = new RadioButton("Evil");
        evilWon.setToggleGroup(winnerGroup);

        script = new ChoiceBox<>();
        script.getItems().addAll(db.getAllScripts());
        script.setConverter(new DisplayConverter<>(s -> s.model().name()));

        Button addButton = new Button("Add player");
        Button editButton = new Button("Edit player");
        Button removeButton = new Button("Remove player");

        addButton.setOnAction(_ -> addPlayer());
        editButton.setOnAction(_ -> editPlayer());
        removeButton.setOnAction(_ -> removePlayer());
        personSessionTable = new TableView<>();
        var noSelection = personSessionTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        removeButton.disableProperty().bind(noSelection);

        List<Button> buttons = List.of(addButton, editButton, removeButton);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        HBox buttonRow = new HBox(5);
        buttonRow.getChildren().addAll(buttons);

        HBox aboveTable = new HBox(5);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        aboveTable.getChildren().addAll(new Label("People"), spacer, buttonRow);

        TableHelper.buildTableColumns(personSessionTable,
                new ColumnDef<>("Player", ps -> Database.getInstance().getPerson(ps.getEither().personId()).model().name()),
                new ColumnDef<>("Role", ps -> ps.getEither().role()),
                new ColumnDef<>("Alignment", ps -> ps.getEither().good() ? "Good" : "Evil"),
                new ColumnDef<>("Died on day", ps -> {
                    Integer day = ps.getEither().deathOnDay();
                    return day == null ? "" : String.valueOf(day);
                }),
                new ColumnDef<>("Cause of death", ps -> {
                    String cause = ps.getEither().causeOfDeath();
                    return cause == null ? "" : cause;
                }),
                new ColumnDef<>("Note", ps -> ps.getEither().note() == null ? "" : ps.getEither().note())
        );
        personSessionTable.getItems().setAll(this.personSessions);
        personSessionTable.refresh();

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

        root.getChildren().addAll(grid, aboveTable, personSessionTable, new Label("Notes"), note, submitButton);

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

    private void addPlayer() {
        // Todo: Adding a player with a name that already exists makes it impossible to submit changes due to the
        //  uniqueness constraint being violated. This needs to prevented from happening.
        PersonSession newPersonSession = new PersonSessionDialog(null).showAndWait();
        if (newPersonSession == null) return;
        personSessions.add(MaybeFromDb.of(newPersonSession));
        updateTable();
    }

    private void editPlayer() {
        // Todo: how editing work needs to be overhauled. Changing players on an edit has strange results.
        MaybeFromDb<PersonSession> sessionFromDb = personSessionTable.getSelectionModel().getSelectedItem();
        PersonSession editedPersonSession = new PersonSessionDialog(sessionFromDb).showAndWait();
        if (editedPersonSession == null) return;
        personSessions.removeIf(current -> current.getEither().personId() == editedPersonSession.personId());
        personSessions.add(MaybeFromDb.of(editedPersonSession));
        updateTable();
    }

    private void removePlayer() {
        MaybeFromDb<PersonSession> sessionFromDb = personSessionTable.getSelectionModel().getSelectedItem();
        this.personSessions.remove(sessionFromDb);
        updateTable();
    }

    private void updateTable() {
        personSessionTable.getItems().setAll(personSessions);
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
        return "Editing " + session.model().date();
    }

    private boolean cannotSubmit() {
        return datePicker.getValue() == null || storyteller.getSelectionModel().isEmpty() || (!goodWon.isSelected() && !evilWon.isSelected()) || script.getSelectionModel().isEmpty();
    }

    private void submit() {
        if (cannotSubmit()) return;
        Database db = Database.getInstance();
        String text = note.getText();
        Session newSession = new Session(datePicker.getValue(), storyteller.getValue().id(), goodWon.isSelected(), script.getValue().id(), text.isEmpty() ? null : text);
        int sessionId;
        if (session == null) {
            sessionId = db.insertSession(newSession).id();
        } else {
            sessionId = session.id();
            db.updateSession(session.with(newSession));
            db.deleteAllPersonSessionsForSession(session.id());
        }
        for (MaybeFromDb<PersonSession> personSession : personSessions) {
            db.insertPersonSession(personSession.getEither().withSessionId(sessionId));
        }
        closeTab();
    }
}
