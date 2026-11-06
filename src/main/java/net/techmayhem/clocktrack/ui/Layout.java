package net.techmayhem.clocktrack.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Layout constants shared by all screens.
public final class Layout {
    public static final double SCROLL_SPEED = 2;

    private Layout() {}

    /// The amount of spacing used between elements within a UI pane.
    public static final int SPACING = 5;

    /// The amount of spacing used between the root UI element and its border.
    public static final int PADDING = 10;

    /// Places two nodes side by side, each taking half of the width.
    public static GridPane sideBySide(Node left, Node right) {
        return sideBySide(left, right, PADDING * 2, 50);
    }

    /// Places two nodes side by side. The left node takes the given percentage of the width, the right one the rest.
    public static GridPane sideBySide(Node left, Node right, double spacing, double leftPercent) {
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

    public static VBox section(String title, Node content) {
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

    public static void acceleratedScroll(ScrollPane root, ScrollEvent event) {
        double scrollable = root.getContent().getBoundsInLocal().getHeight()
                - root.getViewportBounds().getHeight();
        if (scrollable <= 0) return;
        double delta = event.getDeltaY() * SCROLL_SPEED / scrollable;
        root.setVvalue(Math.clamp(root.getVvalue() - delta, 0, 1));
        event.consume();
    }
}
