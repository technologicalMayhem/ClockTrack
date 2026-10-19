package net.techmayhem.clocktrack.gamedata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/// Blood on the Clocktower roles loaded from bundled game data.
///
/// Roles resolves each character's name and ability text using the active locale when a translation is available.
/// If a character has no translation for the active locale, the default bundled text is used instead.
public class Roles {
    private static final String ROLES_PATH = "/net/techmayhem/clocktrack/gamedata/roles.json";
    private static final String LOCALE_PATH = "/net/techmayhem/clocktrack/gamedata/locale/%s.json";

    @Nullable private static Roles instance;

    private final HashMap<String, Role> resolvedCharacter;

    /// Gets the shared roles instance, creating it with the default locale if needed.
    ///
    /// @return the shared roles instance
    public static Roles getInstance() {
        if (instance == null) instance = new Roles();
        return instance;
    }

    /// Loads the shared roles instance using the given locale.
    ///
    /// @param locale the locale to use for translated role text
    public static void loadLocale(Locale locale) {
        instance = new Roles(locale);
    }

    /// Gets a role by id.
    ///
    /// Role ids are matched case-insensitively.
    ///
    /// @param id the role id to look up
    /// @return the matching role, or `null` if no role exists with that id
    public @Nullable Role get(String id) {
        return resolvedCharacter.get(id.toLowerCase(Locale.ROOT));
    }

    private Roles() {
        this(Locale.getDefault());
    }

    private Roles(Locale locale) {
        JsonArray json = readJson(ROLES_PATH).getAsJsonArray();
        JsonObject translations = readTranslations(locale);

        resolvedCharacter = new HashMap<>();
        for (JsonElement element : json) {
            JsonObject role = element.getAsJsonObject();
            String id = role.get("id").getAsString();
            JsonObject translation = translations.getAsJsonObject(id); // null if not translated
            resolvedCharacter.put(
                    id,
                    new Role(
                            id,
                            localized(translation, role, "name"),
                            localized(translation, role, "ability"),
                            Team.fromJson(role.get("team").getAsString())));
        }
    }

    private static String localized(@Nullable JsonObject translation, JsonObject role, String field) {
        if (translation != null && translation.has(field)) {
            return translation.get(field).getAsString();
        }
        return role.get(field).getAsString();
    }

    private static JsonObject readTranslations(Locale locale) {
        String path = LOCALE_PATH.formatted(locale.getLanguage());
        if (Roles.class.getResource(path) == null) {
            return new JsonObject();
        }
        return readJson(path).getAsJsonObject().getAsJsonObject("roles");
    }

    private static JsonElement readJson(String path) {
        InputStream stream = Roles.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing bundled resource: " + path);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
