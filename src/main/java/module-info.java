@org.jspecify.annotations.NullMarked
module ClockTrack {
    requires java.sql;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.jspecify;
    requires org.tinylog.api;

    exports net.techmayhem.clocktrack;
}