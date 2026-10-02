package net.techmayhem.clocktrack.database;

import static org.sqlite.SQLiteErrorCode.*;

import java.sql.SQLException;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class DatabaseException extends RuntimeException {
    private boolean recoverable;

    public DatabaseException(String message, @Nullable SQLException cause, boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

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

    public boolean isRecoverable() {
        return recoverable;
    }

    public void setIrrecoverable() {
        recoverable = false;
    }
}
