package net.techmayhem.clocktrack.ui.script;

import java.util.*;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.gamedata.*;
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.projections.ScriptStatistics;
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.Styles;
import net.techmayhem.clocktrack.ui.component.StatBlocks;
import net.techmayhem.clocktrack.ui.component.StatBlocks.StatBlock;

/// A screen that shows the statistics of a script: its results by player count, characters, demons, and players.
public class ScriptView extends Screen {
    private final ScrollPane root;
    private final String name;

    private static final int PLAYER_RANKING_SIZE = 10;
    private static final double CHART_HEIGHT = 400;

    ScriptView(Script script, ScriptSummary summary, ScriptStatistics statistics, List<PlayerSummary> players) {
        SessionRoles roles;
        try {
            roles = SessionRoles.of(script, Roles.getInstance());
        } catch (ScriptFormatException e) {
            throw new RuntimeException("Failed to parse script: " + e.getMessage(), e);
        }
        ScriptReport report = ScriptReport.of(statistics, roles, players);

        root = new ScrollPane();
        root.setFitToWidth(true);

        VBox content = new VBox(Layout.SPACING);
        content.setPadding(new Insets(Layout.PADDING));
        root.setContent(content);

        root.addEventFilter(ScrollEvent.SCROLL, event -> Layout.acceleratedScroll(root, event));

        name = "View: " + script.name();

        if (report.results().games() == 0) {
            content.getChildren().addAll(new VBox(Layout.SPACING, header(summary), noGamesPane(script)));
            return;
        }

        content.getChildren()
                .addAll(new VBox(
                        Layout.SPACING,
                        header(summary),
                        statBlocks(report),
                        Layout.section("Results by player count", resultsPane(report)),
                        Layout.section("Characters", characterPane(roles, report)),
                        Layout.section("Demons", demonsPane(report)),
                        Layout.section("Players", playersPane(report))));
    }

    private VBox noGamesPane(Script script) {
        return Layout.section("No data to show", new Label(script.name() + " has never been played."));
    }

    private VBox header(ScriptSummary summary) {
        Text name = new Text(summary.name());
        name.getStyleClass().add(Styles.TITLE_TEXT);
        String firstLastGameText;
        if (summary.firstPlayed() != null && summary.lastPlayed() != null) {
            firstLastGameText = "First game " + I18n.formatDate(summary.firstPlayed()) + " · Last game "
                    + I18n.formatDate(summary.lastPlayed());
        } else {
            firstLastGameText = "";
        }
        return new VBox(Layout.SPACING, name, new Text(firstLastGameText));
    }

    private StatBlocks statBlocks(ScriptReport report) {
        String commonPlayerCountText = report.resultsByPlayerCount().entrySet().stream()
                .max(Comparator.comparingInt((Map.Entry<Integer, Rate> entry) ->
                                entry.getValue().games())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .orElse("―");

        Rate results = report.results();
        int evilWins = results.games() - results.wins();
        StatBlock gamesPlayed = new StatBlock(Integer.toString(results.games()), "Games played");
        StatBlock goodWinrate = new StatBlock(
                rate(results.wins(), results.games()), "Winrate good " + results.wins() + " of " + results.games());
        StatBlock evilWinrate =
                new StatBlock(rate(evilWins, results.games()), "Winrate evil " + evilWins + " of " + results.games());
        StatBlock commonPlayerCount = new StatBlock(commonPlayerCountText, "Common player count");

        return new StatBlocks(gamesPlayed, goodWinrate, evilWinrate, commonPlayerCount);
    }

    private static Node resultsPane(ScriptReport report) {
        SortedMap<Integer, Rate> resultsByPlayerCount = report.resultsByPlayerCount();
        int mostGames = resultsByPlayerCount.values().stream()
                .mapToInt(Rate::games)
                .max()
                .orElse(0);
        double tickUnit = Math.max(1, Math.ceil(mostGames / 10.0)); // whole steps, ~10 ticks at most

        List<String> categories = resultsByPlayerCount.keySet().stream()
                .map(playerCount -> playerCount + " Players")
                .toList();

        CategoryAxis categoryAxis = new CategoryAxis(FXCollections.observableArrayList(categories));
        categoryAxis.setLabel("Players");
        NumberAxis numberAxis = new NumberAxis(0, Math.ceil(Math.max(mostGames, 5) / tickUnit) * tickUnit, tickUnit);
        numberAxis.setMinorTickVisible(false);
        numberAxis.setLabel("Wins");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(categoryAxis, numberAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(CHART_HEIGHT);
        chart.getStyleClass().add(Styles.PLAYER_COUNT_RESULT_CHART);

        XYChart.Series<String, Number> goodWin = new XYChart.Series<>();
        goodWin.setName("Good wins");
        XYChart.Series<String, Number> evilWin = new XYChart.Series<>();
        evilWin.setName("Evil wins");

        for (Map.Entry<Integer, Rate> entry : resultsByPlayerCount.entrySet()) {
            String category = entry.getKey() + " Players";
            Rate rate = entry.getValue();
            int evilWins = rate.games() - rate.wins();
            if (rate.wins() > 0) {
                goodWin.getData().add(new XYChart.Data<>(category, rate.wins()));
            }
            if (evilWins > 0) {
                evilWin.getData().add(new XYChart.Data<>(category, evilWins));
            }
        }

        chart.getData().add(goodWin);
        chart.getData().add(evilWin);

        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip.install(data.getNode(), new Tooltip(series.getName() + ": " + data.getYValue()));
            }
        }

        return chart;
    }

