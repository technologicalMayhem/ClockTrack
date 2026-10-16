package net.techmayhem.clocktrack.database;

import static org.sqlite.SQLiteErrorCode.*;

import java.sql.SQLException;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Thrown to indicate that a database operation has failed.
///
/// A recoverable exception affects only the failed operation. An irrecoverable exception indicates that the database
/// may be in an inconsistent state.
public class DatabaseException extends RuntimeException {
    private boolean recoverable;

    /// Constructs a new exception with the specified message, cause, and recoverability.
    ///
    /// @param message The detail message
    /// @param cause The cause, or null if there is none
    /// @param recoverable Whether the exception is recoverable
    public DatabaseException(String message, @Nullable SQLException cause, boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    /// Constructs a new exception with the specified message and cause. Recoverability is determined by the SQLite
    /// error code of the cause.
    ///
    /// @param message The detail message
    /// @param cause The cause
    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
        this.recoverable = isRecoverableErrorCode(cause);
    }

    private boolean isRecoverableErrorCode(SQLException e) {
        // Chops off everything but the low byte, as we are not interested in the extended codes
        int primaryCode = e.getErrorCode() & 0xFF;
        return Set.of(SQLITE_CONSTRAINT.code, SQLITE_READONLY.code, SQLITE_TOOBIG.code)
                .contains(primaryCode);
    }

    /// Returns whether this exception is recoverable.
    ///
    /// @return True if this exception is recoverable
    public boolean isRecoverable() {
        return recoverable;
    }

    /// Marks this exception as irrecoverable.
    public void setIrrecoverable() {
        recoverable = false;
    }
}
