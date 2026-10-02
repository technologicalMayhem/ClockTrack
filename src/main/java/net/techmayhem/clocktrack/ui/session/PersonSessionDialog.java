package net.techmayhem.clocktrack.ui.session;

import java.util.List;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Person;
import net.techmayhem.clocktrack.model.PersonSession;
import net.techmayhem.clocktrack.ui.component.DisplayConverter;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import org.jspecify.annotations.Nullable;

public class PersonSessionDialog {
    private final ComboBox<FromDb<Person>> personChoice;
    private final TextField roleField;
    private final CheckBox died;
    private final Spinner<Integer> deathOnDaySpinner;
    private final TextField causeOfDeathField;
    private final RadioButton goodRadio;
    private final RadioButton evilRadio;
    private final TextArea noteText;

    private final Stage stage;
    private final List<FromDb<Person>> unavailablePeople;

    private boolean shouldSubmit = false;

    public PersonSessionDialog(
            @Nullable PersonSession personSession,
            List<FromDb<Person>> availablePeople,
            List<FromDb<Person>> unavailablePeople) {
        this.unavailablePeople = unavailablePeople;
        List<FromDb<Person>> allPeople = Stream.concat(availablePeople.stream(), this.unavailablePeople.stream())
                .toList();
        String title;
        if (personSession != null) {
            String name = allPeople.stream()
                    .filter(person -> person.id() == personSession.personId())
                    .findFirst()
                    .map(person -> person.model().name())
                    .orElse("player");
            title = "Editing Session for " + name;
        } else {
            title = "Creating a new session person entry";
        }
        stage = Dialogs.createStage(title);

        personChoice = new ComboBox<>();
        personChoice.getItems().addAll(allPeople);
        personChoice.setConverter(
                new DisplayConverter<>(personFromDb -> personFromDb.model().name()));
        personChoice.setCellFactory(_ -> new PersonChoiceCell());

        roleField = new TextField();

        died = new CheckBox("Died?");
        deathOnDaySpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
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

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(_ -> {
            shouldSubmit = true;
            stage.close();
        });
        submitButton
                .disableProperty()
                .bind(Bindings.createBooleanBinding(
                        this::cannotSubmit,
                        personChoice.valueProperty(),
                        roleField.textProperty(),
                        died.selectedProperty(),
                        deathOnDaySpinner.valueProperty(),
                        causeOfDeathField.textProperty(),
                        alignmentGroup.selectedToggleProperty()));

        GridPane grid = new GridPane(5.0, 5.0);
        grid.addRow(0, new Label("Person"), personChoice);
        grid.addRow(1, new Label("Role"), roleField);
        grid.addRow(2, new Label("Death"), new HBox(5.0, died, deathOnDaySpinner, causeOfDeathField));
        grid.addRow(3, new Label("Alignment"), new HBox(5.0, goodRadio, evilRadio));

        VBox vBox = Dialogs.createVBox();
        vBox.getChildren().addAll(grid, new Label("Notes"), noteText, submitButton);

        if (personSession != null) {
            personChoice.getItems().stream()
                    .filter(tablePerson -> tablePerson.id() == personSession.personId())
                    .findFirst()
                    .ifPresent(personChoice::setValue);
            roleField.setText(personSession.role());
            if (personSession.deathOnDay() != null && personSession.causeOfDeath() != null) {
                died.setSelected(true);
                deathOnDaySpinner.getValueFactory().setValue(personSession.deathOnDay());
                causeOfDeathField.setText(personSession.causeOfDeath());
            }
            if (personSession.good()) {
                goodRadio.setSelected(true);
            } else {
                evilRadio.setSelected(true);
            }
            noteText.setText(personSession.note());
        }

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
    }

    public @Nullable PersonSession showAndWait() {
        stage.showAndWait();
        if (!shouldSubmit || cannotSubmit()) return null;

        int personId = personChoice.getSelectionModel().getSelectedItem().id();
        String role = roleField.getText();
        Integer deathOnDay = died.isSelected() ? deathOnDaySpinner.getValue() : null;
        String causeOfDeath = died.isSelected() ? causeOfDeathField.getText() : null;
        boolean good = goodRadio.isSelected();
        String note = noteText.getText();

        return new PersonSession(-1, personId, role, deathOnDay, causeOfDeath, good, note);
    }

    private boolean cannotSubmit() {
        return personChoice.getSelectionModel().isEmpty()
                || isSelectedPersonUnavailable()
                || roleField.getText().isBlank()
                || (died.isSelected() && causeOfDeathField.getText().isBlank())
                || (!goodRadio.isSelected() && !evilRadio.isSelected());
    }

    private boolean isSelectedPersonUnavailable() {
        FromDb<Person> selected = personChoice.getValue();
        return unavailablePeople.contains(selected);
    }

    private class PersonChoiceCell extends ListCell<FromDb<Person>> {
        @Override
        protected void updateItem(FromDb<Person> person, boolean empty) {
            super.updateItem(person, empty);
            if (empty) {
                setText(null);
                setDisable(false);
                setOpacity(1.0);
                return;
            }
            boolean unavailable = unavailablePeople.contains(person);
            setText(person.model().name());
            setDisable(unavailable);
            setOpacity(unavailable ? 0.4 : 1.0);
        }
    }
}
