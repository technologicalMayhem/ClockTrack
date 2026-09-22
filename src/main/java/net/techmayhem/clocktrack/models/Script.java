package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Script {
    private String name;
    @Nullable
    private String json;

    public Script(String name, @Nullable String json) {
        this.setName(name);
        this.setJson(json);
    }

    public static Script map(ResultSet rs) throws SQLException {
        return new Script(
                rs.getString("name"),
                rs.getString("json")
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable String getJson() {
        return json;
    }

    public void setJson(@Nullable String json) {
        this.json = json;
    }
}
