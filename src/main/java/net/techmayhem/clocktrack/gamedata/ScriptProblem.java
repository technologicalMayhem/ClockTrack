package net.techmayhem.clocktrack.gamedata;

/// A problem found while parsing a script.
///
/// @param entry The 1-based position of the entry in the script's top-level array, counting the `_meta` entry, or 0
/// if the problem concerns the whole script
/// @param type The kind of problem
/// @param detail Additional information that depends on the type, see [ProblemType]
public record ScriptProblem(int entry, ProblemType type, String detail) {}
