package net.techmayhem.clocktrack.ui.player;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.projections.PlayerStatistics;
import net.techmayhem.clocktrack.ui.Screen;

/// A screen that shows the statistics of a player. Currently, it is a placeholder that only shows the statistics as
/// text.
public class PlayerView extends Screen {
    private final StackPane root;
    private final String name;

    PlayerView(PlayerStatistics statistics) {
        root = new StackPane();
        Text e = new Text(statistics.toString());
        e.setWrappingWidth(400);
        root.getChildren().add(e);
        name = "View: " + statistics.name();
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
