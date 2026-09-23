package net.techmayhem.clocktrack;

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

public class DatabaseException extends RuntimeException {
    private boolean recoverable;

    public DatabaseException(String message, @Nullable SQLException cause, boolean recoverable) {
        super(message, cause);
        this.recoverable = recoverable;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public void setIrrecoverable() {
        recoverable = false;
    }
}
