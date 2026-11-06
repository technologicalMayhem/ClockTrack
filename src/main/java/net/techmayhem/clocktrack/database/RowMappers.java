package net.techmayhem.clocktrack.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.function.Function;
import net.techmayhem.clocktrack.model.*;
import net.techmayhem.clocktrack.projections.*;
import org.jspecify.annotations.Nullable;

/// Maps rows of query results to model objects and projections. Each method maps the current row of the given result
/// set.
class RowMappers {
    /// Maps the current row to a [FromDb], taking its id from the `id` column.
    ///
    /// @param rs The result set, positioned at the row to map
    /// @param mapper The mapper for the value
    /// @param <T> The type of the value
    /// @return The value with its id
    static <T> FromDb<T> fromDb(ResultSet rs, FromResultSet<T> mapper) throws SQLException {
        return new FromDb<>(rs.getInt("id"), mapper.map(rs));
    }

    /// Maps the current row to a [Player].
    static Player player(ResultSet rs) throws SQLException {
        return new Player(rs.getString("name"), rs.getString("notes"));
    }

    /// Maps the current row to a [PlayerSession].
    static PlayerSession playerSession(ResultSet rs) throws SQLException {
        return new PlayerSession(
                rs.getInt("session_id"),
                getInteger(rs, "player_id"),
                rs.getString("role"),
                getInteger(rs, "death_in_phase"),
                rs.getString("cause_of_death"),
                rs.getBoolean("good"),
                rs.getString("note"));
    }

    /// Maps the current row to a [Script].
    static Script script(ResultSet rs) throws SQLException {
        return new Script(rs.getString("name"), rs.getString("json"));
    }

    /// Maps the current row to a [Session].
    static Session session(ResultSet rs) throws SQLException {
        return new Session(
                LocalDate.parse(rs.getString("date")),
                getInteger(rs, "storyteller_id"),
                rs.getBoolean("good_won"),
                rs.getInt("script_id"),
                rs.getString("note"));
    }

    /// Maps the current row to a [PlayerSummary].
    static PlayerSummary playerSummary(ResultSet rs) throws SQLException {
        return new PlayerSummary(
                rs.getInt("id"),
                rs.getString("name"),
                mapIfNotNull(rs.getString("first_game"), LocalDate::parse),
                mapIfNotNull(rs.getString("last_game"), LocalDate::parse),
                rs.getInt("games_played"),
                rs.getInt("games_storytold"));
    }

    /// Maps the current row to a [ScriptSummary].
    static ScriptSummary scriptSummary(ResultSet rs) throws SQLException {
        return new ScriptSummary(
                rs.getInt("id"),
                rs.getString("name"),
                mapIfNotNull(rs.getString("first_played"), LocalDate::parse),
                mapIfNotNull(rs.getString("last_played"), LocalDate::parse),
                rs.getInt("times_played"),
                rs.getInt("good_wins"));
    }

    /// Maps the current row to a [SessionSummary].
    static SessionSummary sessionSummary(ResultSet rs) throws SQLException {
        return new SessionSummary(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getBoolean("good_won"),
                rs.getString("storyteller"),
                rs.getString("script_name"),
                rs.getInt("player_count"));
    }

    /// Maps the current row to a [Rate], from the `games` and `wins` columns.
    static Rate rate(ResultSet rs) throws SQLException {
        return new Rate(rs.getInt("games"), rs.getInt("wins"));
    }

    /// Maps the current row to a [PlayerStatistics.ScriptPlays].
    static PlayerStatistics.ScriptPlays scriptPlays(ResultSet rs) throws SQLException {
        return new PlayerStatistics.ScriptPlays(rs.getString("name"), rs.getInt("games"));
    }

    /// Maps the current row to a [PlayerStatistics.Teammate].
    static PlayerStatistics.Teammate teammate(ResultSet rs) throws SQLException {
        return new PlayerStatistics.Teammate(rs.getString("name"), rate(rs));
    }

    /// Maps the current row to a [GameInfo].
    static GameInfo gameInfo(ResultSet rs) throws SQLException {
        return new GameInfo(
                rs.getInt("session_id"),
                LocalDate.parse(rs.getString("date")),
                rs.getBoolean("good"),
                rs.getBoolean("won"),
                rs.getBoolean("survived"));
    }

    /// Maps the current row to a [ScriptStatistics.Session].
    static ScriptStatistics.Session scriptStatisticsSession(ResultSet rs) throws SQLException {
        return new ScriptStatistics.Session(
                LocalDate.parse(rs.getString("date")), rs.getBoolean("good_won"), getInteger(rs, "storyteller_id"));
    }

    /// Maps the current row to a [ScriptStatistics.Seat].
    static ScriptStatistics.Seat scriptStatisticsSeat(ResultSet rs) throws SQLException {
        return new ScriptStatistics.Seat(
                rs.getInt("session_id"),
                getInteger(rs, "player_id"),
                rs.getString("role"),
                rs.getBoolean("good"),
                rs.getBoolean("good_won"));
    }

    /// Reads a nullable integer column. Returns null if the column is SQL NULL.
    private static @Nullable Integer getInteger(ResultSet rs, String columnLabel) throws SQLException {
        int i = rs.getInt(columnLabel);
        return rs.wasNull() ? null : i;
    }

    /// Applies the function to the value, unless the value is null.
    private static <In, Out> @Nullable Out mapIfNotNull(@Nullable In in, Function<In, Out> map) {
        if (in != null) {
            return map.apply(in);
        } else {
            return null;
        }
    }
}
