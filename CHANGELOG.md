# Changelog

## 1.1.0

- **Boss phase warnings** for 38 bosses: a pop-up such as "⚠ PHASE 3: CLOCK IN 2.1%" as a boss nears its next phase, and a "PHASE 3: CLOCK!" alert when it starts.
- New settings: phase warnings on/off, **warn within** (1–15% boss HP) and warning sound.
- **Boss HP is now its own HUD element**, showing the boss's name, phase tick marks on the bar and the next phase.
- **HUD editor** now handles every element separately: drag to move, scroll over an element to resize just that one, Shift to snap, R to reset.
- **Redesigned settings menu**: a wider two-column layout that fits on screen without scrolling.
- Debug mode now also logs boss bars.

## 1.0.0

Initial release.

- Live, peak and average DPS, total damage and fight time.
- Boss HP % with a colour-coded bar.
- Reads Telos's custom damage-number font, and only counts damage you deal.
- Settings menu (default key: O, or `/dps`) and a drag-and-drop HUD editor.
- Settings are saved to `config/telosutils.json`.
