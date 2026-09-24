package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public record PersonSession(int sessionId, int personId, String role, @Nullable Integer deathOnDay,
                            @Nullable String causeOfDeath, boolean good, @Nullable String note) {

    public static PersonSession map(ResultSet rs) throws SQLException {
        return new PersonSession(
                rs.getInt("session_id"),
                rs.getInt("person_id"),
                rs.getString("role"),
                rs.getObject("death_on_day", Integer.class),
                rs.getString("cause_of_death"),
                rs.getBoolean("good"),
                rs.getString("note")
        );
    }
}
