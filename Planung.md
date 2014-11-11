# Ziel Formulierung

Es soll eine Anwendung erstellt werden, die zu den durch den Benutzer ausgewählten TV Serien die neuesten Informationen (Nachrichten) sowie Episoden aus mehreren Quellen zusammenträgt. Des weiteren wird eine "to watch"-Liste gespeichert, die die noch an zu sehenden Episoden der TV Shows speichert.

Für alle Serien gibt es eine Übersicht und eine Suche, 

Bei (durch den Benutzer festgelegten) wichtigen Neuigkeiten, wird eine Notification per Push sofort an den Android Client gesendet. Es lassen sich optional die Release-Daten neuer Episoden in den Kalender des Gerätes synchronisieren (soweit die API dafür auf dem Gerät verfügbar ist).

# Design

Der Aggregator Service selbst wird als gehosteter Service implementiert, welcher dann aus mehreren Quellen Informationen sammelt, auf ein gemeinsames Format bringt und dieses in einer Datenbank speichert.

Auf dem Webservice werden ebenfalls die Account-Daten der Benutzer gespeichert, z.B. die Serien, welche für den Benutzer von Interesse sind.

In der Android Client-Anwendung kann sich der Benutzer einloggen, woraufhin seine Daten vom Webservice auf der Gerät geladen und dort bearbeitet bzw. angezeigt werden können.

# Dokumentation

Es muss nur Dokumentation für den Android client erstellt werden!

Dies beinhaltet JavaDoc für alle Klassen und ein Klassendiagramm um den Zusammenhang der Klassen dar zu stellen.

# Implementation

## Web Service

- [ ] Aggregator Provider: Findet zu einer gegebenen Serie alle möglichen Informationen auf der Seite des jeweiligen Providers
- [ ] Crawl Jobs: Zeigergesteuert durchsuchen der Datenbestände nach Neuigkeiten 

### API

- [ ] Authentifizierung mit User-Credentials (Session/OAuth)
- [ ] Read-Only API für die Abfrage von Informationen (Episoden-Daten, Nachrichten)
- [ ] Write API, zum bewerten von Episoden (eventuell weiteres?)
- [ ] Push notification zum senden an den Benutzer

## Android Client

### Activities

- [ ] Kalender: Übersicht der bald erscheinenden Episoden aller markierten Serien
- [ ] "Watch Again": Welche Episoden habe ich vor "einiger Zeit" gut bewertet, um sie eventuell erneut zu sehen
- [ ] Übersicht: Alle Informationen (Cast, Episoden-Anzahl, Bewertungen) zu einer Bestimmten Serie, möglichkeit diese Serie ab jetzt zu "verfolgen"
- [ ] Staffeln/Staffel/Episode: Alle Staffeln/Episoden einer Serie (Beschreibung, Bewertung, Gesehen?)
- [ ] Suche: Findet neue Serien nach Suchbegriff
- [ ] Watchlist: Liste aller "neuen" Episoden, welche noch nicht gesehen wurden.

### Features

- [ ] Einstellbare Push-Notifications: Der Benutzer kann Ereignisse (Neue Episode, News) global oder per Serie auswählen, um für diese sofort eine Benachrichtigung zu erhalten.
- [ ] Synchronisation um kommende Episoden in den lokalen Kalender ein zu tragen

# Quellen

* IMDB (Keine API, HTML Parsen...?)
* TMDB (http://docs.themoviedb.apiary.io/)
* Rotten Tomatoes (http://developer.rottentomatoes.com/)
* Serienjunkies.de (http://www.serienjunkies.de/rss/serie/<serie>.xml RSS Parsen)
* Netflix (http://developer.netflix.com/docs/read/REST_API_Reference)