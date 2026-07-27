# Arbeitszeit Geofence

Private Android-App zur automatischen Arbeitszeiterfassung per Arbeitsplatz-Geofence.

Die App erkennt über einen konfigurierbaren Geofence, ob sich das Gerät im Arbeitsbereich befindet. Beim Betreten des Bereichs wird die Arbeitszeit automatisch gestartet, beim Verlassen automatisch beendet. Zusätzlich sind manuelle Start-/Stop-/Pause-Funktionen, Tagesabschluss, Auswertungen und Bearbeitung von Einträgen möglich.

> Status: private Testversion  
> Aktuelle Version: `0.7.0`  
> Distribution: signierte Release-APK über GitHub Actions  
> Play Store: aktuell nicht geplant

---

## Funktionsumfang

### Zeiterfassung

- Automatischer Start per Geofence ENTER
- Automatischer Stop per Geofence EXIT
- Manuelles Starten und Stoppen
- Manuelles Starten und Stoppen von Pausen
- Sichtbare Android-Benachrichtigung während laufender Arbeitszeit
- Notification-Aktionen:
  - Stop
  - Pause Start
  - Pause Stop

### Geofence

- Arbeitsplatz per aktueller Position speichern
- Arbeitsplatz-Marker per langem Druck auf die Karte setzen
- Radius in Metern editierbar
- OpenStreetMap/osmdroid-Kartenansicht
- Geofence registrieren und entfernen

### Auswertung

- Ansicht nach:
  - Tag
  - Woche
  - Monat
  - Jahr
- Anzeige von:
  - Bruttozeit
  - Pause
  - Nettozeit
  - Sollzeit
  - Saldo
  - Anzahl Einträge
  - Tage mit Einträgen

### Tagesabschluss

- Manueller Tagesabschluss
- Automatischer Tagesabschluss über geplanten Worker
- Standardzeit aktuell: `23:59`
- Laufender Block wird bei Tagesabschluss geschlossen
- Quelle:
  - `DayClose`
- Kommentare:
  - `Manueller Tagesabschluss`
  - `Automatischer Tagesabschluss`

### Bearbeitung / Reset

- Einträge bearbeiten
  - Startzeit
  - Endzeit
  - Pausenzeit
  - Kommentar
- Einzelne Einträge löschen
- Alle Einträge löschen
- App Reset:
  - Einträge löschen
  - Tracking-State zurücksetzen
  - Einstellungen auf Standard setzen

### Einstellungen

- Sollzeit pro Tag in Minuten
- Standardpause in Minuten
- Tagesabschlusszeit im Format `HH:mm`
- Geofence-Radius
- Arbeitsplatz-Koordinate
- Schicht über Mitternacht ist vorbereitet, aktuell aber noch deaktiviert

---

## Installation

Die App wird aktuell nicht über den Play Store verteilt.

Installation erfolgt über die signierte Release-APK aus GitHub Actions:

```text
Actions
→ Android APK
→ erfolgreicher Workflow-Lauf
→ Artifacts
→ ArbeitszeitGeofence-release-apk
→ app-release.apk
