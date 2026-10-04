package net.techmayhem.clocktrack.ui.component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * A compact summary of what is wrong with a form.
 *
 * <p>Shows the first error as text. If there are more, an underlined "And N more" follows it, and
 * hovering over that shows the rest in a tooltip. Nothing is shown while the form is valid.
 *
 * <p>The same check also drives {@link #hasErrors()}, so a submit button can be bound to it and the
 * button state and the message always agree.
 */
public class ErrorSummary extends HBox {
    private static final Duration TOOLTIP_DELAY = Duration.seconds(0.1);

    private final ObjectProperty<List<String>> errors;
    private final BooleanBinding hasErrors;
    private final Label message;
    private final Label more;
    private final Tooltip moreTooltip;

    /**
     * Creates a summary that re-runs {@code validator} whenever one of the {@code dependencies}
     * changes.
     *
     * <p>The validator runs once during construction, so everything it reads must already be
     * initialized before this constructor is called.
     *
     * <p>Every observable the validator reads must be listed in {@code dependencies}. A missing one
     * leaves the message out of date until another dependency happens to change.
     *
     * @param validator    returns the current error messages in the order they should appear, or an
     *                     empty list if the form is valid
     * @param dependencies the properties the validator reads
     */
    public ErrorSummary(Callable<List<String>> validator, Observable... dependencies) {
        message = new Label();
        more = new Label();
        more.setUnderline(true);
        moreTooltip = new Tooltip();
        moreTooltip.setShowDelay(TOOLTIP_DELAY);

        more.setTooltip(moreTooltip);

        getChildren().addAll(message, more);

        errors = new SimpleObjectProperty<>(List.of());
        errors.addListener((_, _, list) -> update(list));
        errors.bind(Bindings.createObjectBinding(validator, dependencies));

        hasErrors = Bindings.createBooleanBinding(() -> !errors.get().isEmpty(), errors);
    }

    /**
     * Whether the form currently has at least one error.
     *
     * @return a binding that is {@code true} while the validator returns any messages
     */
    public BooleanBinding hasErrors() {
        return hasErrors;
    }

    /**
     * Updates the labels and tooltip to show the given errors. Called whenever the validator's result changes.
     *
     * @param list the list of errors created by the validator
     */
    private void update(List<String> list) {
        if (list.isEmpty()) {
            message.setText("");
        } else {
            message.setText(list.getFirst() + ". ");
        }
        if (list.size() > 1) {
            more.setText("And " + (list.size() - 1) + " more");
            String tooltipText = list.stream().skip(1).collect(Collectors.joining("\n"));
            moreTooltip.setText(tooltipText);
        } else {
            more.setText("");
            moreTooltip.setText("");
        }
    }
}
