package net.techmayhem.clocktrack.ui;

@FunctionalInterface
public interface OverviewFactory {
    EntityOverview create(ScreenHost host);
}
