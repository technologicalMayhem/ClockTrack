package net.techmayhem.clocktrack.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;

class Schema {
    static void createSchema(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute("""
                    CREATE TABLE player(
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
                        storyteller_id INTEGER NOT NULL,
                        good_won BOOLEAN NOT NULL,
                        script_id INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(storyteller_id) REFERENCES player(id),
                        FOREIGN KEY(script_id) REFERENCES script(id)
                    );
                    """);
            statement.execute("""
                    CREATE TABLE player_session(
                        id INTEGER PRIMARY KEY,
                        session_id INTEGER NOT NULL,
                        player_id INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        death_on_day INTEGER,
                        cause_of_death TEXT,
                        good BOOLEAN NOT NULL,
                        note TEXT,
                        FOREIGN KEY(session_id) REFERENCES session(id),
                        FOREIGN KEY(player_id) REFERENCES player(id),
                        UNIQUE(session_id, player_id)
                    );
                    """);
        }
    }

    /// Checks if the schema needs to be set up. Returns true if no tables have been created yet.
    ///
    /// If tables have already been created but do not match the expected schema, a `DatabaseException` is thrown.
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
