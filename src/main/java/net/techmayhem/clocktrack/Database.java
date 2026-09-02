package net.techmayhem.clocktrack;

import java.sql.*;

public class Database implements AutoCloseable {
    private Connection connection;

    private static Database instance;

    private Database() throws SQLException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:ClockTrack.db");
        } catch (SQLException e) {
            System.err.println("Fatal: could not connect to database: " + e.getMessage());
            System.exit(1);
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            try {
                instance = new Database();
            } catch (SQLException e) {
                System.err.println("Could not connect to database: " + e.getMessage());
                System.exit(1);
            }
        }
        return instance;
    }

    public void initSchema() throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute("""
                CREATE TABLE person(
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL
                );
                """);
        statement.execute("""
                CREATE TABLE script(
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    json TEXT
                );
                """);
        statement.execute("""
                CREATE TABLE session(
                    id INTEGER PRIMARY KEY,
                    date DATE NOT NULL,
                    storyteller INTEGER NOT NULL,
                    good_won BOOLEAN NOT NULL,
                    script INTEGER NOT NULL,
                    note TEXT,
                    FOREIGN KEY(storyteller) REFERENCES person(id),
                    FOREIGN KEY(script) REFERENCES script(id)
                );
                """);
        statement.execute("""
                CREATE TABLE person_session(
                    id INTEGER PRIMARY KEY,
                    session_id INTEGER NOT NULL,
                    person_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    death_on_day INTEGER,
                    cause_of_death TEXT,
                    good BOOLEAN NOT NULL,
                    note TEXT,
                    FOREIGN KEY(session_id) REFERENCES session(id),
                    FOREIGN KEY(person_id) REFERENCES person(id)
                );
                """);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        instance = null;
    }
}
