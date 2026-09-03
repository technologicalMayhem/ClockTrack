package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface FromResultSet<T> {
    T map(ResultSet rs) throws SQLException;
}
