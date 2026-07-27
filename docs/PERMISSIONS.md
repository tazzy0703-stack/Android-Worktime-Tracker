# Berechtigungen

Die App verwendet folgende Android-Berechtigungen:

```text
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
ACCESS_BACKGROUND_LOCATION
POST_NOTIFICATIONS
FOREGROUND_SERVICE
FOREGROUND_SERVICE_LOCATION
INTERNET
ACCESS_NETWORK_STATE
```

## Standort

Standortberechtigungen werden für den Arbeitsplatz-Geofence benötigt.

Zweck:

- Arbeitsplatzbereich erkennen
- ENTER-/EXIT-Ereignisse verarbeiten
- Arbeitszeit automatisch starten und stoppen

## Hintergrundstandort

Hintergrundstandort wird benötigt, damit Geofence-Events auch erkannt werden können, wenn die App nicht aktiv geöffnet ist.

## Benachrichtigungen

Benachrichtigungen werden verwendet, um laufende Arbeitszeit sichtbar zu machen.

Die Notification enthält Aktionen:

```text
Stop
Pause Start
Pause Stop
```

## Foreground Service

Der Foreground Service ist erforderlich, damit laufende Arbeitszeiterfassung für den Nutzer sichtbar bleibt.

## Netzwerk

Internet und Netzwerkstatus werden für OpenStreetMap-Kartenkacheln verwendet.

Es findet aktuell keine Cloud-Synchronisation statt.
