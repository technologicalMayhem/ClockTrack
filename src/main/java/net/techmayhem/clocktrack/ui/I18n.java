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

public class I18n {
    private static final int FRACTION_DIGITS = 1;
    private static @Nullable ResourceBundle bundle;
    private static Locale locale = Locale.getDefault();
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    public static void setLocale(Locale locale) {
        I18n.locale = locale;
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle(I18n.class.getPackageName(), locale);
        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
    }

    public static String localize(
            @PropertyKey(resourceBundle = "net.techmayhem.clocktrack.ui") String key, Object... args) {
        if (bundle == null) return key;
        if (!bundle.containsKey(key)) return errorString(key, args);
        String translationString = bundle.getString(key);
        Object[] fixedArguments = Arrays.stream(args).map(I18n::fixFormatting).toArray();
        return new MessageFormat(translationString, locale).format(fixedArguments);
    }

    private static Object fixFormatting(Object o) {
        if (o instanceof LocalDate localDate) {
            return formatDate(localDate);
        }
        return o;
    }

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

    public static String formatPercentage(double value) {
        NumberFormat formatter = NumberFormat.getPercentInstance(locale);
        formatter.setMaximumFractionDigits(FRACTION_DIGITS);
        formatter.setMinimumFractionDigits(FRACTION_DIGITS);
        return formatter.format(value);
    }
}