    private static Node characterPane(SessionRoles roles, ScriptReport report) {
        Team[] teams = {Team.TOWNSFOLK, Team.OUTSIDER, Team.MINION, Team.DEMON};

        Map<Role, Integer> roleFrequency = report.appearances().stream()
                .collect(Collectors.toMap(ScriptReport.Appearance::role, ScriptReport.Appearance::games));

        int totalGames = report.results().games();

        List<Label> nameLabels = new ArrayList<>();
        List<Label> countLabels = new ArrayList<>();
        ColumnConstraints nameColumn = new ColumnConstraints();
        ColumnConstraints barColumn = new ColumnConstraints();
        barColumn.setHgrow(Priority.ALWAYS);
        barColumn.setFillWidth(true);
        ColumnConstraints countColumn = new ColumnConstraints();

        VBox townsfolk = new VBox(Layout.PADDING * 2);
        VBox others = new VBox(Layout.PADDING * 2);
        for (Team team : teams) {
            VBox teamSection = new VBox(Layout.SPACING);
            teamSection.setMaxWidth(Double.MAX_VALUE);
            HBox teamSectionHeader = new HBox(Layout.SPACING);
            teamSectionHeader.setAlignment(Pos.CENTER_LEFT);
            Label teamLabel = subheading(I18n.teamName(team));
            Rectangle teamColor = new Rectangle();
            teamColor.getStyleClass().addAll(Styles.TEAM_FILL, Styles.teamClass(team));
            DoubleBinding halfLabelHeight = teamLabel.heightProperty().divide(2);
            teamColor.heightProperty().bind(halfLabelHeight);
            teamColor.widthProperty().bind(halfLabelHeight);
            teamSectionHeader.getChildren().addAll(teamColor, teamLabel);

            GridPane roleGrid = new GridPane(Layout.PADDING, Layout.SPACING);
            roleGrid.setMaxWidth(Double.MAX_VALUE);
            roleGrid.getColumnConstraints().addAll(nameColumn, barColumn, countColumn);
            List<Role> teamRoles = roles.roleChoices().stream()
                    .filter(role -> role.team() == team)
                    .toList();
            int rowIndex = 0;
            for (Role role : teamRoles) {
                int appearances = roleFrequency.getOrDefault(role, 0);
                double appearanceShare = share(appearances, totalGames);
                Label nameLabel = new Label(role.name());
                ProgressBar appearanceBar = new ProgressBar(appearanceShare);
                appearanceBar.getStyleClass().addAll(Styles.TEAM_BAR, Styles.teamClass(team));
                appearanceBar.setMaxWidth(Double.MAX_VALUE);
                Label countLabel = new Label(appearances + " - " + I18n.formatPercentage(appearanceShare, 0));
                roleGrid.addRow(rowIndex, nameLabel, appearanceBar, countLabel);
                nameLabels.add(nameLabel);
                countLabels.add(countLabel);
                rowIndex++;
            }

            teamSection.getChildren().addAll(teamSectionHeader, roleGrid);
            if (team == Team.TOWNSFOLK) {
                townsfolk.getChildren().add(teamSection);
            } else {
                others.getChildren().add(teamSection);
            }
        }

        nameColumn.minWidthProperty().bind(widestLabel(nameLabels));
        countColumn.minWidthProperty().bind(widestLabel(countLabels));

        return Layout.sideBySide(townsfolk, others);
    }

