package net.techmayhem.clocktrack.ui;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.projections.Rate;
import org.jspecify.annotations.Nullable;

/// The base of the screens that show statistics: a scrollable page that opens with a header and continues with
/// sections.
///
/// A subclass passes the name of its tab to the constructor and fills the page with [#setContent]. It also gets the
/// building blocks that all statistics screens share, such as sections, rates, and the axis of a chart that counts
/// things.
public abstract class StatisticsView extends Screen {
    /// The factor by which the page scrolls faster than JavaFX scrolls it by default.
    private static final double SCROLL_SPEED = 2;

    private static final int DEFAULT_LEFT_PERCENT = 50;
    private static final double MAX_TICK_COUNT = 10.0;
    private static final int MIN_AXIS_MAXIMUM = 5;

    /// A dash, used when a value is unavailable. This is the horizontal bar (U+2015).
    protected static final String DASH = "―";

    private final ScrollPane root = new ScrollPane();
    private final VBox content = new VBox(Layout.SPACING);
    private final String name;

    /// Constructs a new statistics screen with the specified tab name. The page stays empty until [#setContent] is
    /// called.
    ///
    /// @param name The name of the screen, shown as the title of its tab
    protected StatisticsView(String name) {
        this.name = name;

        content.setPadding(new Insets(Layout.PADDING));
        root.setFitToWidth(true);
        root.setContent(content);
        root.addEventFilter(ScrollEvent.SCROLL, event -> acceleratedScroll(root, event));
    }

    @Override
    protected final Parent getView() {
        return root;
    }

    @Override
    protected final String getName() {
        return name;
    }

    /// Fills the page with a header and the given sections, replacing what the page showed before.
    ///
    /// @param title The title in the header
    /// @param firstGame The date of the first game, or null if there is none
    /// @param lastGame The date of the last game, or null if there is none
    /// @param sections The nodes below the header, from top to bottom
    protected final void setContent(
            String title, @Nullable LocalDate firstGame, @Nullable LocalDate lastGame, Node... sections) {
        VBox page = new VBox(Layout.SPACING, header(title, firstGame, lastGame));
        page.getChildren().addAll(sections);
        content.getChildren().setAll(page);
    }

    private static VBox header(String title, @Nullable LocalDate firstGame, @Nullable LocalDate lastGame) {
        Text titleText = new Text(title);
        titleText.getStyleClass().add(Styles.TITLE_TEXT);
        String firstLastGameText;
        if (firstGame != null && lastGame != null) {
            firstLastGameText = localize("statistics.header.firstLastGame", firstGame, lastGame);
        } else {
            firstLastGameText = "";
        }
        return new VBox(Layout.SPACING, titleText, new Text(firstLastGameText));
    }

    private static void acceleratedScroll(ScrollPane root, ScrollEvent event) {
        double scrollable = root.getContent().getBoundsInLocal().getHeight()
                - root.getViewportBounds().getHeight();
        if (scrollable <= 0) return;
        double delta = event.getDeltaY() * SCROLL_SPEED / scrollable;
        root.setVvalue(Math.clamp(root.getVvalue() - delta, 0, 1));
        event.consume();
    }

    /// Creates a section: a framed box with a title bar above the given content.
    ///
    /// @param title The text of the title bar
    /// @param content The content below the title bar
    /// @return The section
    protected static VBox section(String title, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.SECTION_TITLE);
        heading.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(content);
        body.getStyleClass().add(Styles.SECTION_CONTENT);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox section = new VBox(heading, body);
        section.getStyleClass().add(Styles.SECTION);
        section.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return section;
    }

    /// Creates the section that tells that there is no data to show.
    ///
    /// @param message The explanation shown in the section
    /// @return The section
    protected static VBox noDataSection(String message) {
        return section(localize("statistics.section.noData"), new Label(message));
    }

    /// Places two nodes side by side, each taking half of the width.
    ///
    /// @param left The node on the left
    /// @param right The node on the right
    /// @return The grid that holds both nodes
    protected static GridPane sideBySide(Node left, Node right) {
        return sideBySide(left, right, Layout.PADDING * 2, DEFAULT_LEFT_PERCENT);
    }

    /// Places two nodes side by side. The left node takes the given percentage of the width, the right one the rest.
    ///
    /// @param left The node on the left
    /// @param right The node on the right
    /// @param spacing The space between the nodes
    /// @param leftPercent The percentage of the width that the left node takes
    /// @return The grid that holds both nodes
    protected static GridPane sideBySide(Node left, Node right, double spacing, double leftPercent) {
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

    /// Creates a bold label that introduces a group of rows.
    ///
    /// @param text The text of the label
    /// @return The label
    protected static Label subheading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.SUBHEADING);
        return label;
    }

    /// Returns the share of a total as a percentage text, or a dash if the total is 0.
    ///
    /// @param count The part of the total
    /// @param total The total
    /// @return The formatted percentage, or a dash
    protected static String rate(int count, int total) {
        return total == 0 ? DASH : I18n.formatPercentage((double) count / total, 0);
    }

    /// Returns the part of a total as a number between 0 and 1, or 0 if the total is 0.
    ///
    /// @param count The part of the total
    /// @param total The total
    /// @return The share of the total
    protected static double share(int count, int total) {
        return total == 0 ? 0 : (double) count / total;
    }

    /// Returns the share of wins among the games of a rate, or 0 if there are no games.
    ///
    /// @param rate The games and wins
    /// @return The share of wins
    protected static double winRate(Rate rate) {
        return share(rate.wins(), rate.games());
    }

    /// Creates the axis of a chart that counts things. It shows whole numbers and about ten ticks at most, and it ends
    /// at 5 or higher, so a chart with small counts does not look empty.
    ///
    /// @param mostCount The highest count in the chart
    /// @param label The title of the axis
    /// @return The axis
    protected static NumberAxis countAxis(int mostCount, String label) {
        double tickUnit = Math.max(1, Math.ceil(mostCount / MAX_TICK_COUNT));
        NumberAxis axis =
                new NumberAxis(0, Math.ceil(Math.max(mostCount, MIN_AXIS_MAXIMUM) / tickUnit) * tickUnit, tickUnit);
        axis.setMinorTickVisible(false);
        axis.setLabel(label);
        return axis;
    }
}
