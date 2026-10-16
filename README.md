<div align="center">
  <img src="assets/icon.svg" alt="Clock Track Logo" width="256">
</div>

# Schulprojekt: ClockTrack

## Projektbeschreibung

Für mein Projekt entwickle ich einen Tracker für meine wöchentlichen Spielrunden des Spiels [Blood on the Clocktower](https://de.wikipedia.org/wiki/Blood_on_the_Clocktower),
die ich selbst als Spielleiter (Storyteller) betreue. Blood on the Clocktower ist ein Social-Deduction-Spiel, ähnlich
wie Werwolf: Ein Team aus guten Spielern versucht, die bösen Spieler zu finden, bevor diese die Überhand gewinnen.

Die Anwendung verwaltet Spieler, Skripte und Sitzungen. Zu jeder Sitzung werden das gespielte Skript, der Storyteller
und das Gewinnerteam gespeichert, sowie für jeden Spieler der Charakter, die Team-Zugehörigkeit und gegebenenfalls der Todeszeitpunkt 
und Todesursache.

Auch können Statistiken zu Spielern und Skripten eingesehen werden. *Noch nicht implementiert*

Beim ersten Start wird die Möglichkeit gegeben einen Test-Datensatz zu nutzen um das Programm in Aktion zu sehen.

Die Anwendung ist auf Deutsch und Englisch verfügbar und richtet sich nach der Sprache des Systems.

## Voraussetzungen

- JDK 25
- Maven wird nicht benötigt, da der Maven Wrapper (`mvnw`) im Repository enthalten ist.

## Starten und Bauen

Die Anwendung wird direkt aus dem Repository gestartet:

```sh
./mvnw javafx:run
```

Für eine eigenständige Version, die ohne installiertes JDK läuft, wird mit jlink ein Image gebaut:

```sh
./mvnw javafx:jlink
```

Zum erstellen des Image wird JDK 25 benötigt. Das Image liegt danach in `target/ClockTrack/`. Gestartet wird es über `target/ClockTrack/bin/ClockTrack`.

## Entwicklung

Der Code ist in folgende Pakete aufgeteilt, alle unterhalb von `net.techmayhem.clocktrack`:

- `database`: Zugriff auf die SQLite-Datenbank und das Datenbankschema
- `model`: Spieler, Skripte, Sitzungen und Teilnahmen, wie sie gespeichert werden
- `projections`: Zusammenfassungen mit Statistiken für die Übersichten
- `sample`: die Beispieldaten
- `ui`: die Benutzeroberfläche, unterteilt in die Bereiche `player`, `script` und `session` sowie gemeinsam genutzte
  Komponenten (`component`) und Dialoge (`dialog`)

Der Code wird mit Spotless und dem palantir-java-format formatiert. Die Formatierung wird mit
`./mvnw spotless:check` geprüft oder mit `./mvnw spotless:apply` angewendet. Ein Pre-Commit-Hook in `.githooks/` führt diese Prüfung vor jedem Commit aus. Maven
installiert ihn automatisch während der `validate`-Phase.

## Weitere Unterlagen

- [Diagramme](Diagramme/): ER-Diagramm, Klassendiagramm und UI-Mockup. Diese stammen aus den ersten Phasen des
  Projektes und bilden nicht den aktuellen Stand ab.
- [TODO-Liste](TODO.md)
- [Lizenz](LICENSE)