# Java addon API

[README](../../README.md) · [简体中文](../zh-CN/API.md) · [KubeJS](KUBEJS.md)

## Dependencies and lifecycle

Target Java 21, Minecraft 1.21.1, NeoForge 21.1.265+, TinkersNirvana 1.21.1-1.0.2+ and this mod. Add both runtime JARs to your development classpath, and declare required `tconstruct` and `conarm` dependencies in your addon's `neoforge.mods.toml`:

```toml
[[dependencies.exampleaddon]]
modId = "tconstruct"
type = "required"
versionRange = "[1.21.1-1.0.2,)"
ordering = "AFTER"
side = "BOTH"

[[dependencies.exampleaddon]]
modId = "conarm"
type = "required"
versionRange = "[1.21.1-1.0.2,)"
ordering = "AFTER"
side = "BOTH"
```

```groovy
dependencies {
    implementation files('libs/TinkersNirvana-1.21.1-1.0.2.jar')
    implementation files('libs/TinkersArmoryNirvana-1.21.1-1.0.2.jar')
}
```

Register definitions in `FMLCommonSetupEvent.enqueueWork`, after item registration and the dependency's setup. KubeJS modification events run later during its plugin `afterInit`. Keep registry edits on the loading/main thread. Runtime hooks resolve definitions by ID; do not rely on object identity surviving replacement. Java addons do not need KubeJS on their classpath unless they use KubeJS classes themselves.

## Create a material and an armor trait

Armor statistics augment a real TinkersNirvana material; they do not replace its tool statistics or ingredient definitions.

```java
import com.gctn.tconstruct.api.MaterialDefinition;
import com.gctn.tinkersarmorynirvana.api.*;

// Execute inside enqueueWork after both dependencies.
ArmoryRegistry.registerTrait(new ArmorTrait("exampleaddon:resolve")
        .maxLevel(2)
        .on("modifyStats", ctx -> ctx.stats.toughness += 1.5F * ctx.level));

new MaterialDefinition("exampleaddon:amethyst")
        .color(0xA77CE3).craftable(true)
        .head(600, 6, 5, 2).handle(1.05F, 50).extra(75)
        .item("minecraft:amethyst_shard", 1)
        .install();

new ArmorMaterialDefinition("exampleaddon:amethyst")
        .core(16, 15).plates(1.1F, 3, 2).trim(7)
        .trait("core", "exampleaddon:resolve", 1)
        .trait("plates", "exampleaddon:resolve", 1)
        .trait("trim", "exampleaddon:resolve", 1)
        .install();
```

Built-in material IDs may be bare (`iron`) or qualified (`tconstruct:iron`); stored IDs use the dependency's canonical identifier. Third-party IDs should include the addon's namespace. A missing Tinkers material, non-finite stat, invalid multiplier, missing trait or excessive trait level fails registration.

| Material method | Meaning |
| --- | --- |
| `core(durability, defense)` | Original base durability multiplier and full-set defense |
| `plates(multiplier, durability, toughness)` | Durability multiplier, additive durability multiplier, toughness per piece |
| `trim(durability)` | Additive durability multiplier |
| `trait(part, id, level)` | Add a trait contribution to `core`, `plates` or `trim` |
| `clearTraits(part)` | Remove contributions from that armor part |
| `install()` | Validate and install/replace this armor definition |

Core and trim durability are multiplied by the slot's original durability factor (boots 13, leggings 15, chest 16, helmet 11), then plating is applied. Defense is distributed by slot (0.14, 0.30, 0.40, 0.16). `modifyStats` receives full-set defense before that distribution. `ArmorData.stats(stack)` returns the resulting single-piece defense used by native attributes. `ArmorData.displayStats(stack)` returns the original unallocated defense for book/workshop/tooltips. Both apply the same pure trait/modifier calculation; do not use display defense as a native attribute value.

Edit an existing material using `new ArmorMaterialDefinition(ArmoryRegistry.material("iron"))`, then install the copy. `removeMaterial(id)` hides its parts from the catalog and leaves existing item identifiers intact; equipment with missing definitions loses protection until definitions return.

## Traits and modifier definitions

`ArmoryRegistry` exposes `registerTrait`, `replaceTrait`, `removeTrait`, `registerModifier`, `removeModifier`, and read-only maps via `traits()`, `modifiers()` and `materials()`. `trait(id)`, `modifier(id)` and `material(id)` look up one entry. Short trait/modifier IDs use `conarm`; use a full ID for addon content. Duplicate registrations throw. `changed()` increments the public definition revision after direct edits.

