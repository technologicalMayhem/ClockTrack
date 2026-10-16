package net.techmayhem.clocktrack.ui;

import static net.techmayhem.clocktrack.ui.I18n.localize;

/// A screen that lists all entities of one kind, like all players. It is the first tab of an [EntitySection] and
/// cannot be closed.
public abstract class EntityOverview extends Screen {
    /// The host for further screens opened from the overview, like editors.
    protected final ScreenHost screenHost;

    /// @param screenHost The host for further screens opened from the overview
    protected EntityOverview(ScreenHost screenHost) {
        this.screenHost = screenHost;
    }

    @Override
    protected String getName() {
        return localize("overview.genericName");
    }

    /// Returns the name of the section, shown as the title of the section's tab in the main window.
    ///
    /// @return The name of the section
    protected abstract String getSectionName();
}
