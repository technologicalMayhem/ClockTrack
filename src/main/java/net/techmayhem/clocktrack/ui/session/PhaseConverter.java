package net.techmayhem.clocktrack.ui.session;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;
import org.jspecify.annotations.Nullable;

final class PhaseConverter extends StringConverter<Integer> {
    private final SpinnerValueFactory<Integer> factory;

    PhaseConverter(SpinnerValueFactory<Integer> factory) {
        this.factory = factory;
    }

    @Override
    public String toString(@Nullable Integer phase) {
        return phase == null ? "" : PhaseFormat.format(phase);
    }

    @Override
    public Integer fromString(@Nullable String text) {
        if (text == null) return factory.getValue();
        return PhaseFormat.parse(text).orElse(factory.getValue());
    }
}
