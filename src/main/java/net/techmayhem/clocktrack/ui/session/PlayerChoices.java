package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.util.StringConverter;
import net.techmayhem.clocktrack.model.FromDb;
import net.techmayhem.clocktrack.model.Player;
import net.techmayhem.clocktrack.ui.component.DisplayConverter;
import org.jspecify.annotations.Nullable;

/// The choices for selecting a player, like the storyteller of a session.
///
/// The first choice is empty and stands for an unknown player. It also stands in for players who have been removed
/// from the database, so every stored session can be shown.
final class PlayerChoices {
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
        return player == null ? localize("generic.unknown") : player.model().name();
    }
}
