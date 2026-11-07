package net.techmayhem.clocktrack.ui.player;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.gamedata.Team;
import net.techmayhem.clocktrack.projections.PlayerStatistics;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.ui.I18n;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.PhaseFormat;
import net.techmayhem.clocktrack.ui.StatisticsView;
import net.techmayhem.clocktrack.ui.Styles;
import net.techmayhem.clocktrack.ui.component.ColumnDef;
import net.techmayhem.clocktrack.ui.component.DonutChart;
import net.techmayhem.clocktrack.ui.component.StatBlocks;
import net.techmayhem.clocktrack.ui.component.StatBlocks.StatBlock;
import net.techmayhem.clocktrack.ui.component.TableHelper;

/// A screen that shows the statistics of a player: their results, characters, survival, scripts, and allies.
public class PlayerView extends StatisticsView {
    private static final double RATING_THRESHOLD = 0.5;
    private static final int BAR_WIDTH_PERCENT = 50;
    private static final int DONUT_START_ANGLE = 90;
    private static final int SURVIVAL_SECTION_PERCENT = 60;
    private static final int DEFAULT_CHART_HEIGHT = 240;
    private static final int TABLE_ROW_HEIGHT = 26;
    /// Extra pixels for the table's border and insets, so a fitted table never shows a scrollbar.
    private static final int TABLE_BORDER_SLACK = 3;

    private static final int MIN_GAMES_FOR_RANKING = 3;
    private static final int RANKING_SIZE = 5;
    private static final int COLLAPSED_ROWS = 10;
    private static final int MIN_DISPLAYED_PHASE = 10;

    private static final double DONUT_SIZE = 300;

    PlayerView(PlayerSummary summary, PlayerStatistics statistics) {
        super(localize("statistics.title", statistics.name()));

        if (statistics.gamesPlayed() == 0) {
            setContent(summary.name(), summary.firstGame(), summary.lastGame(), onlyStorytoldPane(statistics));
            return;
        }

        setContent(
                summary.name(),
                summary.firstGame(),
                summary.lastGame(),
                statBlocks(statistics),
                section(localize("playerView.section.winLoss"), winLossPane(statistics)),
                section(localize("playerView.section.characters"), characterPane(statistics)),
                survivalStreakPane(statistics),
                section(localize("playerView.section.scripts"), scriptsPane(statistics)),
                section(localize("playerView.section.allies"), alliesPane(statistics)));
    }

    /// A pane for when a player has no data.
    private static VBox onlyStorytoldPane(PlayerStatistics statistics) {
        String s = localize("playerView.noData.neverPlayed", statistics.name());
        if (statistics.gamesStorytold() != 0) {
            s += " " + localize("playerView.noData.storytold", statistics.gamesStorytold());
        }
        return noDataSection(s);
    }

    /// Creates a set of blocks holding neat stats about a player.
    private static StatBlocks statBlocks(PlayerStatistics statistics) {
        Roles roles = Roles.getInstance();
        List<Role> regularRoles = roles.ofTeam(Team.TOWNSFOLK, Team.OUTSIDER, Team.MINION, Team.DEMON);
        long regularRolesPlayed = regularRoles.stream()
                .filter(role -> statistics.charactersPlayed().containsKey(role.id()))
                .count();
        int travellerGames = roles.ofTeam(Team.TRAVELLER).stream()
                .map(role -> statistics.charactersPlayed().get(role.id()))
                .filter(Objects::nonNull)
                .mapToInt(Rate::games)
                .sum();
        int totalRoles = regularRoles.size();

        StatBlock gamesPlayed =
                new StatBlock(Integer.toString(statistics.gamesPlayed()), localize("playerView.stat.gamesPlayed"));
        StatBlock gamesStorytold = new StatBlock(
                Integer.toString(statistics.gamesStorytold()), localize("playerView.stat.gamesStorytold"));
        StatBlock winrate = new StatBlock(
                rate(statistics.wonGood() + statistics.wonEvil(), statistics.gamesPlayed()),
                localize(
                        "playerView.stat.winrate",
                        statistics.wonGood() + statistics.wonEvil(),
                        statistics.gamesPlayed()));
        StatBlock gamesAsATraveller =
                new StatBlock(Integer.toString(travellerGames), localize("playerView.stat.gamesAsTraveller"));
        StatBlock roleCoverage = new StatBlock(
                I18n.formatPercentage((double) regularRolesPlayed / totalRoles, 0),
                localize("playerView.stat.roleCoverage", regularRolesPlayed, totalRoles));
        StatBlock survivalRate = new StatBlock(
                rate(statistics.survived(), statistics.gamesPlayed()),
                localize("playerView.stat.survivalRate", statistics.survived(), statistics.gamesPlayed()));

        return new StatBlocks(gamesPlayed, gamesStorytold, winrate, roleCoverage, gamesAsATraveller, survivalRate);
    }

