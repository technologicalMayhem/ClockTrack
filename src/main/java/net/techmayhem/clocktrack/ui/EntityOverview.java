package net.techmayhem.clocktrack.ui;

import java.util.function.Consumer;

public abstract class EntityOverview extends Screen {
    protected final Consumer<Screen> createTabCallback;

    protected EntityOverview(Consumer<Screen> createTabCallback) {
        this.createTabCallback = createTabCallback;
    }

    @Override
    protected String getName() {
        return "Overview";
    }

    protected abstract String getSectionName();
}
