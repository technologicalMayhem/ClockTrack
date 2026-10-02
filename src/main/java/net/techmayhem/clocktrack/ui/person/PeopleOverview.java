package net.techmayhem.clocktrack.ui.person;

import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Person;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

public class PeopleOverview extends EntityOverview {
    private final HBox root;
    private final TableView<FromDb<Person>> table;

    public PeopleOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
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

        TableColumn<FromDb<Person>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().model().name()));
        table.getColumns().add(nameCol);
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
        List<FromDb<Person>> allPersons = Database.getInstance().getAllPersons();
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
        FromDb<Person> selectedPerson = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Confirm deletion",
                "Do you really want to delete " + selectedPerson.model().name() + "?",
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
        FromDb<Person> selectedPerson = table.getSelectionModel().getSelectedItem();
        Dialogs.showTextDialog(
                "Edit Person",
                "Enter the new name of the person",
                selectedPerson.model().name(),
                "Rename",
                "Cancel",
                s -> updateName(selectedPerson, s));
    }

    private void updateName(FromDb<Person> person, String newName) {
        if (newName.equals(person.model().name())) {
            return;
        }
        Person newPerson = new Person(newName);
        Database.getInstance().updatePerson(person.with(newPerson));
        updateTable();
    }
}
