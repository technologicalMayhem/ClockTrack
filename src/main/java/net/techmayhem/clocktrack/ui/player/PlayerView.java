package net.techmayhem.clocktrack.ui.player;

import java.util.*;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.projections.PlayerStatistics;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.PhaseFormat;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.Styles;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.DonutChart;
import net.techmayhem.clocktrack.ui.component.StatBlocks;
import net.techmayhem.clocktrack.ui.component.StatBlocks.StatBlock;
import net.techmayhem.clocktrack.ui.component.TableHelper;

/// A screen that shows the statistics of a player. Currently, it is a placeholder that only shows the statistics as
/// text.
public class PlayerView extends Screen {
    private final ScrollPane root;
    private final String name;

    private static final int MIN_GAMES_FOR_RANKING = 3;
    private static final int RANKING_SIZE = 5;
    private static final int MIN_DISPLAYED_PHASE = 10;

    private static final double SCROLL_SPEED = 2;
    private static final double DONUT_SIZE = 300;

    PlayerView(PlayerSummary summary, PlayerStatistics statistics) {
        root = new ScrollPane();
        root.setFitToWidth(true);

        VBox content = new VBox(Layout.SPACING);
        content.setPadding(new Insets(Layout.PADDING));
        root.setContent(content);

        root.addEventFilter(ScrollEvent.SCROLL, event -> {
            double scrollable = root.getContent().getBoundsInLocal().getHeight()
                    - root.getViewportBounds().getHeight();
            if (scrollable <= 0) return;
            double delta = event.getDeltaY() * SCROLL_SPEED / scrollable;
            root.setVvalue(Math.clamp(root.getVvalue() - delta, 0, 1));
            event.consume();
        });

        name = "View: " + statistics.name();

        if (statistics.gamesPlayed() == 0) {
            content.getChildren().addAll(new VBox(Layout.SPACING, header(summary), onlyStorytoldPane(statistics)));
            return;
        }

        content.getChildren()
                .addAll(new VBox(
                        Layout.SPACING,
                        header(summary),
                        statBlocks(statistics),
                        section("Wins and Losses", winLossPane(statistics)),
                        section("Characters", characterPane(statistics)),
                        survivalStreakPane(statistics),
                        section("Scripts", scriptsPane(statistics)),
                        section("Allies", alliesPane(statistics))));
    }

    private VBox onlyStorytoldPane(PlayerStatistics statistics) {
        return section(
                "Only storytold",
                new Label(statistics.name() + " has only been storyteller so far. They have storytold "
                        + statistics.gamesStorytold() + " games."));
    }

    private VBox header(PlayerSummary summary) {
        Text name = new Text(summary.name());
        name.getStyleClass().add(Styles.STAT_BLOCK_VALUE);
        return new VBox(
                Layout.SPACING,
                name,
                new Text("First game " + summary.firstGame() + " · Last game " + summary.lastGame()));
    }

    private StatBlocks statBlocks(PlayerStatistics statistics) {
        StatBlock gamesPlayed = new StatBlock(Integer.toString(statistics.gamesPlayed()), "Games played");
        StatBlock gamesStorytold = new StatBlock(Integer.toString(statistics.gamesStorytold()), "Games storytold");
        StatBlock winrate = new StatBlock(
                rate(statistics.wonGood() + statistics.wonEvil(), statistics.gamesPlayed()),
                "Winrate\n" + (statistics.wonGood() + statistics.wonEvil()) + " of " + statistics.gamesPlayed());
        StatBlock winrateGood = new StatBlock(
                rate(statistics.wonGood(), (statistics.wonGood() + statistics.lossGood())),
                "Winrate good\n" + statistics.wonGood() + " of " + (statistics.wonGood() + statistics.lossGood()));
        StatBlock winrateEvil = new StatBlock(
                rate(statistics.wonEvil(), statistics.wonEvil() + statistics.lossEvil()),
                "Winrate evil\n" + statistics.wonEvil() + " of " + (statistics.wonEvil() + statistics.lossEvil()));
        StatBlock survivalRate = new StatBlock(
                rate(statistics.survived(), statistics.gamesPlayed()),
                "Survival rate\n" + statistics.survived() + " of " + statistics.gamesPlayed());

        return new StatBlocks(gamesPlayed, gamesStorytold, winrate, winrateGood, winrateEvil, survivalRate);
    }

