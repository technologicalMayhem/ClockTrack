package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;

public record Script(String name, @Nullable String json) {

    public static Script map(ResultSet rs) throws SQLException {
        return new Script(rs.getString("name"), rs.getString("json"));
    }
}
