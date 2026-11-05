package net.techmayhem.clocktrack.ui.player;

import java.util.*;
import java.util.stream.IntStream;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.gamedata.Team;
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

    PlayerView(PlayerSummary summary, PlayerStatistics statistics) {
        root = new ScrollPane();
        root.setFitToWidth(true);

        VBox content = new VBox(Layout.SPACING);
        content.setPadding(new Insets(Layout.PADDING));
        root.setContent(content);

        name = "View: " + statistics.name();

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
        GridPane top5 = new GridPane(Layout.PADDING * 3, 0);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        top5.getColumnConstraints().addAll(half, half);
        top5.add(buildTop5(statistics, true), 0, 0);
        top5.add(buildTop5(statistics, false), 1, 0);
        top5.setMaxWidth(Double.MAX_VALUE);

        Roles roles = Roles.getInstance();

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
                        PlayerView::teamName),
                new ColumnDef<>("Games", entry -> entry.getValue().games()),
                new ColumnDef<>("Wins", entry -> entry.getValue().wins()),
                new ColumnDef<>(
                        "Win Percentage", entry -> winRate(entry.getValue()), rate -> I18n.formatPercentage(rate, 0)));
        characterStatsTable.getItems().addAll(sortedByGames(statistics));

        return new VBox(Layout.SPACING, top5, tableHeader, characterStatsTable);
    }

    private static String teamName(Team team) {
        return switch (team) {
            case TOWNSFOLK -> "Townsfolk";
            case OUTSIDER -> "Outsider";
            case MINION -> "Minion";
            case DEMON -> "Demon";
            case TRAVELLER -> "Traveller";
            case FABLED -> "Fabled";
            case LORIC -> "Loric";
        };
    }

    private static VBox buildTop5(PlayerStatistics statistics, boolean top) {
        Comparator<Map.Entry<String, Rate>> comparator = getComparator(top);

        List<Map.Entry<String, Rate>> entries = statistics.charactersPlayed().entrySet().stream()
                .filter(entry -> entry.getValue().games() >= MIN_GAMES_FOR_RANKING)
                .sorted(comparator)
                .limit(RANKING_SIZE)
                .toList();

        VBox container = new VBox(Layout.SPACING);
        Label header = new Label(top ? "Top 5 Characters" : "Bottom 5 Characters");
        header.getStyleClass().add(Styles.CHARACTER_SECTION_HEADER);
        container.getChildren().add(header);
        if (entries.isEmpty()) {
            container.getChildren().add(new Label("No character with " + MIN_GAMES_FOR_RANKING + " games yet."));
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

        Roles roles = Roles.getInstance();
        int rowIndex = 0;
        for (Map.Entry<String, Rate> entry : entries) {
            Role role = roles.get(entry.getKey());
            Label label = new Label(role == null ? entry.getKey() : role.name());

            // The top list shows the share of games won, the bottom list the share of games lost.
            Rate rate = entry.getValue();
            int count = top ? rate.wins() : rate.games() - rate.wins();
            double share = (double) count / rate.games();

            ProgressBar bar = new ProgressBar(share);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add(top ? Styles.CHARACTER_SECTION_BOX_WIN : Styles.CHARACTER_SECTION_BOX_LOSS);

            Label rateLabel = new Label(count + "/" + rate.games() + " - " + I18n.formatPercentage(share, 0));

            grid.addRow(rowIndex, label, bar, rateLabel);
            rowIndex++;
        }

        return container;
    }

    private static Comparator<Map.Entry<String, Rate>> getComparator(boolean top) {
        Comparator<Map.Entry<String, Rate>> byWinRate = Comparator.comparingDouble(entry -> winRate(entry.getValue()));
        Comparator<Map.Entry<String, Rate>> byGames =
                Comparator.comparingInt(entry -> entry.getValue().games());
        Comparator<Map.Entry<String, Rate>> comparator = top ? byWinRate.reversed() : byWinRate;
        // Ties go to the character with more games, then by id, so the order is the same on every open.
        comparator = comparator.thenComparing(byGames.reversed()).thenComparing(Map.Entry::getKey);
        return comparator;
    }

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

    private DonutChart winLossPane(PlayerStatistics statistics) {
        int total = statistics.gamesPlayed();
        List<PieChart.Data> data = List.of(
                slice("Good wins", statistics.wonGood(), total),
                slice("Good loss", statistics.lossGood(), total),
                slice("Evil loss", statistics.lossEvil(), total),
                slice("Evil wins", statistics.wonEvil(), total));
        DonutChart donutChart = new DonutChart();
        donutChart.getStyleClass().add(Styles.WIN_LOSS_PIE);
        donutChart.setClockwise(false);
        donutChart.setStartAngle(90);
        donutChart.getData().addAll(data);
        return donutChart;
    }

    private static GridPane survivalStreakPane(PlayerStatistics statistics) {
        GridPane row = new GridPane();
        row.setHgap(Layout.SPACING);

        ColumnConstraints wide = new ColumnConstraints();
        wide.setPercentWidth(60);
        ColumnConstraints narrow = new ColumnConstraints();
        narrow.setPercentWidth(40);
        row.getColumnConstraints().addAll(wide, narrow);

        VBox survival = section("Survival", buildSurvival(statistics));
        VBox streaks = section("Streaks", buildStreaks(statistics));
        survival.setMaxHeight(Double.MAX_VALUE);
        streaks.setMaxHeight(Double.MAX_VALUE);

        row.add(survival, 0, 0);
        row.add(streaks, 1, 0);

        return row;
    }

    private static VBox buildSurvival(PlayerStatistics statistics) {
        int played = statistics.gamesPlayed();
        String share = played == 0 ? "–" : I18n.formatPercentage((double) statistics.survived() / played, 0);
        Label summary = new Label("Survived " + statistics.survived() + " of " + played + " games (" + share + ")");

        Map<Integer, Integer> deaths = new HashMap<>(statistics.deathsByPhase());
        if (deaths.isEmpty()) {
            return new VBox(Layout.SPACING, summary, new Label("No deaths recorded."));
        }

        IntStream.rangeClosed(1, 10).forEach(phase -> {
            if (!deaths.containsKey(phase)) {
                deaths.put(phase, 0);
            }
        });

        int lastPhase = Collections.max(deaths.keySet());
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

    private Node alliesPane(PlayerStatistics statistics) {
        return placeholder();
    }

    private Node scriptsPane(PlayerStatistics statistics) {
        return placeholder();
    }

    private static PieChart.Data slice(String name, int count, int total) {
        double percent = total == 0 ? 0 : 100.0 * count / total;
        return new PieChart.Data(String.format("%s (%.0f%%)", name, percent), count);
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

    private static Node placeholder() {
        VBox node = new VBox();
        node.setPrefHeight(200);
        return node;
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
