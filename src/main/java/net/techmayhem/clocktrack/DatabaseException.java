package net.techmayhem.clocktrack;

import java.sql.SQLException;
import org.jspecify.annotations.Nullable;

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
