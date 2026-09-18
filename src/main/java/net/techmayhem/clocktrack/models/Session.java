package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Session {
    public LocalDate date;
    public int storyteller;
    public boolean goodWon;
    public int script;
    @Nullable
    public String note;

    public Session(LocalDate date, int storyteller, boolean goodWon, int script, @Nullable String note) {
        this.date = date;
        this.storyteller = storyteller;
        this.goodWon = goodWon;
        this.script = script;
        this.note = note;
    }

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