`ArmorTrait.on(hook, Consumer<ArmorContext>)` appends behavior. `clear(hook)` removes that hook's existing handlers. `maxLevel(n)` limits resolved strength. `stacking("max")` is the default; `sum` adds every contribution; `unique` adds each distinct initial strength once. The result is capped at `maxLevel`. Builtin Magnetic/Mundane use `unique`: strengths 2, 1 and 1 resolve to 3.

```java
ArmoryRegistry.registerModifier(new ArmorModifier("exampleaddon:amethyst_guard")
        .maxLevel(3).pointsPerLevel(8).slotsPerLevel(1)
        .equipment("head", "chest", "legs", "feet")
        .on("incomingDamage", ctx -> ctx.amount *= .96F));
```

A modifier level starts with the first point in a new `pointsPerLevel` group. `slotsOnce(true)` charges its slot cost only once, even when later levels are added. `slotsPerLevel(0)` creates a free modifier. `conflicts("namespace:id", ...)` prevents installation together with those traits/modifiers. `accessory(true)` enforces the one-traveller-accessory-per-item rule. Custom recipes are required to supply modifier ingredients; see the KubeJS guide for the native JSON format.

## Callback contract

`ArmorContext` contains `armor`, nullable `wearer`, `id`, resolved `level`, a copied per-definition `state` compound, nullable `source`, mutable `amount`, and nullable `stats`.

| Hook | Context and effect |
| --- | --- |
| `modifyStats` | `stats` is present; `wearer` and `source` are null. Pure and deterministic. |
| `inventoryTick` | Server-side living owner; runs even when armor is not equipped or is broken. |
| `armorTick` | Server player wearing initialized, unbroken armor. |
| `incomingDamage` | Incoming damage before native armor reduction. Write `amount`. |
| `armorEffectiveness` | Start at factor 1. Modify it to change this piece's contribution to native armor reduction. |
| `beforeDamage` | Damage after armor/magic reduction, before absorption/health deduction. Write `amount`. |
| `afterDamage` | Actual positive damage, after it is applied. Use for world reactions, not damage replacement. |
| `outgoingDamage` | Attacker is the wearer; write outgoing `amount`. |
| `durabilityDamage` | Positive durability loss before it is applied; write `amount`. |
| `repair` | Requested healing amount; write `amount`. `wearer` may be null during previews. |
| `fall` | `amount` is fall distance; write distance. |
| `knockback` | `amount` is knockback strength. May run on both logical sides. |
| `jump` | Wearer is jumping; `amount` is the builtin shulkerweight bonus. |
| `milk` | Called after milk is consumed. |
| `experience` | Dropped block/mob experience; write `amount`. |
| `equip` / `unequip` | Server equipment-change notifications. Armor may already be broken. |

State changes require `ctx.save()`. It merges that definition's state into the latest item component, preserving other callbacks' saved data. `modifyStats` and preview-time `repair` callbacks must never save, consume items, use randomness, send packets or modify the world. They run repeatedly for previews, tooltips and rebuilds. Prefer actual combat/tick hooks for mutations. Callback functions can be invoked well after startup and must not retain player/menu references beyond their needed lifetime.

`ArmorStats` exposes `durability`, `defense`, `toughness`, `modifiers`, `speed`, `knockbackResistance`, `health`, `swimSpeed`, `attack`, `attackSpeed`, `reach`, and `stepHeight`; immutable `baseDurability`, `baseDefense`, `baseToughness` preserve pre-trait values. Speed/attack/attack-speed bonuses are multiplicative fractions; the other attribute additions are native values. `stepHeight` is an additional height. The final stat values must be finite, durability positive and slots nonnegative.

## Items, persistence and damage

`ArmorData.create(ModularArmorItem, core, plates, trim)` builds initialized armor. `ArmorPartItem.create(material)` creates a material-tagged part. `ArmoryContent.ARMORS` and `PARTS` expose builtin suppliers. Only the four humanoid armor slots are supported by builtin assembly.

`ArmorData.read(stack)` returns a copy of the persistent `conarm:armor` component. It stores `Version`, `Core`, `Plates`, `Trim`, `Modifiers` (ID → integer points), `State` (ID → compound), `Disabled`, optional `Polished`, `EmbossedTraits`, repair count and leveling data. Write back through `ArmorData.write`. Never edit another stack's preview or shared component in place.

`ArmorStorage` handles the separate `conarm:storage` component. It stores `Inventories` for `travel_sack`, `potion_belt` and `travel_belt`, with numbered slots containing Minecraft's registry-aware item serialization. Separating storage keeps normal stat lookups from copying a full backpack. Both components persist and synchronize using native component codecs.

Use `ArmorData.rebuild`, `damage`, `repair`, `level`, `points`, `freeSlots`, `traits` and `broken` rather than duplicating their logic. Broken equipment loses its native attributes and active abilities while its stack and storage remain intact. Repair restores protection. Native anvil naming is allowed; ordinary enchanting, two-item anvil repair and vanilla repair-item recipes are disabled. This preserves the mod's workshop-based progression and avoids consuming armor containing a backpack.

