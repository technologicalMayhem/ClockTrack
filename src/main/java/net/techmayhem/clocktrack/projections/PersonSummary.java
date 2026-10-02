package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record PersonSummary(
        int id,
        String name,
        @Nullable LocalDate firstGame,
        @Nullable LocalDate lastGame,
        int games_played,
        int games_storytold) {}
