package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;
import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public record ColumnDef<T, V>(String name, Function<T, V> value, Function<V, String> display) {
    public ColumnDef(String name, Function<T, V> value) {
        this(name, value, Object::toString);
    }
}
