# Changelog

## v1.0.0 - 2026

Erster stabiler Release der Anwendung.

### Funktionen

- Geofence-basierte Arbeitszeiterfassung
- Manuelles Starten und Stoppen
- Pausenverwaltung
- Automatischer Tagesabschluss
- Dashboard mit Kennzahlen
- Tages-, Wochen-, Monats- und Jahresansicht
- Bearbeitung von Arbeitszeiteinträgen
- CSV-Export
- Sondertage (Urlaub, Feiertag, Krank, Frei)
- Konfigurierbare Arbeitstage
- OpenStreetMap-Integration

### Oberfläche

- Matrix-inspiriertes Design
- Dunkles Theme
- KPI-Dashboard
- Verbesserte Lesbarkeit

### Technische Basis

- Kotlin
- Jetpack Compose
- Room Database
- Google Geofencing API
- OSMdroid

---

## Roadmap

Siehe:

- docs/ROADMAP.md

## v0.7.0

- Einträge editierbar:
  - Startzeit
  - Endzeit
  - Pausenminuten
  - Kommentar
- Einzelne Einträge löschbar
- Alle Einträge löschbar
- App Reset ergänzt
- Arbeitszeitparameter editierbar:
  - Sollzeit pro Tag
  - Standardpause
  - Tagesabschlusszeit

## v0.6.0

- Tagesabschluss ergänzt
- Manueller Tagesabschluss
- Automatischer Tagesabschluss über Worker
- Quelle `DayClose`
- Kommentare:
  - `Manueller Tagesabschluss`
  - `Automatischer Tagesabschluss`
- Schutz gegen doppelten Tagesabschluss
- Zeiten-Ansicht als gemeinsame Scroll-Fläche überarbeitet
- Schicht über Mitternacht vorbereitet

## v0.5.0

- Tag-/Woche-/Monat-/Jahr-Ansichten
- Zusammenfassung je Ansicht:
  - Brutto
  - Pause
  - Netto
  - Soll
  - Saldo
  - Anzahl Blöcke
  - Tage mit Einträgen
- Gefilterte Eintragsliste je Zeitraum

## v0.4.0

- Geofence ENTER startet Arbeitszeit automatisch
- Geofence EXIT stoppt Arbeitszeit automatisch
- Einträge mit Quelle `Geofence`
- Notification startet und stoppt automatisch mit Geofence-Status
- Manuelle Erfassung bleibt parallel nutzbar

## v0.3.4

- Marker per langem Druck auf Karte setzen
- Radius als Eingabefeld statt Slider
- Radiusvalidierung ergänzt
- Geofence-Setup verbessert

## v0.3.3

- osmdroid/OpenStreetMap integriert
- Kartenansicht im Geofence-Bereich
- Arbeitsplatz-Marker
- Radius-Kreis auf Karte
- OSM-Attribution

## v0.3.1

- Navigation ergänzt:
  - Zeiten
  - Einstellungen
- Geofence-Einstellungen in eigenem Bereich
- Platz für spätere Arbeitszeitparameter

## v0.3.0

- Arbeitsplatz-Geofence konfigurierbar
- Radius einstellbar
- Aktuelle Position als Arbeitsplatz speicherbar
- Geofence registrieren und entfernen
- ENTER/EXIT zunächst diagnostisch

## v0.2.0

- Permanente Android-Benachrichtigung während aktiver Zeiterfassung
- Notification-Aktionen:
  - Stop
  - Pause Start
  - Pause Stop
- Notification verschwindet nach Stop
- Foreground Service ergänzt

## v0.1.1

- GitHub Actions Build ergänzt
- Debug-APK als Artifact
- GitHub-Projektstruktur ergänzt
- Erste Unit-Tests

## v0.1.0

- Initialer technischer Prototyp
- Kotlin/Compose-Grundlage
- Room-Datenbank
- Manuelles Start/Stop
- Einfache Eintragsanzeige
