# NHTotem 0.1.4

This update fixes a handful of issues that were easy to miss in normal play testing. One of them was causing XP to drop even when the player kept their level.

## Fixes

- **XP no longer drops when keep-level is enabled.** `setKeepLevel(true)` keeps the player's level at respawn, but it does not stop the death event from dropping experience. NHTotem now clears the dropped XP as well.
- **Totem names with spaces can be given again.** Commands like `/nhtotem give Totem of Gold Steve` now parse the full name instead of stopping at the first word.
- **Duplicate stable IDs are rejected.** If two totems use the same `id`, the second one is skipped with a warning. Before, death could trigger the wrong totem.
- **Respawn commands run when they should.** A totem is checked against the death world. The respawn world no longer suppresses the respawn commands.
- **Invalid drop probabilities are corrected.** `NaN`, infinity, and values outside `0.0` to `1.0` no longer cause every item to drop.
- **ItemsAdder is declared as a soft dependency.** This fixes class access on servers where ItemsAdder loads after NHTotem. API linkage errors are now caught instead of escaping.
- **CraftEngine reload is safer.** The plugin no longer schedules a CraftEngine refresh after it has been disabled.
- **Help no longer mentions the old commands.** `/nhtotem help` now only lists the current command set.
- **Material names are trimmed.** Leading or trailing spaces around `CE:` items no longer break the CraftEngine prefix check.

## Compatibility

- Paper / Purpur 1.20.1 to 26.3
- Spigot 1.20.1+ (built on the Bukkit API, not fully regression-tested)
- CraftEngine 26.9.2 (optional)
- ItemsAdder (optional)
- Java 17 bytecode, Java 17+ runtime, Java 25+ for Minecraft 26.1+ servers
- Folia is not supported

## Testing

24 MockBukkit tests pass on the Paper 26.3 API. The new tests cover:

- Keeping XP without dropping experience
- Giving a totem name with spaces
- Rejecting duplicate IDs
- Falling back from an invalid `NaN` drop probability
- Running respawn commands when the respawn world is disabled

The build was also started on a real Purpur 26.3 server with CraftEngine 26.9.2. All seven tutorial totems loaded.

## Install

1. Stop the server.
2. Remove the old NHTotem jar.
3. Put `NHTotem-0.1.4.jar` into `plugins/`.
4. Start the server.
5. Edit `plugins/NHTotem/config.yml`.
6. Run `/nhtotem reload`.

## SHA-256

```text
NHTotem-0.1.4.jar
6CE3CF94FA82FF67BD87B68EC8DEB00D2AEB9337FD7CA25BBA33449BB2E5E071

NHTotem-CraftEngine-Tutorial-v1.1.zip
9A465D8BAFA8FF2BAB6ABDA4F84377CB7F1C454E635238955887B88AB7E724FD
```
