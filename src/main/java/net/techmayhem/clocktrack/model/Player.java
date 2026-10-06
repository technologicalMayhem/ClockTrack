package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public record Player(PlayerName name, @Nullable String notes) {}
