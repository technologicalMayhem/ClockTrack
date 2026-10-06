package net.techmayhem.clocktrack.ui.player;

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
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;

public class PlayerOverview extends EntityOverview {
    private final HBox root;
    private final TableView<PlayerSummary> table;

    public PlayerOverview(ScreenHost screenHost) {
        super(screenHost);
        table = new TableView<>();
        root = new HBox();
        buildUi();
        updateTable();
    }

    @Override
    protected String getSectionName() {
        return "Players";
    }

    private void buildUi() {
        Button createButton = new Button("Add player");
        Button editButton = new Button("Edit player");
        Button deleteButton = new Button("Delete player");

        createButton.setOnAction(_ -> createNewPlayer());
        editButton.setOnAction(_ -> editPlayer());
        deleteButton.setOnAction(_ -> deleteSelectedPlayer());
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        List<Button> buttons = List.of(createButton, editButton, deleteButton);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        VBox buttonColumn = new VBox();
        buttonColumn.setSpacing(Layout.SPACING);
        buttonColumn.getChildren().addAll(buttons);

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>("Name", PlayerSummary::name),
                new ColumnDef<>("First game", PlayerSummary::firstGame),
                new ColumnDef<>("Latest game", PlayerSummary::lastGame),
                new ColumnDef<>("Games played", PlayerSummary::gamesPlayed),
                new ColumnDef<>("Games storytold", PlayerSummary::gamesStorytold));
        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(Layout.PADDING));
        root.setSpacing(Layout.SPACING);
        root.getChildren().addAll(table, buttonColumn);
    }

    @Override
    protected void onShow() {
        updateTable();
    }

    private void updateTable() {
        List<PlayerSummary> allPlayers = Database.getInstance().getPlayerSummaries();
        table.setItems(FXCollections.observableArrayList(allPlayers));
    }

    private void createNewPlayer() {
        screenHost.open(new PlayerEditor(null));
    }

    private void editPlayer() {
        PlayerSummary selectedItem = table.getSelectionModel().getSelectedItem();
        FromDb<Player> player = Database.getInstance().getPlayer(selectedItem.id());
        PlayerEditor playerEditor = new PlayerEditor(player);
        screenHost.open(playerEditor);
    }

    private void deleteSelectedPlayer() {
        PlayerSummary selectedPlayer = table.getSelectionModel().getSelectedItem();
        boolean isReferenced = Database.getInstance().isPlayerReferenced(selectedPlayer.id());
        String prompt = isReferenced
                ? selectedPlayer.name()
                        + " cannot be deleted as they show up in one or more sessions. Selecting delete will anonymize them instead. Do you want to proceed?"
                : "Do you really want to delete " + selectedPlayer.name() + "?";
        Dialogs.showConfirmDialog("Confirm deletion", prompt + "\nThis cannot be undone!", "Proceed", "Cancel", () -> {
            Database.getInstance().deletePlayer(selectedPlayer.id());
            updateTable();
        });
    }

    @Override
    protected Parent getView() {
        return root;
    }
}
