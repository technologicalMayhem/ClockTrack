package net.techmayhem.clocktrack.ui.script;

import java.text.NumberFormat;
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
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import org.jspecify.annotations.Nullable;

public class ScriptsOverview extends EntityOverview {
    private static final NumberFormat PERCENTAGE_FORMATTER = buildPercentageFormatter();
    private static final int WINRATE_FRACTION_DIGITS = 1;
    private final HBox root;
    private final TableView<ScriptSummary> table;

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
        buttonColumn.setSpacing(Layout.SPACING);
        buttonColumn.getChildren().addAll(buttons);

        TableHelper.buildTableColumns(
                table,
                new ColumnDef<>("Name", ScriptSummary::name),
                new ColumnDef<>("Times played", ScriptSummary::timesPlayed),
                new ColumnDef<>("First played", ScriptSummary::firstPlayed),
                new ColumnDef<>("Last played", ScriptSummary::lastPlayed),
                new ColumnDef<>("Good winrate", ScriptsOverview::calculateWinrate, ScriptsOverview::formatWinrate));

        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(Layout.PADDING));
        root.setSpacing(Layout.SPACING);
        root.getChildren().addAll(table, buttonColumn);
    }

    private static @Nullable Double calculateWinrate(ScriptSummary summary) {
        return summary.timesPlayed() == 0 ? null : (double) summary.goodWins() / summary.timesPlayed();
    }

    private static String formatWinrate(@Nullable Double winrate) {
        return winrate == null ? "-" : PERCENTAGE_FORMATTER.format(winrate);
    }

    private static NumberFormat buildPercentageFormatter() {
        NumberFormat formatter = NumberFormat.getPercentInstance();
        formatter.setMaximumFractionDigits(WINRATE_FRACTION_DIGITS);
        formatter.setMinimumFractionDigits(WINRATE_FRACTION_DIGITS);
        return formatter;
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
        List<ScriptSummary> allScripts = Database.getInstance().getScriptSummaries();
        table.setItems(FXCollections.observableArrayList(allScripts));
    }

    private void createNewScript() {
        screenHost.open(new ScriptEditor(null));
    }

    private void viewScript() {
        ScriptSummary selectedItem = table.getSelectionModel().getSelectedItem();
        FromDb<Script> script = Database.getInstance().getScript(selectedItem.id());
        ScriptView sessionView = new ScriptView(script.model());
        screenHost.open(sessionView);
    }

    private void editScript() {
        ScriptSummary selectedItem = table.getSelectionModel().getSelectedItem();
        FromDb<Script> script = Database.getInstance().getScript(selectedItem.id());
        ScriptEditor sessionEdit = new ScriptEditor(script);
        screenHost.open(sessionEdit);
    }

    private void deleteScript() {
        ScriptSummary selectedItem = table.getSelectionModel().getSelectedItem();
        Dialogs.showConfirmDialog(
                "Delete script",
                "Do you really want to delete the script '" + selectedItem.name() + "'?",
                "Delete script",
                "Cancel",
                () -> {
                    Database.getInstance().deleteScript(selectedItem.id());
                    updateTable();
                });
    }
}