    private static VBox characterPane(PlayerStatistics statistics) {
        Roles roles = Roles.getInstance();

        List<Ranked> characters = statistics.charactersPlayed().entrySet().stream()
                .map(entry -> {
                    Role role = roles.get(entry.getKey());
                    return new Ranked(role == null ? entry.getKey() : role.name(), entry.getValue());
                })
                .toList();
        Split split = split(characters);
        GridPane rankings = sideBySide(
                rankingBox(
                        "Best characters",
                        "No character with " + MIN_GAMES_FOR_RANKING + " games and more than half of them won.",
                        split.best(),
                        true),
                rankingBox(
                        "Worst characters",
                        "No character with " + MIN_GAMES_FOR_RANKING + " games and less than half of them won.",
                        split.worst(),
                        false));

        HBox tableHeader = new HBox(Layout.SPACING);
        Label allPlayedCharacters = new Label("All played characters");
        allPlayedCharacters.getStyleClass().add(Styles.CHARACTER_SECTION_HEADER);
        tableHeader.getChildren().addAll(allPlayedCharacters);

        TableView<Map.Entry<String, Rate>> characterStatsTable = new TableView<>();
        TableHelper.buildTableColumns(
                characterStatsTable,
                new ColumnDef<>("Character", entry -> {
                    Role role = roles.get(entry.getKey());
                    return role == null ? entry.getKey() : role.name();
                }),
                new ColumnDef<>(
                        "Type",
                        entry -> {
                            Role role = roles.get(entry.getKey());
                            return role == null ? null : role.team();
                        },
                        I18n::teamName),
                new ColumnDef<>("Games", entry -> entry.getValue().games()),
                new ColumnDef<>("Wins", entry -> entry.getValue().wins()),
                new ColumnDef<>(
                        "Win Percentage", entry -> winRate(entry.getValue()), rate -> I18n.formatPercentage(rate, 0)));
        characterStatsTable.getItems().addAll(sortedByGames(statistics));
        characterStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        fitToRows(characterStatsTable);

        return new VBox(Layout.SPACING, rankings, tableHeader, characterStatsTable);
    }

    /// Splits the rows with enough games into the best and the worst, each at most [#RANKING_SIZE] long. The best
    /// won more than half of their games, the worst less than half. Rows at exactly half are in neither.
    private static Split split(List<Ranked> rows) {
        List<Ranked> ranked = rows.stream()
                .filter(row -> row.rate().games() >= MIN_GAMES_FOR_RANKING)
                .toList();
        List<Ranked> best = ranked.stream()
                .filter(row -> winRate(row.rate()) > 0.5)
                .sorted(byWinRate(true))
                .limit(RANKING_SIZE)
                .toList();
        List<Ranked> worst = ranked.stream()
                .filter(row -> winRate(row.rate()) < 0.5)
                .sorted(byWinRate(false))
                .limit(RANKING_SIZE)
                .toList();
        return new Split(best, worst);
    }

    /// Builds a titled list of bars that show the share of games won. `best` picks the style of the bars.
    private static VBox rankingBox(String title, String emptyText, List<Ranked> rows, boolean best) {
        VBox container = new VBox(Layout.SPACING, header(title));
        if (rows.isEmpty()) {
            container.getChildren().add(new Label(emptyText));
            return container;
        }

        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        ColumnConstraints nameConstraint = new ColumnConstraints();
        ColumnConstraints barConstraint = new ColumnConstraints();
        ColumnConstraints statConstraint = new ColumnConstraints();
        nameConstraint.setHgrow(Priority.ALWAYS);
        barConstraint.setPercentWidth(50);
        statConstraint.setHgrow(Priority.SOMETIMES);
        grid.getColumnConstraints().addAll(nameConstraint, barConstraint, statConstraint);
        container.getChildren().add(grid);

        int rowIndex = 0;
        for (Ranked row : rows) {
            Rate rate = row.rate();
            double share = winRate(rate);

            ProgressBar bar = new ProgressBar(share);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add(best ? Styles.CHARACTER_SECTION_BOX_WIN : Styles.CHARACTER_SECTION_BOX_LOSS);

            Label rateLabel = new Label(rate.wins() + "/" + rate.games() + " - " + I18n.formatPercentage(share, 0));

            grid.addRow(rowIndex, new Label(row.name()), bar, rateLabel);
            rowIndex++;
        }

        return container;
    }

