package net.techmayhem.clocktrack;

import net.techmayhem.clocktrack.models.*;
import org.jspecify.annotations.Nullable;
import org.tinylog.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Database implements AutoCloseable {
    private final Connection connection;

    @Nullable
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
            instance.initSchema();
        }
        return instance;
    }

    /// Checks the database and sets up the schema if necessary. If the schema is invalid, exits the application.
    public void initSchema() {
        if (isDbSetup()) {
            return;
        }

        Logger.info("Creating database schema");
        runTransaction(conn -> {
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
            return null;
        });
    }

    /// Checks if the schema needs to be set up. Returns true if no tables have been created yet.
    ///
    /// If tables have already been created but do not match the expected schema, an error is printed instead and the application exits.
    private Boolean isDbSetup() {
        HashSet<String> foundTables = runTransaction(conn -> {
            HashSet<String> tables = new HashSet<>();
            try (Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
            }
            return tables;
        });

        HashSet<String> expectedTables = new HashSet<>(List.of("person", "script", "session", "person_session"));
        if (foundTables.isEmpty()) {
            return false;
        }
        if (!(foundTables.containsAll(expectedTables) && foundTables.size() == expectedTables.size())) {
            String message = "Invalid schema\nExpected table: " + expectedTables + "\nActual tables: " + foundTables;
            throw new DatabaseException(message, null, false);
        }
        return true;
    }

    public FromDb<Person> insertPerson(Person person) {
        return runTransaction(conn -> {
            String sql = "INSERT INTO person(name) VALUES (?)";
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, person.getName());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new FromDb<>(keys.getInt(1), person);
                }
            }
        });
    }

    public FromDb<Person> getPerson(int id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return FromDb.map(rs, Person::map);
                    } else {
                        throw new DatabaseException("No person with id " + id, null, true);
                    }
                }
            }
        });
    }

    public List<FromDb<Person>> getAllPersons() {
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
            return result;
        });
    }

    public void updatePerson(FromDb<Person> person) {
        runTransaction(conn -> {
            String sql = "UPDATE person SET name = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, person.model().getName());
                statement.setInt(2, person.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void deletePerson(int id) {
        runTransaction(conn -> {
            String sql = "DELETE FROM person WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public FromDb<Session> insertSession(Session session) {
        return runTransaction(conn -> {
            String sql = "INSERT INTO session(date, storyteller, good_won, script, note) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, session.getDate().toString());
                statement.setInt(2, session.getStoryteller());
                statement.setBoolean(3, session.isGoodWon());
                statement.setInt(4, session.getScript());
                statement.setString(5, session.getNote());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new FromDb<>(keys.getInt(1), session);
                }
            }
        });
    }

    public FromDb<Session> getSession(int id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM session WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return FromDb.map(rs, Session::map);
                    } else {
                        throw new DatabaseException("No Session with id " + id, null, true);
                    }
                }
            }
        });
    }

    public List<FromDb<Session>> getAllSessions() {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM session";
            ArrayList<FromDb<Session>> result = new ArrayList<>();
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
                try (ResultSet rs = statement.getResultSet()) {
                    while (rs.next()) {
                        result.add(FromDb.map(rs, Session::map));
                    }
                }
            }
            return result;
        });
    }

    public void updateSession(FromDb<Session> session) {
        runTransaction(conn -> {
            String sql = "UPDATE session SET date = ?, storyteller = ?, good_won = ?, script = ?, note = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                Session model = session.model();
                statement.setString(1, model.getDate().toString());
                statement.setInt(2, model.getStoryteller());
                statement.setBoolean(3, model.isGoodWon());
                statement.setInt(4, model.getScript());
                statement.setString(5, model.getNote());
                statement.setInt(6, session.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void deleteSession(int id) {
        runTransaction(conn -> {
            String sql = "DELETE FROM session WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public FromDb<PersonSession> insertPersonSession(PersonSession personSession) {
        return runTransaction(conn -> {
            String sql = "INSERT INTO person_session(session_id, person_id, role, death_on_day, cause_of_death, good, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, personSession.getSessionId());
                statement.setInt(2, personSession.getPersonId());
                statement.setString(3, personSession.getRole());
                if (personSession.getDeathOnDay() == null) {
                    statement.setNull(4, Types.INTEGER);
                } else {
                    statement.setInt(4, personSession.getDeathOnDay());
                }
                statement.setString(5, personSession.getCauseOfDeath());
                statement.setBoolean(6, personSession.isGood());
                statement.setString(7, personSession.getNote());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new FromDb<>(keys.getInt(1), personSession);
                }
            }
        });
    }

    public FromDb<PersonSession> getPersonSession(int id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person_session WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return FromDb.map(rs, PersonSession::map);
                    } else {
                        throw new DatabaseException("No PersonSession with id " + id, null, true);
                    }
                }
            }
        });
    }

    public List<FromDb<PersonSession>> getAllPersonSessionsForSession(int session_id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person_session WHERE session_id = ?";
            ArrayList<FromDb<PersonSession>> result = new ArrayList<>();
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, session_id);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(FromDb.map(rs, PersonSession::map));
                    }
                }
            }
            return result;
        });
    }

    public List<FromDb<PersonSession>> getAllPersonSessionsForPerson(int person_id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM person_session WHERE person_id = ?";
            ArrayList<FromDb<PersonSession>> result = new ArrayList<>();
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, person_id);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(FromDb.map(rs, PersonSession::map));
                    }
                }
            }
            return result;
        });
    }

    public void updatePersonSession(FromDb<PersonSession> personSession) {
        runTransaction(conn -> {
            String sql = "UPDATE person_session SET role = ?, death_on_day = ?, cause_of_death = ?, good = ?, note = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                PersonSession model = personSession.model();
                statement.setString(1, model.getRole());
                if (model.getDeathOnDay() == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, model.getDeathOnDay());
                }
                statement.setString(3, model.getCauseOfDeath());
                statement.setBoolean(4, model.isGood());
                statement.setString(5, model.getNote());
                statement.setInt(6, personSession.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void deletePersonSession(int id) {
        runTransaction(conn -> {
            String sql = "DELETE FROM person_session WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public FromDb<Script> insertScript(Script script) {
        return runTransaction(conn -> {
            String sql = "INSERT INTO script(name, json) VALUES (?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, script.getName());
                statement.setString(2, script.getJson());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return new FromDb<>(keys.getInt(1), script);
                }
            }
        });
    }

    public FromDb<Script> getScript(int id) {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM script WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return FromDb.map(rs, Script::map);
                    } else {
                        throw new DatabaseException("No script with id " + id, null, true);
                    }
                }
            }
        });
    }

    public List<FromDb<Script>> getAllScripts() {
        return runTransaction(conn -> {
            String sql = "SELECT * FROM script";
            ArrayList<FromDb<Script>> result = new ArrayList<>();
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
                try (ResultSet rs = statement.getResultSet()) {
                    while (rs.next()) {
                        result.add(FromDb.map(rs, Script::map));
                    }
                }
            }
            return result;
        });
    }

    public void updateScript(FromDb<Script> script) {
        runTransaction(conn -> {
            String sql = "UPDATE script SET name = ?, json = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                Script model = script.model();
                statement.setString(1, model.getName());
                statement.setString(2, model.getJson());
                statement.setInt(3, script.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void deleteScript(int id) {
        runTransaction(conn -> {
            String sql = "DELETE FROM script WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }

            return null;
        });
    }

    @FunctionalInterface
    interface SqlFunction<T extends @Nullable Object> {
        T apply(Connection conn) throws SQLException;
    }

    /// Wrapper function for common database exception handling logic.
    private <T> T runTransaction(SqlFunction<T> body) {
        try {
            T value = body.apply(connection);
            connection.commit();
            return value;
        } catch (SQLException e) {
            DatabaseException dbException = new DatabaseException(e.getMessage(), e, isRecoverable(e));
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                dbException.setIrrecoverable();
                dbException.addSuppressed(rollbackException);
            }
            throw dbException;
        }
    }

    private boolean isRecoverable(SQLException e) {
        return false;
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
