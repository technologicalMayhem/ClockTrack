package net.techmayhem.clocktrack.screens;

import javafx.scene.Parent;
import javafx.scene.control.Tab;

public abstract class Screen {

    protected Screen() {
    }

    protected abstract Parent getView();

    protected abstract String getName();

    protected void onShow() {
    }

    protected void onHide() {
    }

    boolean isDirty() {
        return false;
    }

    public Tab buildTab() {
        Tab tab = new Tab(getName(), getView());
        tab.setOnSelectionChanged(_ -> {
            if (tab.isSelected()) {
                onShow();
            } else {
                onHide();
            }
        });
        return tab;
    }
}
