package net.techmayhem.clocktrack.gamedata;

import java.util.Locale;

/// The character type a Blood on the Clocktower character belongs to. Referred to as a team in technical contexts.
///
/// [#TOWNSFOLK] and [#OUTSIDER] are good, [#MINION] and [#DEMON] are evil. [#TRAVELLER] can be good or evil.
/// [#FABLED] and [#LORIC] are neither. They are special rules the Storyteller adds to a game or script. They cannot be
/// player characters, die or lose their ability.
public enum Team {
    /// A Townsfolk is a good character with an ability that benefits the good team.
    ///
    /// They are the most common character in a typical game and have a large variety of abilities from gathering
    /// information to protecting other players from the demon.
    TOWNSFOLK,
    /// An Outsider is a good character with an ability that hinders the good team.
    ///
    /// Their presence varies based on the player count of a game. Their abilities include things such as falsely
    /// registering as evil, mistakenly believing they are a townsfolk, or even losing the game for their entire
    /// team if they are executed.
    OUTSIDER,
    /// A Minion is an evil character with an ability that hinders the good team.
    ///
    /// They act as support for their demon, undermining the efforts of the town through deceit and trickery.
    /// Their abilities include things that cause abilities to malfunction, save the demon from death, and force the
    /// good team to work against their best interests.
    MINION,
    /// The Demon is an evil character with the ability to kill. They are the primary cause of discord in the town,
    /// and the ultimate target the good team must defeat.
    ///
    /// In addition to their ability to kill each night, the demon has extra protections, such as being able to
    /// jump to the body of a different evil player, turn their victims to their side, or even twist the very
    /// fabric of reality so that all information received is false.
    DEMON,
    /// A Traveller is a player character that can enter or exit a game at any point.
    ///
    /// Their identity and ability are known to all players, but their alignment may be either good or evil
    /// (which is decided by the Storyteller upon their entry to the game). Their abilities vary wildly but are
    /// nearly always exceptionally powerful and can significantly change the outcome of a game.
    TRAVELLER,
    /// Fabled are characters for the Storyteller. They’re more like special rules than characters all on their own.
    ///
    /// They cannot be killed and they alter the game itself. Fabled characters are not extra spice to alter a game
    /// for the sake of a twist - they help the Storyteller to run more balanced, more inclusive games and to solve
    /// real-world issues that may arise.
    FABLED,
    /// Loric are characters for the Storyteller. They’re more like special rules than characters all on their own.
    ///
    /// They cannot be killed and they alter the game itself. Loric characters bring extra spice to alter a game for
    /// the sake of a twist - while the [Fabled](#FABLED) solve problems, the Loric introduce new problems to make
    /// the game feel fresh and interesting.
    LORIC;

    static Team fromJson(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
