# Telos Utils

Lightweight, client-side utilities for **Telos Realms**. Currently it has a **DPS meter** and a **boss HP display**.

It reads the damage numbers that pop up over enemies when you hit them and turns them into live DPS, peak DPS, average DPS and fight totals. It also reads the boss bar at the top of the screen, so you can see exactly how much health the boss has left.

## Features

- **Live DPS**: damage from the last 5 seconds ÷ 5 (adjustable window).
- **Peak DPS**: the highest DPS you hit during the fight.
- **Average DPS**: sampled every second, and only while you're actually hitting, so walking away from the boss doesn't tank your average.
- **Total damage and fight time**.
- **Boss HP %** with a colour-coded bar (green → yellow → red), read from the boss bar.
- **Fight summary in chat** when a fight ends.
- **Only counts the damage you deal**, not the damage you take.
- **Settings menu**: press **O** (rebindable) or type `/dps`.
- **HUD editor**: drag to move, scroll to resize, hold Shift to snap to the centre.
- All settings are saved between sessions.

## How it works

Telos draws damage numbers with its own resource-pack font. The mod reads that font from the server's resource pack, so it keeps working when Telos updates the pack. Boss HP comes from the fill of the vanilla boss bar, which Telos sets to the boss's remaining health.

The mod is **entirely client-side**. It only reads what's already on your screen and never sends anything to the server.

## Commands

| Command | What it does |
|---|---|
| `/dps` or `/dps menu` | Opens the settings menu |
| `/dps reset` | Clears the current stats |
| `/dps toggle` | Shows or hides the HUD |
| `/dps range <1-64>` | Only count damage numbers within this many blocks |
| `/dps debug` | Prints every floating text the mod sees, for troubleshooting |

## Requirements

- Minecraft **26.1.2**
- **Fabric Loader** 0.18 or newer
- **Fabric API**

## Notes

- Damage numbers from other players near you look the same as yours, so in a group the meter shows roughly the damage of everyone nearby. Lower the range in the menu to reduce this.
- The UI style is inspired by [Melinoe](https://modrinth.com/project/fsZHbO2r).

## Building from source

You need JDK 25. Then run:

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

MIT. Found a bug? [Open an issue](https://github.com/ShyGuy03/telos-utils/issues).
