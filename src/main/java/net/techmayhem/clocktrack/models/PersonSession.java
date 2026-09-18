package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonSession {
    public final int sessionId;
    public final int personId;
    public String role;
    @Nullable
    public Integer deathOnDay;
    @Nullable
    public String causeOfDeath;
    public boolean good;
    @Nullable
    public String note;

    public PersonSession(int sessionId, int personId, String role, @Nullable Integer deathOnDay, @Nullable String causeOfDeath, boolean good, @Nullable String note) {
        this.sessionId = sessionId;
        this.personId = personId;
        this.role = role;
        this.deathOnDay = deathOnDay;
        this.causeOfDeath = causeOfDeath;
        this.good = good;
        this.note = note;
    }

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
