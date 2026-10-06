package net.techmayhem.clocktrack.model;

import org.jspecify.annotations.Nullable;

public sealed interface PlayerName {
    record Named(String value) implements PlayerName {
        @Override
        public String toString() {
            return value;
        }
    }

    record Anonymized(int id) implements PlayerName {
        @Override
        public String toString() {
            return "Deleted player #" + id;
        }
    }

    static PlayerName fromColumn(int id, @Nullable String column) {
        return column == null ? new Anonymized(id) : new Named(column);
    }

    default @Nullable String toColumn() {
        return this instanceof Named(String value) ? value : null;
    }

    default boolean isAnonymized() {
        return this instanceof Anonymized;
    }
}
