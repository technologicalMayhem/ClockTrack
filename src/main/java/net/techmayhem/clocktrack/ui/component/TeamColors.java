package net.techmayhem.clocktrack.ui.component;

import javafx.scene.paint.Color;
import net.techmayhem.clocktrack.gamedata.Team;

public class TeamColors {
    public static Color getColor(Team team) {
        return switch (team) {
            case TOWNSFOLK -> Color.web("#00a9ff");
            case OUTSIDER -> Color.web("#7fd4ff");
            case MINION -> Color.web("#ff7f7f");
            case DEMON -> Color.web("#ff0000");
            case TRAVELLER -> getColor(Team.TOWNSFOLK).interpolate(getColor(Team.DEMON), 0.5);
            case FABLED -> Color.web("#fce72d");
            case LORIC -> Color.web("#00d61c");
        };
    }
}
