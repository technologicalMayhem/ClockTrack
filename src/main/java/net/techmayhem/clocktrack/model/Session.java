package net.techmayhem.clocktrack.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/// Represents a session of Blood on the Clocktower.
///
/// @param date The date of the session
/// @param storytellerId The id of the storyteller, or null if unknown
/// @param goodWon Whether the good side won
/// @param scriptId The id of the script
/// @param note An optional note about the session
public record Session(
        LocalDate date,
        @Nullable Integer storytellerId,
        boolean goodWon,
        int scriptId,
        @Nullable String note) {}
