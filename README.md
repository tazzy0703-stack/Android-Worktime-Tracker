<img width="1080" height="2186" alt="times" src="https://github.com/user-attachments/assets/8860d837-d588-49e1-9be1-c7f089ea4052" />
<img width="1080" height="2186" alt="settings" src="https://github.com/user-attachments/assets/1b405e4f-37aa-42f4-8731-4e4938ea07fc" />
<img width="1080" height="2186" alt="dashboard" src="https://github.com/user-attachments/assets/5cfeb97a-e0f5-43d2-8404-a3947f6cd064" />
# Arbeitszeit Geofence

Private Android-App zur automatischen Arbeitszeiterfassung per Arbeitsplatz-Geofence.

Die App erkennt über einen konfigurierbaren Geofence, ob sich das Gerät im definierten Arbeitsbereich befindet. Beim Betreten des Bereichs kann die Arbeitszeit automatisch gestartet und beim Verlassen automatisch beendet werden. Zusätzlich stehen manuelle Funktionen, Auswertungen, Korrekturmöglichkeiten und eine Kartenansicht zur Verfügung.

> Status: Stabiler Privat-Release  
> Version: `1.0.0`  
> Plattform: Android  
> Distribution: GitHub Releases (APK)  
> Play Store: aktuell nicht geplant

---

## Funktionen

### Arbeitszeiterfassung

-   Automatischer Start per Geofence ENTER
-   Automatischer Stop per Geofence EXIT
-   Manuelles Starten und Stoppen
-   Manuelle Pausenverwaltung
-   Permanente Android-Benachrichtigung während aktiver Zeiterfassung
-   Tagesabschluss manuell oder automatisch

### Auswertung

-   Tagesansicht
-   Wochenansicht
-   Monatsansicht
-   Jahresansicht
-   Arbeitszeit-Salden
-   Detailansicht einzelner Arbeitstage
-   Bearbeitung bestehender Einträge
-   Löschen einzelner Einträge
-   CSV-Export von Auswertungen

### Planung

-   Konfigurierbare Soll-Arbeitszeit
-   Standard-Pausenzeit
-   Individuelle Arbeitstage
-   Sondertage (Urlaub, Feiertag, Krank, Frei)

### Standort

-   Arbeitsplatzverwaltung über OpenStreetMap
-   Kartenansicht mit osmdroid
-   Konfigurierbarer Geofence-Radius
-   Registrierung und Entfernung von Geofences

---

## Screenshots

### Dashboard

/docs/images/dashboard.png

### Zeiterfassung

/docs/images/times.png

### Einstellungen

/docs/images/settings.png

---

## Installation

Die aktuelle APK steht über die GitHub Releases zur Verfügung.

### APK herunterladen

```text
GitHub
→ Releases
→ Latest Release
→ app-release.apk
```

### Installation

```text
1. APK herunterladen
2. Installation aus unbekannten Quellen erlauben
3. APK installieren
4. Standortberechtigungen freigeben
```

---

## Technische Basis

-   Kotlin
-   Jetpack Compose
-   Room Database
-   Google Geofencing API
-   Android Foreground Services
-   OpenStreetMap / osmdroid

---

## Dokumentation

Weitere Unterlagen befinden sich im Verzeichnis `docs`.

### Projektunterlagen

-   [docs/CHANGELOG.md](docs/CHANGELOG.md)
-   [docs/INSTALLATION.md](docs/INSTALLATION.md)
-   [docs/PERMISSIONS.md](docs/PERMISSIONS.md)
-   [docs/PRIVACY_NOTES.md](docs/PRIVACY_NOTES.md)
-   [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md)
-   [docs/ROADMAP.md](docs/ROADMAP.md)
-   [docs/TESTPLAN.md](docs/TESTPLAN.md)

### Design

-   [design/MATRIX_THEME.md](design/MATRIX_THEME.md)

---

## Datenschutz

Die Anwendung arbeitet vollständig lokal auf dem Gerät.

-   Keine Cloud-Anbindung
-   Keine Benutzerkonten
-   Keine Telemetrie
-   Keine Datenübertragung an externe Dienste
-   Arbeitszeitdaten verbleiben auf dem Gerät

Weitere Informationen sind in den Datenschutz-Hinweisen dokumentiert.

---

## Projektstatus

Version 1.0 stellt einen funktionsfähigen und stabilen Stand für die private Nutzung bereit.

Enthalten sind:

-   Geofence-basierte Arbeitszeiterfassung
-   Dashboard
-   Arbeitszeitauswertung
-   CSV-Export
-   Sondertage
-   Kartenintegration
-   Matrix-UI

---

## Lizenz / Nutzung

Private Software.

```text
Copyright (c) 2026 Kai Becker.
All rights reserved.
```

Dieses Projekt wird ohne Open-Source-Lizenz veröffentlicht. Nutzung, Weitergabe und Änderungen bedürfen der Zustimmung des Rechteinhabers.
