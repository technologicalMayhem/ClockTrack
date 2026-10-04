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

        createButton.setOnAction(_ -> spawnNewPlayerDialog());
        editButton.setOnAction(_ -> spawnEditPlayerDialog());
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

    private void spawnNewPlayerDialog() {
        Dialogs.showTextDialog(
                "Add player", "Enter the name of the player to add", "", "Add", "Cancel", this::createPlayer);
    }

    private void createPlayer(String name) {
        Database.getInstance().insertPlayer(new Player(name));
        updateTable();
    }

    private void deleteSelectedPlayer() {
        PlayerSummary selectedPlayer = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Confirm deletion",
                "Do you really want to delete " + selectedPlayer.name() + "?",
                "Delete",
                "Cancel",
                () -> {
                    Database.getInstance().deletePlayer(selectedPlayer.id());
                    updateTable();
                });
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void spawnEditPlayerDialog() {
        PlayerSummary selectedPlayer = table.getSelectionModel().getSelectedItem();
        Dialogs.showTextDialog(
                "Edit player",
                "Enter the new name of the player",
                selectedPlayer.name(),
                "Rename",
                "Cancel",
                s -> updateName(selectedPlayer, s));
    }

    private void updateName(PlayerSummary player, String newName) {
        if (newName.equals(player.name())) {
            return;
        }
        Player newPlayer = new Player(newName);
        Database.getInstance().updatePlayer(new FromDb<>(player.id(), newPlayer));
        updateTable();
    }
}
