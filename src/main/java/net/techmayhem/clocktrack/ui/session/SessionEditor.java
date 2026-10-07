package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.*;
import java.util.stream.Collectors;
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
import net.techmayhem.clocktrack.ui.component.ErrorSummary;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import org.jspecify.annotations.Nullable;

public class SessionEditor extends Screen {
    private final VBox root;

    @Nullable private final FromDb<Session> session;

    private final List<PlayerSession> playerSessions;
    private final List<FromDb<Player>> players;
    private final PlayerChoices playerChoices;

    private final DatePicker datePicker;
    private final ChoiceBox<Optional<FromDb<Player>>> storytellerChoice;
    private final RadioButton goodWon;
    private final RadioButton evilWon;
    private final ChoiceBox<FromDb<Script>> script;
    private final TextArea note;
    private final TableView<PlayerSession> playerSessionTable;
    private final ErrorSummary errorSummary;

    public SessionEditor(@Nullable FromDb<Session> session, @Nullable List<FromDb<PlayerSession>> playerSessions) {
        root = new VBox(Layout.SPACING);
        root.setPadding(new Insets(Layout.PADDING));
        this.session = session;
        if (playerSessions != null) {
            this.playerSessions =
                    playerSessions.stream().map(FromDb::model).collect(Collectors.toCollection(ArrayList::new));
        } else {
            this.playerSessions = new ArrayList<>();
        }

        Database db = Database.getInstance();

        datePicker = new DatePicker();

        players = db.getAllPlayers();
        playerChoices = new PlayerChoices(players);

        storytellerChoice = new ChoiceBox<>();
        storytellerChoice.getItems().addAll(playerChoices.items());
        storytellerChoice.setConverter(playerChoices.converter());

        ToggleGroup winnerGroup = new ToggleGroup();
        goodWon = new RadioButton(localize("generic.good"));
        goodWon.setToggleGroup(winnerGroup);
        evilWon = new RadioButton(localize("generic.evil"));
        evilWon.setToggleGroup(winnerGroup);

        script = new ChoiceBox<>();
        script.getItems().addAll(db.getAllScripts());
        script.setConverter(new DisplayConverter<>(s -> s.model().name()));

        Button addButton = new Button(localize("sessionEditor.addPlayer"));
        Button editButton = new Button(localize("sessionEditor.editPlayer"));
        Button removeButton = new Button(localize("sessionEditor.removePlayer"));

        addButton.setOnAction(_ -> addPlayer());
        editButton.setOnAction(_ -> editPlayer());
        removeButton.setOnAction(_ -> removePlayer());
        playerSessionTable = new TableView<>();
        var noSelection =
                playerSessionTable.getSelectionModel().selectedItemProperty().isNull();
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
        aboveTable.getChildren().addAll(new Label(localize("sessionEditor.labelPlayerTable")), spacer, buttonRow);

        TableHelper.buildTableColumns(
                playerSessionTable,
                new ColumnDef<>(localize("sessionEditor.table.player"), ps -> playerChoices.nameFor(ps.playerId())),
                new ColumnDef<>(localize("sessionEditor.table.role"), PlayerSession::role),
                new ColumnDef<>(localize("sessionEditor.table.alignment"), ps -> ps.good() ? "Good" : "Evil"),
                new ColumnDef<>(localize("sessionEditor.table.deathOnDay"), ps -> {
                    Integer day = ps.deathOnDay();
                    return day == null ? "" : String.valueOf(day);
                }),
                new ColumnDef<>(localize("sessionEditor.table.causeOfDeath"), ps -> {
                    String cause = ps.causeOfDeath();
                    return cause == null ? "" : cause;
                }),
                new ColumnDef<>(localize("sessionEditor.table.note"), ps -> ps.note() == null ? "" : ps.note()));
        playerSessionTable.getItems().setAll(this.playerSessions);
        playerSessionTable.refresh();

        note = new TextArea();

        int row = 0;
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        grid.addRow(row++, new Label(localize("sessionEditor.label.date")), datePicker);
        grid.addRow(row++, new Label(localize("sessionEditor.label.storyteller")), storytellerChoice);
        grid.addRow(
                row++, new Label(localize("sessionEditor.label.winner")), new HBox(Layout.SPACING, goodWon, evilWon));
        grid.addRow(row, new Label(localize("sessionEditor.label.script")), script);

        errorSummary = new ErrorSummary(
                this::validate,
                datePicker.valueProperty(),
                storytellerChoice.valueProperty(),
                winnerGroup.selectedToggleProperty(),
                script.valueProperty(),
                playerSessionTable.getItems());

        Button submitButton = new Button(localize("generic.submit"));
        submitButton.setOnAction(_ -> submit());
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        root.getChildren()
                .addAll(
                        grid,
                        aboveTable,
                        playerSessionTable,
                        new Label(localize("sessionEditor.label.notes")),
                        note,
                        errorSummary,
                        submitButton);

        if (session != null) {
            Session model = session.model();
            datePicker.setValue(model.date());
            storytellerChoice.setValue(playerChoices.itemFor(model.storytellerId()));
            if (model.goodWon()) {
                goodWon.setSelected(true);
            } else {
                evilWon.setSelected(true);
            }
            script.setValue(db.getScript(model.scriptId()));
            if (model.note() != null) note.setText(model.note());
        }
    }

