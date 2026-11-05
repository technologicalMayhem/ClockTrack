package net.techmayhem.clocktrack.ui;

import net.techmayhem.clocktrack.gamedata.Team;

public class Styles {
    public static final String WIN_LOSS_PIE = "winLossPie";
    public static final String STAT_BLOCK_VALUE = "statBlockValue";
    public static final String TEAM_BAR = "teamBar";
    public static final String DONUT_CHART = "donutChart";

    public static final String TEAM_TOWNSFOLK = "teamTownsfolk";
    public static final String TEAM_OUTSIDER = "teamOutsider";
    public static final String TEAM_MINION = "teamMinion";
    public static final String TEAM_DEMON = "teamDemon";
    public static final String TEAM_TRAVELLER = "teamTraveller";
    public static final String TEAM_FABLED = "teamFabled";
    public static final String TEAM_LORIC = "teamLoric";

    public static String teamClass(Team team) {
        return switch (team) {
            case TOWNSFOLK -> TEAM_TOWNSFOLK;
            case OUTSIDER -> TEAM_OUTSIDER;
            case MINION -> TEAM_MINION;
            case DEMON -> TEAM_DEMON;
            case TRAVELLER -> TEAM_TRAVELLER;
            case FABLED -> TEAM_FABLED;
            case LORIC -> TEAM_LORIC;
        };
    }
}
