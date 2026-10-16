package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NonNls;

/// Formats and parses phases of a game, like "Night 1" or "Day 2".
///
/// Phases are numbered from 1 and alternate between night and day: night 1 is phase 1, day 1 is phase 2, night 2 is
/// phase 3, and so on. Besides the localized words, parsing also accepts the letters N and D.
final class PhaseFormat {
    static final int FIRST_PHASE = 1;
    static final int MAX_NUMBER = 99;
    static final int LAST_PHASE = 2 * MAX_NUMBER;

    private static final @NonNls String NIGHT_LETTER = "N";
    private static final @NonNls String DAY_LETTER = "D";

    private static final Pattern COMPLETE = Pattern.compile("(\\p{L}+)\\s*(\\d{1,2})");
    private static final Pattern TYPING = Pattern.compile("(\\p{L}*)(\\s?)(\\d{0,2})");

    private PhaseFormat() {}

    static String format(int phase) {
        boolean night = phase % 2 == 1;
        int number = night ? (phase + 1) / 2 : phase / 2;
        String word = night ? localize("phase.night") : localize("phase.day");
        return word + " " + number;
    }

    /// Empty unless the text is a complete, in-range phase like "Night 2", "Tag 1", "N2" or "D1".
    static OptionalInt parse(String text) {
        Matcher matcher = COMPLETE.matcher(text.strip());
        if (!matcher.matches()) return OptionalInt.empty();
        String label = matcher.group(1);
        boolean night = isNight(label);
        if (!night && !isDay(label)) return OptionalInt.empty();
        int number = Integer.parseInt(matcher.group(2));
        if (number < 1) return OptionalInt.empty();
        return OptionalInt.of(night ? 2 * number - 1 : 2 * number);
    }

    /// For the input filter: true if the text could still become a valid phase while typing.
    static boolean isTypedInput(String text) {
        if (text.isEmpty()) return true;
        Matcher matcher = TYPING.matcher(text);
        if (!matcher.matches()) return false;
        String label = matcher.group(1);
        if (label.isEmpty()) return false;
        boolean complete = isNight(label) || isDay(label);
        boolean hasRest = !matcher.group(2).isEmpty() || !matcher.group(3).isEmpty();
        if (hasRest) return complete;
        return complete || isNightPrefix(label) || isDayPrefix(label);
    }

    private static boolean isNight(String label) {
        return label.equalsIgnoreCase(localize("phase.night")) || label.equalsIgnoreCase(NIGHT_LETTER);
    }

    private static boolean isDay(String label) {
        return label.equalsIgnoreCase(localize("phase.day")) || label.equalsIgnoreCase(DAY_LETTER);
    }

    private static boolean isNightPrefix(String label) {
        return isPrefix(label, localize("phase.night"));
    }

    private static boolean isDayPrefix(String label) {
        return isPrefix(label, localize("phase.day"));
    }

    private static boolean isPrefix(String label, String word) {
        return word.regionMatches(true, 0, label, 0, label.length());
    }
}
