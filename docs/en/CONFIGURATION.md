# Configuration

[README](../../README.md) · [简体中文](../zh-CN/CONFIGURATION.md)

NeoForge generates `config/conarm-server.toml` and, on clients, `config/conarm-client.toml`. Stop the game/server before making permanent changes. Restart after changing progression settings so existing item caches and both sides use the same rules. KubeJS definitions are a separate startup customization layer.

## Server settings

| Key | Default | Meaning |
| --- | --- | --- |
| `spawnWithBook` | `true` | Give the Armory Addendum on the player's first join to that world |
| `bounceDurability` | `true` | Spend one durability point on a successful Bouncy boots bounce |
| `leveling.enabled` | `false` | Enable native armor leveling, replacing the optional legacy integration |
| `leveling.startingModifiers` | `3` | Base starting slots while leveling is enabled; 0–1024 |
| `leveling.maximumLevels` | `0` | Configured cap; 0 uses the implementation ceiling of 1000 |
| `leveling.baseXp` | `100` | XP needed for the first level |
| `leveling.levelMultiplier` | `2.0` | Growth of each subsequent XP requirement; at least 1 |
| `leveling.xpGainCap` | `100` | Maximum armor XP from one positive damage event |
| `leveling.damageToXp` | `0.25` | Converts original damage into armor XP, with a minimum gain of 1 |

When enabled, valid armor that experiences positive damage gains XP unless the damage bypasses armor. Armor starts at level 0 and gains one modifier slot per level. Progress is saved on each armor item and survives repair and part replacement. XP requirements are bounded to integer range. The supported level ceiling is 1000, including when no smaller configured cap is selected. This feature requires no Tool Leveling mod.

Disabling leveling stops XP gains and excludes its bonus slots from recalculation; saved progress remains on the item. Existing modifiers are not deleted. Lowering the slot budget can leave an item with no free slots until the budget is increased or the item is rebuilt with appropriate parts.

`spawnWithBook` uses a persisted player flag, so reconnecting or respawning does not duplicate the manual. If the player's inventory is full, the initial book is dropped beside them.

## Client settings

| Key | Default | Meaning |
| --- | --- | --- |
| `compactView` | `false` | Hide the armor preview mannequin beside the station interface |

The accessory-menu key is a normal Minecraft keybinding, **G** by default, and is edited in Controls rather than TOML. Traveller toggles are stored independently on each armor item. Storage accessories and toggle accessories require the item to be equipped and unbroken.

## Shared material and smeltery settings

TinkersNirvana owns material-input tags, whether castable materials may be directly crafted, clay-cast availability and its smeltery options. This mod reuses those settings rather than maintaining conflicting copies. Armor material stats, traits and modifier behavior can be edited through Java/KubeJS. Native item and modifier recipes can be changed with datapacks or `ServerEvents.recipes`.
