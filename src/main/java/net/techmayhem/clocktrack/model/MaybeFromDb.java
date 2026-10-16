package net.techmayhem.clocktrack.model;

/// A sum type to represent both a saved model and an unsaved model.
///
/// The primary use is to enable code that does not care whether a model is saved or not but still preserves the
/// database id of the model.
///
/// @param <T> The type of the model
public sealed interface MaybeFromDb<T> {

    /// The form for a model saved in the database.
    ///
    /// @param fromDb The model from the database
    /// @param <T> The type of the model
    record Persisted<T>(FromDb<T> fromDb) implements MaybeFromDb<T> {}

    /// The form for a model that has not yet been saved.
    ///
    /// @param raw The unsaved model
    /// @param <T> The type of the model
    record Unsaved<T>(T raw) implements MaybeFromDb<T> {}

    /// Convenience method to create a new instance of MaybeFromDb from a saved model.
    ///
    /// @param fromDb The model from the database
    /// @param <T> The type of the model
    /// @return The new instance of MaybeFromDb
    static <T> MaybeFromDb<T> of(FromDb<T> fromDb) {
        return new Persisted<>(fromDb);
    }

    /// Convenience method to create a new instance of MaybeFromDb from an unsaved model.
    ///
    /// @param raw The unsaved model
    /// @param <T> The type of the model
    /// @return The new instance of MaybeFromDb
    static <T> MaybeFromDb<T> of(T raw) {
        return new Unsaved<>(raw);
    }

    /// Access the underlying model regardless of the actual variant.
    ///
    /// @return The underlying model
    default T value() {
        return switch (this) {
            case Persisted<T> p -> p.fromDb().model();
            case Unsaved<T> u -> u.raw();
        };
    }
}
