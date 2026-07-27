# Installation

## APK herunterladen

Die App wird privat über GitHub Actions verteilt.

Pfad:

```text
GitHub Repository
→ Actions
→ Android APK
→ erfolgreicher Workflow-Lauf
→ Artifacts
→ ArbeitszeitGeofence-release-apk
```

Artifact herunterladen und entpacken.

Darin liegt:

```text
app-release.apk
```

## Installation auf Android

1. `app-release.apk` auf das Android-Gerät kopieren.
2. APK öffnen.
3. Falls Android die Installation blockiert:

```text
Einstellungen öffnen
→ Installation aus dieser Quelle erlauben
→ Zurück
→ Installieren
```

4. App starten.
5. Berechtigungen erlauben.
6. Einstellungen öffnen.
7. Geofence konfigurieren.

## Wichtiger Hinweis

Für produktive Tests immer diese APK verwenden:

```text
app-release.apk
```

Nicht verwenden:

```text
app-debug.apk
```

Debug- und Release-Versionen haben unterschiedliche Signatur-/Paketkonfigurationen und können zu Update-Problemen führen.
