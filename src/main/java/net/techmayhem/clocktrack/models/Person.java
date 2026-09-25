package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Person(String name) {

    public static Person map(ResultSet rs) throws SQLException {
        return new Person(rs.getString("name"));
    }

    @Override
    public String toString() {
        return name;
    }
}

