package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public record Player(String name, @Nullable String notes) {}
