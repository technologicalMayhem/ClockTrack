package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Script(String name, @Nullable String json) {

    public static Script map(ResultSet rs) throws SQLException {
        return new Script(
                rs.getString("name"),
                rs.getString("json")
        );
    }

    @Override
    public String toString() {
        return name;
    }
}
