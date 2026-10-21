package net.techmayhem.clocktrack.gamedata;

/// The kinds of problems [ScriptParser] can find in a script.
///
/// Each problem is reported as a [ScriptProblem]. Its `entry` is the 1-based position of the offending entry in the
/// script's top-level array, with the `_meta` entry counted like any other. What its `detail` holds depends on the
/// type and is listed on each constant.
///
/// [#SYNTAX] and [#NOT_AN_ARRAY] concern the whole script. They stop parsing, are always reported alone and use 0 as
/// their entry. All other problems are collected for the whole script, and a single entry can have several of them.
/// An entry with a problem is left out of the duplicate check, so it never causes a [#DUPLICATE_ID].
public enum ProblemType {
    /// The text is not valid JSON.
    ///
    /// Detail: the line number of the error or an empty string if the line is unknown.
    SYNTAX,

    /// The text is valid JSON, but its top-level value is not an array.
    ///
    /// Detail: an empty string.
    NOT_AN_ARRAY,

    /// An entry is neither a string nor an object.
    ///
    /// Detail: the entry as JSON text.
    INVALID_ENTRY,

    /// A required field of an object entry is missing or its value is not a string.
    ///
    /// Every object entry needs an `id`. An object that has a `name`, `team` or `ability` is a homebrew character
    /// and needs all three of them as well. The fields of the `_meta` entry are optional.
    ///
    /// Detail: the name of the field.
    MISSING_FIELD,

    /// A reference to an official character uses an identifier that is not in the bundled character data.
    ///
    /// References are string entries and objects with an `id` but none of `name`, `team` or `ability`.
    ///
    /// Detail: the identifier as written in the script.
    UNKNOWN_ROLE,

    /// The identifier of a homebrew character is empty or contains something other than the lowercase letters `a-z`
    /// and the digits `0-9`.
    ///
    /// Detail: the identifier as written in the script.
    INVALID_ID,

    /// The `team` of a homebrew character is not one of `townsfolk`, `outsider`, `minion`, `demon`, `traveller`,
    /// `fabled` or `loric`.
    ///
    /// Case is ignored, so `Townsfolk` and `TOWNSFOLK` are valid as well.
    ///
    /// Detail: the team as written in the script.
    INVALID_TEAM,

    /// An entry resolves to the same character identifier as an earlier entry.
    ///
    /// Detail: the resolved identifier.
    DUPLICATE_ID
}
