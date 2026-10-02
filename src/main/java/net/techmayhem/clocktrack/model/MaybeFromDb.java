package net.techmayhem.clocktrack.model;

public sealed interface MaybeFromDb<T> {

    record Persisted<T>(FromDb<T> fromDb) implements MaybeFromDb<T> {}

    record Unsaved<T>(T raw) implements MaybeFromDb<T> {}

    static <T> MaybeFromDb<T> of(FromDb<T> fromDb) {
        return new Persisted<>(fromDb);
    }

    static <T> MaybeFromDb<T> of(T raw) {
        return new Unsaved<>(raw);
    }

    default T value() {
        return switch (this) {
            case Persisted<T> p -> p.fromDb().model();
            case Unsaved<T> u -> u.raw();
        };
    }
}
