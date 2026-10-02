package net.techmayhem.clocktrack.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import net.techmayhem.clocktrack.model.*;

public class RowMappers {
    public static <T> FromDb<T> fromDb(ResultSet rs, FromResultSet<T> mapper) throws SQLException {
        return new FromDb<>(rs.getInt("id"), mapper.map(rs));
    }

    public static Person person(ResultSet rs) throws SQLException {
        return new Person(rs.getString("name"));
    }

    public static PersonSession personSession(ResultSet rs) throws SQLException {
        int day = rs.getInt("death_on_day");
        Integer deathOnDay = rs.wasNull() ? null : day;

        return new PersonSession(
                rs.getInt("session_id"),
                rs.getInt("person_id"),
                rs.getString("role"),
                deathOnDay,
                rs.getString("cause_of_death"),
                rs.getBoolean("good"),
                rs.getString("note"));
    }

    public static Script script(ResultSet rs) throws SQLException {
        return new Script(rs.getString("name"), rs.getString("json"));
    }

    public static Session session(ResultSet rs) throws SQLException {
        return new Session(
                LocalDate.parse(rs.getString("date")),
                rs.getInt("storyteller"),
                rs.getBoolean("good_won"),
                rs.getInt("script"),
                rs.getString("note"));
    }
}
