package net.techmayhem.clocktrack;

import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Person;
import org.tinylog.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Database implements AutoCloseable {
    private final Connection connection;

    private static Database instance;

    private Database() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:ClockTrack.db");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        connection.setAutoCommit(false);
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
    public void initSchema() {
        if (isDbSetup()) {
            return;
        }

        Logger.info("Creating database schema");
        Result<Void> result = runTransaction(conn -> {
            try (Statement statement = conn.createStatement()) {
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
            }
            return Result.ok();
        });
        if (result instanceof Result.Err<Void>(String message)) {
            Logger.error("Failed to create database: {}", message);
            System.exit(1);
        }
    }

    /// Checks if the schema needs to be set up. Returns true if no tables have been created yet.
    ///
    /// If tables have already been created but do not match the expected schema, an error is printed instead and the application exits.
    private boolean isDbSetup() {
        Result<HashSet<String>> result = runTransaction(conn -> {
            HashSet<String> tables = new HashSet<>();
            try (Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
            }
            return new Result.Ok<>(tables);
        });

        HashSet<String> foundTables = switch (result) {
            case Result.Ok<HashSet<String>>(HashSet<String> tables) -> tables;
            case Result.Err<HashSet<String>>(String message) -> {
                Logger.error("Failed to read database schema: {}", message);
                System.exit(1);
                yield new HashSet<>(); // unreachable
            }
        };

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

    public Result<FromDb<Person>> insertPerson(Person person) {
        return runTransaction(conn -> {
            String sql = "INSERT INTO person(name) VALUES (?)";
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, person.name);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new Result.Ok<>(new FromDb<>(keys.getInt(1), person));
                }
            }
        });
    }

    public Result<FromDb<Person>> getPerson(int id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return new Result.Ok<>(FromDb.map(rs, Person::map));
                    } else {
                        return new Result.Err<>("No person with id " + id);
                    }
                }
            }
        });
    }

    public Result<List<FromDb<Person>>> getAllPersons() {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person";
            ArrayList<FromDb<Person>> result = new ArrayList<>();
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
                try (ResultSet rs = statement.getResultSet()) {
                    while (rs.next()) {
                        result.add(FromDb.map(rs, Person::map));
                    }
                }
            }
            return new Result.Ok<>(result);
        });
    }

    public Result<Void> updatePerson(FromDb<Person> person) {
        return runTransaction(conn -> {
            String sql = "UPDATE person SET name = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, person.model().name);
                statement.setInt(2, person.id());
                statement.executeUpdate();
            }
            return Result.ok();
        });
    }

    public Result<Void> deletePerson(int id) {
        return runTransaction(conn -> {
            String sql = "DELETE FROM person WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            return Result.ok();
        });
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        Result<T> apply(Connection conn) throws SQLException;
    }

    /// Wrapper function for common database exception handling logic.
    private <T> Result<T> runTransaction(SqlFunction<T> body) {
        try {
            Result<T> value = body.apply(connection);
            connection.commit();
            return value;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException sqlException) {
                Logger.error("Could not rollback transaction: {}", sqlException.getMessage());
            }
            return new Result.Err<>(e.getMessage());
        }
    }

    public sealed interface Result<T> {
        record Ok<T>(T value) implements Result<T> {
        }

        record Err<T>(String message) implements Result<T> {
        }

        static Result<Void> ok() {
            return new Ok<>(null);
        }
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
