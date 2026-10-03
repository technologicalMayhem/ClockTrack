@org.jspecify.annotations.NullMarked
module net.techmayhem.clocktrack {
    requires java.sql;
    requires javafx.controls;
    requires static org.jspecify;
    requires org.xerial.sqlitejdbc;

    exports net.techmayhem.clocktrack to
            javafx.graphics;
}