## Custom workshop operations

`ArmoryWorkshop.registerPlanner(id, BiFunction<Container, Level, Plan>)` runs before builtin planners. Return null when not applicable. Planners are startup registrations and must be pure. Inputs are exactly six slots, indexed 0–5. Capture a copy of every input in the plan snapshot.

`Plan(result, consumed, remainders, snapshot)` defensively copies its data and checks consumption counts. `valid(container)` checks the complete current snapshot, not just counts. `commit(container)` validates again, consumes once and returns remainder stacks. The menu gives those stacks to the player or drops overflow; the result preview itself is never saved in the block entity. A reused/obsolete plan fails validation. Do not commit during preview or independently consume inputs from a callback.

`ArmoryWorkshop.applyModifier` returns an independent result or `ItemStack.EMPTY` for rejection. The public workshop planner also powers the TinkersNirvana native station adapter. Custom planners can therefore extend both station paths when the source item is one of the adapter's supported armor/pattern classes.

`plan(input, level, allowModifiers)` restricts builtin modifier recipes, polishing and embossment to the forge when false. The original two-argument overload keeps its permissive behavior for callers. Registered custom planners run first in either case; an addon owns any additional progression policy for its operations. `rename(input, name)` creates a pure rename-only plan for armor with no other inputs.

`ArmoryMenu` reserves menu slot 0 for the result, 1–6 for the six container inputs, and 7–42 for the player inventory. The default selection is repair; selection 1–4 is helmet/chest/legs/boots. `ArmoryStationLayout` supplies coordinates and accepted part types. Menu selection uses server-validated vanilla menu buttons. Do not modify input ownership or commit preview deconstruction parts directly; the block entity preserves the original armor until the first part is taken.

## Casting and visual extensions

`ArmoryCasting.bind(materialId, fluidId)` binds an existing registered fluid for armor-part casting. Call during startup before server start. It does not register a fluid. A material must also be castable in TinkersNirvana. Native metal/clay casts and melting rules are installed at server start. Respect the dependency's clay-cast configuration.

`ArmorVisuals.register(modifierId, textureId, color, geometry)` registers a worn overlay during mod construction, before the armor-material registry is built. `textureId` omits `textures/` and `.png`, for example `exampleaddon:models/modifiers/rune`. Geometry is `armor`, `belt`, `sack`, `goggles` or `cloak`; use the matching original UV layout. Register visuals on both physical sides. Client-only classes must remain in client subscribers. Builtin icon composition uses the original sprite layers and is recreated on resource reload.

Translation keys are `trait.<namespace>.<path>.name/.desc`, `modifier.<namespace>.<path>.name/.desc`, and ordinary item keys. The manual reads current definitions. API compatibility is source-level for this port; the old `c4.conarm`/Forge 1.12 ABI is not retained.

The manual loads the original `assets/conarm/book/index.json` and section arrays. Localized pages are at `assets/conarm/book/<language>/<section>/<entry>.json`, with English fallback; labels/flavor text use `<language>/language.lang`, and appearance uses `appearance.json` plus localized overrides. Original page types include `text`, `imageText2`, `armor`, `armormaterial`, `armormodifier` and `armormodifier_polished`. Material pages resolve live armor definitions in native material order; additional modifiers are appended to their corresponding original section. Modifier diagrams use current `conarm:armor_modifier` recipes. Reopen after a reload. Fixed original page templates have bounded text areas: resource-pack authors should keep text within the source layout. Bookmarks use the book item's `CUSTOM_DATA` key `conarm:BookPage`. Mantle's MIT and TinkersAntique's LGPL notices are included. See [UI source mapping](../UI_PORT.md).

Seven existing armor-part items are registered through TinkersNirvana's `AntiqueParts.registerExternal`. This makes native stencil selection, selected-pattern labels, part building and part storage recognize the real armor items. The live material predicate honors later KubeJS edits. No second set of items or automatic native casts is registered. Addon authors with their own parts can use the same [external part API](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/blob/codex/armory-parts-api/docs/EXTERNAL_PARTS.md), using a namespaced ID/stat, pure material predicate, independent-stack factory and localized name. Register on both physical sides after items exist; the addon remains responsible for its own casting, rendering and armor assembly rules.

For item models, use Minecraft 1.21's `textures/item/` and `textures/block/` directories or explicitly add your sprite sources to `assets/minecraft/atlases/blocks.json`. A model's PNG can exist and still render as missing when the target atlas does not collect it. Direct worn-armor textures and GUI textures are loaded separately and do not need blocks-atlas entries.
