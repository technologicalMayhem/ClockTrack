# Schulprojekt: ClockTrack

## Projektbeschreibung

Für mein Projekt entwickle ich einen Tracker für meine wöchentlichen Spielrunden des Spiels Blood on the Clocktower, die ich selbst als Spielleiter (Storyteller) betreue.

Die Anwendung verwaltet Spieler, Sitzungen und die jeweiligen Rollenzuweisungen pro Sitzung, inklusive Informationen wie Team-Zugehörigkeit, Todestag und Todesursache innerhalb einer Partie. Zusätzlich lässt sich optional das verwendete Skript (Script) zu jeder Sitzung hinterlegen. Die Daten stammen aus echten, bereits gespielten Runden meiner eigenen Gruppe. Als Zusatzfunktion plane ich eine Statistik-Auswertung, zum Beispiel Gewinnraten nach Team oder die am häufigsten gespielten Rollen pro Spieler.

Die Datenbank besteht aus vier Tabellen mit mehreren Fremdschlüsselbeziehungen sowie einer zusammengesetzten Eindeutigkeits-Constraint zwischen Sitzung und Spieler.

Als User Interface erstelle ich eine Konsolenanwendung die einzelnen Menüs in einer Stack-Architektur präsentiert. Dies erlaubt es Dialog ineinander zu verschachteln. Es gibt Menüs zum Verwalten von Sitzungen, Spielern und zum Einsehen von Gesamt-Statistiken und Statistiken zu einzelnen Spielern.

