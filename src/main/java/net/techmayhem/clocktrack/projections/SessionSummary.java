package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/// A summary of a session, as shown in the sessions overview.
///
/// The names of the storyteller and the script and the number of players are looked up when the summary is retrieved.
///
/// @param id The id of the session
/// @param date The date of the session
/// @param goodWon Whether the good side won
/// @param storyteller The name of the storyteller, or null if unknown
/// @param scriptName The name of the script
/// @param playerCount The number of players in the session, not counting the storyteller
public record SessionSummary(
        int id, LocalDate date, boolean goodWon, @Nullable String storyteller, String scriptName, int playerCount) {}
