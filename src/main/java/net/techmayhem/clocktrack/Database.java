package net.techmayhem.clocktrack;

import org.tinylog.Logger;

import java.sql.*;
import java.util.HashSet;
import java.util.List;

public class Database implements AutoCloseable {
    private final Connection connection;

    private static Database instance;

    private Database() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:ClockTrack.db");
    }

    public static Database getInstance() {
        if (instance == null) {
            try {
                instance = new Database();
            } catch (SQLException e) {
                Logger.error("Could not connect to database: " + e.getMessage());
                System.exit(1);
            }
        }
        return instance;
    }

    /// Checks the database and sets up the schema if necessary. If the schema is invalid, exits the application.
    public void initSchema() throws SQLException {
        if (isDbSetup()) {
            return;
        }

        Logger.info("Creating database schema");
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
                    FOREIGN KEY(person_id) REFERENCES person(id),
                    UNIQUE(session_id, person_id)
                );
                """);
        statement.close();
    }

    /// Checks if the schema needs to be set up. Returns true if no tables have been created yet.
    ///
    /// If tables have already been created but do not match the expected schema, an error is printed instead and the application exits.
    private boolean isDbSetup() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
        HashSet<String> foundTables = new HashSet<>();
        while (rs.next()) {
            foundTables.add(rs.getString("name"));
        }
        rs.close();
        statement.close();
        HashSet<String> expectedTables = new HashSet<>(List.of("person", "script", "session", "person_session"));
        if (foundTables.isEmpty()) {
            return false;
        }
        if (!(foundTables.containsAll(expectedTables) && foundTables.size() == expectedTables.size())) {
            Logger.error("Invalid schema\nExpected table: {}\nActual tables: {}", expectedTables, foundTables);
            System.exit(1);
        }
        return true;
    }

    /// Used to print a ResultSet. For debugging purposes.
    @SuppressWarnings("unused")
    private void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        // print header
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(meta.getColumnName(i));
            if (i < columnCount) System.out.print(" | ");
        }
        System.out.println();

        // print rows
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rs.getString(i));
                if (i < columnCount) System.out.print(" | ");
            }
            System.out.println();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        instance = null;
    }
}
