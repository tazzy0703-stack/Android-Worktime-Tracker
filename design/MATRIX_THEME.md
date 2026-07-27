# Matrix Theme Konzept

## Ziel

Die App soll weg von einer Formular-/Prototypen-Optik und hin zu einer dunklen, modernen Dashboard-Oberfläche.

Stilrichtung:

```text
Dark Matrix / Terminal / Neon Dashboard
```

## Farbkonzept

### Hintergrund

- Fast schwarz
- Optional leicht grünlicher Ton
- Karten und Panels leicht heller als Hintergrund

### Primärfarbe

- Neon-Grün
- Für aktive Arbeitszeit
- Für positive Salden
- Für wichtige Statusränder

### Sekundärfarbe

- Cyan / Türkis
- Für Kartenakzente
- Für neutrale Interaktionen

### Warnfarbe

- Amber / Orange
- Für Pause aktiv
- Für Hinweise

### Negativfarbe

- Rot / Magenta
- Für negative Salden
- Für kritische Aktionen wie Löschen/Reset

## Status-Kacheln

### Arbeitszeit aktiv

Darstellung:

```text
Große Status-Kachel
Neon-grüner Rand
Text: ARBEITSZEIT AKTIV
laufende Dauer prominent
```

### Pause aktiv

Darstellung:

```text
Status-Kachel in Amber/Cyan
Text: PAUSE AKTIV
Pause-Dauer prominent
Arbeitszeit bleibt sichtbar
```

### Gestoppt

Darstellung:

```text
Dunkle neutrale Kachel
Text: GESTOPPT
kein Neon-Rand
```

## Dashboard-Idee

```text
┌─────────────────────────────┐
│ ARBEITSZEIT AKTIV           │
│ 06:42 netto heute            │
│ +00:18 Saldo                 │
└─────────────────────────────┘

[Start] [Stop] [Pause]

[Tag] [Woche] [Monat] [Jahr]

┌ Zusammenfassung ────────────┐
│ Brutto  08:15               │
│ Pause   00:45               │
│ Netto   07:30               │
│ Soll    07:12               │
│ Saldo  +00:18               │
└─────────────────────────────┘

┌ Eintrag ────────────────────┐
│ 07:03–15:18  Geofence       │
│ Pause 45m | Netto 7h30      │
│ [Bearbeiten] [Löschen]      │
└─────────────────────────────┘
```

## Icons

Geplant:

- Start: Play
- Stop: Stop
- Pause: Pause
- Geofence: Location/Pin
- Export: Download/File
- Bearbeiten: Edit
- Löschen: Trash
- Tagesabschluss: Calendar/Check
- Saldo positiv/negativ: Pfeil oder +/- Badge

## UI-Ziele

- Weniger Formular-Optik
- Mehr Kacheln
- Bessere visuelle Hierarchie
- Aktiver Status muss sofort erkennbar sein
- Saldo farblich klar darstellen
- Kritische Aktionen klar trennen
