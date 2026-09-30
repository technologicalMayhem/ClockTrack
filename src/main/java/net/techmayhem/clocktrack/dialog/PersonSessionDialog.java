package net.techmayhem.clocktrack.dialog;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.Database;
import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Person;
import net.techmayhem.clocktrack.models.PersonSession;
import net.techmayhem.clocktrack.screens.DisplayConverter;
import net.techmayhem.clocktrack.screens.MaybeFromDb;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class PersonSessionDialog {
    private final ChoiceBox<FromDb<Person>> personChoice;
    private final TextField roleField;
    private final CheckBox died;
    private final Spinner<Integer> deathOnDaySpinner;
    private final TextField causeOfDeathField;
    private final RadioButton goodRadio;
    private final RadioButton evilRadio;
    private final TextArea noteText;

    private final Stage stage;

    private boolean submit = false;

    public PersonSessionDialog(@Nullable MaybeFromDb<PersonSession> personSession) {
        Database db = Database.getInstance();
        String title;
        if (personSession != null) {
            FromDb<Person> person = db.getPerson(personSession.getEither().personId());
            title = "Editing Session for " + person.model().name();
        } else {
            title = "Creating a new session person entry";
        }
        stage = Dialogs.createStage(title);

        List<FromDb<Person>> people = db.getAllPersons();
        personChoice = new ChoiceBox<>();
        personChoice.getItems().addAll(people);
        personChoice.setConverter(new DisplayConverter<>(personFromDb -> personFromDb.model().name()));

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
            submit = true;
            stage.close();
        });
        submitButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::cannotSubmit,
                personChoice.valueProperty(),
                roleField.textProperty(),
                died.selectedProperty(),
                deathOnDaySpinner.valueProperty(),
                causeOfDeathField.textProperty(),
                alignmentGroup.selectedToggleProperty()
        ));

        GridPane grid = new GridPane(5.0, 5.0);
        grid.addRow(0, new Label("Person"), personChoice);
        grid.addRow(1, new Label("Role"), roleField);
        grid.addRow(2, new Label("Death"), new HBox(5.0, died, deathOnDaySpinner, causeOfDeathField));
        grid.addRow(3, new Label("Alignment"), new HBox(5.0, goodRadio, evilRadio));

        VBox vBox = Dialogs.createVBox();
        vBox.getChildren().addAll(grid, new Label("Notes"), noteText, submitButton);

        if (personSession != null) {
            PersonSession model = personSession.getEither();
            personChoice.getItems().stream().filter(tablePerson -> tablePerson.id() == model.personId()).findFirst().ifPresent(personChoice::setValue);
            roleField.setText(model.role());
            if (model.deathOnDay() != null && model.causeOfDeath() != null) {
                died.setSelected(true);
                deathOnDaySpinner.getValueFactory().setValue(model.deathOnDay());
                causeOfDeathField.setText(model.causeOfDeath());
            }
            if (model.good()) {
                goodRadio.setSelected(true);
            } else {
                evilRadio.setSelected(true);
            }
            noteText.setText(model.note());
        }

        Scene dialogScene = new Scene(vBox);
        stage.setScene(dialogScene);
    }

    public @Nullable PersonSession showAndWait() {
        stage.showAndWait();
        if (!submit && cannotSubmit()) return null;

        int personId = personChoice.getSelectionModel().getSelectedItem().id();
        String role = roleField.getText();
        Integer deathOnDay = died.isSelected() ? deathOnDaySpinner.getValue() : null;
        String causeOfDeath = died.isSelected() ? causeOfDeathField.getText() : null;
        boolean good = goodRadio.isSelected();
        String note = noteText.getText();

        return new PersonSession(-1, personId, role, deathOnDay, causeOfDeath, good, note);
    }

    private boolean cannotSubmit() {
        return personChoice.getSelectionModel().isEmpty() || roleField.getText().isBlank() || (died.isSelected() && causeOfDeathField.getText().isBlank()) || (!goodRadio.isSelected() && !evilRadio.isSelected());
    }
}
