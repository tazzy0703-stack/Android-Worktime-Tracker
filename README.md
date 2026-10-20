# Arbeitszeit Geofence

Private Android-App zur lokalen Arbeitszeiterfassung mit Arbeitsplatz-Geofence.

> **Letzter veröffentlichter Release:** [`v1.5.0`](https://github.com/tazzy0703-stack/Android-Worktime-Tracker/releases/tag/v1.5.0)
> **Plattform:** Android 14 oder neuer (minSdk 34)
> **Distribution:** APK über GitHub Releases
> **Status:** Änderungen auf `main` werden vor einem nächsten Release im Alltag getestet.

## Funktionen

- Arbeitszeit automatisch beim Betreten und Verlassen eines konfigurierbaren Arbeitsplatz-Geofence starten bzw. beenden
- Manuelles Starten, Stoppen und Erfassen von Pausen; dauerhafte Statusbenachrichtigung
- Manueller und automatischer Tagesabschluss
- Tages-, Wochen-, Monats- und Jahresauswertung mit Sollzeit, Nettozeit, Gutschrift und Saldo
- Tagessoll in Stunden eingeben; individuelle Arbeitstage bestimmen Wochen- und Periodensoll
- Urlaub, Feiertag und Krankheit werden an geplanten Arbeitstagen mit Sollzeit gutgeschrieben; „Frei“ erzeugt keine Gutschrift
- Pausen werden nur manuell erfasst und abgezogen; der Pausen-Richtwert wird nicht automatisch verwendet
- Arbeitszeiteinträge bearbeiten, löschen und als CSV exportieren
- Arbeitsplatz auf einer OpenStreetMap-Karte festlegen und Geofence-Radius konfigurieren
- Geofence-Wiederherstellung nach Geräte-Neustart und App-Update

## Screenshots

### Dashboard

<img width="360" alt="Dashboard" src="screenshots/dashboard.png" />

### Zeiterfassung

<img width="360" alt="Zeiterfassung" src="screenshots/times.png" />

### Einstellungen

<img width="360" alt="Einstellungen" src="screenshots/settings.png" />

## Installation

Die APK des letzten veröffentlichten Releases findest du unter [GitHub Releases](https://github.com/tazzy0703-stack/Android-Worktime-Tracker/releases). Lade das APK-Asset herunter und installiere es auf dem Smartphone. Android kann die einmalige Freigabe zur Installation aus dieser Quelle verlangen.

Für automatische Geofence-Erkennung müssen Standortdienste und präziser Standort aktiviert sein. Stelle in den Android-App-Berechtigungen den Standortzugriff auf **„Immer zulassen“** und erlaube Benachrichtigungen. Lege anschließend Arbeitsplatz und Radius fest und registriere den Geofence in der App. Android und herstellerspezifische Akku-Optimierungen können Hintergrundereignisse verzögern; eine sekundengenaue Erkennung ist nicht garantiert.

## Build über GitHub Actions

Der Workflow **Android APK** startet bei Pushes auf `main` und kann auch manuell ausgeführt werden. Er führt die Unit-Tests aus und stellt eine Debug-APK (`app-debug.apk`) als Actions-Artifact bereit. Die signierte Release-APK (`app-release.apk`) wird nur erstellt, wenn die Repository-Secrets für die Android-Release-Signierung eingerichtet sind. Eine Debug-APK ist für Tests und nicht als reguläres Update gedacht.

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
