@org.jspecify.annotations.NullMarked
module net.techmayhem.clocktrack {
    requires java.sql;
    requires javafx.controls;
    requires org.tinylog.api;
    requires static org.jspecify;

    exports net.techmayhem.clocktrack;
}