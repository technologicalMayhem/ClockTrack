package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

/// Represents a players' state in a session.
///
/// The [sessionId] and [playerId] must be unique among all PlayerSessions.
///
/// TODO: Document this properly
///
/// @param sessionId The unique identifier for the session
/// @param playerId The unique identifier for the player
/// @param role The role of the player in the session
/// @param deathInPhase The phase in which the player died
/// @param causeOfDeath The cause of the player's death
/// @param good Whether the player was good or evil
/// @param note Additional notes about the player's session
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