    /// Returns the preferred width of the widest label. It is recomputed whenever one of the labels is resized, which
    /// happens once the CSS has been applied.
    private static DoubleBinding widestLabel(List<Label> labels) {
        return Bindings.createDoubleBinding(
                () -> labels.stream()
                        .mapToDouble(label -> label.prefWidth(-1))
                        .max()
                        .orElse(0),
                labels.stream().map(Label::widthProperty).toArray(Observable[]::new));
    }

    private static Node demonsPane(ScriptReport report) {
        GridPane grid = barGrid();

        int rowIndex = 0;
        for (Map.Entry<Role, Rate> entry : sortedByGames(report.resultsByDemon())) {
            Rate rate = entry.getValue();
            double share = winRate(rate);

            ProgressBar bar = new ProgressBar(share);
            bar.getStyleClass().addAll(Styles.TEAM_BAR, Styles.teamClass(Team.DEMON));
            bar.setMaxWidth(Double.MAX_VALUE);

            Label rateLabel = new Label(rate.wins() + " of " + rate.games() + " - " + I18n.formatPercentage(share, 0));

            grid.addRow(rowIndex, new Label(entry.getKey().name()), bar, rateLabel);
            rowIndex++;
        }

        return new VBox(Layout.SPACING, subheading("How often did each demon win?"), grid);
    }

    /// Returns the demons, most games first. Ties are ordered by role id, so the order is stable.
    private static List<Map.Entry<Role, Rate>> sortedByGames(Map<Role, Rate> results) {
        return results.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Role, Rate> entry) ->
                                entry.getValue().games())
                        .reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .toList();
    }

    private static Node playersPane(ScriptReport report) {
        return Layout.sideBySide(
                gamesBox(
                        "Played the most",
                        report.mostPlayed().stream().limit(PLAYER_RANKING_SIZE).toList()),
                gamesBox(
                        "Told the most",
                        report.storytellers().stream()
                                .limit(PLAYER_RANKING_SIZE)
                                .toList()));
    }

    private static Node gamesBox(String title, List<ScriptReport.PlayerGames> players) {
        GridPane grid = barGrid();
        grid.getColumnConstraints().get(2).setHalignment(HPos.RIGHT);

        int most =
                players.stream().mapToInt(ScriptReport.PlayerGames::games).max().orElse(1);
        int rowIndex = 0;
        for (ScriptReport.PlayerGames entry : players) {
            ProgressBar bar = new ProgressBar((double) entry.games() / most);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add(Styles.SCRIPTS_BAR);

            grid.addRow(rowIndex, new Label(entry.player().name()), bar, new Label(String.valueOf(entry.games())));
            rowIndex++;
        }

        VBox box = new VBox(Layout.SPACING, subheading(title), grid);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /// Builds a grid for rows of a name, a bar that takes half of the width, and a stat.
    private static GridPane barGrid() {
        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        ColumnConstraints nameConstraint = new ColumnConstraints();
        ColumnConstraints barConstraint = new ColumnConstraints();
        ColumnConstraints statConstraint = new ColumnConstraints();
        barConstraint.setHgrow(Priority.ALWAYS);
        nameConstraint.setHgrow(Priority.SOMETIMES);
        statConstraint.setHgrow(Priority.SOMETIMES);
        grid.getColumnConstraints().addAll(nameConstraint, barConstraint, statConstraint);
        return grid;
    }

    private static Label subheading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.SUBHEADING);
        return label;
    }

    private static double winRate(Rate rate) {
        return share(rate.wins(), rate.games());
    }

    private static double share(int count, int total) {
        return total == 0 ? 0 : (double) count / total;
    }

    private static String rate(int won, int total) {
        return total == 0 ? "―" : I18n.formatPercentage((double) won / total, 0);
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected String getName() {
        return name;
    }
}
