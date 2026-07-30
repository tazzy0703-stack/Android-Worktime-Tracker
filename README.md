# Arbeitszeit Geofence Vibe Coded

Private Android-App zur automatischen Arbeitszeiterfassung per Arbeitsplatz-Geofence.

Die App erkennt über einen konfigurierbaren Geofence, ob sich das Gerät im Arbeitsbereich befindet. Beim Betreten des Bereichs wird die Arbeitszeit automatisch gestartet, beim Verlassen automatisch beendet. Zusätzlich sind manuelle Start-/Stop-/Pause-Funktionen, Tagesabschluss, Auswertungen und Bearbeitung von Einträgen möglich.

> Status: private Testversion  
> Aktuelle Version: `0.7.0`  
> Distribution: signierte Release-APK über GitHub Actions  
> Play Store: aktuell nicht geplant

---

## Kernfunktionen

-   Automatischer Start per Geofence ENTER
-   Automatischer Stop per Geofence EXIT
-   Manuelles Starten und Stoppen
-   Manuelle Pause
-   Permanente Android-Benachrichtigung während laufender Arbeitszeit
-   Tagesabschluss manuell und automatisch
-   Tag-/Woche-/Monat-/Jahr-Ansichten
-   Bearbeitung und Löschung von Einträgen
-   App Reset
-   Editierbare Arbeitszeitparameter
-   OpenStreetMap/osmdroid-Kartenansicht

---

## Installation

Die App wird aktuell privat verteilt und nicht über den Play Store veröffentlicht.

Installiert wird die signierte Release-APK aus GitHub Actions:

```text
Actions
→ Android APK
→ erfolgreicher Workflow-Lauf
→ Artifacts
→ ArbeitszeitGeofence-release-apk
→ app-release.apk
```

Wichtig:

```text
Immer app-release.apk verwenden.
Nicht app-debug.apk für produktive Tests nutzen.
```

---

## Dokumentation

Weitere Unterlagen liegen unter:

```text
docs/
design/
```

Wichtige Dateien:

-   [`docs/CHANGELOG.md`](http://localhost:52555/Static24Q2y6E8/externalProject/docs/CHANGELOG.md)
-   [`docs/ROADMAP.md`](http://localhost:52555/Static24Q2y6E8/externalProject/docs/ROADMAP.md)
-   [`docs/TESTPLAN.md`](http://localhost:52555/Static24Q2y6E8/externalProject/docs/TESTPLAN.md)
-   [`docs/INSTALLATION.md`](http://localhost:52555/Static24Q2y6E8/externalProject/docs/INSTALLATION.md)
-   [`docs/RELEASE_PROCESS.md`](http://localhost:52555/Static24Q2y6E8/externalProject/docs/RELEASE_PROCESS.md)
-   [`design/MATRIX_THEME.md`](http://localhost:52555/Static24Q2y6E8/externalProject/design/MATRIX_THEME.md)

---

## Private Nutzung

Diese App ist für private Nutzung und begrenzte Verteilung vorgesehen.

Eine Veröffentlichung im Google Play Store ist aktuell nicht geplant, da der Aufwand für Store-Policy, Background-Location-Freigabe, Store-Listing und Review für den kleinen Nutzerkreis nicht sinnvoll ist.

---

## Lizenz / Nutzung

Private App.

```text
Copyright (c) 2026 Kai Becker.
All rights reserved.
```

Keine öffentliche Open-Source-Lizenz.