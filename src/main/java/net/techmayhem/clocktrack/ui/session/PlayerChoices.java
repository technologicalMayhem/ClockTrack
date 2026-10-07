package net.techmayhem.clocktrack.ui.session;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.util.StringConverter;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.ui.component.DisplayConverter;
import org.jspecify.annotations.Nullable;

final class PlayerChoices {
    private static final String UNKNOWN = "Unknown";

    private final List<Optional<FromDb<Player>>> items;

    PlayerChoices(List<FromDb<Player>> players) {
        items = Stream.concat(
                        Stream.of(Optional.<FromDb<Player>>empty()),
                        players.stream().map(Optional::of))
                .toList();
    }

    List<Optional<FromDb<Player>>> items() {
        return items;
    }

    StringConverter<Optional<FromDb<Player>>> converter() {
        return new DisplayConverter<>(item -> name(item.orElse(null)));
    }

    Optional<FromDb<Player>> itemFor(@Nullable Integer playerId) {
        return items.stream()
                .filter(item -> Objects.equals(item.map(FromDb::id).orElse(null), playerId))
                .findFirst()
                .orElseThrow();
    }

    String nameFor(@Nullable Integer playerId) {
        return name(itemFor(playerId).orElse(null));
    }

    private String name(@Nullable FromDb<Player> player) {
        return player == null ? UNKNOWN : player.model().name();
    }
}
