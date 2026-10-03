package net.techmayhem.clocktrack.ui.person;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Person;
import net.techmayhem.clocktrack.projections.PersonSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

public class PeopleOverview extends EntityOverview {
    private final HBox root;
    private final TableView<PersonSummary> table;

    public PeopleOverview(ScreenHost screenHost) {
        super(screenHost);
        table = new TableView<>();
        root = new HBox();
        buildUi();
        updateTable();
    }

    @Override
    protected String getSectionName() {
        return "People";
    }

    private void buildUi() {
        Button createButton = new Button("Add person");
        Button editButton = new Button("Edit person");
        Button deleteButton = new Button("Delete person");

        createButton.setOnAction(_ -> spawnNewPersonDialog());
        editButton.setOnAction(_ -> spawnEditPersonDialog());
        deleteButton.setOnAction(_ -> deleteSelectedPerson());
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        List<Button> buttons = List.of(createButton, editButton, deleteButton);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        VBox buttonColumn = new VBox();
        buttonColumn.setSpacing(5);
        buttonColumn.getChildren().addAll(buttons);

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>("Name", PersonSummary::name),
                new ColumnDef<>("First game", PersonSummary::firstGame),
                new ColumnDef<>("Latest game", PersonSummary::lastGame),
                new ColumnDef<>("Games played", PersonSummary::games_played),
                new ColumnDef<>("Games storytold", PersonSummary::games_storytold));
        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(10));
        root.setSpacing(5);
        root.getChildren().addAll(table, buttonColumn);
    }

    @Override
    protected void onShow() {
        updateTable();
    }

    private void updateTable() {
        List<PersonSummary> allPersons = Database.getInstance().getPersonSummaries();
        table.setItems(FXCollections.observableArrayList(allPersons));
    }

    private void spawnNewPersonDialog() {
        Dialogs.showTextDialog(
                "Add Person", "Enter the name of the person to add", "", "Add", "Cancel", this::createPerson);
    }

    private void createPerson(String name) {
        Database.getInstance().insertPerson(new Person(name));
        updateTable();
    }

    private void deleteSelectedPerson() {
        PersonSummary selectedPerson = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Confirm deletion",
                "Do you really want to delete " + selectedPerson.name() + "?",
                "Delete",
                "Cancel",
                () -> {
                    Database.getInstance().deletePerson(selectedPerson.id());
                    updateTable();
                });
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void spawnEditPersonDialog() {
        PersonSummary selectedPerson = table.getSelectionModel().getSelectedItem();
        Dialogs.showTextDialog(
                "Edit Person",
                "Enter the new name of the person",
                selectedPerson.name(),
                "Rename",
                "Cancel",
                s -> updateName(selectedPerson, s));
    }

    private void updateName(PersonSummary person, String newName) {
        if (newName.equals(person.name())) {
            return;
        }
        Person newPerson = new Person(newName);
        Database.getInstance().updatePerson(new FromDb<>(person.id(), newPerson));
        updateTable();
    }
}
