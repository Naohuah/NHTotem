# NHTotem

<p align="center">
  <img src="brand/totem/nhtotem-logo-green-256.png" width="180" alt="NHTotem">
</p>

NHTotem adds a totem that keeps your inventory and XP when you die. It is meant for servers where death should still cost something, but losing an entire inventory is too harsh.

## What it does

When a player dies, NHTotem looks through their inventory for a configured totem. If it finds one, it removes a single totem and keeps the player's items and XP. The totem can be anywhere in the main inventory or in the offhand. It does not need to be held.

That is the main difference from a simple keepInventory toggle. The totem is the price. You can give it to players, sell it in a shop, use it as a dungeon reward, or hand it out only during events.

Each totem can have its own drop chance, so some items still fall on the ground. You can enable Vanishing Curse clearing if you want death to hurt a little more, or blacklist worlds where the totem should not work.

One detail that is easy to miss: keeping the player's level does not automatically stop XP orbs from dropping. NHTotem clears the death event's dropped XP as well, so players do not leave a pile of experience on the ground while also keeping their level.

## Configuration

Totems are configured in `plugins/NHTotem/config.yml`. A minimal example:

```yaml
totems:
  "&6Totem of Gold":
    id: "totem_gold"
    material: TOTEM_OF_UNDYING
    drop-probability: 0.0
    clear-curse: false
    disabled-worlds: []
```

The `id` is optional, but it is worth setting one. It lets you rename the totem later without breaking items that are already in player inventories.

Give a totem with:

```text
/nhtotem give Totem of Gold <player>
```

Totem names can contain spaces. If you leave out the player name, the command gives the totem to yourself. `/nhtotem list` shows every loaded totem.

## Custom items

If you use CraftEngine or ItemsAdder, you can point a totem at one of their items:

```yaml
material: "CE:nhtotem_tutorial:totem_gold"
material: "IA:namespace:item_id"
```

Neither plugin is required. Vanilla materials work fine.

The repository also includes a CraftEngine pack with seven colored totem textures, item definitions, and seven example configs. You can use it as-is or replace the textures with your own. It is a starting point, not a full progression or economy system.

## Compatibility

I have tested NHTotem on Paper and Purpur 1.20.1, 1.21.4, and 26.3. It is built against the Bukkit API, so Spigot should work too, but I have not run the same in-game death tests on Spigot. If something breaks there, please open an issue with the server version and the relevant part of your config.

Folia is not supported.

CraftEngine 26.9.2 was used for testing. ItemsAdder support uses reflection, so a major API change on their side may require an update here.

The current build is 0.1.4 and passes 24 automated tests.

## Installation

1. Stop the server.
2. Put `NHTotem-0.1.4.jar` into `plugins/`.
3. Start the server.
4. Edit `plugins/NHTotem/config.yml`.
5. Run `/nhtotem reload`.

NHTotem uses Java 17 bytecode. Java 17 or newer is enough to run it, but Minecraft 26.1+ servers require Java 25 or newer.

## Links

- [Documentation](https://github.com/Naohuah/NHTotem/tree/main/docs)
- [CraftEngine tutorial pack](https://github.com/Naohuah/NHTotem/tree/main/docs/craftengine-tutorial)
- [Issues and feature requests](https://github.com/Naohuah/NHTotem/issues)
- [Latest release](https://github.com/Naohuah/NHTotem/releases/latest)

## License

NHTotem is released under the MIT License. Minecraft and the original Totem of Undying artwork belong to Mojang Studios. The totem-derived brand assets remain subject to their original rights.

---

NowHere Studios
