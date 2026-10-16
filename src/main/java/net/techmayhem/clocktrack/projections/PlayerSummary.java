package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/// A summary of a player and their statistics, as shown in the player overview.
///
/// The statistics are computed from all sessions when the summary is retrieved.
///
/// @param id The id of the player
/// @param name The name of the player
/// @param firstGame The date of the player's first session as player or storyteller, or null if they have none
/// @param lastGame The date of the player's last session as player or storyteller, or null if they have none
/// @param gamesPlayed The number of sessions the player took part in as a player
/// @param gamesStorytold The number of sessions the player was the storyteller of
public record PlayerSummary(
        int id,
        String name,
        @Nullable LocalDate firstGame,
        @Nullable LocalDate lastGame,
        int gamesPlayed,
        int gamesStorytold) {}
