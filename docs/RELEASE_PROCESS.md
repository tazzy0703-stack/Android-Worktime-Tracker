# Release-Prozess

## Voraussetzungen

GitHub Actions nutzt folgende Secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

Der Keystore muss dauerhaft gesichert werden. Ohne denselben Keystore können spätere Updates der Release-App nicht sauber installiert werden.

## Version erhöhen

Vor einem Release in `app/build.gradle.kts` erhöhen:

```kotlin
versionCode = <neue Nummer>
versionName = "<neue Version>"
```

Beispiel:

```kotlin
versionCode = 12
versionName = "0.7.0"
```

## Commit und Push

```powershell
git status
git add .
git commit -m "Release v0.x.x"
git push origin main
```

## GitHub Actions prüfen

Nach dem Push läuft der Workflow:

```text
Android APK
```

Erwartete Artifacts:

```text
ArbeitszeitGeofence-debug-apk
ArbeitszeitGeofence-release-apk
```

Für Installation relevant:

```text
ArbeitszeitGeofence-release-apk
→ app-release.apk
```

## Update-Test

Nach Installation prüfen:

```text
1. App startet
2. Daten bleiben erhalten
3. Version ist aktualisiert
4. Kernfunktionen funktionieren
```
