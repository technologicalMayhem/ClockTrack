package net.techmayhem.clocktrack.ui;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jetbrains.annotations.PropertyKey;
import org.jspecify.annotations.Nullable;

public class I18n {
    private static @Nullable ResourceBundle bundle;
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    public static void loadLocale() {
        Locale locale = Locale.getDefault();
        bundle = ResourceBundle.getBundle(I18n.class.getPackageName(), locale);
        Locale.setDefault(locale);
        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
    }

    public static String localize(
            @PropertyKey(resourceBundle = "net.techmayhem.clocktrack.ui") String key, Object... args) {
        if (bundle == null) return key;
        if (!bundle.containsKey(key)) return errorString(key, args);
        String translationString = bundle.getString(key);
        List<String> stringArguments =
                Arrays.stream(args).map(I18n::argumentMapper).toList();
        return MessageFormat.format(translationString, stringArguments);
    }

    private static String argumentMapper(Object o) {
        if (o instanceof LocalDate localDate) {
            return formatDate(localDate);
        }
        return o.toString();
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
}
