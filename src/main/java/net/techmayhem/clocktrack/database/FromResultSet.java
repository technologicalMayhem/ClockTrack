package net.techmayhem.clocktrack.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/// Maps a row of a [ResultSet] to an object.
///
/// @param <T> The type of the resulting object
@FunctionalInterface
interface FromResultSet<T> {
    /// Maps the current row of the given result set.
    ///
    /// @param rs The result set, positioned at the row to map
    /// @return The mapped object
    /// @throws SQLException If a column cannot be read
    T map(ResultSet rs) throws SQLException;
}
