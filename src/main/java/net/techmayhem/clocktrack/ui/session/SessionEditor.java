package net.techmayhem.clocktrack.ui.session;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    final List<Optional<Integer>> selectablePlayers;

    private final DatePicker datePicker;
    private final ChoiceBox<Optional<Integer>> storytellerChoice;
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
        selectablePlayers = Stream.concat(
                        Stream.of(Optional.<Integer>empty()),
                        players.stream().map(FromDb::id).map(Optional::of))
                .toList();

        storytellerChoice = new ChoiceBox<>();
        storytellerChoice.getItems().addAll(selectablePlayers);
        storytellerChoice.setConverter(new DisplayConverter<>(p -> playerNameForId(p.orElse(null))));

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
        aboveTable.getChildren().addAll(new Label("Players"), spacer, buttonRow);

        TableHelper.buildTableColumns(
                playerSessionTable,
                new ColumnDef<>("Player", ps -> playerNameForId(ps.playerId())),
                new ColumnDef<>("Role", PlayerSession::role),
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
        playerSessionTable.getItems().setAll(this.playerSessions);
        playerSessionTable.refresh();

        note = new TextArea();

        int row = 0;
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        grid.addRow(row++, new Label("Date"), datePicker);
        grid.addRow(row++, new Label("Storyteller"), storytellerChoice);
        grid.addRow(row++, new Label("Winner"), new HBox(Layout.SPACING, goodWon, evilWon));
        grid.addRow(row, new Label("Script"), script);

        errorSummary = new ErrorSummary(
                this::validate,
                datePicker.valueProperty(),
                storytellerChoice.valueProperty(),
                winnerGroup.selectedToggleProperty(),
                script.valueProperty(),
                playerSessionTable.getItems());

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(_ -> submit());
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        root.getChildren()
                .addAll(grid, aboveTable, playerSessionTable, new Label("Notes"), note, errorSummary, submitButton);

        if (session != null) {
            Session model = session.model();
            datePicker.setValue(model.date());
            storytellerChoice.setValue(Optional.ofNullable(session.model().storytellerId()));
            if (model.goodWon()) {
                goodWon.setSelected(true);
            } else {
                evilWon.setSelected(true);
            }
            script.setValue(db.getScript(model.scriptId()));
            if (model.note() != null) note.setText(model.note());
        }
    }

    String playerNameForId(@Nullable Integer playerId) {
        return playerId == null
                ? "Unknown"
                : players.stream()
                        .filter(player -> player.id() == playerId)
                        .findFirst()
                        .orElseThrow()
                        .model()
                        .name();
    }

    boolean isPlayerInUse(int playerId) {
        return playerSessions.stream().anyMatch(ps -> Objects.equals(ps.playerId(), playerId));
    }

    private void addPlayer() {
        PlayerSession newPlayerSession = new PlayerSessionDialog(null, this).showAndWait();
        if (newPlayerSession == null) return;
        playerSessions.add(newPlayerSession);
        updateTable();
    }

    private void editPlayer() {
        PlayerSession selected = playerSessionTable.getSelectionModel().getSelectedItem();
        int index = playerSessions.indexOf(selected);
        PlayerSession edited = new PlayerSessionDialog(selected, this).showAndWait();
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
            return "Creating Session";
        }
        return "Editing " + session.model().date();
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (datePicker.getValue() == null) {
            errors.add("Date is required");
        }
        if (storytellerChoice.getSelectionModel().isEmpty()) {
            errors.add("Storyteller is required");
        }
        if (!goodWon.isSelected() && !evilWon.isSelected()) {
            errors.add("Winner is required");
        }
        if (script.getSelectionModel().isEmpty()) {
            errors.add("Script is required");
        }
        if (duplicatePlayer()) {
            errors.add("A player cannot be in a game more than once");
        }
        if (storytellerIsPlayer()) {
            errors.add("The storyteller cannot also be a player");
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
        Integer storytellerId = storytellerChoice.getValue().orElse(null);
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
                storytellerChoice.getValue().orElse(null),
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
