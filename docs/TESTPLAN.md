# Testplan

## Allgemein

Nach jeder neuen Release-APK prüfen:

```text
1. Update über bestehende Release-App funktioniert
2. App startet
3. Daten bleiben erhalten
4. Geofence-Einstellungen bleiben erhalten
5. Notification funktioniert
```

## Geofence ENTER

Vorbedingung:

- Geofence ist registriert
- Gerät befindet sich außerhalb des Geofence

Test:

```text
1. In Geofence-Bereich bewegen
2. Einige Minuten warten
3. Notification prüfen
4. App öffnen
```

Erwartung:

```text
Arbeitszeit startet automatisch.
Notification erscheint.
Status zeigt Geofence Innen.
```

## Geofence EXIT

Vorbedingung:

- Arbeitszeit läuft automatisch oder manuell
- Gerät befindet sich innerhalb des Geofence

Test:

```text
1. Geofence-Bereich deutlich verlassen
2. Einige Minuten warten
3. App öffnen
```

Erwartung:

```text
Arbeitszeit stoppt automatisch.
Notification verschwindet.
Ein Eintrag mit Quelle Geofence wird gespeichert.
```

## Pause

Test:

```text
1. Arbeitszeit starten
2. Pause Start drücken
3. Einige Minuten warten
4. Pause Stop drücken
5. Arbeitszeit stoppen
```

Erwartung:

```text
Pausenzeit wird im Eintrag berücksichtigt.
Nettozeit ist Bruttozeit minus Pause.
```

## Tagesabschluss manuell

Test:

```text
1. Arbeitszeit starten
2. Tagesabschluss drücken
3. Eintrag prüfen
```

Erwartung:

```text
Arbeitszeit wird geschlossen.
Quelle ist DayClose.
Kommentar ist Manueller Tagesabschluss.
```

## Tagesabschluss automatisch

Test:

```text
1. Tagesabschlusszeit setzen
2. Arbeitszeit aktiv lassen
3. Tagesabschlusszeit abwarten
4. App öffnen
```

Erwartung:

```text
Arbeitszeit wurde geschlossen.
Quelle ist DayClose.
Kommentar ist Automatischer Tagesabschluss.
```

Hinweis: Android kann WorkManager-Ausführungen durch Akku-/Doze-Regeln verzögern.

## Eintrag bearbeiten

Test:

```text
1. Eintrag öffnen
2. Startzeit ändern
3. Endzeit ändern
4. Pause ändern
5. Kommentar ändern
6. Speichern
```

Erwartung:

```text
Eintrag wird aktualisiert.
Summen werden neu berechnet.
```

## App Reset

Test:

```text
1. App Reset ausführen
2. App neu öffnen
```

Erwartung:

```text
Einträge sind gelöscht.
Tracking-State ist gestoppt.
Einstellungen sind auf Standard zurückgesetzt.
```
