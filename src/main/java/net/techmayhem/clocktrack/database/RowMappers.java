package net.techmayhem.clocktrack.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.function.Function;
import net.techmayhem.clocktrack.model.*;
import net.techmayhem.clocktrack.projections.PlayerSummary;
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.projections.SessionSummary;
import org.jspecify.annotations.Nullable;

class RowMappers {
    static <T> FromDb<T> fromDb(ResultSet rs, FromResultSet<T> mapper) throws SQLException {
        return new FromDb<>(rs.getInt("id"), mapper.map(rs));
    }

    static Player player(ResultSet rs) throws SQLException {
        return new Player(rs.getString("name"));
    }

    static PlayerSession playerSession(ResultSet rs) throws SQLException {
        int day = rs.getInt("death_on_day");
        Integer deathOnDay = rs.wasNull() ? null : day;

        return new PlayerSession(
                rs.getInt("session_id"),
                rs.getInt("person_id"),
                rs.getString("role"),
                deathOnDay,
                rs.getString("cause_of_death"),
                rs.getBoolean("good"),
                rs.getString("note"));
    }

    static Script script(ResultSet rs) throws SQLException {
        return new Script(rs.getString("name"), rs.getString("json"));
    }

    static Session session(ResultSet rs) throws SQLException {
        return new Session(
                LocalDate.parse(rs.getString("date")),
                rs.getInt("storyteller_id"),
                rs.getBoolean("good_won"),
                rs.getInt("script_id"),
                rs.getString("note"));
    }

    static PlayerSummary playerSummary(ResultSet rs) throws SQLException {
        return new PlayerSummary(
                rs.getInt("id"),
                rs.getString("name"),
                mapIfNotNull(rs.getString("first_game"), LocalDate::parse),
                mapIfNotNull(rs.getString("last_game"), LocalDate::parse),
                rs.getInt("games_played"),
                rs.getInt("games_storytold"));
    }

    static ScriptSummary scriptSummary(ResultSet rs) throws SQLException {
        return new ScriptSummary(
                rs.getInt("id"),
                rs.getString("name"),
                mapIfNotNull(rs.getString("first_played"), LocalDate::parse),
                mapIfNotNull(rs.getString("last_played"), LocalDate::parse),
                rs.getInt("times_played"),
                rs.getInt("good_wins"));
    }

    static SessionSummary sessionSummary(ResultSet rs) throws SQLException {
        return new SessionSummary(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getBoolean("good_won"),
                rs.getString("storyteller"),
                rs.getString("script_name"),
                rs.getInt("player_count"));
    }

    private static <In, Out> @Nullable Out mapIfNotNull(@Nullable In in, Function<In, Out> map) {
        if (in != null) {
            return map.apply(in);
        } else {
            return null;
        }
    }
}
