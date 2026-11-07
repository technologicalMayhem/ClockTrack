package net.techmayhem.clocktrack.gamedata;

import java.util.*;
import net.techmayhem.clocktrack.model.Script;
import org.jspecify.annotations.Nullable;

/// The roles and causes of death that are valid in a session on a given script: the script's characters plus all
/// official Travellers. A script's own definition of a character replaces the official one.
public final class SessionRoles {
    /// Death by execution.
    public static final String EXECUTION = "execution";
    /// An unknown cause of death.
    public static final String UNKNOWN = "unknown";

    private static final List<Team> CAUSE_ORDER =
            List.of(Team.DEMON, Team.MINION, Team.TOWNSFOLK, Team.OUTSIDER, Team.TRAVELLER);

    private final Map<String, Role> roles;

    private SessionRoles(Map<String, Role> roles) {
        this.roles = roles;
    }

    /// Builds an instance of SessionRoles with all roles from the given script and official roles.
    /// @param script The script to build the roles from.
    /// @param official The official roles to use.
    /// @return A new instance of SessionRoles.
    /// @throws ScriptFormatException If the script is not formatted correctly.
    public static SessionRoles of(Script script, Roles official) throws ScriptFormatException {
        Map<String, Role> roles =
                new LinkedHashMap<>(ScriptParser.parse(script.json(), official).roles());
        for (Role traveller : official.ofTeam(Team.TRAVELLER)) {
            roles.putIfAbsent(traveller.id(), traveller);
        }
        return new SessionRoles(Collections.unmodifiableMap(roles));
    }

    /// The roles a player can have, sorted by team and in script order within a team. Fabled and Loric are left out.
    /// @return A list of roles that a player can have, sorted by team and in script order within a team.
    public List<Role> roleChoices() {
        return roles.values().stream()
                .filter(role -> CAUSE_ORDER.contains(role.team()))
                .sorted(Comparator.comparing(Role::team))
                .toList();
    }

    /// The causes of death: execution, unknown, then the characters with Demons and Minions first.
    /// @return A list of causes of death
    public List<String> causeChoices() {
        List<String> causes = new ArrayList<>(List.of(EXECUTION, UNKNOWN));
        roleChoices().stream()
                .sorted(Comparator.comparingInt(role -> CAUSE_ORDER.indexOf(role.team())))
                .map(Role::id)
                .forEach(causes::add);
        return causes;
    }

    /// Checks if a given role ID is in this session.
    /// @param id The role ID to check.
    /// @return True if the role ID is valid and in this game.
    public boolean isValidRole(String id) {
        Role role = roles.get(id);
        return role != null && CAUSE_ORDER.contains(role.team());
    }

    /// Checks if a given cause ID is valid for this session.
    /// @param id The cause ID to check.
    /// @return True if the cause ID is valid and in this game.
    public boolean isValidCause(String id) {
        return id.equals(EXECUTION) || id.equals(UNKNOWN) || isValidRole(id);
    }

    /// Retrieves a role by its ID.
    /// @param id The ID of the role to retrieve.
    /// @return The role with the specified ID, or null if not found.
    public @Nullable Role get(String id) {
        return roles.get(id);
    }
}
