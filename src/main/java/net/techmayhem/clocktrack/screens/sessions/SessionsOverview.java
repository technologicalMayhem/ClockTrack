package net.techmayhem.clocktrack.screens.sessions;

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
import net.techmayhem.clocktrack.models.Session;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;
import net.techmayhem.clocktrack.utils.TableColumn;
import net.techmayhem.clocktrack.utils.TableHelper;

import java.util.List;
import java.util.function.Consumer;

public class SessionsOverview extends Overview {
    private final HBox root;
    private final TableView<FromDb<Session>> table;

    public SessionsOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
        table = new TableView<>();
        root = new HBox();
        buildUi();
        updateTable();
    }

    private void buildUi() {
        Button createButton = new Button("New session");
        Button viewButton = new Button("View session");
        Button editButton = new Button("Edit session");
        Button deleteButton = new Button("Delete session");

        createButton.setOnAction(_ -> createNewSession());
        viewButton.setOnAction(_ -> viewSession());
        editButton.setOnAction(_ -> editSession());
        deleteButton.setOnAction(_ -> deleteSession());
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

        TableHelper.buildTableColumns(table,
                new TableColumn<>("Id", session -> String.valueOf(session.id())),
                new TableColumn<>("Date", session -> session.model().date().toString()),
                new TableColumn<>("Storyteller", session -> Database.getInstance().getPerson(session.model().storyteller()).model().name()),
                new TableColumn<>("Winner", session -> session.model().goodWon() ? "Good" : "Evil"),
                new TableColumn<>("Script", session -> Database.getInstance().getScript(session.model().script()).model().name()));

        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(10));
        root.setSpacing(5);
        root.getChildren().addAll(table, buttonColumn);
    }

    @Override
    protected String getSectionName() {
        return "Sessions";
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
        List<FromDb<Session>> allSessions = Database.getInstance().getAllSessions();
        table.setItems(FXCollections.observableArrayList(allSessions));
    }

    private void createNewSession() {
        createTabCallback.accept(new SessionEdit(null));
    }

    private void viewSession() {
        FromDb<Session> selectedItem = table.getSelectionModel().getSelectedItem();
        SessionView sessionView = new SessionView(selectedItem.model());
        createTabCallback.accept(sessionView);
    }

    private void editSession() {
        FromDb<Session> selectedItem = table.getSelectionModel().getSelectedItem();
        SessionEdit sessionEdit = new SessionEdit(selectedItem);
        createTabCallback.accept(sessionEdit);
    }

    private void deleteSession() {
        FromDb<Session> selectedItem = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog("Delete session", "Do you really want to delete the session from" + selectedItem.model().date() + "?", "Delete session", "Cancel", () -> {
            Database.getInstance().deleteSession(selectedItem.id());
            updateTable();
        });
    }
}
