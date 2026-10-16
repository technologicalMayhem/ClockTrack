package net.techmayhem.clocktrack.ui;

/// Creates the overview of an [EntitySection].
///
/// The overview needs a [ScreenHost] to open further screens, and that host is the section itself. So the section
/// creates the overview through this factory while it is being constructed.
@FunctionalInterface
public interface OverviewFactory {
    /// Creates the overview.
    ///
    /// @param host The host that opens further screens of the overview
    /// @return The overview
    EntityOverview create(ScreenHost host);
}
