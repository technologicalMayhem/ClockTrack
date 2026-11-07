package net.techmayhem.clocktrack.gamedata;

import java.util.List;

/// Gets thrown when the [ScriptParser] fails to parse a script. Contains a list of one or more problems describing
/// the issue.
public class ScriptFormatException extends Exception {
    private final List<ScriptProblem> problems;

    /// @param problems The problems found whilst attempting to parse a script
    public ScriptFormatException(List<ScriptProblem> problems) {
        super(problems.size() + " problem(s) in script");
        this.problems = List.copyOf(problems);
    }

    /// @return The problems found whilst attempting to parse a script
    public List<ScriptProblem> problems() {
        return problems;
    }
}
