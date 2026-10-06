package net.techmayhem.clocktrack.projections;

import java.time.LocalDate;
import net.techmayhem.clocktrack.model.PlayerName;

public record SessionSummary(
        int id, LocalDate date, boolean goodWon, PlayerName storyteller, String scriptName, int playerCount) {}
