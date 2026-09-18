package net.techmayhem.clocktrack.screens;

import javafx.scene.Parent;
import javafx.scene.control.Tab;

public abstract class Screen {

    protected Screen() {
    }

    protected abstract Parent getView();

    protected abstract String getName();

    void onShow() {
    }

    void onHide() {
    }

    boolean isDirty() {
        return false;
    }

    public Tab buildTab() {
        return new Tab(getName(), getView());
    }
}
