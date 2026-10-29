package net.techmayhem.clocktrack.ui.component;

import javafx.geometry.Bounds;
import javafx.scene.chart.PieChart;
import javafx.scene.shape.Circle;

public class DonutChart extends PieChart {
    private static final double HOLE_RATIO = 0.7; // hole radius relative to the pie radius

    private final Circle hole = new Circle();

    public DonutChart() {
        hole.setMouseTransparent(true);
        hole.setStyle("-fx-fill: -fx-background;"); // same as the default chart background
    }

    @Override
    protected void layoutChartChildren(double top, double left, double width, double height) {
        super.layoutChartChildren(top, left, width, height);
        if (!getChartChildren().contains(hole)) {
            getChartChildren().add(hole);
        }
        hole.toFront();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (PieChart.Data d : getData()) {
            if (d.getNode() == null) continue;
            Bounds b = d.getNode().getBoundsInParent();
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        if (minX > maxX) {
            hole.setVisible(false);
            return;
        }
        hole.setVisible(true);
        hole.setCenterX((minX + maxX) / 2);
        hole.setCenterY((minY + maxY) / 2);
        hole.setRadius((maxX - minX) / 2 * HOLE_RATIO);
    }
}
