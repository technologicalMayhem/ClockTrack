package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

/// Represents a player's state in a [Session]. A `PlayerSession` holds all data that is specific to a player within a
/// session.
///
/// The following properties are true for any valid `PlayerSession`:
/// - The combination of [sessionId] and [playerId] is unique among all `PlayerSession`s.
/// - A non-null [deathInPhase] requires a non-null [causeOfDeath]. And vice versa.
///
/// The [playerId] may be null. This occurs when a player has been anonymized and removed from the database.
///
/// @param sessionId The id of the session
/// @param playerId The id of the player, or null if the player has been removed
/// @param role The role of the player in the session
/// @param deathInPhase The phase in which the player died, or null if they survived
/// @param causeOfDeath The cause of the player's death, or null if they survived
/// @param good Whether the player was good
/// @param note Additional notes about the player's session
public record PlayerSession(
        int sessionId,
        @Nullable Integer playerId,
        String role,
        @Nullable Integer deathInPhase,
        @Nullable String causeOfDeath,
        boolean good,
        @Nullable String note) {

    /// Returns a copy with the given session id.
    ///
    /// @param sessionId The new session id
    /// @return A copy with the given session id
    public PlayerSession withSessionId(int sessionId) {
        return new PlayerSession(
                sessionId, this.playerId, this.role, this.deathInPhase, this.causeOfDeath, this.good, this.note);
    }
}
