package net.techmayhem.clocktrack.ui;

import java.util.Locale;
import net.techmayhem.clocktrack.gamedata.Team;
import org.jetbrains.annotations.NonNls;

/// The names of the style classes defined in `style.css`, and the lookup of the style class of a team.
///
/// The constants and the rules in `style.css` follow the same order.
@NonNls
public final class Styles {
    private Styles() {}

    // Teams

    /// Sets the fill of a shape to the color of the team set by the style class of that team.
    public static final String TEAM_FILL = "team-fill";

    /// Sets the accent of a progress bar to the color of the team set by the style class of that team.
    public static final String TEAM_BAR = "team-bar";

    // Text

    /// The large bold text of a title or of the value of a stat block.
    public static final String TITLE_TEXT = "title-text";

    /// The bold text above a group of related rows.
    public static final String SUBHEADING = "subheading";

    /// The gray text of secondary information, such as a share, a date or a description.
    public static final String CAPTION = "caption";

    /// The bold text of a value that has a [#CAPTION] next to it.
    public static final String VALUE_SMALL = "value-small";

    // Sections

    /// The outer frame of a section.
    public static final String SECTION = "section";

    /// The title bar at the top of a section.
    public static final String SECTION_TITLE = "section-title";

    /// The content area of a section.
    public static final String SECTION_CONTENT = "section-content";

    // Stat blocks

    /// The framed box of a single block in a row of stat blocks.
    public static final String STAT_BLOCK = "stat-block";

    // Wins and losses

    /// Sets the colors of the slices of the chart that shows the wins and losses.
    public static final String WIN_LOSS_CHART = "win-loss-chart";

    /// Sets the fill of the hole in the middle of a donut chart, so it matches the background.
    public static final String DONUT_HOLE = "donut-hole";

    /// The large bold text that shows the total in the hole of a donut chart.
    public static final String DONUT_TOTAL = "donut-total";

    /// The small round color marker in front of a legend entry.
    public static final String LEGEND_SWATCH = "legend-swatch";

    /// The background color that stands for a game won as good.
    public static final String WIN_LOSS_GOOD_WIN = "win-loss-good-win";

    /// The background color that stands for a game lost as good.
    public static final String WIN_LOSS_GOOD_LOSS = "win-loss-good-loss";

    /// The background color that stands for a game lost as evil.
    public static final String WIN_LOSS_EVIL_LOSS = "win-loss-evil-loss";

    /// The background color that stands for a game won as evil.
    public static final String WIN_LOSS_EVIL_WIN = "win-loss-evil-win";

    /// The round marker that shows the result of a recent game. Combined with [#RECENT_GAME_WON] or
    /// [#RECENT_GAME_LOST].
    public static final String RECENT_GAME = "recent-game";

    /// Fills the marker of a recent game that was won.
    public static final String RECENT_GAME_WON = "recent-game-won";

    /// Outlines the marker of a recent game that was lost.
    public static final String RECENT_GAME_LOST = "recent-game-lost";

    // Bars and charts

    /// The progress bar of a ranking entry that is among the best.
    public static final String BEST_BAR = "best-bar";

    /// The progress bar of a ranking entry that is among the worst.
    public static final String WORST_BAR = "worst-bar";

    /// The progress bar of an ally on the good team.
    public static final String GOOD_ALLY_BAR = "good-ally-bar";

    /// The progress bar of an ally on the evil team.
    public static final String EVIL_ALLY_BAR = "evil-ally-bar";

    /// The progress bar that shows a number of games.
    public static final String GAMES_BAR = "games-bar";

    /// Sets the color of the bars of the chart that shows when a player died.
    public static final String SURVIVAL_CHART = "survival-chart";

    /// Sets the colors of the bars of the chart that shows the results by player count.
    public static final String PLAYER_COUNT_CHART = "player-count-chart";

    /// Returns the style class that sets the team color for the given team. It is meant to be combined with
    /// [#TEAM_FILL] or [#TEAM_BAR].
    ///
    /// @param team The team
    /// @return The style class of the team
    public static String teamClass(Team team) {
        return "team-" + team.name().toLowerCase(Locale.ROOT);
    }
}
