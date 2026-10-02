package net.techmayhem.clocktrack.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record Session(
        LocalDate date,
        int storyteller,
        boolean goodWon,
        int script,
        @Nullable String note) {}
