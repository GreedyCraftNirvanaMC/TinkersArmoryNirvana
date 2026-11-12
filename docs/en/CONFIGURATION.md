# Configuration

[README](../../README.md) · [简体中文](../zh-CN/CONFIGURATION.md)

NeoForge generates `config/conarm-server.toml` and, on clients, `config/conarm-client.toml`. Stop the game/server before making permanent changes. KubeJS definitions are a separate startup customization layer.

## Server settings

| Key | Default | Meaning |
| --- | --- | --- |
| `spawnWithBook` | `true` | Give the Armory Addendum on the player's first join to that world |
| `bounceDurability` | `true` | Spend one durability point on a successful Bouncy boots bounce |

There are no built-in armor progression settings. Addons own experience gain, levels, rewards and their configuration through the [extension API](EXTENSIONS.md). The old `[leveling]` configuration is no longer read.

`spawnWithBook` uses a persisted player flag, so reconnecting or respawning does not duplicate the manual. If the player's inventory is full, the initial book is dropped beside them.

## Client settings

| Key | Default | Meaning |
| --- | --- | --- |
| `compactView` | `false` | Hide the armor preview mannequin beside the station interface |

Accessory keys are normal Minecraft bindings: **G** for helmet, **H** for chestplate and **J** for leggings. Edit them separately in Controls rather than TOML. They directly operate the installed accessory. Traveller toggles are stored independently on each armor item. Storage accessories and toggle accessories require the item to be equipped and unbroken.

## Shared material and smeltery settings

TinkersNirvana owns material-input tags, whether castable materials may be directly crafted, clay-cast availability and its smeltery options. This mod reuses those settings rather than maintaining conflicting copies. Armor material stats, traits and modifier behavior can be edited through Java/KubeJS. Native item and modifier recipes can be changed with datapacks or `ServerEvents.recipes`.
