package net.techmayhem.clocktrack.ui.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.model.*;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.DisplayConverter;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import org.jspecify.annotations.Nullable;

public class SessionEditor extends Screen {
    private final VBox root;

    @Nullable private final FromDb<Session> session;

    private final List<PersonSession> personSessions;
    private final List<FromDb<Person>> people;

    private final DatePicker datePicker;
    private final ChoiceBox<FromDb<Person>> storytellerChoice;
    private final RadioButton goodWon;
    private final RadioButton evilWon;
    private final ChoiceBox<FromDb<Script>> script;
    private final TextArea note;
    private final TableView<PersonSession> personSessionTable;

    public SessionEditor(@Nullable FromDb<Session> session, @Nullable List<FromDb<PersonSession>> personSessions) {
        root = new VBox(Layout.SPACING);
        root.setPadding(new Insets(Layout.PADDING));
        this.session = session;
        if (personSessions != null) {
            this.personSessions =
                    personSessions.stream().map(FromDb::model).collect(Collectors.toCollection(ArrayList::new));
        } else {
            this.personSessions = new ArrayList<>();
        }

        Database db = Database.getInstance();

        datePicker = new DatePicker();

        people = db.getAllPersons();
        storytellerChoice = new ChoiceBox<>();
        storytellerChoice.getItems().addAll(people);
        storytellerChoice.setConverter(new DisplayConverter<>(p -> p.model().name()));

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
        var noSelection =
                personSessionTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        removeButton.disableProperty().bind(noSelection);

        List<Button> buttons = List.of(addButton, editButton, removeButton);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        HBox buttonRow = new HBox(Layout.SPACING);
        buttonRow.getChildren().addAll(buttons);

        HBox aboveTable = new HBox(Layout.SPACING);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        aboveTable.getChildren().addAll(new Label("People"), spacer, buttonRow);

        TableHelper.buildTableColumns(
                personSessionTable,
                new ColumnDef<>(
                        "Player",
                        ps -> Database.getInstance()
                                .getPerson(ps.personId())
                                .model()
                                .name()),
                new ColumnDef<>("Role", PersonSession::role),
                new ColumnDef<>("Alignment", ps -> ps.good() ? "Good" : "Evil"),
                new ColumnDef<>("Died on day", ps -> {
                    Integer day = ps.deathOnDay();
                    return day == null ? "" : String.valueOf(day);
                }),
                new ColumnDef<>("Cause of death", ps -> {
                    String cause = ps.causeOfDeath();
                    return cause == null ? "" : cause;
                }),
                new ColumnDef<>("Note", ps -> ps.note() == null ? "" : ps.note()));
        personSessionTable.getItems().setAll(this.personSessions);
        personSessionTable.refresh();

        note = new TextArea();

        int row = 0;
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        grid.addRow(row++, new Label("Date"), datePicker);
        grid.addRow(row++, new Label("Storyteller"), storytellerChoice);
        grid.addRow(row++, new Label("Winner"), new HBox(Layout.SPACING, goodWon, evilWon));
        grid.addRow(row, new Label("Script"), script);

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(_ -> submit());
        submitButton
                .disableProperty()
                .bind(Bindings.createBooleanBinding(
                        this::cannotSubmit,
                        datePicker.valueProperty(),
                        storytellerChoice.valueProperty(),
                        winnerGroup.selectedToggleProperty(),
                        script.valueProperty(),
                        personSessionTable.getItems()));

        root.getChildren().addAll(grid, aboveTable, personSessionTable, new Label("Notes"), note, submitButton);

        if (session != null) {
            Session model = session.model();
            datePicker.setValue(model.date());
            storytellerChoice.setValue(db.getPerson(model.storytellerId()));
            if (model.goodWon()) {
                goodWon.setSelected(true);
            } else {
                evilWon.setSelected(true);
            }
            script.setValue(db.getScript(model.scriptId()));
            if (model.note() != null) note.setText(model.note());
        }
    }

    private void addPlayer() {
        PersonSession newPersonSession =
                new PersonSessionDialog(null, availablePeople(null), unavailablePeople(null)).showAndWait();
        if (newPersonSession == null) return;
        personSessions.add(newPersonSession);
        updateTable();
    }

    private void editPlayer() {
        PersonSession selected = personSessionTable.getSelectionModel().getSelectedItem();
        // Look the row up by value. The table can be sorted, so its selection index is not the index in personSessions.
        int index = personSessions.indexOf(selected);
        PersonSession edited =
                new PersonSessionDialog(selected, availablePeople(selected), unavailablePeople(selected)).showAndWait();
        if (edited == null) return;
        personSessions.set(index, edited);
        updateTable();
    }

    private boolean isUnavailable(FromDb<Person> p, @Nullable PersonSession editing) {
        if (editing != null && editing.personId() == p.id()) return false;
        boolean hasStoryteller = !storytellerChoice.getSelectionModel().isEmpty();
        if (hasStoryteller && storytellerChoice.getValue().id() == p.id()) return true;
        return personSessions.stream().anyMatch(ps -> ps != editing && ps.personId() == p.id());
    }

    private List<FromDb<Person>> availablePeople(@Nullable PersonSession editing) {
        return people.stream().filter(p -> !isUnavailable(p, editing)).toList();
    }

    private List<FromDb<Person>> unavailablePeople(@Nullable PersonSession editing) {
        return people.stream().filter(p -> isUnavailable(p, editing)).toList();
    }

    private void removePlayer() {
        PersonSession sessionFromDb = personSessionTable.getSelectionModel().getSelectedItem();
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
        return datePicker.getValue() == null
                || storytellerChoice.getSelectionModel().isEmpty()
                || (!goodWon.isSelected() && !evilWon.isSelected())
                || script.getSelectionModel().isEmpty()
                || duplicatePlayer()
                || storytellerIsPlayer();
    }

    private boolean duplicatePlayer() {
        HashSet<Integer> names = new HashSet<>();
        for (PersonSession personSession : personSessions) {
            if (names.contains(personSession.personId())) return true;
            else names.add(personSession.personId());
        }
        return false;
    }

    private boolean storytellerIsPlayer() {
        if (storytellerChoice.getSelectionModel().isEmpty()) return false;
        FromDb<Person> currentStoryteller = storytellerChoice.getValue();
        return personSessions.stream().anyMatch(personSession -> personSession.personId() == currentStoryteller.id());
    }

    private void submit() {
        if (cannotSubmit()) return;
        Database db = Database.getInstance();
        String text = note.getText();
        Session newSession = new Session(
                datePicker.getValue(),
                storytellerChoice.getValue().id(),
                goodWon.isSelected(),
                script.getValue().id(),
                text.isEmpty() ? null : text);
        if (session == null) {
            db.saveSession(MaybeFromDb.of(newSession), personSessions);
        } else {
            db.saveSession(MaybeFromDb.of(session.with(newSession)), personSessions);
        }
        closeTab();
    }
}
