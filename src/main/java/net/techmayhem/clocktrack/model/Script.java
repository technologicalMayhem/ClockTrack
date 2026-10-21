package net.techmayhem.clocktrack.model;

/// Represents a Blood on the Clocktower script.
///
/// @param name The name of the script
/// @param json The JSON representation of the script. Scripts are expected to follow the [BOTC script
/// schema](https://github.com/ThePandemoniumInstitute/botc-release/blob/main/script-schema.json).
public record Script(String name, String json) {}
