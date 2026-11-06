package net.techmayhem.clocktrack.ui;

import net.techmayhem.clocktrack.gamedata.Team;

public class Styles {
    public static final String SUBHEADING = "subheading";

    public static final String WIN_LOSS_PIE = "winLossPie";
    public static final String STAT_BLOCK_VALUE = "statBlockValue";
    public static final String TEAM_BAR = "teamBar";
    public static final String DONUT_CHART = "donutChart";
    public static final String DONUT_TOTAL = "donut-total";

    public static final String WIN_LOSS_GOOD_WIN = "win-loss-good-win";
    public static final String WIN_LOSS_GOOD_LOSS = "win-loss-good-loss";
    public static final String WIN_LOSS_EVIL_LOSS = "win-loss-evil-loss";
    public static final String WIN_LOSS_EVIL_WIN = "win-loss-evil-win";

    public static final String STAT_CAPTION = "stat-caption";
    public static final String STAT_VALUE_SMALL = "stat-value-small";

    public static final String LEGEND_SWATCH = "legend-swatch";
    public static final String RECENT_GAME = "recent-game";
    public static final String WON = "won";
    public static final String LOST = "lost";

    public static final String STAT_SECTION = "stat-section";
    public static final String STAT_SECTION_TITLE = "stat-section-title";
    public static final String STAT_SECTION_CONTENT = "stat-section-content";

    public static final String CHARACTER_SECTION_BOX_WIN = "character-section-box-win";
    public static final String CHARACTER_SECTION_BOX_LOSS = "character-section-box-loss";
    public static final String CHARACTER_SECTION_BOX_UNCOLORED = "character-section-box-uncolored";

    public static final String SURVIVAL_BAR_CHART = "survival-bar-chart";

    public static final String SCRIPTS_BAR = "scripts-bars";

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
