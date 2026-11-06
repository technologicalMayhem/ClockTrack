package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Represents the games played on a script, as needed to compute its statistics.
///
/// @param sessions The sessions played on the script, one entry per session
/// @param seats The seats of those sessions, one entry per player per session
public record ScriptStatistics(List<Session> sessions, List<Seat> seats) {

    /// Represents a session played on the script.
    ///
    /// @param date The date of the session
    /// @param goodWon Whether the good team won
    /// @param storytellerId The id of the storyteller, or null if unknown
    public record Session(
            LocalDate date, boolean goodWon, @Nullable Integer storytellerId) {}

    /// Represents a seat in a session played on the script.
    ///
    /// @param sessionId The id of the session
    /// @param playerId The id of the player, or null if the player was removed
    /// @param role The role identifier of the player's final character
    /// @param good Whether the player was on the good team at the end
    /// @param goodWon Whether the good team won the session
    public record Seat(int sessionId, @Nullable Integer playerId, String role, boolean good, boolean goodWon) {}
}
