package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record ScriptSummary(
        int id,
        String name,
        @Nullable LocalDate firstPlayed,
        @Nullable LocalDate lastPlayed,
        int timesPlayed,
        int goodWins) {}
