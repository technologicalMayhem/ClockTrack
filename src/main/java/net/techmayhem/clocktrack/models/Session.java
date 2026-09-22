package net.techmayhem.clocktrack.models;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Session {
    private LocalDate date;
    private int storyteller;
    private boolean goodWon;
    private int script;
    @Nullable
    private String note;

    public Session(LocalDate date, int storyteller, boolean goodWon, int script, @Nullable String note) {
        this.setDate(date);
        this.setStoryteller(storyteller);
        this.setGoodWon(goodWon);
        this.setScript(script);
        this.setNote(note);
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getStoryteller() {
        return storyteller;
    }

    public void setStoryteller(int storyteller) {
        this.storyteller = storyteller;
    }

    public boolean isGoodWon() {
        return goodWon;
    }

    public void setGoodWon(boolean goodWon) {
        this.goodWon = goodWon;
    }

    public int getScript() {
        return script;
    }

    public void setScript(int script) {
        this.script = script;
    }

    public @Nullable String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }
}
