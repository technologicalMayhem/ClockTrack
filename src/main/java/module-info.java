@org.jspecify.annotations.NullMarked
module ClockTrack {
    requires java.sql;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.tinylog.api;
    requires static org.jspecify;

    exports net.techmayhem.clocktrack;
}