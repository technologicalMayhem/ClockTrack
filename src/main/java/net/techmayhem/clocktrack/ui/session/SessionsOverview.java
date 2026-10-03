package net.techmayhem.clocktrack.ui.session;

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
import net.techmayhem.clocktrack.model.PersonSession;
import net.techmayhem.clocktrack.model.Session;
import net.techmayhem.clocktrack.projections.SessionSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

public class SessionsOverview extends EntityOverview {
    private final HBox root;
    private final TableView<SessionSummary> table;

    public SessionsOverview(ScreenHost screenHost) {
        super(screenHost);
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

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>("Date", SessionSummary::date),
                new ColumnDef<>("Script", SessionSummary::scriptName),
                new ColumnDef<>("Storyteller", SessionSummary::storyteller),
                new ColumnDef<>("Winner", SessionSummary::goodWon, goodWon -> goodWon ? "Good" : "Evil"),
                new ColumnDef<>("Player count", SessionSummary::playerCount));

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
        List<SessionSummary> allSessions = Database.getInstance().getSessionSummaries();
        table.setItems(FXCollections.observableArrayList(allSessions));
    }

    private void createNewSession() {
        screenHost.open(new SessionEditor(null, null));
    }

    private void viewSession() {
        SessionSummary selectedItem = table.getSelectionModel().getSelectedItem();
        FromDb<Session> session = Database.getInstance().getSession(selectedItem.id());
        SessionView sessionView = new SessionView(session.model());
        screenHost.open(sessionView);
    }

    private void editSession() {
        SessionSummary selectedItem = table.getSelectionModel().getSelectedItem();
        FromDb<Session> session = Database.getInstance().getSession(selectedItem.id());
        List<FromDb<PersonSession>> personSessions =
                Database.getInstance().getAllPersonSessionsForSession(session.id());
        SessionEditor sessionEditor = new SessionEditor(session, personSessions);
        screenHost.open(sessionEditor);
    }

    private void deleteSession() {
        SessionSummary selectedItem = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Delete session",
                "Do you really want to delete the session from" + selectedItem.date() + "?",
                "Delete session",
                "Cancel",
                () -> {
                    Database.getInstance().deleteSession(selectedItem.id());
                    updateTable();
                });
    }
}
