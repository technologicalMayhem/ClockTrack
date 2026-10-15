package net.techmayhem.clocktrack.ui.script;

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
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.ui.EntityOverview;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.ScreenHost;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.TableHelper;
import net.techmayhem.clocktrack.ui.dialog.Dialogs;
import org.jspecify.annotations.Nullable;

public class ScriptsOverview extends EntityOverview {
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
        Button createButton = new Button(localize("scriptsOverview.new"));
        Button viewButton = new Button(localize("scriptsOverview.view"));
        Button editButton = new Button(localize("scriptsOverview.edit"));
        Button deleteButton = new Button(localize("scriptsOverview.delete"));

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
                new ColumnDef<>(localize("scriptsOverview.summary.name"), ScriptSummary::name),
                new ColumnDef<>(localize("scriptsOverview.summary.timesPlayed"), ScriptSummary::timesPlayed),
                new ColumnDef<>(
                        localize("scriptsOverview.summary.firstPlayed"), ScriptSummary::firstPlayed, I18n::formatDate),
                new ColumnDef<>(
                        localize("scriptsOverview.summary.lastPlayed"), ScriptSummary::lastPlayed, I18n::formatDate),
                new ColumnDef<>(
                        localize("scriptsOverview.summary.goodWinrate"),
                        ScriptsOverview::calculateWinrate,
                        ScriptsOverview::formatWinrate));

        HBox.setHgrow(table, Priority.ALWAYS);

        root.setPadding(new Insets(Layout.PADDING));
        root.setSpacing(Layout.SPACING);
        root.getChildren().addAll(table, buttonColumn);
    }

    private static @Nullable Double calculateWinrate(ScriptSummary summary) {
        return summary.timesPlayed() == 0 ? null : (double) summary.goodWins() / summary.timesPlayed();
    }

    private static String formatWinrate(@Nullable Double winrate) {
        return winrate == null ? "-" : I18n.formatPercentage(winrate);
    }

    @Override
    protected String getSectionName() {
        return localize("scriptsOverview.title");
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
                localize("scriptsOverview.dialog.delete.title"),
                localize("scriptsOverview.dialog.delete.message", selectedItem.name()),
                localize("scriptsOverview.dialog.delete.confirm"),
                localize("generic.cancel"),
                () -> {
                    Database.getInstance().deleteScript(selectedItem.id());
                    updateTable();
                });
    }
}
