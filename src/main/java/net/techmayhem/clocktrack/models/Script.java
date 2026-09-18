package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Script {
    public String name;
    @Nullable
    public String json;

    public Script(String name, @Nullable String json) {
        this.name = name;
        this.json = json;
    }

    public static Script map(ResultSet rs) throws SQLException {
        return new Script(
                rs.getString("name"),
                rs.getString("json")
        );
    }
}
