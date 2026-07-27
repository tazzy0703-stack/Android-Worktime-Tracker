# Roadmap

## v0.7.x – Realtest und Stabilisierung

Ziel: App mehrere Tage unter realen Bedingungen testen.

Beobachten:

- Geofence ENTER startet zuverlässig
- Geofence EXIT stoppt zuverlässig
- Keine doppelten Blöcke
- Tagesabschluss verhält sich korrekt
- Pausenlogik passt
- Update-Installation bleibt sauber
- Akkuoptimierung beeinflusst Geofence nicht kritisch

## v0.8.0 – Export

Geplant:

- CSV-Export
- Export nach Zeitraum:
  - Tag
  - Woche
  - Monat
  - Jahr
  - Alle
- Spalten:
  - Datum
  - Start
  - Ende
  - Brutto
  - Pause
  - Netto
  - Soll
  - Saldo
  - Quelle
  - Kommentar
- Export über Android-Dateidialog
- Dateiname z. B.:

```text
arbeitszeit_export_2026-07-27.csv
```

Optional später:

- JSON-Backup
- Restore-Funktion

## v0.9.0 – Matrix Theme / Dashboard UI

Geplant:

- Dunkles Matrix-Style-Theme
- Dashboard-Optik statt Formular-Optik
- Karten-/Zeiten-Kacheln
- Farbige Saldoanzeige
- Icons
- Hervorgehobener Status:
  - Arbeitszeit aktiv
  - Pause aktiv
  - Gestoppt
- Optische Trennung von:
  - Status
  - Statistik
  - Einträgen
  - Einstellungen

## v1.0.0 – Private Stable Release

Geplant:

- Realtest-Fixes
- Export finalisieren
- UI finalisieren
- Berechtigungs-/Akkuhinweise ergänzen
- Technische Bereinigung
- Signierte private Release-Version
