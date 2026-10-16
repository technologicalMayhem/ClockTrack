package net.techmayhem.clocktrack.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;

/// Creates and validates the database schema.
class Schema {
    /// Creates all tables.
    ///
    /// @param conn The connection to the database
    /// @throws SQLException If a table cannot be created
    static void createSchema(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute("""
                    CREATE TABLE player(
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        notes TEXT
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
                        storyteller_id INTEGER,
                        good_won BOOLEAN NOT NULL,
                        script_id INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(storyteller_id) REFERENCES player(id) ON DELETE SET NULL,
                        FOREIGN KEY(script_id) REFERENCES script(id)
                    );
                    """);
            statement.execute("""
                    CREATE TABLE player_session(
                        id INTEGER PRIMARY KEY,
                        session_id INTEGER NOT NULL,
                        player_id INTEGER,
                        role TEXT NOT NULL,
                        death_in_phase INTEGER,
                        cause_of_death TEXT,
                        good BOOLEAN NOT NULL,
                        note TEXT,
                        FOREIGN KEY(session_id) REFERENCES session(id),
                        FOREIGN KEY(player_id) REFERENCES player(id) ON DELETE SET NULL,
                        UNIQUE(session_id, player_id)
                    );
                    """);
        }
    }

    /// Checks whether the schema has been set up.
    ///
    /// @param conn The connection to the database
    /// @return True if all expected tables exist, false if the database contains no tables
    /// @throws DatabaseException If the existing tables do not match the expected tables
    /// @throws SQLException If the tables cannot be read
    static boolean isDbSetup(Connection conn) throws SQLException {
        HashSet<String> foundTables = new HashSet<>();
        try (Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (rs.next()) {
                foundTables.add(rs.getString("name"));
            }
        }

        HashSet<String> expectedTables = new HashSet<>(List.of("player", "script", "session", "player_session"));
        if (foundTables.isEmpty()) {
            return false;
        }
        if (!(foundTables.containsAll(expectedTables) && foundTables.size() == expectedTables.size())) {
            String message = "Invalid schema\nExpected table: " + expectedTables + "\nActual tables: " + foundTables;
            throw new DatabaseException(message, null, false);
        }
        return true;
    }
}
