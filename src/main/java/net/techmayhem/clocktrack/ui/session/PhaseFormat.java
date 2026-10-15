package net.techmayhem.clocktrack.ui.session;

import static net.techmayhem.clocktrack.ui.I18n.localize;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PhaseFormat {
    static final int FIRST_PHASE = 1;
    static final int LAST_PHASE = 2 * 99;

    private static final String NIGHT_LETTER = "N";
    private static final String DAY_LETTER = "D";

    private static final Pattern COMPLETE = Pattern.compile("(\\p{L}+)\\s*(\\d+)");
    private static final Pattern TYPING = Pattern.compile("(\\p{L}*)(\\s?)(\\d*)");

    private PhaseFormat() {}

    static String format(int phase) {
        boolean night = phase % 2 == 1;
        int number = night ? (phase + 1) / 2 : phase / 2;
        String word = night ? localize("phase.night") : localize("phase.day");
        return word + " " + number;
    }

    /** Empty unless the text is a complete, in-range phase like "Night 2", "Tag 1", "N2" or "D1". */
    static OptionalInt parse(String text) {
        Matcher matcher = COMPLETE.matcher(text.strip());
        if (!matcher.matches()) return OptionalInt.empty();
        String label = matcher.group(1);
        boolean night = isNight(label);
        if (!night && !isDay(label)) return OptionalInt.empty();
        try {
            int number = Integer.parseInt(matcher.group(2));
            if (number < 1 || number > LAST_PHASE) return OptionalInt.empty(); // also rules out overflow below
            int phase = night ? 2 * number - 1 : 2 * number;
            return phase <= LAST_PHASE ? OptionalInt.of(phase) : OptionalInt.empty();
        } catch (NumberFormatException _) {
            return OptionalInt.empty(); // digit string too long for an int
        }
    }

    /** For the input filter: true if the text could still become a valid phase while typing. */
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
