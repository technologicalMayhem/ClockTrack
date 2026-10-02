package net.techmayhem.clocktrack.ui.script;

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
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

public class ScriptsOverview extends EntityOverview {
    private final HBox root;
    private final TableView<FromDb<Script>> table;

    public ScriptsOverview(ScreenHost screenHost) {
        super(screenHost);
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
        screenHost.open(new ScriptEditor(null));
    }

    private void viewScript() {
        FromDb<Script> selectedItem = table.getSelectionModel().getSelectedItem();
        ScriptView sessionView = new ScriptView(selectedItem.model());
        screenHost.open(sessionView);
    }

    private void editScript() {
        FromDb<Script> selectedItem = table.getSelectionModel().getSelectedItem();
        ScriptEditor sessionEdit = new ScriptEditor(selectedItem);
        screenHost.open(sessionEdit);
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
