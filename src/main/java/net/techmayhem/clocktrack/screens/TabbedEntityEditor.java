package net.techmayhem.clocktrack.screens;

import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class TabbedEntityEditor extends Screen {
    private final Overview overview;
    private final HashMap<Tab, Screen> screens;
    private final TabPane tabPane;
    private final String name;

    public TabbedEntityEditor(Function<Consumer<Screen>, ? extends Overview> overviewFactory) {
        this.overview = overviewFactory.apply(screen -> addScreen(screen, true));
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
        tab.setClosable(closeable);

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        screen.onShow();
    }
}