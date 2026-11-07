package net.techmayhem.clocktrack.gamedata;

/// A Blood on the Clocktower character. Characters are referred to as a role in technical contexts.
///
/// @param id the unique identifier for this character
/// @param name the display name of this character
/// @param ability the ability text shown for this character
/// @param team the character type this character belongs to
public record Role(String id, String name, String ability, Team team) {}
