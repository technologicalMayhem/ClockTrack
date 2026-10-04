package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record PlayerSummary(
        int id,
        String name,
        @Nullable LocalDate firstGame,
        @Nullable LocalDate lastGame,
        int gamesPlayed,
        int gamesStorytold) {}
