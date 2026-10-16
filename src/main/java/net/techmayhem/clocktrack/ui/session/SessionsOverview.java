package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

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
import net.techmayhem.clocktrack.model.PlayerSession;
import net.techmayhem.clocktrack.model.Session;
import net.techmayhem.clocktrack.projections.SessionSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

/// The overview of all sessions, newest first.
///
/// From here, sessions are added, viewed, edited, and deleted.
public class SessionsOverview extends EntityOverview {
    private final HBox root;
    private final TableView<SessionSummary> table;

    /// Creates the overview and loads the sessions from the database.
    ///
    /// @param screenHost The host for the screens opened from the overview
    public SessionsOverview(ScreenHost screenHost) {
        super(screenHost);
        table = new TableView<>();
        root = new HBox();
        buildUi();
        updateTable();
    }

    private void buildUi() {
        Button createButton = new Button(localize("sessionsOverview.new"));
        Button viewButton = new Button(localize("sessionsOverview.view"));
        Button editButton = new Button(localize("sessionsOverview.edit"));
        Button deleteButton = new Button(localize("sessionsOverview.delete"));

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
        buttonColumn.setSpacing(Layout.SPACING);
        buttonColumn.getChildren().addAll(buttons);

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>(localize("sessionsOverview.summary.date"), SessionSummary::date, I18n::formatDate),
                new ColumnDef<>(localize("sessionsOverview.summary.script"), SessionSummary::scriptName),
                new ColumnDef<>(
                        localize("sessionsOverview.summary.storyteller"),
                        s -> s.storyteller() == null ? localize("generic.unknown") : s.storyteller()),
                new ColumnDef<>(
                        localize("sessionsOverview.summary.winner"),
                        SessionSummary::goodWon,
                        goodWon -> goodWon ? localize("generic.good") : localize("generic.evil")),
                new ColumnDef<>(localize("sessionsOverview.summary.playerCount"), SessionSummary::playerCount));

        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(Layout.PADDING));
        root.setSpacing(Layout.SPACING);
        root.getChildren().addAll(table, buttonColumn);
    }

    @Override
    protected String getSectionName() {
        return localize("sessionsOverview.title");
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
        List<FromDb<PlayerSession>> playerSessions =
                Database.getInstance().getAllPlayerSessionsForSession(session.id());
        SessionEditor sessionEditor = new SessionEditor(session, playerSessions);
        screenHost.open(sessionEditor);
    }

    private void deleteSession() {
        SessionSummary selectedItem = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                localize("sessionsOverview.dialog.delete.title"),
                localize("sessionsOverview.dialog.delete.message", selectedItem.date()),
                localize("sessionsOverview.dialog.delete.confirm"),
                localize("generic.cancel"),
                () -> {
                    Database.getInstance().deleteSession(selectedItem.id());
                    updateTable();
                });
    }
}
