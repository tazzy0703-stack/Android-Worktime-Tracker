# Arbeitszeit Geofence

Private Android-App zur lokalen Arbeitszeiterfassung mit Arbeitsplatz-Geofence.

> **Letzter veröffentlichter Release:** [`v1.5.0`](https://github.com/tazzy0703-stack/Android-Worktime-Tracker/releases/tag/v1.5.0)
> **Plattform:** Android 14 oder neuer (minSdk 34)
> **Distribution:** APK über GitHub Releases
> **Status:** Die Überarbeitung auf dem Branch `fix/geofence-review` (siehe [Änderungen gegenüber v1.5.0](#änderungen-gegenüber-v150)) wird im Alltag getestet, bevor sie nach `main` und in einen Release übernommen wird.

## Funktionen

- Arbeitszeit automatisch beim Betreten und Verlassen eines konfigurierbaren Arbeitsplatz-Geofence starten bzw. beenden
- Kurzes Verlassen und Zurückkehren innerhalb von 5 Minuten setzt den letzten automatisch beendeten Block fort, statt einen neuen Eintrag anzulegen
- Anwesenheitsabgleich beim Öffnen der App: Befindest du dich bereits am Arbeitsplatz und die Zeit läuft nicht, wird sie gestartet (nicht nach manuellem Stop oder Tagesabschluss)
- Manuelles Starten, Stoppen und Erfassen von Pausen; dauerhafte Statusbenachrichtigung
- Manueller und automatischer Tagesabschluss
- Tages-, Wochen-, Monats- und Jahresauswertung mit Netto-Arbeit, Soll, Pause und Saldo; bezahlte Abwesenheit wird als Zusatzzeile gezeigt
- Das Soll zählt nur bis einschließlich heute, damit erarbeitete Überstunden im Saldo sichtbar werden; künftige Tage werden als „geplant“ gekennzeichnet
- Tagessoll in Stunden eingeben; individuelle Arbeitstage bestimmen Wochen- und Periodensoll
- Urlaub, Feiertag und Krankheit werden an geplanten Arbeitstagen mit Sollzeit gutgeschrieben; „Frei“ erzeugt keine Gutschrift
- Pausen werden nur manuell erfasst und abgezogen; der Pausen-Richtwert wird nicht automatisch verwendet
- Arbeitszeiteinträge bearbeiten, löschen und als CSV exportieren
- Arbeitsplatz auf einer OpenStreetMap-Karte festlegen und Geofence-Radius konfigurieren
- „Aktuelle Position speichern“ nutzt eine frische Hochpräzisions-Ortung; bei einer Genauigkeit schlechter als 100 m wird abgebrochen, ab 50 m erscheint ein Hinweis
- Nach Änderung von Arbeitsplatz oder Radius wird ein zuvor aktiver Geofence automatisch neu registriert
- Geofence-Wiederherstellung nach Geräte-Neustart und App-Update; schlägt sie fehl, wird sie im Hintergrund mit Wartezeit mehrfach wiederholt

## Screenshots

### Dashboard

<img width="360" alt="Dashboard" src="screenshots/dashboard.png" />

### Zeiterfassung

<img width="360" alt="Zeiterfassung" src="screenshots/times.png" />

### Einstellungen

<img width="360" alt="Einstellungen" src="screenshots/settings.png" />

## Änderungen gegenüber v1.5.0

Stand Branch `fix/geofence-review` (noch nicht in einem Release):

- **Tagesabschluss:** Datum und Endzeit richten sich nach dem geplanten Abschlusszeitpunkt. Läuft der Hintergrundjob verzögert (Energiesparmodus, nach Mitternacht), wird nicht mehr der Folgetag abgeschlossen.
- **Hintergrunddienst:** Aktionen laufen nacheinander und sind gegen fehlende Standortberechtigung und Hintergrundstart-Beschränkungen abgesichert; Eintrag und Zustand werden gemeinsam gespeichert.
- **Geofence:** keine unnötige Neuregistrierung bei jedem App-Start, Wiederholung der Wiederherstellung mit Backoff, Anwesenheitsabgleich beim Öffnen der App.
- **Sicherheit/Konfiguration:** der Wiederherstellungs-Receiver ist nicht mehr exportiert; das Room-Schema wird exportiert (`app/schemas`), damit künftige Datenbankänderungen migrierbar sind.
- **Oberfläche:** einheitliches 2×2-Raster der Kennzahlen, die Karte „Gutschrift-Tage“ entfällt, Dashboard-Schriften sind kleiner.

Die Änderungen sind per Build und Unit-Tests geprüft, das Verhalten am Gerät (insbesondere Verlassen/Betreten, Neustart, Tagesabschluss über Mitternacht) wird im Alltagstest bestätigt.

## Installation

Die APK des letzten veröffentlichten Releases findest du unter [GitHub Releases](https://github.com/tazzy0703-stack/Android-Worktime-Tracker/releases). Lade das APK-Asset herunter und installiere es auf dem Smartphone. Android kann die einmalige Freigabe zur Installation aus dieser Quelle verlangen.

Für automatische Geofence-Erkennung müssen Standortdienste und präziser Standort aktiviert sein. Stelle in den Android-App-Berechtigungen den Standortzugriff auf **„Immer zulassen“** und erlaube Benachrichtigungen. Lege anschließend Arbeitsplatz und Radius fest und registriere den Geofence in der App. Android und herstellerspezifische Akku-Optimierungen können Hintergrundereignisse verzögern; eine sekundengenaue Erkennung ist nicht garantiert. Für zuverlässige Ergebnisse empfiehlt sich ein Radius von mindestens 100 m und die Ausnahme der App von der Akku-Optimierung. Der Anwesenheitsabgleich beim App-Start wertet nur Ortungen mit einer Genauigkeit von höchstens 100 m aus.

## Build über GitHub Actions

Der Workflow **Android APK** startet bei Pushes auf `main` und auf `fix/**`-Branches, bei Pull Requests gegen `main` und kann auch manuell ausgeführt werden. Er führt die Unit-Tests aus und stellt eine Debug-APK (Artifact `ArbeitszeitGeofence-debug-apk`) bereit. Die signierte Release-APK (Artifact `ArbeitszeitGeofence-release-apk`) wird nur erstellt, wenn die Repository-Secrets für die Android-Release-Signierung eingerichtet sind. Nur sie ist mit dem Release-Schlüssel signiert und lässt sich als Update über eine installierte Version einspielen. Die Debug-APK hat eine eigene App-ID (`.debug`) und läuft als separate App neben der Release-Version.

Für einen manuellen Lauf: Repository → **Actions** → **Android APK** → **Run workflow** → Branch auswählen → **Run workflow**. Nach erfolgreichem Lauf das gewünschte APK aus **Artifacts** herunterladen.

## Technische Basis

Kotlin, Jetpack Compose, Room, Google Play Services Geofencing, Android Foreground Service, WorkManager und OpenStreetMap/osmdroid.

## Datenschutz und Nutzung

Arbeitszeit- und Planungseinträge werden lokal auf dem Gerät gespeichert. Für die Kartenansicht werden OpenStreetMap-Kacheln über das Internet geladen. Die App ist privat und wird ohne Open-Source-Lizenz veröffentlicht.

```text
Copyright (c) 2026 Kai Becker.
All rights reserved.
Source code is private and not licensed for redistribution or public use.
```
