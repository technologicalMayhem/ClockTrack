package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

/// A player that can partake in a game session
/// @param name The player's name
/// @param notes Any notes about the player
public record Player(String name, @Nullable String notes) {}
