package net.techmayhem.clocktrack.screens;

import javafx.util.StringConverter;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public class DisplayConverter<T> extends StringConverter<T> {
    private final Function<T, String> display;

    public DisplayConverter(Function<T, String> display) {
        this.display = display;
    }

    @Override
    public String toString(@Nullable T object) {
        return object == null ? "" : display.apply(object);
    }

    @Override
    public T fromString(String string) {
        throw new UnsupportedOperationException("DisplayConverter is display-only");
    }
}