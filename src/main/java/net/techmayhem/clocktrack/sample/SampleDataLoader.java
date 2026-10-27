package net.techmayhem.clocktrack.sample;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import net.techmayhem.clocktrack.database.Database;
import net.techmayhem.clocktrack.database.DatabaseException;
import net.techmayhem.clocktrack.gamedata.SessionRoles;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.MaybeFromDb;
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.model.PlayerSession;
import net.techmayhem.clocktrack.model.Script;
import net.techmayhem.clocktrack.model.Session;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

/// Loads a fixed set of sample data into an empty database.
///
/// The sample data lets new users try ClockTrack without entering their own games first. It contains players, scripts,
/// and sessions of Trouble Brewing. All text, like role names and notes, is in the language of the user.
public final class SampleDataLoader {
    private static final @NonNls String BUNDLE_NAME = "net.techmayhem.clocktrack.sampledata";

    /// Placeholder session id. [Database#saveSession] replaces it with the id of the saved session.
    private static final int NEW_SESSION = 0;

    private final Database db;
    private final ResourceBundle bundle;

    private SampleDataLoader(Database db, ResourceBundle bundle) {
        this.db = db;
        this.bundle = bundle;
    }

    /// Loads the sample data into the given database.
    ///
    /// @param db     The database to load the sample data into
    /// @param locale The locale that determines the language of the sample data
    /// @throws DatabaseException If the database already contains players, scripts, or sessions
    public static void load(Database db, Locale locale) {
        if (!db.getAllPlayers().isEmpty()
                || !db.getAllScripts().isEmpty()
                || !db.getAllSessions().isEmpty()) {
            throw new DatabaseException("Sample data can only be loaded into an empty database", null, true);
        }
        new SampleDataLoader(db, ResourceBundle.getBundle(BUNDLE_NAME, locale)).insertAll();
    }

    private void insertAll() {
        FromDb<Player> gordon = player("Gordon");
        FromDb<Player> barney = player("Barney");
        FromDb<Player> isaac = player("Isaac");
        FromDb<Player> eli = player("Eli");
        FromDb<Player> wallace = player("Wallace");
        FromDb<Player> judith = player("Judith");
        FromDb<Player> alyx = player("Alyx");
        FromDb<Player> lamar = player("Lamar");
        FromDb<Player> dog = player("Dog");
        FromDb<Player> adrian = player("Adrian");

        FromDb<Script> troubleBrewing =
                db.insertScript(new Script(BaseScripts.NAME_TROUBLE_BREWING, BaseScripts.JSON_TROUBLE_BREWING));
        // Deliberately never played, so the overview shows scripts without games.
        db.insertScript(new Script(BaseScripts.NAME_BAD_MOON_RISING, BaseScripts.JSON_BAD_MOON_RISING));
        db.insertScript(new Script(BaseScripts.NAME_SECTS_AND_VIOLETS, BaseScripts.JSON_SECTS_AND_VIOLETS));

        // Saint executed on day one: evil wins immediately.
        session(
                LocalDate.of(2026, 9, 10),
                gordon,
                false,
                troubleBrewing,
                "session.saintExecuted",
                List.of(
                        alive(barney, "washerwoman", true),
                        alive(isaac, "empath", true),
                        alive(eli, "monk", true),
                        alive(wallace, "soldier", true),
                        alive(judith, "fortuneteller", true),
                        dead(alyx, "saint", true, day(1), SessionRoles.EXECUTION),
                        alive(lamar, "poisoner", false),
                        alive(dog, "imp", false)));

        // Good wins by executing the Imp on day two.
        session(
                LocalDate.of(2026, 9, 17),
                barney,
                true,
                troubleBrewing,
                null,
                List.of(
                        alive(gordon, "chef", true),
                        alive(isaac, "undertaker", true),
                        dead(eli, "ravenkeeper", true, night(2), "imp"),
                        alive(wallace, "virgin", true),
                        dead(judith, "investigator", true, day(1), SessionRoles.EXECUTION),
                        alive(adrian, "butler", true),
                        alive(lamar, "spy", false),
                        dead(dog, "imp", false, day(2), SessionRoles.EXECUTION)));

        // The Slayer hits the Imp on day one.
        session(
                LocalDate.of(2026, 9, 24),
                isaac,
                true,
                troubleBrewing,
                null,
                List.of(
                        alive(gordon, "librarian", true),
                        alive(barney, "mayor", true),
                        alive(eli, "slayer", true),
                        alive(wallace, "monk", true),
                        alive(judith, "empath", true),
                        alive(alyx, "drunk", true),
                        alive(adrian, "poisoner", false),
                        dead(dog, "imp", false, day(1), "slayer")));

        // A long game: evil wins once only two players are left.
        session(
                LocalDate.of(2026, 10, 1),
                gordon,
                false,
                troubleBrewing,
                null,
                List.of(
                        dead(barney, "fortuneteller", true, day(3), SessionRoles.EXECUTION),
                        dead(isaac, "monk", true, night(4), "imp"),
                        dead(eli, "chef", true, night(3), "imp"),
                        dead(wallace, "recluse", true, day(1), SessionRoles.EXECUTION),
                        dead(judith, "washerwoman", true, day(2), SessionRoles.EXECUTION),
                        alive(alyx, "spy", false),
                        alive(adrian, "imp", false),
                        dead(lamar, "empath", true, night(2), "imp")));

        // A townsfolk nominates the Virgin and is executed on the spot; good wins on day three.
        session(
                LocalDate.of(2026, 10, 8),
                eli,
                true,
                troubleBrewing,
                null,
                List.of(
                        alive(gordon, "investigator", true),
                        dead(barney, "undertaker", true, night(3), "imp"),
                        alive(isaac, "ravenkeeper", true),
                        withNote(
                                dead(wallace, "empath", true, day(1), SessionRoles.EXECUTION),
                                text("seat.nominatedVirgin")),
                        dead(judith, "virgin", true, night(2), "imp"),
                        alive(alyx, "butler", true),
                        dead(adrian, "poisoner", false, day(2), SessionRoles.EXECUTION),
                        dead(dog, "imp", false, day(3), SessionRoles.EXECUTION)));
    }

    private FromDb<Player> player(@NonNls String name) {
        return db.insertPlayer(new Player(name, null));
    }

    private void session(
            LocalDate date,
            FromDb<Player> storyteller,
            boolean goodWon,
            FromDb<Script> script,
            @PropertyKey(resourceBundle = BUNDLE_NAME) @Nullable String noteKey,
            List<PlayerSession> seats) {
        Session session =
                new Session(date, storyteller.id(), goodWon, script.id(), noteKey == null ? null : text(noteKey));
        db.saveSession(MaybeFromDb.of(session), seats);
    }

    private PlayerSession alive(FromDb<Player> player, String role, boolean good) {
        return new PlayerSession(NEW_SESSION, player.id(), role, null, null, good, null);
    }

    private PlayerSession dead(FromDb<Player> player, String role, boolean good, int phase, String cause) {
        return new PlayerSession(NEW_SESSION, player.id(), role, phase, cause, good, null);
    }

    private static PlayerSession withNote(PlayerSession seat, String note) {
        return new PlayerSession(
                seat.sessionId(),
                seat.playerId(),
                seat.role(),
                seat.deathInPhase(),
                seat.causeOfDeath(),
                seat.good(),
                note);
    }

    private static int night(int number) {
        return 2 * number - 1;
    }

    private static int day(int number) {
        return 2 * number;
    }

    private String text(@PropertyKey(resourceBundle = BUNDLE_NAME) String key) {
        return bundle.getString(key);
    }
}
