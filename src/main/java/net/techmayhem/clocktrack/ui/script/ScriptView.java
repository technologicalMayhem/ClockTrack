package net.techmayhem.clocktrack.ui.script;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.*;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import net.techmayhem.clocktrack.gamedata.*;
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.projections.ScriptStatistics;
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.StatisticsView;
import net.techmayhem.clocktrack.ui.Styles;
import net.techmayhem.clocktrack.ui.component.StatBlocks;
import net.techmayhem.clocktrack.ui.component.StatBlocks.StatBlock;

/// A screen that shows the statistics of a script: its results by player count, characters, demons, and players.
public class ScriptView extends StatisticsView {
    private static final int PLAYER_RANKING_SIZE = 10;
    private static final double CHART_HEIGHT = 400;

    ScriptView(Script script, ScriptSummary summary, ScriptStatistics statistics, List<PlayerSummary> players) {
        super(localize("statistics.title", script.name()));

        SessionRoles roles;
        try {
            roles = SessionRoles.of(script, Roles.getInstance());
        } catch (ScriptFormatException e) {
            throw new RuntimeException("Failed to parse script: " + e.getMessage(), e);
        }
        ScriptReport report = ScriptReport.of(statistics, roles, players);

        if (report.results().games() == 0) {
            setContent(
                    summary.name(),
                    summary.firstPlayed(),
                    summary.lastPlayed(),
                    noDataSection(localize("scriptView.noData.neverPlayed", script.name())));
            return;
        }

        setContent(
                summary.name(),
                summary.firstPlayed(),
                summary.lastPlayed(),
                statBlocks(report),
                section(localize("scriptView.section.results"), resultsPane(report)),
                section(localize("scriptView.section.characters"), characterPane(roles, report)),
                section(localize("scriptView.section.demons"), demonsPane(report)),
                section(localize("scriptView.section.players"), playersPane(report)));
    }

    private static StatBlocks statBlocks(ScriptReport report) {
        String commonPlayerCountText = report.resultsByPlayerCount().entrySet().stream()
                .max(Comparator.comparingInt((Map.Entry<Integer, Rate> entry) ->
                                entry.getValue().games())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .orElse(DASH);

        Rate results = report.results();
        int evilWins = results.games() - results.wins();
        StatBlock gamesPlayed =
                new StatBlock(Integer.toString(results.games()), localize("scriptView.stat.gamesPlayed"));
        StatBlock goodWinrate = new StatBlock(
                rate(results.wins(), results.games()),
                localize("scriptView.stat.winrateGood", results.wins(), results.games()));
        StatBlock evilWinrate = new StatBlock(
                rate(evilWins, results.games()), localize("scriptView.stat.winrateEvil", evilWins, results.games()));
        StatBlock commonPlayerCount =
                new StatBlock(commonPlayerCountText, localize("scriptView.stat.commonPlayerCount"));

        return new StatBlocks(gamesPlayed, goodWinrate, evilWinrate, commonPlayerCount);
    }

    private static Node resultsPane(ScriptReport report) {
        SortedMap<Integer, Rate> resultsByPlayerCount = report.resultsByPlayerCount();
        int mostGames = resultsByPlayerCount.values().stream()
                .mapToInt(Rate::games)
                .max()
                .orElse(0);

        List<String> categories = resultsByPlayerCount.keySet().stream()
                .map(ScriptView::playerCountCategory)
                .toList();

        CategoryAxis categoryAxis = new CategoryAxis(FXCollections.observableArrayList(categories));
        categoryAxis.setLabel(localize("scriptView.results.axis.playerCount"));
        NumberAxis numberAxis = countAxis(mostGames, localize("scriptView.results.axis.wins"));
        StackedBarChart<String, Number> chart = new StackedBarChart<>(categoryAxis, numberAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(CHART_HEIGHT);
        chart.getStyleClass().add(Styles.PLAYER_COUNT_CHART);

        XYChart.Series<String, Number> goodWin = new XYChart.Series<>();
        goodWin.setName(localize("scriptView.results.goodWins"));
        XYChart.Series<String, Number> evilWin = new XYChart.Series<>();
        evilWin.setName(localize("scriptView.results.evilWins"));

        for (Map.Entry<Integer, Rate> entry : resultsByPlayerCount.entrySet()) {
            String category = playerCountCategory(entry.getKey());
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
                Tooltip.install(
                        data.getNode(),
                        new Tooltip(localize("scriptView.results.tooltip", series.getName(), data.getYValue())));
            }
        }

        return chart;
    }

    /// Returns the name of the chart category for the given player count, like "7 Players".
    private static String playerCountCategory(int playerCount) {
        return localize("scriptView.results.category", playerCount);
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
            Label teamLabel = subheading(I18n.teamNamePlural(team));
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
                Label countLabel = new Label(localize(
                        "scriptView.characters.appearances", appearances, I18n.formatPercentage(appearanceShare, 0)));
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

        return sideBySide(townsfolk, others);
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

            Label rateLabel = new Label(
                    localize("scriptView.demons.rate", rate.wins(), rate.games(), I18n.formatPercentage(share, 0)));

            grid.addRow(rowIndex, new Label(entry.getKey().name()), bar, rateLabel);
            rowIndex++;
        }

        return new VBox(Layout.SPACING, subheading(localize("scriptView.demons.title")), grid);
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
        return sideBySide(
                gamesBox(
                        localize("scriptView.players.mostPlayed"),
                        report.mostPlayed().stream().limit(PLAYER_RANKING_SIZE).toList()),
                gamesBox(
                        localize("scriptView.players.mostStorytold"),
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
            bar.getStyleClass().add(Styles.GAMES_BAR);

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
}
