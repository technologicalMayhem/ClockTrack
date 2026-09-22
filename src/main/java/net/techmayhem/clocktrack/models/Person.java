package net.techmayhem.clocktrack.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Person {
    private String name;

    public Person(String name) {
        this.setName(name);
    }

    public static Person map(ResultSet rs) throws SQLException {
        return new Person(rs.getString("name"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

