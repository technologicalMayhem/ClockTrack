package net.techmayhem.clocktrack.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jspecify.annotations.Nullable;

public class I18n {
    private static @Nullable ResourceBundle bundle;

    public static void loadLocale() {
        bundle = ResourceBundle.getBundle(I18n.class.getPackageName(), Locale.GERMAN);
    }

    public static String t(String key, Object... args) {
        if (bundle == null) return key;
        if (!bundle.containsKey(key)) return errorString(key, args);
        String translationString = bundle.getString(key);
        return MessageFormat.format(translationString, args);
    }

    private static String errorString(String key, Object... args) {
        if (args.length > 0) {
            return key + Arrays.toString(args);
        } else {
            return key;
        }
    }
}
