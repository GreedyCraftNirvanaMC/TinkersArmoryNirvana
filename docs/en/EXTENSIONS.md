# Addon state and modifier-slot API

[Java API](API.md) · [简体中文](../zh-CN/EXTENSIONS.md)

Requires TinkersArmoryNirvana 1.21.1-1.0.4+ and TinkersNirvana 1.21.1-1.0.2+. Declare `conarm` with `versionRange="[1.21.1-1.0.4,)"`. This API supplies item-owned state, passive extra modifier slots and a global stat calculation event. It supplies no progression algorithm, experience trigger, level limit, reward schedule, leveling command, sound or tooltip. The addon owns those features.

## Persist state and slot contributions

Every owner must use a full ID such as `exampleaddon:progression`. All methods operate on initialized modular armor; check `ArmorData.initialized(stack)` first. Write methods belong on the logical server/game thread, on the actual owning stack.

| Method | Contract |
| --- | --- |
| `read(armor, id)` | Independent `CompoundTag` copy; empty if the owner has no data |
| `modifierSlots(armor, id)` | That owner's extra slots; zero when absent |
| `totalModifierSlots(armor)` | Sum of stored nonnegative contributions |
| `write(armor, id, data)` | Replace only that owner's data, keeping its slot contribution |
| `setModifierSlots(armor, id, slots)` | Set an absolute extra-slot contribution, keeping that owner's data |
| `update(armor, id, data, slots)` | Save both values together |
| `remove(armor, id)` | Remove only that owner's state and contribution |

Use absolute contributions, not an increment on every tick. Two owners contributing 2 and 1 add three slots to the usual base of three, before other traits/modifiers. Repeating `setModifierSlots(..., 2)` still contributes two. Values must be nonnegative; the final combined stats retain the existing 8192-slot validation limit. Global stat handlers can adjust the calculated count (for example, add `desiredBase - 3`) if an addon needs a different base without overwriting other contributions.

Each mutation builds and validates an independent armor copy before applying its component patch. Rejection leaves the original unchanged. Successful writes rebuild attributes and maximum durability immediately while preserving other owners, current damage, names and storage (subject to normal durability clamping when stats change). Reads and supplied compounds do not share mutable NBT references with the stack.

```java
import com.gctn.tinkersarmorynirvana.api.ArmorExtensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

// Call after your addon decides the level and reward, on the owning server stack.
public static void saveProgress(ItemStack armor, int level, long xp, int bonusSlots) {
    CompoundTag data = ArmorExtensions.read(armor, "exampleaddon:progression");
    data.putInt("Level", level);
    data.putLong("Experience", xp);
    ArmorExtensions.update(armor, "exampleaddon:progression", data, bonusSlots);
}
```

Data is stored under `conarm:armor -> Extensions -> <owner>`, with `Data` and `ModifierSlots` children. It uses the existing native disk/network component codecs and survives repair and part replacement. It remains on the item until the owner changes or removes it, including when the addon is absent. Removing slot contributions preserves installed modifiers; an over-budget item simply has no free slots. The addon decides whether to allow, revoke or compensate rewards. Deconstruction loses addon state along with the original armor.

## Observe gameplay using native events

Use NeoForge events such as `LivingDamageEvent.Post` for a damage-based design. Read `getOriginalDamage()`, `getNewDamage()` and `getSource()` according to your own policy; decide which armor pieces, broken items, damage types and wearers qualify. Mutate the actual equipped stack after checking logical side and `ArmorData.initialized`. The existing material/modifier `afterDamage` callback remains available for designs attached to a trait. The `experience` trait hook controls dropped Minecraft XP; it is not an armor-leveling event.

Add your own client tooltip through NeoForge's `ItemTooltipEvent`. Read synchronized extension data and use your own translation keys. The core adds no level display or messages. `ArmorData.level(stack, modifierId)` remains a modifier-level lookup, not an armor experience level.

## Global armor stat event

Register on the **NeoForge game bus**, consistently on both physical sides:

```java
import com.gctn.tinkersarmorynirvana.api.ArmorExtensions;
import com.gctn.tinkersarmorynirvana.api.ArmorStatsEvent;
import net.neoforged.neoforge.common.NeoForge;

NeoForge.EVENT_BUS.addListener(ArmorStatsEvent.class, event -> {
    var data = ArmorExtensions.read(event.getArmor(), "exampleaddon:attributes");
    event.getStats().toughness += data.getFloat("BonusToughness");
});
```

The event is non-cancellable and applies to every valid modular armor piece, without requiring an installed trait. `getArmor()` returns a fresh independent snapshot; editing it never changes the original. `getStats()` exposes the live mutable result after material stats, traits, modifiers, stored extra slots and polishing. Defense is still the original unallocated value; native slot allocation and validation occur afterward. Both `ArmorData.stats` and `displayStats` invoke this event.

Stat handlers must be deterministic and free of side effects. They run repeatedly for tooltips, previews, server rebuilding and extension validation, with no wearer/world context. Do not call `ArmorExtensions` mutators, `ArmorData.rebuild/stats`, grant XP, save state, send packets or change the world inside them. Read the snapshot and edit only the result. Use gameplay events to store changes beforehand.

## Removal of builtin progression

The old `ArmorLeveling` implementation and all `[leveling]` settings are removed. Armor has three base modifier slots before traits/modifiers and extension contributions. Legacy root `Level`/`Experience` fields are removed on armor write/rebuild, without migration or compensation. Namespaced addon `Data` may use those names normally. No TinkersNirvana source changes are required.
