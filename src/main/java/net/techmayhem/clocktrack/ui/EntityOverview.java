package net.techmayhem.clocktrack.ui;

import static net.techmayhem.clocktrack.ui.I18n.t;

public abstract class EntityOverview extends Screen {
    protected final ScreenHost screenHost;

    protected EntityOverview(ScreenHost screenHost) {
        this.screenHost = screenHost;
    }

    @Override
    protected String getName() {
        return t("overview.genericName");
    }

    protected abstract String getSectionName();
}
