package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

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
    private static final int NO_ID = -1;

    private final PlayerChoices playerChoices;
    private final ComboBox<Optional<FromDb<Player>>> playerChoice;
    private final TextField roleField;
    private final CheckBox died;
    private final Spinner<Integer> deathPhase;
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
            title = localize("playerSessionDialog.title.edit", playerChoices.nameFor(playerSession.playerId()));
        } else {
            title = localize("playerSessionDialog.title.create");
        }
        stage = Dialogs.createStage(title);

        playerChoice = new ComboBox<>();
        playerChoice.getItems().addAll(playerChoices.items());
        playerChoice.setConverter(playerChoices.converter());
        playerChoice.setCellFactory(_ -> new PlayerChoiceCell());

        roleField = new TextField();

        died = new CheckBox(localize("playerSessionDialog.label.died"));

        var phaseFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                PhaseFormat.FIRST_PHASE, PhaseFormat.LAST_PHASE, PhaseFormat.FIRST_PHASE);
        phaseFactory.setConverter(new PhaseConverter(phaseFactory));
        deathPhase = new Spinner<>();
        deathPhase.setValueFactory(phaseFactory);
        deathPhase.setEditable(true);

        TextField phaseEditor = deathPhase.getEditor();
        phaseEditor.setTextFormatter(new TextFormatter<String>(
                change -> PhaseFormat.isTypedInput(change.getControlNewText()) ? change : null));
        phaseEditor.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                deathPhase.commitValue();
                phaseEditor.setText(phaseFactory.getConverter().toString(phaseFactory.getValue()));
            }
        });

        causeOfDeathField = new TextField();
        BooleanBinding diedProperty = died.selectedProperty().not();
        deathPhase.disableProperty().bind(diedProperty);
        causeOfDeathField.disableProperty().bind(diedProperty);

        ToggleGroup alignmentGroup = new ToggleGroup();
        goodRadio = new RadioButton(localize("generic.good"));
        goodRadio.setToggleGroup(alignmentGroup);
        evilRadio = new RadioButton(localize("generic.evil"));
        evilRadio.setToggleGroup(alignmentGroup);

        noteText = new TextArea();

        errorSummary = new ErrorSummary(
                this::validate,
                playerChoice.valueProperty(),
                roleField.textProperty(),
                died.selectedProperty(),
                deathPhase.valueProperty(),
                causeOfDeathField.textProperty(),
                alignmentGroup.selectedToggleProperty());

        Button submitButton = new Button(localize("generic.submit"));
        submitButton.setOnAction(_ -> {
            shouldSubmit = true;
            stage.close();
        });
        submitButton.disableProperty().bind(errorSummary.hasErrors());

        int row = 0;
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        grid.addRow(row++, new Label(localize("playerSessionDialog.label.player")), playerChoice);
        grid.addRow(row++, new Label(localize("playerSessionDialog.label.role")), roleField);
        grid.addRow(
                row++,
                new Label(localize("playerSessionDialog.label.death")),
                new HBox(Layout.SPACING, died, deathPhase, causeOfDeathField));
        grid.addRow(
                row,
                new Label(localize("playerSessionDialog.label.alignment")),
                new HBox(Layout.SPACING, goodRadio, evilRadio));

        VBox vBox = Dialogs.createVBox();
        vBox.getChildren()
                .addAll(
                        grid,
                        new Label(localize("playerSessionDialog.label.notes")),
                        noteText,
                        errorSummary,
                        submitButton);

        if (playerSession != null) {
            playerChoice.setValue(playerChoices.itemFor(playerSession.playerId()));
            roleField.setText(playerSession.role());
            if (playerSession.deathOnDay() != null && playerSession.causeOfDeath() != null) {
                died.setSelected(true);
                deathPhase.getValueFactory().setValue(playerSession.deathOnDay());
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
        Integer deathOnDay = died.isSelected() ? deathPhase.getValue() : null;
        String causeOfDeath = died.isSelected() ? causeOfDeathField.getText() : null;
        boolean good = goodRadio.isSelected();
        String note = noteText.getText();

        return new PlayerSession(NO_ID, playerId, role, deathOnDay, causeOfDeath, good, note);
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (playerChoice.getSelectionModel().isEmpty()) {
            errors.add(localize("playerSessionDialog.inspection.player"));
        } else if (playerChoice
                .getSelectionModel()
                .getSelectedItem()
                .map(this::isTaken)
                .orElse(false)) {
            errors.add(localize("playerSessionDialog.inspection.alreadyInGame"));
        }
        if (roleField.getText().isBlank()) {
            errors.add(localize("playerSessionDialog.inspection.role"));
        }
        if (died.isSelected() && causeOfDeathField.getText().isBlank()) {
            errors.add(localize("playerSessionDialog.inspection.causeOfDeath"));
        }
        if (!goodRadio.isSelected() && !evilRadio.isSelected()) {
            errors.add(localize("playerSessionDialog.inspection.alignment"));
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
