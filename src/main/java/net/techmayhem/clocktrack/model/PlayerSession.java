package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public record PlayerSession(
        int sessionId,
        @Nullable Integer playerId,
        String role,
        @Nullable Integer deathInPhase,
        @Nullable String causeOfDeath,
        boolean good,
        @Nullable String note) {

    public PlayerSession withSessionId(int sessionId) {
        return new PlayerSession(
                sessionId, this.playerId, this.role, this.deathInPhase, this.causeOfDeath, this.good, this.note);
    }
}
