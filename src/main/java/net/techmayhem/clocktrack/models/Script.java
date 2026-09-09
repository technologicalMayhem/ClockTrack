package net.techmayhem.clocktrack.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Script {
    @NotNull
    public String name;
    @Nullable
    public String json;

    public Script(@NotNull String name, @Nullable String json) {
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
