package net.techmayhem.clocktrack.projections;

/// Represents how many of a number of games were won.
///
/// @param games The number of games
/// @param wins The number of those games that were won
public record Rate(int games, int wins) {}
