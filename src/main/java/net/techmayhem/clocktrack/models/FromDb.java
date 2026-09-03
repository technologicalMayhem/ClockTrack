package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public record FromDb<T>(int id, T model) {
    public static <T> FromDb<T> map(ResultSet rs, FromResultSet<T> mapper) throws SQLException {
        return new FromDb<>(rs.getInt("id"), mapper.map(rs));
    }
}
