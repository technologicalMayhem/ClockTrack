package net.techmayhem.clocktrack.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record Session(
        LocalDate date,
        @Nullable Integer storytellerId,
        boolean goodWon,
        int scriptId,
        @Nullable String note) {}
