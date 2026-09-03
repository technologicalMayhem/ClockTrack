package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Person {
    public String name;

    public Person(String name) {
        this.name = name;
    }

    public static Person map(ResultSet rs) throws SQLException {
        return new Person(rs.getString("name"));
    }
}

