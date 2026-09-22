package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonSession {
    private final int sessionId;
    private final int personId;
    private String role;
    @Nullable
    private Integer deathOnDay;
    @Nullable
    private String causeOfDeath;
    private boolean good;
    @Nullable
    private String note;

    public PersonSession(int sessionId, int personId, String role, @Nullable Integer deathOnDay, @Nullable String causeOfDeath, boolean good, @Nullable String note) {
        this.sessionId = sessionId;
        this.personId = personId;
        this.setRole(role);
        this.setDeathOnDay(deathOnDay);
        this.setCauseOfDeath(causeOfDeath);
        this.setGood(good);
        this.setNote(note);
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public @Nullable Integer getDeathOnDay() {
        return deathOnDay;
    }

    public void setDeathOnDay(@Nullable Integer deathOnDay) {
        this.deathOnDay = deathOnDay;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getPersonId() {
        return personId;
    }

    public @Nullable String getCauseOfDeath() {
        return causeOfDeath;
    }

    public void setCauseOfDeath(@Nullable String causeOfDeath) {
        this.causeOfDeath = causeOfDeath;
    }

    public boolean isGood() {
        return good;
    }

    public void setGood(boolean good) {
        this.good = good;
    }

    public @Nullable String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }
}
