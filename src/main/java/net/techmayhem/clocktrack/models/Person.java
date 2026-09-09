package net.techmayhem.clocktrack.models;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Person {
    @NotNull
    public String name;

    public Person(@NotNull String name) {
        this.name = name;
    }

    public static Person map(ResultSet rs) throws SQLException {
        return new Person(rs.getString("name"));
    }
}

