package net.techmayhem.clocktrack.model;

/// Represents a model loaded from the database.
///
/// @param id The database id of the model
/// @param model The loaded model
/// @param <T> The type of the model
public record FromDb<T>(int id, T model) {
    /// Returns a copy with the same database id and the given model.
    ///
    /// @param updatedModel The model to use
    /// @return A copy with the given model
    public FromDb<T> with(T updatedModel) {
        return new FromDb<>(id, updatedModel);
    }
}
