package net.techmayhem.clocktrack.model;

/// Represents a model loaded from the database.
///
/// @param id    The database id of the model
/// @param model The loaded model
/// @param <T>   The type of the model
public record FromDb<T>(int id, T model) {
    /// Creates a copy with the same database id and a new model value.
    ///
    /// @param updatedModel The model value to use
    /// @return The updated database-backed model
    public FromDb<T> with(T updatedModel) {
        return new FromDb<>(id, updatedModel);
    }
}
