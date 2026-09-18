package net.techmayhem.clocktrack.screens;

import java.util.function.Consumer;

public abstract class Overview extends Screen {
    protected final Consumer<Screen> createTabCallback;

    protected Overview(Consumer<Screen> createTabCallback) {
        this.createTabCallback = createTabCallback;
    }

    @Override
    protected String getName() {
        return "Overview";
    }

    abstract protected String getSectionName();
}
