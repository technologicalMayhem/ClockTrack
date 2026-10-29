package net.techmayhem.clocktrack.ui.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.SessionRoles;
import net.techmayhem.clocktrack.gamedata.Team;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.Rate;
import net.techmayhem.clocktrack.projections.ScriptStatistics;

/// The statistics of a script, computed from its games and interpreted with the roles of the script.
///
/// @param results The games played on the script and how many of them good won
/// @param appearances Every character that appeared, most frequent first
/// @param unknownRoles The role identifiers that appeared but are not known to the script, sorted.
/// @param neverAppeared The characters of the script that never appeared, sorted by team and in script order within a
/// team. Travellers are left out, since they are not part of the script.
/// @param resultsByDemon The games and good wins per Demon. Only Demons held by an evil player count.
/// @param resultsByPlayerCount The games and good wins per player count, ordered by player count. Travellers do not
/// count towards the player count.
/// @param mostPlayed The players who played the script, the most games first
/// @param neverPlayed The players who played at least one game, but none on this script, in the order they were given
/// @param storytellers The players who ran the script as the storyteller, the most games first
public record ScriptReport(
        Rate results,
        List<Appearance> appearances,
        List<String> unknownRoles,
        List<Role> neverAppeared,
        Map<Role, Rate> resultsByDemon,
        SortedMap<Integer, Rate> resultsByPlayerCount,
        List<PlayerGames> mostPlayed,
        List<PlayerSummary> neverPlayed,
        List<PlayerGames> storytellers) {

    /// Represents how often a character appeared on the script.
    ///
    /// @param role The character
    /// @param games The number of games the character appeared in
    public record Appearance(Role role, int games) {}

    /// Represents how many games a player took part in on the script.
    ///
    /// @param player The player
    /// @param games The number of games
    public record PlayerGames(PlayerSummary player, int games) {}

    /// Computes the report of a script.
    ///
    /// @param statistics The games played on the script
    /// @param roles The roles of the script
    /// @param players All players used to look up the players in the games and to find those who never played the
    /// script
    /// @return The report of the script
    public static ScriptReport of(ScriptStatistics statistics, SessionRoles roles, List<PlayerSummary> players) {
        Collection<List<ScriptStatistics.Seat>> games = groupByGame(statistics.seats());
        Map<String, Integer> appearanceCounts = countAppearances(games);
        Map<Integer, PlayerSummary> playersById =
                players.stream().collect(Collectors.toMap(PlayerSummary::id, Function.identity()));
        Map<Integer, Integer> gamesByPlayer = countGamesByPlayer(statistics.seats());

        return new ScriptReport(
                results(statistics.sessions()),
                knownAppearances(appearanceCounts, roles),
                unknownRoles(appearanceCounts, roles),
                neverAppeared(appearanceCounts, roles),
                resultsByDemon(games, roles),
                resultsByPlayerCount(games, roles),
                rank(gamesByPlayer, playersById),
                neverPlayed(players, gamesByPlayer),
                rank(countGamesByStoryteller(statistics.sessions()), playersById));
    }

    /// Groups the seats by their session, so each entry holds the seats of one game.
    private static Collection<List<ScriptStatistics.Seat>> groupByGame(List<ScriptStatistics.Seat> seats) {
        return seats.stream()
                .collect(Collectors.groupingBy(ScriptStatistics.Seat::sessionId))
                .values();
    }

    private static Rate results(List<ScriptStatistics.Session> sessions) {
        long goodWins =
                sessions.stream().filter(ScriptStatistics.Session::goodWon).count();
        return new Rate(sessions.size(), (int) goodWins);
    }

    /// Counts the games each role identifier appeared in. A role held by several players in one game counts once.
    private static Map<String, Integer> countAppearances(Collection<List<ScriptStatistics.Seat>> games) {
        Map<String, Integer> counts = new HashMap<>();
        for (List<ScriptStatistics.Seat> seats : games) {
            seats.stream()
                    .map(ScriptStatistics.Seat::role)
                    .distinct()
                    .forEach(role -> counts.merge(role, 1, Integer::sum));
        }
        return counts;
    }

    private static List<Appearance> knownAppearances(Map<String, Integer> appearanceCounts, SessionRoles roles) {
        List<Appearance> appearances = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : appearanceCounts.entrySet()) {
            Role role = roles.get(entry.getKey());
            if (role != null) {
                appearances.add(new Appearance(role, entry.getValue()));
            }
        }
        appearances.sort(Comparator.comparingInt(Appearance::games)
                .reversed()
                .thenComparing(appearance -> appearance.role().name()));
        return appearances;
    }

    private static List<String> unknownRoles(Map<String, Integer> appearanceCounts, SessionRoles roles) {
        return appearanceCounts.keySet().stream()
                .filter(id -> roles.get(id) == null)
                .sorted()
                .toList();
    }

    private static List<Role> neverAppeared(Map<String, Integer> appearanceCounts, SessionRoles roles) {
        return roles.roleChoices().stream()
                .filter(role -> role.team() != Team.TRAVELLER)
                .filter(role -> !appearanceCounts.containsKey(role.id()))
                .toList();
    }

    /// Counts the games and good wins per Demon. Each Demon held by an evil player counts once per game.
    private static Map<Role, Rate> resultsByDemon(Collection<List<ScriptStatistics.Seat>> games, SessionRoles roles) {
        Map<Role, Rate> results = new HashMap<>();
        for (List<ScriptStatistics.Seat> seats : games) {
            Set<Role> demons = new HashSet<>();
            for (ScriptStatistics.Seat seat : seats) {
                Role role = roles.get(seat.role());
                if (role != null && role.team() == Team.DEMON && !seat.good()) {
                    demons.add(role);
                }
            }
            for (Role demon : demons) {
                count(results, demon, goodWon(seats));
            }
        }
        return results;
    }

    private static SortedMap<Integer, Rate> resultsByPlayerCount(
            Collection<List<ScriptStatistics.Seat>> games, SessionRoles roles) {
        SortedMap<Integer, Rate> results = new TreeMap<>();
        for (List<ScriptStatistics.Seat> seats : games) {
            count(results, playerCount(seats, roles), goodWon(seats));
        }
        return results;
    }

    /// Counts the players of one game. Travellers do not count, seats with an unknown role do.
    private static int playerCount(List<ScriptStatistics.Seat> seats, SessionRoles roles) {
        return (int) seats.stream()
                .map(seat -> roles.get(seat.role()))
                .filter(role -> role == null || role.team() != Team.TRAVELLER)
                .count();
    }

    /// Returns whether good won the game the seats belong to. Every seat of a game carries the same result.
    private static boolean goodWon(List<ScriptStatistics.Seat> seats) {
        return seats.getFirst().goodWon();
    }

    private static Map<Integer, Integer> countGamesByPlayer(List<ScriptStatistics.Seat> seats) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (ScriptStatistics.Seat seat : seats) {
            Integer playerId = seat.playerId();
            if (playerId != null) {
                counts.merge(playerId, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Map<Integer, Integer> countGamesByStoryteller(List<ScriptStatistics.Session> sessions) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (ScriptStatistics.Session session : sessions) {
            Integer storytellerId = session.storytellerId();
            if (storytellerId != null) {
                counts.merge(storytellerId, 1, Integer::sum);
            }
        }
        return counts;
    }

    /// Returns the players who played at least one game overall, but none on this script.
    private static List<PlayerSummary> neverPlayed(List<PlayerSummary> players, Map<Integer, Integer> gamesByPlayer) {
        return players.stream()
                .filter(player -> player.gamesPlayed() > 0)
                .filter(player -> !gamesByPlayer.containsKey(player.id()))
                .toList();
    }

    /// Adds a game to the rate stored under the key.
    private static <K> void count(Map<K, Rate> rates, K key, boolean goodWon) {
        rates.merge(
                key, new Rate(1, goodWon ? 1 : 0), (old, game) -> new Rate(old.games() + 1, old.wins() + game.wins()));
    }

    /// Turns games per player identifier into a list, the most games first. Identifiers without a player are skipped.
    private static List<PlayerGames> rank(Map<Integer, Integer> gamesByPlayer, Map<Integer, PlayerSummary> players) {
        List<PlayerGames> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : gamesByPlayer.entrySet()) {
            PlayerSummary player = players.get(entry.getKey());
            if (player != null) {
                result.add(new PlayerGames(player, entry.getValue()));
            }
        }
        result.sort(Comparator.comparingInt(PlayerGames::games)
                .reversed()
                .thenComparing(playerGames -> playerGames.player().name()));
        return result;
    }
}
