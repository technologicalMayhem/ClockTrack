package net.techmayhem.clocktrack.gamedata;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// A parser for Blood on the Clocktower scripts in JSON form.
///
/// A script is a JSON array with one entry per character. An entry either references an official character, as a
/// plain string or as an object with only an `id`, or defines a homebrew character with its own `name`, `team` and
/// `ability`. An optional `_meta` entry carries the name of the script.
///
/// The parser checks the whole script instead of stopping at the first mistake. It collects every problem it finds
/// and reports them together in a [ScriptFormatException].
public final class ScriptParser {
    private static final Pattern HOMEBREW_ID = Pattern.compile("^[a-z0-9]+$");
    private static final Pattern GSON_LOCATION = Pattern.compile("at line (\\d+) column \\d+");

    private static final Gson PRETTY_PRINTER = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private ScriptParser() {}

    /// Parses a script and resolves its entries to roles.
    ///
    /// References to official characters are looked up in `officialRoles`. Case and any characters other than
    /// letters and digits are ignored, so `Fortune Teller`, `fortune_teller` and `fortuneteller` all resolve to
    /// the same role. Homebrew characters are created from the fields of their entry.
    ///
    /// @param json The script JSON
    /// @param officialRoles The roles that references to official characters are resolved against
    /// @return The parsed script
    /// @throws ScriptFormatException If the JSON is not valid or the script has at least one problem
    public static ParsedScript parse(String json, Roles officialRoles) throws ScriptFormatException {
        JsonElement root = readStrict(json);
        if (!root.isJsonArray()) {
            throw new ScriptFormatException(List.of(new ScriptProblem(0, ProblemType.NOT_AN_ARRAY, "")));
        }

        List<ScriptProblem> problems = new ArrayList<>();
        Map<String, Role> roles = new LinkedHashMap<>();
        String name = null;

        JsonArray entries = root.getAsJsonArray();
        for (int i = 0; i < entries.size(); i++) {
            int entry = i + 1;
            JsonElement element = entries.get(i);

            if (isMeta(element)) {
                name = optString(element.getAsJsonObject(), "name");
                continue;
            }
            Role role = resolve(element, entry, officialRoles, problems);
            if (role != null && roles.putIfAbsent(role.id(), role) != null) {
                problems.add(new ScriptProblem(entry, ProblemType.DUPLICATE_ID, role.id()));
            }
        }

        if (!problems.isEmpty()) {
            throw new ScriptFormatException(problems);
        }
        return new ParsedScript(name, Collections.unmodifiableMap(roles));
    }

    /// Returns the script JSON formatted with indentation and one entry per line.
    ///
    /// @param json The script JSON
    /// @return The formatted JSON
    /// @throws ScriptFormatException If the JSON is not syntactically valid
    public static String format(String json) throws ScriptFormatException {
        return PRETTY_PRINTER.toJson(readStrict(json));
    }

    /// Returns the role an entry stands for, or null if the entry has a problem. Problems are added to `problems`.
    private static @Nullable Role resolve(
            JsonElement element, int entry, Roles officialRoles, List<ScriptProblem> problems) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return official(element.getAsString(), entry, officialRoles, problems);
        }
        if (!element.isJsonObject()) {
            problems.add(new ScriptProblem(entry, ProblemType.INVALID_ENTRY, element.toString()));
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String id = required(object, "id", entry, problems);
        if (id == null) return null;

        if (!object.has("name") && !object.has("team") && !object.has("ability")) {
            return official(id, entry, officialRoles, problems);
        }
        return homebrew(object, id, entry, problems);
    }

    /// Returns the official role with the given identifier, or null and a [ProblemType#UNKNOWN_ROLE] problem if there
    /// is none.
    private static @Nullable Role official(String rawId, int entry, Roles officialRoles, List<ScriptProblem> problems) {
        Role role = officialRoles.get(normalize(rawId));
        if (role == null) {
            problems.add(new ScriptProblem(entry, ProblemType.UNKNOWN_ROLE, rawId));
        }
        return role;
    }

    /// Returns a homebrew role created from the fields of an object entry, or null if the entry has a problem.
    private static @Nullable Role homebrew(JsonObject object, String id, int entry, List<ScriptProblem> problems) {
        int problemsBefore = problems.size();
        if (!HOMEBREW_ID.matcher(id).matches()) {
            problems.add(new ScriptProblem(entry, ProblemType.INVALID_ID, id));
        }
        String name = required(object, "name", entry, problems);
        String ability = required(object, "ability", entry, problems);
        String teamValue = required(object, "team", entry, problems);
        Team team = teamValue == null ? null : Team.parse(teamValue);
        if (teamValue != null && team == null) {
            problems.add(new ScriptProblem(entry, ProblemType.INVALID_TEAM, teamValue));
        }

        if (problems.size() > problemsBefore || name == null || ability == null || team == null) {
            return null;
        }
        return new Role(id, name, ability, team);
    }

    static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /// Checks whether an entry is the `_meta` entry of the script.
    private static boolean isMeta(JsonElement element) {
        return element.isJsonObject() && "_meta".equals(optString(element.getAsJsonObject(), "id"));
    }

    /// Returns the value of a string field, or null if the field is missing or not a string.
    private static @Nullable String optString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value != null
                        && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : null;
    }

    /// Returns the value of a required string field, or null and a [ProblemType#MISSING_FIELD] problem if the field is
    /// missing or not a string.
    private static @Nullable String required(JsonObject object, String field, int entry, List<ScriptProblem> problems) {
        String value = optString(object, field);
        if (value == null) {
            problems.add(new ScriptProblem(entry, ProblemType.MISSING_FIELD, field));
        }
        return value;
    }

    /// Parses JSON text in strict mode and rejects any content after the top-level value.
    private static JsonElement readStrict(String json) throws ScriptFormatException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ScriptFormatException(List.of(new ScriptProblem(0, ProblemType.SYNTAX, "")));
            }
            return root;
        } catch (JsonParseException | IOException e) {
            throw new ScriptFormatException(List.of(new ScriptProblem(0, ProblemType.SYNTAX, syntaxLine(e))));
        }
    }

    /// Extracts the line number from a Gson syntax error, or an empty string if the message has none.
    private static String syntaxLine(Exception e) {
        Matcher matcher = GSON_LOCATION.matcher(String.valueOf(e.getMessage()));
        return matcher.find() ? matcher.group(1) : "";
    }
}
