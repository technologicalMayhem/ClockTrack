package net.techmayhem.clocktrack.model;

public record FromDb<T>(int id, T model) {
    public FromDb<T> with(T updatedModel) {
        return new FromDb<>(id, updatedModel);
    }
}
