# GitHub Build Anleitung - Arbeitszeit Geofence

Dieses Repository baut per GitHub Actions automatisch eine Debug-APK.

## Nach dem Upload

1. Repository im Browser öffnen.
2. Tab `Actions` öffnen.
3. Falls GitHub fragt, Actions aktivieren.
4. Workflow `Android Debug APK` auswählen.
5. `Run workflow` anklicken, falls kein Build automatisch startet.
6. Den neuesten Workflow-Lauf öffnen.
7. Warten, bis alle Schritte grün sind.
8. Unten unter `Artifacts` die Datei `ArbeitszeitGeofence-debug-apk` herunterladen.
9. ZIP entpacken. Darin liegt `app-debug.apk`.

## Hinweis

Die Debug-APK ist nur für Tests. Für produktive Nutzung muss später eine signierte Release-APK erzeugt werden.
