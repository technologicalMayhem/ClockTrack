package net.techmayhem.clocktrack.database;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.techmayhem.clocktrack.model.*;
import net.techmayhem.clocktrack.projections.*;
import org.jspecify.annotations.Nullable;

/// The shared connection to the ClockTrack database. All reads and writes of players, scripts, sessions, and the
/// players taking part in each session go through it.
///
/// Each method that reads or writes data runs in a single transaction. On failure, its changes are rolled back and a
/// [DatabaseException] is thrown.
///
/// The class is not thread-safe.
public class Database implements AutoCloseable {
    private static final int EXPECTED_ROWS_AFFECTED_BY_UPDATE = 1;
    private static final int RECENT_GAME_COUNT = 5;
    /// The path of the database file, relative to the working directory.
    public static final String CLOCK_TRACK_DB = "ClockTrack.db";
    /// The JDBC connection string for [#CLOCK_TRACK_DB].
    public static final String CONNECTION_STRING = "jdbc:sqlite:" + CLOCK_TRACK_DB;

    private final Connection connection;

    @Nullable private static Database instance;

    private boolean inTransaction = false;

    private Database() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION_STRING);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        connection.setAutoCommit(false);
    }

    /// Returns the shared instance. On the first call, the connection is established, and the database file and schema
    /// are created if they do not exist.
    ///
    /// @return The shared instance
    /// @throws DatabaseException If the connection cannot be established, or the existing schema does not match the
    /// expected one
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

    /// Checks whether the database file exists.
    ///
    /// @return True if the database file exists
    public static boolean exists() {
        return new File(CLOCK_TRACK_DB).exists();
    }

    /// Inserts a new player.
    ///
    /// @param player The player to insert
    /// @return The inserted player with its generated id
    public FromDb<Player> insertPlayer(Player player) {
        return runTransaction(conn -> insertPlayer(conn, player));
    }

    private static FromDb<Player> insertPlayer(Connection conn, Player player) throws SQLException {
        String sql = "INSERT INTO player(name, notes) VALUES (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, player.name());
            statement.setString(2, player.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new FromDb<>(keys.getInt(1), player);
            }
        }
    }

    /// Retrieves a player by id.
    ///
    /// @param id The id of the player
    /// @return The player
    /// @throws DatabaseException If no player with the given id exists
    public FromDb<Player> getPlayer(int id) {
        return runTransaction(conn -> getPlayer(conn, id));
    }

    private static FromDb<Player> getPlayer(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM player WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return RowMappers.fromDb(rs, RowMappers::player);
                } else {
                    throw new DatabaseException("No player with id " + id, null, true);
                }
            }
        }
    }

    /// Retrieves all players.
    ///
    /// @return All players, in unspecified order
    public List<FromDb<Player>> getAllPlayers() {
        return runTransaction(Database::getAllPlayers);
    }

    private static List<FromDb<Player>> getAllPlayers(Connection conn) throws SQLException {
        String sql = "SELECT * FROM player WHERE name IS NOT NULL";
        ArrayList<FromDb<Player>> result = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
            try (ResultSet rs = statement.getResultSet()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::player));
                }
            }
        }
        return result;
    }

    /// Updates an existing player.
    ///
    /// @param player The player with its updated values
    /// @throws DatabaseException If no player with the given id exists
    public void updatePlayer(FromDb<Player> player) {
        runTransaction(conn -> {
            updatePlayer(conn, player);
            return null;
        });
    }

    private static void updatePlayer(Connection conn, FromDb<Player> player) throws SQLException {
        String sql = "UPDATE player SET name = ?, notes = ? WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, player.model().name());
            statement.setString(2, player.model().notes());
            statement.setInt(3, player.id());
            ensureUpdated(statement.executeUpdate());
        }
    }

    /// Deletes a player. Has no effect if no player with the given id exists.
    ///
    /// Sessions the player took part in are kept, so the history of past games stays intact. Their references to
    /// the player are set to null instead, which anonymizes the player. [#isPlayerReferenced(int)] tells you
    /// beforehand whether that will happen.
    ///
    /// @param id The id of the player
    public void deletePlayer(int id) {
        runTransaction(conn -> {
            deletePlayer(conn, id);
            return null;
        });
    }

    private static void deletePlayer(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM player WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /// Checks whether a player is referenced by any session, either as a storyteller or as a participant.
    ///
    /// @param id The id of the player
    /// @return True if at least one session references the player
    public boolean isPlayerReferenced(int id) {
        return runTransaction(conn -> isPlayerReferenced(conn, id));
    }

    private static boolean isPlayerReferenced(Connection conn, int id) throws SQLException {
        String sql = """
                SELECT EXISTS (SELECT 1 FROM session WHERE storyteller_id = ?)
                    OR EXISTS (SELECT 1 FROM player_session WHERE player_id = ?)
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, id);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    /// Checks whether a script is referenced by any session. A referenced script cannot be deleted.
    ///
    /// @param id The id of the script
    /// @return True if at least one session references the script
    public boolean isScriptReferenced(int id) {
        return runTransaction(conn -> isScriptReferenced(conn, id));
    }

    private boolean isScriptReferenced(Connection conn, int id) throws SQLException {
        String sql = """
                SELECT EXISTS (SELECT 1 FROM session WHERE script_id = ?)
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    /// Retrieves a session by id. Its player sessions are retrieved separately with
    /// [#getAllPlayerSessionsForSession(int)].
    ///
    /// @param id The id of the session
    /// @return The session
    /// @throws DatabaseException If no session with the given id exists
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

    /// Inserts or updates a session together with its player sessions. An unsaved session is inserted, a persisted
    /// session is updated.
    ///
    /// The given player sessions replace all player sessions previously stored for the session. Their session id is
    /// ignored and set to the id of the stored session.
    ///
    /// @param session The session to insert or update
    /// @param playerSessions The player sessions of the session
    /// @throws DatabaseException If the session is persisted but no session with its id exists
    public void saveSession(MaybeFromDb<Session> session, List<PlayerSession> playerSessions) {
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

            deleteAllPlayerSessionsForSession(conn, sessionId);
            for (PlayerSession playerSession : playerSessions) {
                insertPlayerSession(conn, playerSession.withSessionId(sessionId));
            }
            return null;
        });
    }

    private static FromDb<Session> insertSession(Connection conn, Session session) throws SQLException {
        String sql = "INSERT INTO session(date, storyteller_id, good_won, script_id, note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, session.date().toString());
            statement.setObject(2, session.storytellerId());
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

    /// Retrieves all sessions, without their player sessions.
    ///
    /// @return All sessions, in unspecified order
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
            statement.setObject(2, model.storytellerId());
            statement.setBoolean(3, model.goodWon());
            statement.setInt(4, model.scriptId());
            statement.setString(5, model.note());
            statement.setInt(6, session.id());
            ensureUpdated(statement.executeUpdate());
        }
    }

    /// Deletes a session together with its player sessions. Has no effect if no session with the given id exists.
    ///
    /// @param id The id of the session
    public void deleteSession(int id) {
        runTransaction(conn -> {
            deleteAllPlayerSessionsForSession(conn, id);
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

    private static void insertPlayerSession(Connection conn, PlayerSession playerSession) throws SQLException {
        String sql =
                "INSERT INTO player_session(session_id, player_id, role, death_in_phase, cause_of_death, good, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerSession.sessionId());
            statement.setObject(2, playerSession.playerId());
            statement.setString(3, playerSession.role());
            statement.setObject(4, playerSession.deathInPhase());
            statement.setString(5, playerSession.causeOfDeath());
            statement.setBoolean(6, playerSession.good());
            statement.setString(7, playerSession.note());
            statement.executeUpdate();
        }
    }

    /// Retrieves all player sessions of a session.
    ///
    /// @param session_id The id of the session
    /// @return The player sessions of the session, in unspecified order
    public List<FromDb<PlayerSession>> getAllPlayerSessionsForSession(int session_id) {
        return runTransaction(conn -> getAllPlayerSessionsForSession(conn, session_id));
    }

    private static List<FromDb<PlayerSession>> getAllPlayerSessionsForSession(Connection conn, int session_id)
            throws SQLException {
        String sql = "SELECT * FROM player_session WHERE session_id = ?";
        ArrayList<FromDb<PlayerSession>> result = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, session_id);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(RowMappers.fromDb(rs, RowMappers::playerSession));
                }
            }
        }
        return result;
    }

    private static void deleteAllPlayerSessionsForSession(Connection conn, int sessionId) throws SQLException {
        String sql = "DELETE FROM player_session WHERE session_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, sessionId);
            statement.executeUpdate();
        }
    }

    /// Inserts a new script.
    ///
    /// @param script The script to insert
    /// @return The inserted script with its generated id
    public FromDb<Script> insertScript(Script script) {
        return runTransaction(conn -> insertScript(conn, script));
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

    /// Retrieves a script by id.
    ///
    /// @param id The id of the script
    /// @return The script
    /// @throws DatabaseException If no script with the given id exists
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

    /// Retrieves all scripts.
    ///
    /// @return All scripts, in unspecified order
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

    /// Updates an existing script.
    ///
    /// @param script The script with its updated values
    /// @throws DatabaseException If no script with the given id exists
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

    /// Deletes a script. Has no effect if no script with the given id exists.
    ///
    /// @param id The id of the script
    /// @throws DatabaseException If the script is referenced by any session, see [#isScriptReferenced(int)]
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

    /// Retrieves summary statistics for every player.
    ///
    /// @return A summary for every player, ordered by name
    public List<PlayerSummary> getPlayerSummaries() {
        return runTransaction(Database::getPlayerSummaries);
    }

    private static List<PlayerSummary> getPlayerSummaries(Connection conn) throws SQLException {
        String sql = """
                WITH appearances AS (
                    SELECT ps.player_id AS player_id, s.date AS date
                    FROM player_session ps
                    JOIN session s ON s.id = ps.session_id
                    UNION ALL
                    SELECT storyteller_id, date FROM session
                )
                SELECT p.id,
                       p.name,
                       (SELECT MIN(date) FROM appearances a WHERE a.player_id = p.id) AS first_game,
                       (SELECT MAX(date) FROM appearances a WHERE a.player_id = p.id) AS last_game,
                       (SELECT COUNT(*) FROM player_session ps WHERE ps.player_id = p.id) AS games_played,
                       (SELECT COUNT(*) FROM session s WHERE s.storyteller_id = p.id) AS games_storytold
                FROM player p
                ORDER BY p.name
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            ArrayList<PlayerSummary> result = new ArrayList<>();
            while (rs.next()) {
                result.add(RowMappers.playerSummary(rs));
            }
            return result;
        }
    }

    /// Retrieves summary statistics for every script.
    ///
    /// @return A summary for every script, ordered by name
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
                GROUP BY script.id, script.name
                ORDER BY script.name
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

    /// Retrieves summary statistics for every session.
    ///
    /// @return A summary for every session, ordered by date, newest first
    public List<SessionSummary> getSessionSummaries() {
        return runTransaction(Database::getSessionSummaries);
    }

    private static List<SessionSummary> getSessionSummaries(Connection conn) throws SQLException {
        String sql = """
                SELECT
                	s.id,
                	s.date,
                	s.good_won,
                	player.name as storyteller,
                	script.name as script_name,
                	(SELECT COUNT(*) FROM player_session ps WHERE ps.session_id = s.id) AS player_count
                FROM session s
                LEFT JOIN player ON player.id = s.storyteller_id
                JOIN script ON script.id = s.script_id
                ORDER BY s.date DESC
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

    /// Retrieves the statistics of a player over all games they took part in.
    ///
    /// @param playerId The id of the player
    /// @return The statistics of the player
    /// @throws DatabaseException If no player with the given id exists
    public PlayerStatistics getPlayerStatistics(int playerId) {
        return runTransaction(conn -> {
            FromDb<Player> player = getPlayer(conn, playerId);
            List<GameInfo> games = getGameInfos(conn, playerId);

            int wonGood = 0;
            int wonEvil = 0;
            int lossGood = 0;
            int lossEvil = 0;
            int survived = 0;

            int longestWinStreak = 0;
            int longestLossStreak = 0;
            int longestGoodStreak = 0;
            int longestEvilStreak = 0;

            int curWin = 0;
            int curLoss = 0;
            int curGood = 0;
            int curEvil = 0;

            for (GameInfo game : games) {
                if (game.good()) {
                    if (game.won()) {
                        wonGood++;
                    } else {
                        lossGood++;
                    }
                } else {
                    if (game.won()) {
                        wonEvil++;
                    } else {
                        lossEvil++;
                    }
                }
                if (game.survived()) {
                    survived++;
                }

                if (game.won()) {
                    curWin++;
                    curLoss = 0;
                } else {
                    curLoss++;
                    curWin = 0;
                }

                if (game.good()) {
                    curGood++;
                    curEvil = 0;
                } else {
                    curEvil++;
                    curGood = 0;
                }

                longestWinStreak = Math.max(longestWinStreak, curWin);
                longestLossStreak = Math.max(longestLossStreak, curLoss);
                longestGoodStreak = Math.max(longestGoodStreak, curGood);
                longestEvilStreak = Math.max(longestEvilStreak, curEvil);
            }

            List<PlayerStatistics.RecentGame> recentGames = games.reversed().stream()
                    .limit(RECENT_GAME_COUNT)
                    .map(game ->
                            new PlayerStatistics.RecentGame(game.sessionId(), game.date(), game.good(), game.won()))
                    .toList();

            return new PlayerStatistics(
                    player.id(),
                    player.model().name(),
                    countStorytold(conn, playerId),
                    wonGood,
                    wonEvil,
                    lossGood,
                    lossEvil,
                    survived,
                    longestWinStreak,
                    longestLossStreak,
                    longestGoodStreak,
                    longestEvilStreak,
                    getCharactersPlayed(conn, playerId),
                    getDeathsByPhase(conn, playerId),
                    getScriptPlays(conn, playerId),
                    getTeammates(conn, playerId, true),
                    getTeammates(conn, playerId, false),
                    recentGames);
        });
    }

    private static List<GameInfo> getGameInfos(Connection conn, int playerId) throws SQLException {
        String sql = """
                SELECT s.id AS session_id,
                    s.date,
                    ps.good,
                    ps.good = s.good_won AS won,
                    ps.death_in_phase IS NULL AS survived
                FROM player_session ps
                JOIN session s ON s.id = ps.session_id
                WHERE ps.player_id = ?
                ORDER BY s.date, s.id
                """;
        ArrayList<GameInfo> result = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(RowMappers.gameInfo(rs));
                }
            }
        }
        return result;
    }

    private static int countStorytold(Connection conn, int playerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM session WHERE storyteller_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static Map<String, Rate> getCharactersPlayed(Connection conn, int playerId) throws SQLException {
        String sql = """
                SELECT ps.role, COUNT(*) AS games, SUM(ps.good = s.good_won) AS wins
                FROM player_session ps
                JOIN session s ON s.id = ps.session_id
                WHERE ps.player_id = ?
                GROUP BY ps.role
                """;
        HashMap<String, Rate> result = new HashMap<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("role"), RowMappers.rate(rs));
                }
            }
        }
        return result;
    }

    private static Map<Integer, Integer> getDeathsByPhase(Connection conn, int playerId) throws SQLException {
        String sql = """
                SELECT death_in_phase AS phase, COUNT(*) AS deaths
                FROM player_session
                WHERE player_id = ? AND death_in_phase IS NOT NULL
                GROUP BY death_in_phase
                """;
        HashMap<Integer, Integer> result = new HashMap<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("phase"), rs.getInt("deaths"));
                }
            }
        }
        return result;
    }

    private static Map<Integer, PlayerStatistics.ScriptPlays> getScriptPlays(Connection conn, int playerId)
            throws SQLException {
        String sql = """
                SELECT script.id, script.name, COUNT(*) AS games
                FROM player_session ps
                JOIN session s ON s.id = ps.session_id
                JOIN script ON script.id = s.script_id
                WHERE ps.player_id = ?
                GROUP BY script.id, script.name
                """;
        HashMap<Integer, PlayerStatistics.ScriptPlays> result = new HashMap<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("id"), RowMappers.scriptPlays(rs));
                }
            }
        }
        return result;
    }

    private static Map<Integer, PlayerStatistics.Teammate> getTeammates(Connection conn, int playerId, boolean good)
            throws SQLException {
        String sql = """
                SELECT p.id, p.name, COUNT(*) AS games, SUM(me.good = s.good_won) AS wins
                FROM player_session me
                JOIN player_session mate ON mate.session_id = me.session_id
                    AND mate.good = me.good
                    AND mate.player_id <> me.player_id
                JOIN session s ON s.id = me.session_id
                JOIN player p ON p.id = mate.player_id
                WHERE me.player_id = ? AND me.good = ?
                GROUP BY p.id, p.name
                """;
        HashMap<Integer, PlayerStatistics.Teammate> result = new HashMap<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            statement.setBoolean(2, good);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("id"), RowMappers.teammate(rs));
                }
            }
        }
        return result;
    }

    /// Retrieves the statistics of a script over all games it was used in.
    ///
    /// @param scriptId The id of the script
    /// @return The statistics of the script
    /// @throws DatabaseException If no script with the given id exists
    public ScriptStatistics getScriptStatistics(int scriptId) {
        return runTransaction(conn ->
                new ScriptStatistics(getScriptStatSessions(conn, scriptId), getScriptStatSeats(conn, scriptId)));
    }

    private List<ScriptStatistics.Session> getScriptStatSessions(Connection conn, int scriptId) throws SQLException {
        String sql = """
                SELECT
                    s.date,
                    s.good_won,
                    s.storyteller_id
                FROM script
                JOIN session s ON s.script_id = script.id
                WHERE script.id = ?
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, scriptId);
            ArrayList<ScriptStatistics.Session> result = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(RowMappers.scriptStatisticsSession(rs));
            }
            return result;
        }
    }

    private List<ScriptStatistics.Seat> getScriptStatSeats(Connection conn, int scriptId) throws SQLException {
        String sql = """
                SELECT
                    s.id AS session_id,
                    ps.player_id,
                    ps.role,
                    ps.good,
                    s.good_won
                FROM script
                JOIN session s ON s.script_id = script.id\s
                JOIN player_session ps ON ps.session_id = s.id
                WHERE script.id = ?
                """;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, scriptId);
            ArrayList<ScriptStatistics.Seat> result = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(RowMappers.scriptStatisticsSeat(rs));
            }
            return result;
        }
    }

    private static void ensureUpdated(int rowCount) {
        if (rowCount != EXPECTED_ROWS_AFFECTED_BY_UPDATE)
            throw new DatabaseException(
                    "Update changed " + rowCount + " rows instead of " + EXPECTED_ROWS_AFFECTED_BY_UPDATE, null, true);
    }

    /// A function that runs against a database connection.
    @FunctionalInterface
    interface SqlFunction<T extends @Nullable Object> {
        T apply(Connection conn) throws SQLException;
    }

    /// Runs the given function in a single transaction. The transaction is rolled back if the function throws.
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

    /// Closes the database connection. The next call to [#getInstance()] establishes a new connection.
    ///
    /// @throws SQLException If the connection cannot be closed
    @Override
    public void close() throws SQLException {
        connection.close();
        instance = null;
    }
}
