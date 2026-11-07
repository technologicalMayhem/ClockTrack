package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import net.techmayhem.clocktrack.gamedata.Role;
import net.techmayhem.clocktrack.gamedata.Roles;
import net.techmayhem.clocktrack.gamedata.SessionRoles;
import org.jspecify.annotations.Nullable;

/// Display names for role and cause-of-death ids in a session.
///
/// Ids are looked up in the session's script first, then in the official roles. An id that is in neither is shown
/// as it is, so an unexpected id shows up in the table instead of breaking it.
class Lookup {
    private Lookup() {}

    static String role(@Nullable SessionRoles sessionRoles, String id) {
        Role role = sessionRoles == null ? null : sessionRoles.get(id);
        if (role == null) role = Roles.getInstance().get(id);
        return role == null ? id : role.name();
    }

    static String cause(@Nullable SessionRoles sessionRoles, String id) {
        return switch (id) {
            case SessionRoles.EXECUTION -> localize("causeOfDeath.execution");
            case SessionRoles.UNKNOWN -> localize("causeOfDeath.unknown");
            default -> role(sessionRoles, id);
        };
    }
}
