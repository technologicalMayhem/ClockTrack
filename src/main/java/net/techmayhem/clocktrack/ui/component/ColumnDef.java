package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;
import org.jspecify.annotations.NullUnmarked;

/// The definition of a table column, used by [TableHelper#buildTableColumns].
///
/// @param name The title of the column
/// @param value The function that extracts the value of the column from a row
/// @param display The function that formats the value for display
/// @param <T> The type of the table rows
/// @param <V> The type of the column values
@NullUnmarked
public record ColumnDef<T, V>(String name, Function<T, V> value, Function<V, String> display) {
    /// Constructs a new column definition that displays values with [Object#toString()].
    ///
    /// @param name The title of the column
    /// @param value The function that extracts the value of the column from a row
    public ColumnDef(String name, Function<T, V> value) {
        this(name, value, Object::toString);
    }
}
