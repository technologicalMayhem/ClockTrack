package net.techmayhem.clocktrack.ui;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import org.jspecify.annotations.Nullable;

/// A screen of the UI, shown in a tab of its own.
public abstract class Screen {
    @Nullable private Tab tab;

    /// Creates the screen. Its tab is only created on the first call to [#getTab()].
    protected Screen() {}

    /// Returns the content of the screen. It is called once, when the tab is created.
    ///
    /// @return The content of the screen
    protected abstract Parent getView();

    /// Returns the name of the screen, shown as the title of its tab.
    ///
    /// @return The name of the screen
    protected abstract String getName();

    /// Called when the screen is shown. Does nothing by default.
    protected void onShow() {}

    /// Called when the screen is hidden, including when its tab is about to close. Does nothing by default.
    protected void onHide() {}

    /// Returns the tab that shows this screen. The tab is created on the first call, and [#onShow()] and [#onHide()]
    /// are called when it is selected or deselected.
    ///
    /// @return The tab of the screen
    public Tab getTab() {
        if (tab == null) {
            tab = new Tab(getName(), getView());
            tab.setOnSelectionChanged(_ -> {
                if (tab != null && tab.isSelected()) {
                    onShow();
                } else {
                    onHide();
                }
            });
        }
        return tab;
    }

    /// Closes the tab of the screen, the same way the user would. Has no effect if the tab has not been created yet.
    ///
    /// Closing it this way runs the close handlers of the tab, so the screen is cleaned up like any other closed
    /// screen. A close request handler can still cancel the close.
    protected void closeTab() {
        if (tab == null) return;
        EventHandler<Event> request = tab.getOnCloseRequest();
        if (request != null) {
            Event e = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);
            request.handle(e);
            if (e.isConsumed()) return; // handler vetoed the close
        }

        tab.getTabPane().getTabs().remove(tab);

        EventHandler<Event> closed = tab.getOnClosed();
        if (closed != null) {
            closed.handle(new Event(Tab.CLOSED_EVENT));
        }
    }
}
