package net.techmayhem.clocktrack.screens.people;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.techmayhem.clocktrack.Database;
import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Person;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Todo: Implement proper error handling for DB operations
public class PeopleOverview extends Overview {
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
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().model().getName()));
        table.getColumns().add(nameCol);
        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(10));
        root.setSpacing(5);
        root.getChildren().addAll(table, buttonColumn);
    }

    private void updateTable() {
        List<FromDb<Person>> peopleList = new ArrayList<>();
        switch (Database.getInstance().getAllPersons()) {
            case Database.Result.Err<List<FromDb<Person>>> _ -> System.exit(1);
            case Database.Result.Ok<List<FromDb<Person>>> v -> peopleList.addAll(v.value());
        }
        table.setItems(FXCollections.observableArrayList(peopleList));
    }

    private void spawnNewPersonDialog() {
        spawnTextInputDialog("Add Person", "", "Add", this::createPerson);
    }

    private void spawnTextInputDialog(String title, String defaultValue, String accept, Consumer<String> callback) {
        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.setResizable(false);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(root.getScene().getWindow());
        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));
        Label label = new Label("Enter the name of the person");
        TextField textInput = new TextField(defaultValue);
        HBox hBox = new HBox();
        Button acceptButton = new Button(accept);
        acceptButton.setDefaultButton(true);
        acceptButton.setOnAction(_ -> {
            callback.accept(textInput.getText());
            dialog.close();
        });
        acceptButton.setDisable(true);
        textInput.setOnKeyTyped(_ -> acceptButton.setDisable(textInput.getText().isEmpty()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(_ -> dialog.close());
        hBox.getChildren().addAll(acceptButton, spacer, cancelButton);
        dialogVbox.getChildren().addAll(label, textInput, hBox);
        Scene dialogScene = new Scene(dialogVbox);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void createPerson(String name) {
        Database.getInstance().insertPerson(new Person(name));
        updateTable();
    }

    private void deleteSelectedPerson() {
        FromDb<Person> selectedPerson = table.getSelectionModel().getSelectedItem();
        Database.getInstance().deletePerson(selectedPerson.id());
        updateTable();
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void spawnEditPersonDialog() {
        FromDb<Person> selectedPerson = table.getSelectionModel().getSelectedItem();
        spawnTextInputDialog("Edit Person", selectedPerson.model().getName(), "Rename", s -> updateName(selectedPerson, s));
    }

    private void updateName(FromDb<Person> person, String newName) {
        if (newName.equals(person.model().getName())) {
            return;
        }
        person.model().setName(newName);
        Database.getInstance().updatePerson(person);
        updateTable();
    }
}
