# Android Worktime Tracker

Android-App zur automatischen Arbeitszeiterfassung über einen Arbeitsplatz-Geofence.

## Status v0.1.1

- Android 14+ (`minSdk = 34`)
- Kotlin + Jetpack Compose
- Room/SQLite lokal
- Geofence ENTER/EXIT-Grundlage
- manuelles Start/Stop
- manuelles Pause Start/Stop
- keine Rundung
- fehlende Pause = Warnung, kein automatischer Abzug
- GitHub Actions Workflow erzeugt Debug-APK

## APK bauen

Push auf `main` startet `.github/workflows/android-debug-apk.yml`.
Die APK liegt nach erfolgreichem Workflow als Artifact `ArbeitszeitGeofence-debug-apk` bereit.