    /// Shows best and worst characters as well as a table of all characters and the respective stats.
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
                        localize("playerView.characters.best"),
                        localize("playerView.characters.bestEmpty", MIN_GAMES_FOR_RANKING),
                        split.best(),
                        Styles.BEST_BAR),
                rankingBox(
                        localize("playerView.characters.worst"),
                        localize("playerView.characters.worstEmpty", MIN_GAMES_FOR_RANKING),
                        split.worst(),
                        Styles.WORST_BAR));
        Label rankingNote = caption(localize("playerView.characters.note", MIN_GAMES_FOR_RANKING));

        Label sortNote = caption(localize("playerView.characters.sortNote"));

        TableView<Map.Entry<String, Rate>> characterStatsTable = new TableView<>();
        TableHelper.buildTableColumns(
                characterStatsTable,
                new ColumnDef<>(localize("playerView.characters.table.character"), entry -> {
                    Role role = roles.get(entry.getKey());
                    return role == null ? entry.getKey() : role.name();
                }),
                new ColumnDef<>(
                        localize("playerView.characters.table.type"),
                        entry -> {
                            Role role = roles.get(entry.getKey());
                            return role == null ? null : role.team();
                        },
                        I18n::teamName),
                new ColumnDef<>(
                        localize("playerView.characters.table.games"),
                        entry -> entry.getValue().games()),
                new ColumnDef<>(
                        localize("playerView.characters.table.wins"),
                        entry -> entry.getValue().wins()),
                new ColumnDef<>(
                        localize("playerView.characters.table.winPercentage"),
                        entry -> winRate(entry.getValue()),
                        rate -> I18n.formatPercentage(rate, 0)));
        characterStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        fitToRows(characterStatsTable);

        Region spacer = new Region();
        spacer.setPrefHeight(Layout.PADDING);

        List<Map.Entry<String, Rate>> rows = sortedByGames(statistics);
        return new VBox(
                Layout.SPACING,
                rankings,
                rankingNote,
                spacer,
                new VBox(subheading(localize("playerView.characters.all")), sortNote),
                expandableTable(characterStatsTable, rows, localize("playerView.characters.showAll", rows.size())));
    }

    /// Wraps the table with a button that shows all rows or only the first [#COLLAPSED_ROWS].
    ///
    /// @param showAllText The text of the button while only the first rows are shown
    private static <T> VBox expandableTable(TableView<T> table, List<T> rows, String showAllText) {
        BooleanProperty expanded = new SimpleBooleanProperty(false);
        Button toggle = new Button();
        toggle.setVisible(rows.size() > COLLAPSED_ROWS);
        toggle.setManaged(toggle.isVisible());

        Runnable update = () -> {
            boolean showAll = expanded.get() || rows.size() <= COLLAPSED_ROWS;
            table.getItems().setAll(showAll ? rows : rows.subList(0, COLLAPSED_ROWS));
            if (!table.getSortOrder().isEmpty()) {
                table.sort();
            }
            toggle.setText(expanded.get() ? localize("playerView.showFewer") : showAllText);
        };

        toggle.setOnAction(_ -> {
            if (expanded.get()) {
                // The first rows are the first ones in the original order, so a sort would not apply to them.
                table.getSortOrder().clear();
            }
            expanded.set(!expanded.get());
            update.run();
        });
        table.getSortOrder().addListener((ListChangeListener<TableColumn<T, ?>>) _ -> {
            if (!table.getSortOrder().isEmpty()) {
                expanded.set(true);
            }
            update.run();
        });

        update.run();
        return new VBox(Layout.SPACING, table, toggle);
    }

    /// Splits the rows with enough games into the best and the worst, each at most [#RANKING_SIZE] long. The best
    /// won more than half of their games, the worst less than half. Rows at exactly half are in neither.
    private static Split split(List<Ranked> rows) {
        List<Ranked> ranked = rows.stream()
                .filter(row -> row.rate().games() >= MIN_GAMES_FOR_RANKING)
                .toList();
        List<Ranked> best = ranked.stream()
                .filter(row -> winRate(row.rate()) > RATING_THRESHOLD)
                .sorted(byWinRate(true))
                .limit(RANKING_SIZE)
                .toList();
        List<Ranked> worst = ranked.stream()
                .filter(row -> winRate(row.rate()) < RATING_THRESHOLD)
                .sorted(byWinRate(false))
                .limit(RANKING_SIZE)
                .toList();
        return new Split(best, worst);
    }

    /// Builds a titled list of bars that show the share of games won. `barStyle` is the style class of the bars.
    private static VBox rankingBox(String title, String emptyText, List<Ranked> rows, String barStyle) {
        VBox container = new VBox(Layout.SPACING, subheading(title));
        if (rows.isEmpty()) {
            container.getChildren().add(new Label(emptyText));
            return container;
        }

        GridPane grid = new GridPane(Layout.SPACING, Layout.SPACING);
        ColumnConstraints nameConstraint = new ColumnConstraints();
        ColumnConstraints barConstraint = new ColumnConstraints();
        ColumnConstraints statConstraint = new ColumnConstraints();
        nameConstraint.setHgrow(Priority.ALWAYS);
        barConstraint.setPercentWidth(BAR_WIDTH_PERCENT);
        statConstraint.setHgrow(Priority.SOMETIMES);
        grid.getColumnConstraints().addAll(nameConstraint, barConstraint, statConstraint);
        container.getChildren().add(grid);

        int rowIndex = 0;
        for (Ranked row : rows) {
            Rate rate = row.rate();
            double share = winRate(rate);

            ProgressBar bar = new ProgressBar(share);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add(barStyle);

            Label rateLabel = new Label(
                    localize("playerView.ranking.rate", rate.wins(), rate.games(), I18n.formatPercentage(share, 0)));

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
        return sideBySide(winLossDetails(statistics), winLossDonut(statistics), Layout.PADDING, BAR_WIDTH_PERCENT);
    }

    /// Builds the left side of the wins and losses section: a legend with numbers, the win rates, and recent games.
    private static VBox winLossDetails(PlayerStatistics statistics) {
        int total = statistics.gamesPlayed();
        int goodGames = statistics.wonGood() + statistics.lossGood();
        int evilGames = statistics.wonEvil() + statistics.lossEvil();

        GridPane legend = new GridPane(Layout.PADDING, Layout.SPACING);
        ColumnConstraints numberColumn = new ColumnConstraints();
        numberColumn.setHalignment(HPos.RIGHT);
        legend.getColumnConstraints()
                .addAll(new ColumnConstraints(), new ColumnConstraints(), numberColumn, numberColumn);
        legendRow(
                legend,
                0,
                Styles.WIN_LOSS_GOOD_WIN,
                localize("playerView.results.goodWins"),
                statistics.wonGood(),
                total);
        legendRow(
                legend,
                1,
                Styles.WIN_LOSS_GOOD_LOSS,
                localize("playerView.results.goodLosses"),
                statistics.lossGood(),
                total);
        legendRow(
                legend,
                2,
                Styles.WIN_LOSS_EVIL_LOSS,
                localize("playerView.results.evilLosses"),
                statistics.lossEvil(),
                total);
        legendRow(
                legend,
                3,
                Styles.WIN_LOSS_EVIL_WIN,
                localize("playerView.results.evilWins"),
                statistics.wonEvil(),
                total);

        HBox rates = new HBox(
                Layout.PADDING * 4,
                smallStat(
                        localize("playerView.results.winrateGood"),
                        rate(statistics.wonGood(), goodGames),
                        localize("playerView.ofTotal", statistics.wonGood(), goodGames)),
                smallStat(
                        localize("playerView.results.winrateEvil"),
                        rate(statistics.wonEvil(), evilGames),
                        localize("playerView.ofTotal", statistics.wonEvil(), evilGames)),
                smallStat(
                        localize("playerView.results.playedEvil"),
                        rate(evilGames, total),
                        localize("playerView.ofTotal", evilGames, total)));

        return new VBox(
                Layout.PADDING * 2,
                new VBox(Layout.SPACING, subheading(localize("playerView.results.title")), legend),
                new Separator(),
                rates,
                new Separator(),
                recentGames(statistics.recentGames()));
    }

    /// Build a legend row used in the top right of the wins and losses section.
    private static void legendRow(GridPane legend, int row, String colorClass, String label, int count, int total) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll(Styles.LEGEND_SWATCH, colorClass);
        Label share = new Label(rate(count, total));
        share.getStyleClass().add(Styles.CAPTION);
        legend.addRow(row, swatch, new Label(label), new Label(String.valueOf(count)), share);
    }

    /// Builds a small stat row with a caption, value, and detail. Used in the wins and losses section.
    private static VBox smallStat(String caption, String value, String detail) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add(Styles.CAPTION);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(Styles.VALUE_SMALL);
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add(Styles.CAPTION);
        return new VBox(captionLabel, valueLabel, detailLabel);
    }

    /// Builds a strip with one circle per recent game, newest first, marked W for won and L for lost.
    private static VBox recentGames(List<PlayerStatistics.RecentGame> games) {
        HBox strip = new HBox(Layout.PADDING * 2);
        for (PlayerStatistics.RecentGame game : games) {
            Label result = new Label(
                    game.won() ? localize("playerView.recentGames.won") : localize("playerView.recentGames.lost"));
            result.getStyleClass()
                    .addAll(Styles.RECENT_GAME, game.won() ? Styles.RECENT_GAME_WON : Styles.RECENT_GAME_LOST);

            Label date = new Label(I18n.formatDate(game.date()));
            date.getStyleClass().add(Styles.CAPTION);

            VBox entry = new VBox(Layout.SPACING, result, date);
            entry.setAlignment(Pos.CENTER);
            strip.getChildren().add(entry);
        }
        return new VBox(Layout.PADDING, subheading(localize("playerView.recentGames.title", games.size())), strip);
    }

    /// Builds the donut of wins and losses by alignment, with the number of games in its center.
    private static StackPane winLossDonut(PlayerStatistics statistics) {
        int total = statistics.gamesPlayed();
        List<PieChart.Data> data = List.of(
                new PieChart.Data(localize("playerView.results.goodWins"), statistics.wonGood()),
                new PieChart.Data(localize("playerView.results.goodLosses"), statistics.lossGood()),
                new PieChart.Data(localize("playerView.results.evilLosses"), statistics.lossEvil()),
                new PieChart.Data(localize("playerView.results.evilWins"), statistics.wonEvil()));

        DonutChart donutChart = new DonutChart();
        donutChart.getStyleClass().add(Styles.WIN_LOSS_CHART);
        donutChart.setClockwise(false);
        donutChart.setStartAngle(DONUT_START_ANGLE);
        donutChart.setLegendVisible(false);
        donutChart.setLabelsVisible(false);
        donutChart.setPrefSize(DONUT_SIZE, DONUT_SIZE);
        donutChart.setMaxSize(DONUT_SIZE, DONUT_SIZE);
        donutChart.getData().addAll(data);
        for (PieChart.Data slice : data) {
            Tooltip.install(
                    slice.getNode(),
                    new Tooltip(localize(
                            "playerView.donut.tooltip",
                            slice.getName(),
                            (int) slice.getPieValue(),
                            rate((int) slice.getPieValue(), total))));
        }

        Label totalValue = new Label(String.valueOf(total));
        totalValue.getStyleClass().add(Styles.DONUT_TOTAL);
        Label totalCaption = new Label(localize("playerView.donut.games", total));
        totalCaption.getStyleClass().add(Styles.CAPTION);
        VBox center = new VBox(totalValue, totalCaption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);

        return new StackPane(donutChart, center);
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.CAPTION);
        label.setWrapText(true);
        return label;
    }

    private static GridPane survivalStreakPane(PlayerStatistics statistics) {
        return sideBySide(
                section(localize("playerView.section.survival"), survivalPane(statistics)),
                section(localize("playerView.section.streaks"), streaksPane(statistics)),
                Layout.SPACING,
                SURVIVAL_SECTION_PERCENT);
    }

    private static VBox survivalPane(PlayerStatistics statistics) {
        int played = statistics.gamesPlayed();
        String share = rate(statistics.survived(), played);
        Label summary = new Label(localize("playerView.survival.summary", statistics.survived(), played, share));

        if (statistics.deathsByPhase().isEmpty()) {
            return new VBox(Layout.SPACING, summary, new Label(localize("playerView.survival.noDeaths")));
        }

        int lastPhase = Math.max(Collections.max(statistics.deathsByPhase().keySet()), MIN_DISPLAYED_PHASE);
        int mostDeaths = Collections.max(statistics.deathsByPhase().values());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = countAxis(mostDeaths, localize("playerView.survival.axis"));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(DEFAULT_CHART_HEIGHT);
        chart.setCategoryGap(Layout.PADDING);
        chart.getStyleClass().add(Styles.SURVIVAL_CHART);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int phase = PhaseFormat.FIRST_PHASE; phase <= lastPhase; phase++) {
            series.getData()
                    .add(new XYChart.Data<>(
                            PhaseFormat.format(phase),
                            statistics.deathsByPhase().getOrDefault(phase, 0)));
        }
        chart.getData().add(series);

        for (XYChart.Data<String, Number> bar : series.getData()) {
            Tooltip.install(
                    bar.getNode(),
                    new Tooltip(localize("playerView.survival.barTooltip", bar.getXValue(), bar.getYValue())));
        }

        return new VBox(Layout.SPACING, summary, chart);
    }

    private static VBox streaksPane(PlayerStatistics statistics) {
        StatBlock longestWinStreak =
                new StatBlock(String.valueOf(statistics.longestWinStreak()), localize("playerView.streaks.longestWin"));
        StatBlock longestLossStreak = new StatBlock(
                String.valueOf(statistics.longestLossStreak()), localize("playerView.streaks.longestLoss"));
        StatBlock longestGoodStreak = new StatBlock(
                String.valueOf(statistics.longestGoodStreak()), localize("playerView.streaks.longestGood"));
        StatBlock longestEvilStreak = new StatBlock(
                String.valueOf(statistics.longestEvilStreak()), localize("playerView.streaks.longestEvil"));

        return new VBox(
                Layout.SPACING,
                new StatBlocks(longestWinStreak, longestLossStreak),
                new StatBlocks(longestGoodStreak, longestEvilStreak));
    }

    private static Node alliesPane(PlayerStatistics statistics) {
        GridPane rankings = sideBySide(
                allyRanking(localize("playerView.allies.bestGood"), statistics.goodTeammates(), Styles.GOOD_ALLY_BAR),
                allyRanking(localize("playerView.allies.bestEvil"), statistics.evilTeammates(), Styles.EVIL_ALLY_BAR));
        Label note = caption(localize("playerView.allies.note", MIN_GAMES_FOR_RANKING));
        return new VBox(Layout.SPACING, rankings, note);
    }

    private static VBox allyRanking(String title, Map<Integer, PlayerStatistics.Teammate> teammates, String barStyle) {
        List<Ranked> rows = teammates.values().stream()
                .map(teammate -> new Ranked(teammate.name(), teammate.rate()))
                .filter(row -> row.rate().games() >= MIN_GAMES_FOR_RANKING && winRate(row.rate) > RATING_THRESHOLD)
                .sorted(byWinRate(true))
                .limit(RANKING_SIZE)
                .toList();
        return rankingBox(title, localize("playerView.allies.empty", MIN_GAMES_FOR_RANKING), rows, barStyle);
    }

    private static Node scriptsPane(PlayerStatistics statistics) {
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
                        .reversed()
                        .thenComparing(PlayerStatistics.ScriptPlays::name))
                .toList();
        int highestPlays = entries.stream()
                .mapToInt(PlayerStatistics.ScriptPlays::games)
                .max()
                .orElse(0);
        int gridRow = 0;
        for (PlayerStatistics.ScriptPlays entry : entries) {
            ProgressBar progressBar = new ProgressBar((double) entry.games() / highestPlays);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.getStyleClass().add(Styles.GAMES_BAR);
            grid.addRow(gridRow, new Label(entry.name()), progressBar, new Label(String.valueOf(entry.games())));
            gridRow++;
        }

        return grid;
    }

    /// Makes the table exactly as tall as its rows, so it never scrolls on its own.
    private static void fitToRows(TableView<?> table) {
        table.setFixedCellSize(TABLE_ROW_HEIGHT);
        table.prefHeightProperty()
                .bind(Bindings.size(table.getItems())
                        .add(1)
                        .multiply(table.getFixedCellSize())
                        .add(TABLE_BORDER_SLACK));
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);
    }
}
