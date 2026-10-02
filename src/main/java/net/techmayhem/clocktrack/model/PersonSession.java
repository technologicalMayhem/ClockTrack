package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public record PersonSession(
        int sessionId,
        int personId,
        String role,
        @Nullable Integer deathOnDay,
        @Nullable String causeOfDeath,
        boolean good,
        @Nullable String note) {

    public PersonSession withSessionId(int sessionId) {
        return new PersonSession(
                sessionId, this.personId, this.role, this.deathOnDay, this.causeOfDeath, this.good, this.note);
    }
}
