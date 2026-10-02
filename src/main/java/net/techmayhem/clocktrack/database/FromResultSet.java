package net.techmayhem.clocktrack.database;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface FromResultSet<T> {
    T map(ResultSet rs) throws SQLException;
}
