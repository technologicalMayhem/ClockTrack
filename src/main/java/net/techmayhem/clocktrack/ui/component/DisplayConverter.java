package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;
import javafx.util.StringConverter;
import org.jspecify.annotations.Nullable;

/// A [StringConverter] for controls that only display values, like a [javafx.scene.control.ChoiceBox]. Converting text
/// back to a value is not supported.
///
/// @param <T> The type of the values
public class DisplayConverter<T> extends StringConverter<T> {
    private final Function<T, String> display;

    /// @param display The function that formats a value for display
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
