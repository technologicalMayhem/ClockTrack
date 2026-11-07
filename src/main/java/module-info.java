/// ClockTrack, a desktop app for tracking games of Blood on the Clocktower. It records players, scripts, and sessions
/// in a local SQLite database and shows statistics about them.
@org.jspecify.annotations.NullMarked
module net.techmayhem.clocktrack {
    requires java.sql;
    requires javafx.controls;
    requires static org.jspecify;
    requires org.xerial.sqlitejdbc;
    requires static org.jetbrains.annotations;
    requires com.google.gson;

    exports net.techmayhem.clocktrack to
            javafx.graphics;
}
