package net.techmayhem.clocktrack.ui.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.model.PlayerSession;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.component.ErrorSummary;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import org.jspecify.annotations.Nullable;

class PlayerSessionDialog {
    private static final int FIRST_DAY = 1;
    private static final int NO_ID = -1;

    private final PlayerChoices playerChoices;
    private final ComboBox<Optional<FromDb<Player>>> playerChoice;
    private final TextField roleField;
    private final CheckBox died;
    private final Spinner<Integer> deathOnDaySpinner;
    private final TextField causeOfDeathField;
    private final RadioButton goodRadio;
    private final RadioButton evilRadio;
    private final TextArea noteText;

    private final Stage stage;
    private final ErrorSummary errorSummary;
    private final Set<Integer> takenPlayerIds;

    private boolean shouldSubmit = false;

    public PlayerSessionDialog(
            @Nullable PlayerSession playerSession, List<FromDb<Player>> players, Set<Integer> takenPlayerIds) {
        this.takenPlayerIds = takenPlayerIds;
        playerChoices = new PlayerChoices(players);
        String title;
        if (playerSession != null) {
            title = "Editing Session for " + playerChoices.nameFor(playerSession.playerId());
        } else {
            title = "Creating a new session player entry";
        }
        stage = Dialogs.createStage(title);

        playerChoice = new ComboBox<>();
        playerChoice.getItems().addAll(playerChoices.items());
        playerChoice.setConverter(playerChoices.converter());
        playerChoice.setCellFactory(_ -> new PlayerChoiceCell());

        roleField = new TextField();

        died = new CheckBox("Died?");
        deathOnDaySpinner = new Spinner<>(FIRST_DAY, Integer.MAX_VALUE, FIRST_DAY);
        deathOnDaySpinner.setEditable(true);
        causeOfDeathField = new TextField();
        BooleanBinding diedProperty = died.selectedProperty().not();
        deathOnDaySpinner.disableProperty().bind(diedProperty);
        causeOfDeathField.disableProperty().bind(diedProperty);

        ToggleGroup alignmentGroup = new ToggleGroup();
        goodRadio = new RadioButton("Good");
        goodRadio.setToggleGroup(alignmentGroup);
        evilRadio = new RadioButton("Evil");
        evilRadio.setToggleGroup(alignmentGroup);

        noteText = new TextArea();

        errorSummary = new ErrorSummary(
                this::validate,
                playerChoice.valueProperty(),
                roleField.textProperty(),
                died.selectedProperty(),
                deathOnDaySpinner.valueProperty(),
                causeOfDeathField.textProperty(),
                alignmentGroup.selectedToggleProperty());

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(_ -> {
            shouldSubmit = true;
            stage.close();
        });
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        int row = 0;
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        grid.addRow(row++, new Label("Player"), playerChoice);
        grid.addRow(row++, new Label("Role"), roleField);
        grid.addRow(row++, new Label("Death"), new HBox(Layout.SPACING, died, deathOnDaySpinner, causeOfDeathField));
        grid.addRow(row, new Label("Alignment"), new HBox(Layout.SPACING, goodRadio, evilRadio));

        VBox vBox = Dialogs.createVBox();
        vBox.getChildren().addAll(grid, new Label("Notes"), noteText, errorSummary, submitButton);

        if (playerSession != null) {
            playerChoice.setValue(playerChoices.itemFor(playerSession.playerId()));
            roleField.setText(playerSession.role());
            if (playerSession.deathOnDay() != null && playerSession.causeOfDeath() != null) {
                died.setSelected(true);
                deathOnDaySpinner.getValueFactory().setValue(playerSession.deathOnDay());
                causeOfDeathField.setText(playerSession.causeOfDeath());
            }
            if (playerSession.good()) {
                goodRadio.setSelected(true);
            } else {
                evilRadio.setSelected(true);
            }
            noteText.setText(playerSession.note());
        }

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
    }

    public @Nullable PlayerSession showAndWait() {
        stage.showAndWait();
        if (!shouldSubmit || errorSummary.hasErrors().get()) return null;

        Integer playerId = playerChoice
                .getSelectionModel()
                .getSelectedItem()
                .map(FromDb::id)
                .orElse(null);
        String role = roleField.getText();
        Integer deathOnDay = died.isSelected() ? deathOnDaySpinner.getValue() : null;
        String causeOfDeath = died.isSelected() ? causeOfDeathField.getText() : null;
        boolean good = goodRadio.isSelected();
        String note = noteText.getText();

        return new PlayerSession(NO_ID, playerId, role, deathOnDay, causeOfDeath, good, note);
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (playerChoice.getSelectionModel().isEmpty()) {
            errors.add("Player is required");
        } else if (playerChoice
                .getSelectionModel()
                .getSelectedItem()
                .map(this::isTaken)
                .orElse(false)) {
            errors.add("The selected player is already in this game");
        }
        if (roleField.getText().isBlank()) {
            errors.add("Role is required");
        }
        if (died.isSelected() && causeOfDeathField.getText().isBlank()) {
            errors.add("Cause of death is required");
        }
        if (!goodRadio.isSelected() && !evilRadio.isSelected()) {
            errors.add("Alignment is required");
        }

        return errors;
    }

    private boolean isTaken(FromDb<Player> player) {
        return takenPlayerIds.contains(player.id());
    }

    private class PlayerChoiceCell extends ListCell<Optional<FromDb<Player>>> {
        private static final double OPACITY_NORMAL = 1.0;
        private static final double OPACITY_UNAVAILABLE = 0.4;

        @Override
        protected void updateItem(Optional<FromDb<Player>> choice, boolean empty) {
            super.updateItem(choice, empty);
            if (empty) {
                setText(null);
                setDisable(false);
                setOpacity(OPACITY_NORMAL);
                return;
            }
            boolean unavailable = choice.map(PlayerSessionDialog.this::isTaken).orElse(false);
            setText(playerChoices.converter().toString(choice));
            setDisable(unavailable);
            setOpacity(unavailable ? OPACITY_UNAVAILABLE : OPACITY_NORMAL);
        }
    }
}
