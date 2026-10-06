package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record SessionSummary(
        int id, LocalDate date, boolean goodWon, @Nullable String storyteller, String scriptName, int playerCount) {}
