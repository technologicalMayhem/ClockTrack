package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/// A summary of a script and its statistics, as shown in the scripts overview.
///
/// The statistics are computed from all sessions when the summary is retrieved.
///
/// @param id The id of the script
/// @param name The name of the script
/// @param firstPlayed The date the script was first played, or null if it has never been played
/// @param lastPlayed The date the script was last played, or null if it has never been played
/// @param timesPlayed The number of sessions played with the script
/// @param goodWins The number of those sessions the good side won
public record ScriptSummary(
        int id,
        String name,
        @Nullable LocalDate firstPlayed,
        @Nullable LocalDate lastPlayed,
        int timesPlayed,
        int goodWins) {}
