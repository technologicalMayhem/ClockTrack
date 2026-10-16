package net.techmayhem.clocktrack.ui;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jetbrains.annotations.PropertyKey;
import org.jspecify.annotations.Nullable;

/// Localizes texts, dates, and numbers for the UI.
///
/// Texts come from the `ui` resource bundle, in the language of the locale set with [#setLocale(Locale)].
public class I18n {
    private static final int FRACTION_DIGITS = 1;
    private static @Nullable ResourceBundle bundle;
    private static Locale locale = Locale.getDefault();
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    /// Sets the locale used for all localization. It also becomes the default locale.
    ///
    /// @param locale The locale to use
    public static void setLocale(Locale locale) {
        I18n.locale = locale;
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle(I18n.class.getPackageName(), locale);
        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
    }

    /// Returns the localized text for the given key, with the arguments inserted as in [MessageFormat].
    ///
    /// If no locale has been set yet, the key itself is returned. If the key is missing from the bundle, the key
    /// and the arguments are returned instead. That way, a missing text shows up plainly in the UI instead of
    /// crashing it.
    ///
    /// @param key The key of the text
    /// @param args The arguments to insert into the text
    /// @return The localized text
    public static String localize(
            @PropertyKey(resourceBundle = "net.techmayhem.clocktrack.ui") String key, Object... args) {
        if (bundle == null) return key;
        if (!bundle.containsKey(key)) return errorString(key, args);
        String translationString = bundle.getString(key);
        Object[] fixedArguments = Arrays.stream(args).map(I18n::fixFormatting).toArray();
        return new MessageFormat(translationString, locale).format(fixedArguments);
    }

    /// Formats dates up front, since [MessageFormat] cannot format a [LocalDate].
    private static Object fixFormatting(Object o) {
        if (o instanceof LocalDate localDate) {
            return formatDate(localDate);
        }
        return o;
    }

    /// Formats a date in the medium style of the locale.
    ///
    /// @param localDate The date to format
    /// @return The formatted date
    public static String formatDate(LocalDate localDate) {
        return dateTimeFormatter.format(localDate);
    }

    private static String errorString(String key, Object... args) {
        if (args.length > 0) {
            return key + Arrays.toString(args);
        } else {
            return key;
        }
    }

    /// Formats a value as a percentage with one fraction digit.
    ///
    /// @param value The value to format, where 1 is 100%
    /// @return The formatted percentage
    public static String formatPercentage(double value) {
        NumberFormat formatter = NumberFormat.getPercentInstance(locale);
        formatter.setMaximumFractionDigits(FRACTION_DIGITS);
        formatter.setMinimumFractionDigits(FRACTION_DIGITS);
        return formatter.format(value);
    }
}