    /// Orders by win rate, highest first with `top`, then by games, then by name, so ties keep the same order.
    private static Comparator<Ranked> byWinRate(boolean top) {
        Comparator<Ranked> byRate = Comparator.comparingDouble(row -> winRate(row.rate()));
        Comparator<Ranked> byGames = Comparator.comparingInt(row -> row.rate().games());
        return (top ? byRate.reversed() : byRate)
                .thenComparing(byGames.reversed())
                .thenComparing(Ranked::name);
    }

    /// Places two nodes side by side, each taking half of the width.
    private static GridPane sideBySide(Node left, Node right) {
        return sideBySide(left, right, Layout.PADDING, 50);
    }

    /// Places two nodes side by side. The left node takes the given percentage of the width, the right one the rest.
    private static GridPane sideBySide(Node left, Node right, double spacing, double leftPercent) {
        GridPane grid = new GridPane(spacing, 0);
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(leftPercent);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(100 - leftPercent);
        grid.getColumnConstraints().addAll(leftColumn, rightColumn);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    /// A row of a ranking: what is ranked, and its games and wins.
    ///
    /// @param name The display name
    /// @param rate The games and how many of them were won
    private record Ranked(String name, Rate rate) {}

    /// The best and the worst rows of a ranking. No row is in both.
    ///
    /// @param best The best rows, best first
    /// @param worst The worst rows, worst first
    private record Split(List<Ranked> best, List<Ranked> worst) {}

    private static double winRate(Rate rate) {
        return rate.games() == 0 ? 0 : (double) rate.wins() / rate.games();
    }

    /// Returns the played characters, most games first. Ties are ordered by role id, so the order is stable.
    private static List<Map.Entry<String, Rate>> sortedByGames(PlayerStatistics statistics) {
        return statistics.charactersPlayed().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Rate> entry) ->
                                entry.getValue().games())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();
    }

    private static GridPane winLossPane(PlayerStatistics statistics) {
        return sideBySide(winLossDetails(statistics), winLossDonut(statistics), Layout.PADDING, 55);
    }

    /// Builds the left side of the wins and losses section: a legend with numbers, the win rates and recent games.
    private static VBox winLossDetails(PlayerStatistics statistics) {
        int total = statistics.gamesPlayed();
        int goodGames = statistics.wonGood() + statistics.lossGood();
        int evilGames = statistics.wonEvil() + statistics.lossEvil();

        GridPane legend = new GridPane(Layout.PADDING, Layout.SPACING);
        ColumnConstraints numberColumn = new ColumnConstraints();
        numberColumn.setHalignment(HPos.RIGHT);
        numberColumn.setMinWidth(40);
        legend.getColumnConstraints()
                .addAll(new ColumnConstraints(), new ColumnConstraints(), numberColumn, numberColumn);
        legendRow(legend, 0, Styles.WIN_LOSS_GOOD_WIN, "Good wins", statistics.wonGood(), total);
        legendRow(legend, 1, Styles.WIN_LOSS_GOOD_LOSS, "Good losses", statistics.lossGood(), total);
        legendRow(legend, 2, Styles.WIN_LOSS_EVIL_LOSS, "Evil losses", statistics.lossEvil(), total);
        legendRow(legend, 3, Styles.WIN_LOSS_EVIL_WIN, "Evil wins", statistics.wonEvil(), total);

        HBox rates = new HBox(
                Layout.PADDING * 4,
                smallStat(
                        "Winrate good",
                        rate(statistics.wonGood(), goodGames),
                        statistics.wonGood() + " of " + goodGames),
                smallStat(
                        "Winrate evil",
                        rate(statistics.wonEvil(), evilGames),
                        statistics.wonEvil() + " of " + evilGames),
                smallStat("Played evil", rate(evilGames, total), evilGames + " of " + total));

        return new VBox(
                Layout.PADDING * 2,
                new VBox(Layout.SPACING, header("Results"), legend),
                new Separator(),
                rates,
                new Separator(),
                recentGames(statistics.recentGames()));
    }

    private static void legendRow(GridPane legend, int row, String colorClass, String label, int count, int total) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll(Styles.LEGEND_SWATCH, colorClass);
        Label share = new Label(rate(count, total));
        share.getStyleClass().add(Styles.STAT_CAPTION);
        legend.addRow(row, swatch, new Label(label), new Label(String.valueOf(count)), share);
    }

    private static VBox smallStat(String caption, String value, String detail) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add(Styles.STAT_CAPTION);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(Styles.STAT_VALUE_SMALL);
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add(Styles.STAT_CAPTION);
        return new VBox(captionLabel, valueLabel, detailLabel);
    }

    /// Builds a strip with one circle per recent game, newest first, marked W for won and L for lost.
    private static VBox recentGames(List<PlayerStatistics.RecentGame> games) {
        HBox strip = new HBox(Layout.PADDING * 1.5);
        for (PlayerStatistics.RecentGame game : games) {
            Label result = new Label(game.won() ? "W" : "L");
            result.getStyleClass().addAll(Styles.RECENT_GAME, game.won() ? Styles.WON : Styles.LOST);

            Label date = new Label(I18n.formatDate(game.date()));
            date.getStyleClass().add(Styles.STAT_CAPTION);

            VBox entry = new VBox(Layout.SPACING, result, date);
            entry.setAlignment(Pos.CENTER);
            Tooltip.install(
                    entry,
                    new Tooltip(I18n.formatDate(game.date()) + ": " + (game.won() ? "won" : "lost") + " as "
                            + (game.good() ? "good" : "evil")));
            strip.getChildren().add(entry);
        }
        return new VBox(Layout.SPACING, header("Last " + games.size() + " games, newest first"), strip);
    }

    /// Builds the donut of wins and losses by alignment, with the number of games in its center.
    private static StackPane winLossDonut(PlayerStatistics statistics) {
        int total = statistics.gamesPlayed();
        List<PieChart.Data> data = List.of(
                new PieChart.Data("Good wins", statistics.wonGood()),
                new PieChart.Data("Good losses", statistics.lossGood()),
                new PieChart.Data("Evil losses", statistics.lossEvil()),
                new PieChart.Data("Evil wins", statistics.wonEvil()));

        DonutChart donutChart = new DonutChart();
        donutChart.getStyleClass().add(Styles.WIN_LOSS_PIE);
        donutChart.setClockwise(false);
        donutChart.setStartAngle(90);
        donutChart.setLegendVisible(false);
        donutChart.setLabelsVisible(false);
        donutChart.setPrefSize(DONUT_SIZE, DONUT_SIZE);
        donutChart.setMaxSize(DONUT_SIZE, DONUT_SIZE);
        donutChart.getData().addAll(data);
        for (PieChart.Data slice : data) {
            Tooltip.install(
                    slice.getNode(),
                    new Tooltip(slice.getName() + ": " + (int) slice.getPieValue() + " ("
                            + rate((int) slice.getPieValue(), total) + ")"));
        }

        Label totalValue = new Label(String.valueOf(total));
        totalValue.getStyleClass().add(Styles.DONUT_TOTAL);
        Label totalCaption = new Label("games");
        totalCaption.getStyleClass().add(Styles.STAT_CAPTION);
        VBox center = new VBox(totalValue, totalCaption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);

        return new StackPane(donutChart, center);
    }

    private static Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.CHARACTER_SECTION_HEADER);
        return label;
    }

    private static GridPane survivalStreakPane(PlayerStatistics statistics) {
        return sideBySide(
                section("Survival", buildSurvival(statistics)),
                section("Streaks", buildStreaks(statistics)),
                Layout.SPACING,
                60);
    }

    private static VBox buildSurvival(PlayerStatistics statistics) {
        int played = statistics.gamesPlayed();
        String share = played == 0 ? "–" : I18n.formatPercentage((double) statistics.survived() / played, 0);
        Label summary = new Label("Survived " + statistics.survived() + " of " + played + " games (" + share + ")");

        Map<Integer, Integer> deaths = new HashMap<>(statistics.deathsByPhase());
        if (deaths.isEmpty()) {
            return new VBox(Layout.SPACING, summary, new Label("No deaths recorded."));
        }

        int lastPhase = Math.max(Collections.max(deaths.keySet()), MIN_DISPLAYED_PHASE);
        int mostDeaths = Collections.max(deaths.values());
        double tickUnit = Math.max(1, Math.ceil(mostDeaths / 10.0)); // whole steps, ~10 ticks at most

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, Math.ceil(Math.max(mostDeaths, 5) / tickUnit) * tickUnit, tickUnit);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel("Deaths");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(240);
        chart.setCategoryGap(8);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int phase = PhaseFormat.FIRST_PHASE; phase <= lastPhase; phase++) {
            series.getData().add(new XYChart.Data<>(PhaseFormat.format(phase), deaths.getOrDefault(phase, 0)));
        }
        chart.getData().add(series);

        for (XYChart.Data<String, Number> bar : series.getData()) {
            Tooltip.install(bar.getNode(), new Tooltip(bar.getXValue() + ": " + bar.getYValue()));
        }

        return new VBox(Layout.SPACING, summary, chart);
    }

    private static VBox buildStreaks(PlayerStatistics statistics) {
        StatBlock longestWinStreak = new StatBlock(String.valueOf(statistics.longestWinStreak()), "Longest Win Streak");
        StatBlock longestLossStreak =
                new StatBlock(String.valueOf(statistics.longestLossStreak()), "Longest Loss Streak");
        StatBlock longestGoodStreak =
                new StatBlock(String.valueOf(statistics.longestGoodStreak()), "Longest Good Streak");
        StatBlock longestEvilStreak =
                new StatBlock(String.valueOf(statistics.longestEvilStreak()), "Longest Evil Streak");

        return new VBox(
                Layout.SPACING,
                new StatBlocks(longestWinStreak, longestLossStreak),
                new StatBlocks(longestGoodStreak, longestEvilStreak));
    }

    private static Node alliesPane(PlayerStatistics statistics) {
        GridPane rankings = sideBySide(
                allyRanking("Best allies as good", statistics.goodTeammates()),
                allyRanking("Best allies as evil", statistics.evilTeammates()));
        Label note = new Label("Only allies with at least " + MIN_GAMES_FOR_RANKING + " games on the same team.");
        return new VBox(Layout.SPACING, rankings, note);
    }

    private static VBox allyRanking(String title, Map<Integer, PlayerStatistics.Teammate> teammates) {
        List<Ranked> rows = teammates.values().stream()
                .map(teammate -> new Ranked(teammate.name(), teammate.rate()))
                .filter(row -> row.rate().games() >= MIN_GAMES_FOR_RANKING)
                .sorted(byWinRate(true))
                .limit(RANKING_SIZE)
                .toList();
        return rankingBox(title, "No ally with " + MIN_GAMES_FOR_RANKING + " games together yet.", rows, true);
    }

    private Node scriptsPane(PlayerStatistics statistics) {
        GridPane grid = new GridPane();
        grid.setHgap(Layout.PADDING);
        grid.setVgap(Layout.SPACING);
        ColumnConstraints outer = new ColumnConstraints();
        outer.setHgrow(Priority.SOMETIMES);
        ColumnConstraints inner = new ColumnConstraints();
        inner.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(outer, inner, outer);

        List<PlayerStatistics.ScriptPlays> entries = statistics.scriptPlays().values().stream()
                .sorted(Comparator.comparingInt(PlayerStatistics.ScriptPlays::games)
                        .reversed())
                .toList();
        int highestPlays = entries.stream()
                .mapToInt(PlayerStatistics.ScriptPlays::games)
                .max()
                .orElse(0);
        int gridRow = 0;
        for (PlayerStatistics.ScriptPlays entry : entries) {
            ProgressBar progressBar = new ProgressBar((double) entry.games() / highestPlays);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            grid.addRow(gridRow, new Label(entry.name()), progressBar, new Label(String.valueOf(entry.games())));
            gridRow++;
        }

        return grid;
    }

    private static void colorBars(XYChart.Data<Number, String> d) {
        if (d.getExtraValue() instanceof Role role)
            d.getNode().getStyleClass().addAll(Styles.TEAM_BAR, Styles.teamClass(role.team()));
    }

    private static VBox section(String title, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.STAT_SECTION_TITLE);
        heading.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(content);
        body.getStyleClass().add(Styles.STAT_SECTION_CONTENT);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox section = new VBox(heading, body);
        section.getStyleClass().add(Styles.STAT_SECTION);
        section.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return section;
    }

    /// Makes the table exactly as tall as its rows, so it never scrolls on its own.
    private static void fitToRows(TableView<?> table) {
        table.setFixedCellSize(26);
        table.prefHeightProperty().bind(Bindings.size(table.getItems()).add(1.1).multiply(table.getFixedCellSize()));
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);
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
