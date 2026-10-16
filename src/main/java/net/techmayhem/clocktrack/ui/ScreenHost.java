package net.techmayhem.clocktrack.ui;

/// Function exposed from a [Screen] hosting other screens.
@FunctionalInterface
public interface ScreenHost {
    /// Opens the given screen and shows it.
    ///
    /// @param screen The screen to open
    void open(Screen screen);
}
