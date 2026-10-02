package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;

public record SessionSummary(
        int id, LocalDate date, boolean goodWon, String storyteller, String scriptName, int playerCount) {}
