package net.techmayhem.clocktrack.screens;

import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TabbedEntityEditor extends Screen {
    private final Overview overview;
    private final List<Screen> screens;
    private final TabPane tabPane;
    private final String name;

    public TabbedEntityEditor(Function<Consumer<Screen>, ? extends Overview> overviewFactory) {
        this.overview = overviewFactory.apply(this::addScreen);
        this.screens = new ArrayList<>();
        this.tabPane = new TabPane();
        this.name = overview.getSectionName();

        Tab overviewTab = overview.buildTab();
        overviewTab.setClosable(false);
        tabPane.getTabs().addFirst(overviewTab);
        overview.onShow();
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
        for (Screen screen : screens) {
            if (screen.isDirty()) {
                return true;
            }
        }
        return false;
    }

    private void addScreen(Screen screen) {
        screens.add(screen);

        Tab tab = new Tab(screen.getName(), screen.getView());
        tab.setOnCloseRequest(_ -> screen.onHide());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        screen.onShow();
    }
}