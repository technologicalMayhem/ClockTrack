package net.techmayhem.clocktrack.ui;

import java.util.HashMap;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/// A section of the main window for one kind of entity, like players.
///
/// The section is a tab in the main window with tabs of its own. The first one is the overview, which stays open.
/// Every screen opened from the overview, like an editor, gets another tab that can be closed again.
public class EntitySection extends Screen {
    private final HashMap<Tab, Screen> screens;
    private final TabPane tabPane;
    private final String name;

    /// Creates a section with the overview created by the given factory.
    ///
    /// @param factory The factory that creates the overview
    public EntitySection(OverviewFactory factory) {
        EntityOverview overview = factory.create(screen -> addScreen(screen, true));
        this.screens = new HashMap<>();
        this.tabPane = new TabPane();
        this.name = overview.getSectionName();
        addScreen(overview, false);
    }

    @Override
    protected void onShow() {
        Screen screen = screens.get(tabPane.getSelectionModel().getSelectedItem());
        if (screen == null) return;
        screen.onShow();
    }

    @Override
    protected void onHide() {
        Screen screen = screens.get(tabPane.getSelectionModel().getSelectedItem());
        if (screen == null) return;
        screen.onHide();
    }

    @Override
    protected Parent getView() {
        return tabPane;
    }

    @Override
    protected String getName() {
        return name;
    }

    public boolean isDirty() {
        for (Screen screen : screens.values()) {
            if (screen.isDirty()) {
                return true;
            }
        }
        return false;
    }

    private void addScreen(Screen screen, boolean closeable) {
        Tab tab = screen.getTab();
        screens.put(tab, screen);
        tab.setOnSelectionChanged(_ -> {
            if (tab.isSelected()) {
                screen.onShow();
            } else {
                screen.onHide();
            }
        });
        tab.setOnCloseRequest(_ -> screen.onHide());
        tab.setOnClosed(_ -> screens.remove(tab));
        tab.setClosable(closeable);

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        screen.onShow();
    }
}
