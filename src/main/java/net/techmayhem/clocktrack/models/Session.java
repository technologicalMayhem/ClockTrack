package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public record Session(LocalDate date, int storyteller, boolean goodWon, int script, @Nullable String note) {

    public static Session map(ResultSet rs) throws SQLException {
        return new Session(
                LocalDate.parse(rs.getString("date")),
                rs.getInt("storyteller"),
                rs.getBoolean("good_won"),
                rs.getInt("script"),
                rs.getString("note")
        );
    }
}
