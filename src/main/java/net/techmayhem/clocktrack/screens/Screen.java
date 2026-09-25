package net.techmayhem.clocktrack.screens;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import org.jspecify.annotations.Nullable;

public abstract class Screen {
    @Nullable
    private Tab tab;

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

    public Tab getTab() {
        if (tab == null) {
            tab = new Tab(getName(), getView());
            tab.setOnSelectionChanged(_ -> {
                if (tab.isSelected()) {
                    onShow();
                } else {
                    onHide();
                }
            });
        }
        return tab;
    }

    protected void closeTab() {
        if (tab == null) return;
        EventHandler<Event> request = tab.getOnCloseRequest();
        if (request != null) {
            Event e = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);
            request.handle(e);
            if (e.isConsumed()) return;   // handler vetoed the close
        }

        tab.getTabPane().getTabs().remove(tab);

        EventHandler<Event> closed = tab.getOnClosed();
        if (closed != null) {
            closed.handle(new Event(Tab.CLOSED_EVENT));
        }
    }
}
