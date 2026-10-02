package net.techmayhem.clocktrack.ui;

public abstract class EntityOverview extends Screen {
    protected final ScreenHost screenHost;

    protected EntityOverview(ScreenHost screenHost) {
        this.screenHost = screenHost;
    }

    @Override
    protected String getName() {
        return "Overview";
    }

    protected abstract String getSectionName();
}
