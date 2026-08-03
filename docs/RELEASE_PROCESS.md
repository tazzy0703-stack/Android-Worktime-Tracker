# Release-Prozess

## Entwicklung

Änderungen werden lokal entwickelt und getestet.

## Build

Release Build erzeugen:

```bash
./gradlew assembleRelease
```

## Test

Vor jedem Release prüfen:

- App Start
- Geofence
- Dashboard
- Export
- Sondertage
- Tagesabschluss

## Veröffentlichung

1. Git Tag erstellen
2. GitHub Release anlegen
3. APK hochladen
4. Changelog aktualisieren

## Versionierung

Format:

```text
MAJOR.MINOR.PATCH
```

Beispiel:

```text
1.0.0
```
