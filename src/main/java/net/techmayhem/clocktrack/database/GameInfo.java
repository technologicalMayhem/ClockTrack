package net.techmayhem.clocktrack.database;

import java.time.LocalDate;

/// Represents a single game of a player, as needed to compute their statistics.
///
/// @param sessionId The id of the session
/// @param date The date of the session
/// @param good Whether the player was on the good team at the end of the game
/// @param won Whether the player's team won
/// @param survived Whether the player was still alive at the end of the game
record GameInfo(int sessionId, LocalDate date, boolean good, boolean won, boolean survived) {}
