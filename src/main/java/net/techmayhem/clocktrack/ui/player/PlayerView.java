package net.techmayhem.clocktrack.ui.player;

import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.gamedata.Team;
import net.techmayhem.clocktrack.projections.PlayerStatistics;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Screen;
import net.techmayhem.clocktrack.ui.component.DonutChart;
import net.techmayhem.clocktrack.ui.component.TeamColors;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.stream.Collectors;

/// A screen that shows the statistics of a player. Currently, it is a placeholder that only shows the statistics as
/// text.
public class PlayerView extends Screen {
    private final StackPane root;
    private final String name;

    @NonNls
    String PIEC_CHART_CSS = """
                CHART_COLOR_1: #00a9ff;
                CHART_COLOR_2: #7fd4ff;
                CHART_COLOR_3: #ff7f7f;
                CHART_COLOR_4: #ff0000;
            """;

    PlayerView(PlayerStatistics statistics) {
        root = new StackPane();
        Text e = new Text(statistics.toString());
        e.setWrappingWidth(400);
        int total = statistics.gamesPlayed();
        List<PieChart.Data> data = List.of(
                slice("Good wins", statistics.wonGood(), total),
                slice("Good loss", statistics.lossGood(), total),
                slice("Evil loss", statistics.lossEvil(), total),
                slice("Evil wins", statistics.wonEvil(), total)
        );
        DonutChart donutChart = new DonutChart();
        donutChart.setStyle(PIEC_CHART_CSS);
        donutChart.setClockwise(false);
        donutChart.setStartAngle(90);
        donutChart.getData().addAll(data);

        NumberAxis xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();

        int max = statistics.charactersPlayed().values().stream().map(Rate::games).max(Integer::compare).orElse(1);

        xAxis.setLabel("Times played");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(max);
        xAxis.setTickUnit(Math.max(1, Math.ceil(max / 10.0))); // whole steps, ~10 ticks at most
        xAxis.setMinorTickVisible(false);

        BarChart<Number, String> characterChart = new BarChart<>(xAxis, yAxis);
        characterChart.setLegendVisible(false);
        characterChart.setAnimated(false);

        Roles instance = Roles.getInstance();
        Map<Role, Rate> roles = statistics.charactersPlayed().entrySet().stream()
                .map(entry -> {
                    Role role = instance.get(entry.getKey());
                    return role == null ? null : Map.entry(role, entry.getValue());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        roles.forEach((key, value) -> series.getData().add(new XYChart.Data<>(value.games(), key.name(), key)));
        characterChart.getData().add(series);
        characterChart.getData().getFirst().getData().forEach(PlayerView::colorBars);

        root.getChildren().addAll(new VBox(Layout.SPACING, donutChart, characterChart));
        name = "View: " + statistics.name();
    }

    private static PieChart.Data slice(String name, int count, int total) {
        double percent = total == 0 ? 0 : 100.0 * count / total;
        return new PieChart.Data(String.format("%s (%.0f%%)", name, percent), count);
    }

    private static void colorBars(XYChart.Data<Number, String> d) {
        if (d.getExtraValue() instanceof Role role)
            d.getNode().setStyle("-fx-bar-fill: " + toCss(TeamColors.getColor(role.team())) + ";");
    }

    private static String toCss(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
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
