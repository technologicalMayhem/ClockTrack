package net.techmayhem.clocktrack.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.techmayhem.clocktrack.model.*;
import net.techmayhem.clocktrack.projections.PersonSummary;
import net.techmayhem.clocktrack.projections.ScriptSummary;
import net.techmayhem.clocktrack.projections.SessionSummary;
import org.jspecify.annotations.Nullable;

public class Database implements AutoCloseable {
    private final Connection connection;

    @Nullable private static Database instance;

    private boolean inTransaction = false;

    private Database() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:ClockTrack.db");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        connection.setAutoCommit(false);
    }

    public static Database getInstance() {
        if (instance == null) {
            Database db;
            try {
                db = new Database();
            } catch (SQLException e) {
                throw new DatabaseException("Could not connect to database: " + e.getMessage(), e, false);
            }
            db.initSchema();
            instance = db;
        }
        return instance;
    }

    /// Checks the database and sets up the schema if necessary. If the schema is invalid, a `DatabaseException` is
    /// thrown.
    private void initSchema() {
        runTransaction(conn -> {
            if (Schema.isDbSetup(conn)) {
                return null;
            }
            Schema.createSchema(conn);
            return null;
        });
    }

    public void insertPerson(Person person) {
        runTransaction(conn -> insertPerson(conn, person));
    }

    private static FromDb<Person> insertPerson(Connection conn, Person person) throws SQLException {
        String sql = "INSERT INTO person(name) VALUES (?)";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, person.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new FromDb<>(keys.getInt(1), person);
            }
        }
    }

    public FromDb<Person> getPerson(int id) {
        return runTransaction(conn -> getPerson(conn, id));
    }

    private static FromDb<Person> getPerson(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM person WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return RowMappers.fromDb(rs, RowMappers::person);
                } else {
                    throw new DatabaseException("No person with id " + id, null, true);
                }
            }
        }
    }

    public List<FromDb<Person>> getAllPersons() {
        return runTransaction(Database::getAllPersons);
    }

    private static List<FromDb<Person>> getAllPersons(Connection conn) throws SQLException {
        String sql = "SELECT * FROM person";
        ArrayList<FromDb<Person>> result = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
            try (ResultSet rs = statement.getResultSet()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::person));
                }
            }
        }
        return result;
    }

    public void updatePerson(FromDb<Person> person) {
        runTransaction(conn -> {
            updatePerson(conn, person);
            return null;
        });
    }

    private static void updatePerson(Connection conn, FromDb<Person> person) throws SQLException {
        String sql = "UPDATE person SET name = ? WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, person.model().name());
            statement.setInt(2, person.id());
            ensureUpdated(statement.executeUpdate());
        }
    }

    public void deletePerson(int id) {
        runTransaction(conn -> {
            deletePerson(conn, id);
            return null;
        });
    }

    private static void deletePerson(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM person WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public FromDb<Session> getSession(int id) {
        return runTransaction(conn -> getSession(conn, id));
    }

    private static FromDb<Session> getSession(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM session WHERE session.id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return RowMappers.fromDb(rs, RowMappers::session);
                } else {
                    throw new DatabaseException("No session with id " + id, null, true);
                }
            }
        }
    }

    public void saveSession(MaybeFromDb<Session> session, List<PersonSession> personSessions) {
        runTransaction(conn -> {
            int sessionId;
            switch (session) {
                case MaybeFromDb.Persisted<Session> v -> {
                    FromDb<Session> fromDb = v.fromDb();
                    updateSession(conn, fromDb);
                    sessionId = fromDb.id();
                }
                case MaybeFromDb.Unsaved<Session> v ->
                    sessionId = insertSession(conn, v.raw()).id();
            }

            deleteAllPersonSessionsForSession(conn, sessionId);
            for (PersonSession personSession : personSessions) {
                insertPersonSession(conn, personSession.withSessionId(sessionId));
            }
            return null;
        });
    }

    private static FromDb<Session> insertSession(Connection conn, Session session) throws SQLException {
        String sql = "INSERT INTO session(date, storyteller_id, good_won, script_id, note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, session.date().toString());
            statement.setInt(2, session.storytellerId());
            statement.setBoolean(3, session.goodWon());
            statement.setInt(4, session.scriptId());
            statement.setString(5, session.note());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new FromDb<>(keys.getInt(1), session);
            }
        }
    }

    public List<FromDb<Session>> getAllSessions() {
        return runTransaction(Database::getAllSessions);
    }

    private static List<FromDb<Session>> getAllSessions(Connection conn) throws SQLException {
        String sql = "SELECT * FROM session";
        ArrayList<FromDb<Session>> result = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
            try (ResultSet rs = statement.getResultSet()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::session));
                }
            }
        }
        return result;
    }

    private static void updateSession(Connection conn, FromDb<Session> session) throws SQLException {
        String sql =
                "UPDATE session SET date = ?, storyteller_id = ?, good_won = ?, script_id = ?, note = ? WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            Session model = session.model();
            statement.setString(1, model.date().toString());
            statement.setInt(2, model.storytellerId());
            statement.setBoolean(3, model.goodWon());
            statement.setInt(4, model.scriptId());
            statement.setString(5, model.note());
            statement.setInt(6, session.id());
            ensureUpdated(statement.executeUpdate());
        }
    }

    public void deleteSession(int id) {
        runTransaction(conn -> {
            deleteAllPersonSessionsForSession(conn, id);
            deleteSession(conn, id);
            return null;
        });
    }

    private static void deleteSession(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM session WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void insertPersonSession(Connection conn, PersonSession personSession) throws SQLException {
        String sql =
                "INSERT INTO person_session(session_id, person_id, role, death_on_day, cause_of_death, good, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, personSession.sessionId());
            statement.setInt(2, personSession.personId());
            statement.setString(3, personSession.role());
            if (personSession.deathOnDay() == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, personSession.deathOnDay());
            }
            statement.setString(5, personSession.causeOfDeath());
            statement.setBoolean(6, personSession.good());
            statement.setString(7, personSession.note());
            statement.executeUpdate();
        }
    }

    public List<FromDb<PersonSession>> getAllPersonSessionsForSession(int session_id) {
        return runTransaction(conn -> getAllPersonSessionsForSession(conn, session_id));
    }

    private static List<FromDb<PersonSession>> getAllPersonSessionsForSession(Connection conn, int session_id)
            throws SQLException {
        String sql = "SELECT * FROM person_session WHERE session_id = ?";
        ArrayList<FromDb<PersonSession>> result = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, session_id);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::personSession));
                }
            }
        }
        return result;
    }

    private static void deleteAllPersonSessionsForSession(Connection conn, int sessionId) throws SQLException {
        String sql = "DELETE FROM person_session WHERE session_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, sessionId);
            statement.executeUpdate();
        }
    }

    public void insertScript(Script script) {
        runTransaction(conn -> insertScript(conn, script));
    }

    private static FromDb<Script> insertScript(Connection conn, Script script) throws SQLException {
        String sql = "INSERT INTO script(name, json) VALUES (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, script.name());
            statement.setString(2, script.json());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new FromDb<>(keys.getInt(1), script);
            }
        }
    }

    public FromDb<Script> getScript(int id) {
        return runTransaction(conn -> getScript(conn, id));
    }

    private static FromDb<Script> getScript(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM script WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return RowMappers.fromDb(rs, RowMappers::script);
                } else {
                    throw new DatabaseException("No script with id " + id, null, true);
                }
            }
        }
    }

    public List<FromDb<Script>> getAllScripts() {
        return runTransaction(Database::getAllScripts);
    }

    private static List<FromDb<Script>> getAllScripts(Connection conn) throws SQLException {
        String sql = "SELECT * FROM script";
        ArrayList<FromDb<Script>> result = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
            try (ResultSet rs = statement.getResultSet()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::script));
                }
            }
        }
        return result;
    }

    public void updateScript(FromDb<Script> script) {
        runTransaction(conn -> {
            updateScript(conn, script);
            return null;
        });
    }

    private static void updateScript(Connection conn, FromDb<Script> script) throws SQLException {
        String sql = "UPDATE script SET name = ?, json = ? WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            Script model = script.model();
            statement.setString(1, model.name());
            statement.setString(2, model.json());
            statement.setInt(3, script.id());
            ensureUpdated(statement.executeUpdate());
        }
    }

    public void deleteScript(int id) {
        runTransaction(conn -> {
            deleteScript(conn, id);
            return null;
        });
    }

    private static void deleteScript(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM script WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<PersonSummary> getPersonSummaries() {
        return runTransaction(Database::getPersonSummaries);
    }

    private static List<PersonSummary> getPersonSummaries(Connection conn) throws SQLException {
        String sql = """
                WITH appearances AS (
                    SELECT ps.person_id AS person_id, s.date AS date
                    FROM person_session ps
                    JOIN session s ON s.id = ps.session_id
                    UNION ALL
                    SELECT storyteller_id, date FROM session
                )
                SELECT p.id,
                       p.name,
                       (SELECT MIN(date) FROM appearances a WHERE a.person_id = p.id) AS first_game,
                       (SELECT MAX(date) FROM appearances a WHERE a.person_id = p.id) AS last_game,
                       (SELECT COUNT(*) FROM person_session ps WHERE ps.person_id = p.id) AS games_played,
                       (SELECT COUNT(*) FROM session s WHERE s.storyteller_id = p.id) AS games_storytold
                FROM person p
                ORDER BY p.name
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            ArrayList<PersonSummary> result = new ArrayList<>();
            while (rs.next()) {
                result.add(RowMappers.personSummary(rs));
            }
            return result;
        }
    }

    public List<ScriptSummary> getScriptSummaries() {
        return runTransaction(Database::getScriptSummaries);
    }

    private static List<ScriptSummary> getScriptSummaries(Connection conn) throws SQLException {
        String sql = """
                SELECT
                	script.id,
                	script.name,
                	MIN(session.date) as first_played,
                	MAX(session.date) as last_played,
                	COUNT(session.id) as times_played,
                	COUNT(CASE WHEN session.good_won = true THEN 1 END) as good_wins
                FROM script
                LEFT JOIN session ON session.script_id = script.id
                GROUP BY script.id
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            ArrayList<ScriptSummary> result = new ArrayList<>();
            while (rs.next()) {
                result.add(RowMappers.scriptSummary(rs));
            }
            return result;
        }
    }

    public List<SessionSummary> getSessionSummaries() {
        return runTransaction(Database::getSessionSummaries);
    }

    private static List<SessionSummary> getSessionSummaries(Connection conn) throws SQLException {
        String sql = """
                SELECT
                	s.id,
                	s.date,
                	s.good_won,
                	person.name as storyteller,
                	script.name as script_name,
                	COUNT(ps.id) as player_count
                FROM session s
                LEFT JOIN person ON person.id = s.storyteller_id
                LEFT JOIN script ON script.id = s.script_id
                LEFT JOIN person_session ps on ps.session_id = s.id
                GROUP BY s.id
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            ArrayList<SessionSummary> result = new ArrayList<>();
            while (rs.next()) {
                result.add(RowMappers.sessionSummary(rs));
            }
            return result;
        }
    }

    private static void ensureUpdated(int rowCount) {
        if (rowCount != 1) throw new DatabaseException("Update changed " + rowCount + " rows instead of 1", null, true);
    }

    @FunctionalInterface
    interface SqlFunction<T extends @Nullable Object> {
        T apply(Connection conn) throws SQLException;
    }

    /// Wrapper function for common database exception handling logic.
    private <T> T runTransaction(SqlFunction<T> body) {
        if (inTransaction)
            throw new IllegalStateException(
                    "Nested transaction. runTransaction must not be called within a transaction.");
        inTransaction = true;
        try {
            T value = body.apply(connection);
            connection.commit();
            return value;
        } catch (SQLException e) {
            DatabaseException dbException = new DatabaseException(e.getMessage(), e);
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                dbException.setIrrecoverable();
                dbException.addSuppressed(rollbackException);
            }
            throw dbException;
        } catch (RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException re) {
                e.addSuppressed(re);
            }
            throw e;
        } finally {
            inTransaction = false;
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        instance = null;
    }
}
