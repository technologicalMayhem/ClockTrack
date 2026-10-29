package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/// Represents the statistics of a single player, aggregated over all games they took part in.
///
/// Games as the storyteller only count towards `gamesStorytold`, everything else covers the games the player actually
/// played. A game counts as won when the player's alignment at the end matches the team that won, and as lost
/// otherwise.
///
/// The maps only hold entries for keys that occurred, so a character the player never played has no entry.
///
/// @param id The id of the player
/// @param name The name of the player
/// @param gamesStorytold The number of games the player ran as the storyteller
/// @param wonGood The number of games won on the good team
/// @param wonEvil The number of games won on the evil team
/// @param lossGood The number of games lost on the good team
/// @param lossEvil The number of games lost on the evil team
/// @param survived The number of games in which the player was still alive at the end
/// @param longestWinStreak The most games won in a row
/// @param longestLossStreak The most games lost in a row
/// @param longestGoodStreak The most games played on the good team in a row, regardless of the result
/// @param longestEvilStreak The most games played on the evil team in a row, regardless of the result
/// @param charactersPlayed The games and wins per character, keyed by the role identifier
/// @param deathsByPhase The number of deaths per phase, keyed by the phase number. Phases count nights and days
/// alternately from 1, so night 1 is 1 and day 1 is 2.
/// @param scriptPlays The games per script, keyed by the script identifier
/// @param goodTeammates The games and wins together with each teammate on the good team, keyed by the player identifier
/// @param evilTeammates The games and wins together with each teammate on the evil team, keyed by the player identifier
/// @param recentGames The player's last five games, newest first. Empty if the player has no games.
public record PlayerStatistics(
        int id,
        String name,
        int gamesStorytold,
        int wonGood,
        int wonEvil,
        int lossGood,
        int lossEvil,
        int survived,
        int longestWinStreak,
        int longestLossStreak,
        int longestGoodStreak,
        int longestEvilStreak,
        Map<String, Rate> charactersPlayed,
        Map<Integer, Integer> deathsByPhase,
        Map<Integer, ScriptPlays> scriptPlays,
        Map<Integer, Teammate> goodTeammates,
        Map<Integer, Teammate> evilTeammates,
        List<RecentGame> recentGames) {

    /// Constructs a new set of statistics with the specified values. The maps and the list are copied.
    public PlayerStatistics {
        charactersPlayed = Map.copyOf(charactersPlayed);
        deathsByPhase = Map.copyOf(deathsByPhase);
        scriptPlays = Map.copyOf(scriptPlays);
        goodTeammates = Map.copyOf(goodTeammates);
        evilTeammates = Map.copyOf(evilTeammates);
        recentGames = List.copyOf(recentGames);
    }

    /// Returns the number of games the player took part in, not counting games as the storyteller.
    ///
    /// @return The number of games played
    public int gamesPlayed() {
        return wonGood + wonEvil + lossGood + lossEvil;
    }

    /// Represents how often the player played a script.
    ///
    /// @param name The name of the script
    /// @param games The number of games the player played on the script
    public record ScriptPlays(String name, int games) {}

    /// Represents another player the player was on the same team with.
    ///
    /// @param name The name of the teammate
    /// @param rate The games played together on the same team, and how many of them were won
    public record Teammate(String name, Rate rate) {}

    /// Represents one of the player's recent games.
    ///
    /// @param sessionId The id of the session
    /// @param date The date of the session
    /// @param good Whether the player was on the good team at the end of the game
    /// @param won Whether the player's team won
    public record RecentGame(int sessionId, LocalDate date, boolean good, boolean won) {}
}