    /** Returns the ids of the players in the game, ignoring the given entry. */
    private Set<Integer> takenPlayerIds(@Nullable PlayerSession ignored) {
        return playerSessions.stream()
                .filter(ps -> ps != ignored)
                .map(PlayerSession::playerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void addPlayer() {
        PlayerSession newPlayerSession = new PlayerSessionDialog(null, players, takenPlayerIds(null)).showAndWait();
        if (newPlayerSession == null) return;
        playerSessions.add(newPlayerSession);
        updateTable();
    }

    private void editPlayer() {
        PlayerSession selected = playerSessionTable.getSelectionModel().getSelectedItem();
        int index = playerSessions.indexOf(selected);
        PlayerSession edited = new PlayerSessionDialog(selected, players, takenPlayerIds(selected)).showAndWait();
        if (edited == null) return;
        playerSessions.set(index, edited);
        updateTable();
    }

    private void removePlayer() {
        PlayerSession sessionFromDb = playerSessionTable.getSelectionModel().getSelectedItem();
        this.playerSessions.remove(sessionFromDb);
        updateTable();
    }

    private void updateTable() {
        playerSessionTable.getItems().setAll(playerSessions);
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected String getName() {
        if (session == null) {
            return localize("sessionEditor.titleCreateSession");
        }
        return localize("sessionEditor.titleEditSession", session.model().date());
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (datePicker.getValue() == null) {
            errors.add(localize("sessionEditor.inspection.date"));
        }
        if (storytellerChoice.getSelectionModel().isEmpty()) {
            errors.add(localize("sessionEditor.inspection.storyteller"));
        }
        if (!goodWon.isSelected() && !evilWon.isSelected()) {
            errors.add(localize("sessionEditor.inspection.winner"));
        }
        if (script.getSelectionModel().isEmpty()) {
            errors.add(localize("sessionEditor.inspection.script"));
        }
        if (duplicatePlayer()) {
            errors.add(localize("sessionEditor.inspection.duplicatePlayer"));
        }
        if (storytellerIsPlayer()) {
            errors.add(localize("sessionEditor.inspection.storytellerIsPlayer"));
        }

        return errors;
    }

    private boolean duplicatePlayer() {
        HashSet<Integer> names = new HashSet<>();
        for (PlayerSession playerSession : playerSessions) {
            if (names.contains(playerSession.playerId())) {
                return true;
            } else if (playerSession.playerId() != null) {
                names.add(playerSession.playerId());
            }
        }
        return false;
    }

    private boolean storytellerIsPlayer() {
        if (storytellerChoice.getSelectionModel().isEmpty()) return false;
        Integer storytellerId = storytellerChoice.getValue().map(FromDb::id).orElse(null);
        if (storytellerId == null) return false;
        return playerSessions.stream()
                .anyMatch(playerSession -> playerSession.playerId() != null
                        && playerSession.playerId().equals(storytellerId));
    }

    private void submit() {
        if (errorSummary.hasErrors().get()) return;
        Database db = Database.getInstance();
        String text = note.getText();
        Session newSession = new Session(
                datePicker.getValue(),
                storytellerChoice.getValue().map(FromDb::id).orElse(null),
                goodWon.isSelected(),
                script.getValue().id(),
                text.isEmpty() ? null : text);
        if (session == null) {
            db.saveSession(MaybeFromDb.of(newSession), playerSessions);
        } else {
            db.saveSession(MaybeFromDb.of(session.with(newSession)), playerSessions);
        }
        closeTab();
    }
}
