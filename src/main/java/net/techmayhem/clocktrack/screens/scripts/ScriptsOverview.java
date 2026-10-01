package net.techmayhem.clocktrack.screens.scripts;

import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.techmayhem.clocktrack.Database;
import net.techmayhem.clocktrack.dialog.Dialogs;
import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Script;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;
import net.techmayhem.clocktrack.utils.ColumnDef;
import net.techmayhem.clocktrack.utils.TableHelper;

public class ScriptsOverview extends Overview {
    private final HBox root;
    private final TableView<FromDb<Script>> table;

    public ScriptsOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
        table = new TableView<>();
        root = new HBox();
        buildUi();
        updateTable();
    }

    private void buildUi() {
        Button createButton = new Button("New script");
        Button viewButton = new Button("View script");
        Button editButton = new Button("Edit script");
        Button deleteButton = new Button("Delete script");

        createButton.setOnAction(_ -> createNewScript());
        viewButton.setOnAction(_ -> viewScript());
        editButton.setOnAction(_ -> editScript());
        deleteButton.setOnAction(_ -> deleteScript());
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        viewButton.disableProperty().bind(noSelection);
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        List<Button> buttons = List.of(createButton, viewButton, editButton, deleteButton);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        VBox buttonColumn = new VBox();
        buttonColumn.setSpacing(5);
        buttonColumn.getChildren().addAll(buttons);

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>("Id", session -> String.valueOf(session.id())),
                new ColumnDef<>("Name", session -> session.model().name()));

        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(10));
        root.setSpacing(5);
        root.getChildren().addAll(table, buttonColumn);
    }

    @Override
    protected String getSectionName() {
        return "Scripts";
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected void onShow() {
        updateTable();
    }

    private void updateTable() {
        List<FromDb<Script>> allScripts = Database.getInstance().getAllScripts();
        table.setItems(FXCollections.observableArrayList(allScripts));
    }

    private void createNewScript() {
        createTabCallback.accept(new ScriptEdit(null));
    }

    private void viewScript() {
        FromDb<Script> selectedItem = table.getSelectionModel().getSelectedItem();
        ScriptView sessionView = new ScriptView(selectedItem.model());
        createTabCallback.accept(sessionView);
    }

    private void editScript() {
        FromDb<Script> selectedItem = table.getSelectionModel().getSelectedItem();
        ScriptEdit sessionEdit = new ScriptEdit(selectedItem);
        createTabCallback.accept(sessionEdit);
    }

    private void deleteScript() {
        FromDb<Script> selectedItem = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Delete script",
                "Do you really want to delete the script '"
                        + selectedItem.model().name() + "'?",
                "Delete script",
                "Cancel",
                () -> {
                    Database.getInstance().deleteScript(selectedItem.id());
                    updateTable();
                });
    }
}
