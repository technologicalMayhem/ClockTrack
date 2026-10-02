package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public record Script(String name, @Nullable String json) {}
