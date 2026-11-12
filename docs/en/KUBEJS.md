# KubeJS customization

[README](../../README.md) · [Java API and complete callback table](API.md) · [简体中文](../zh-CN/KUBEJS.md)

The integration targets KubeJS NeoForge 2101.7.2 and is verified with build.381. KubeJS remains optional. Use matching startup definitions on the client and server. Copy the runnable example directory [`examples/kubejs`](../../examples/kubejs) into the game directory only when you want to enable its example content.

## File placement and reload behavior

| Directory | Purpose | Apply changes |
| --- | --- | --- |
| `kubejs/startup_scripts/` | Materials, traits, modifiers and armor behavior | Restart client and server |
| `kubejs/server_scripts/` | Native recipes and tags | `/reload` or restart |
| `kubejs/client_scripts/` | Client language additions | Restart/reload client scripts as supported by KubeJS |

Startup order is: item/registry registration → Java common setup → TinkersNirvana plugin definitions and modification events → Armory trait event → Armory modifier event → Armory material event. Armory events run from the documented KubeJS plugin `afterInit`, with `conarm` ordered after its dependencies. Do not create armor materials before their underlying Tinkers material exists.

## Add a material

```js
StartupEvents.registry('tconstruct:material', event => {
  event.create('example:amethyst')
    .color(0xA77CE3).craftable(true)
    .head(600, 6, 5, 2).handle(1.05, 50).extra(75)
    .item('minecraft:amethyst_shard', 1)
})

ArmoryEvents.modifyTraits(event => {
  event.create('example:resolve', trait => {
    trait.maxLevel(2).on('modifyStats', ctx => {
      ctx.stats.toughness += 1.5 * ctx.level
    })
  })
})

ArmoryEvents.modifyMaterials(event => {
  event.create('example:amethyst', material => {
    material.core(16, 15).plates(1.1, 3, 2).trim(7)
      .trait('core', 'example:resolve', 1)
      .trait('plates', 'example:resolve', 1)
      .trait('trim', 'example:resolve', 1)
  })
})
```

`core`, `plates`, `trim`, `trait`, `clearTraits` and validation match the Java API. An armor definition supplies no input items by itself; those belong to TinkersNirvana. Craftable materials can use armor patterns at the Armor Station. Castable materials use smeltery casting. Builtin material IDs accept `iron` or `tconstruct:iron`; use a namespace for custom materials.

## Edit and remove definitions

```js
ArmoryEvents.modifyMaterials(event => {
  event.modify('wood', material => {
    material.core(6, 8)
      .clearTraits('trim')
      .trait('trim', 'example:resolve', 1)
  })
  // event.remove('paper')
})

ArmoryEvents.modifyTraits(event => {
  event.modify('conarm:ecological', trait => {
    trait.clear('inventoryTick').on('inventoryTick', ctx => {
      // A callback runs on the server; throttle expensive/world-changing work.
      if (ctx.wearer.tickCount % 200 === 0) {
        const ArmorData = Java.loadClass('com.gctn.tinkersarmorynirvana.armor.ArmorData')
        ArmorData.repair(ctx.armor, ctx.wearer, 1)
      }
    })
  })
})
```

`modify` receives an editable definition; material edits are copied and validated before installation. `create` rejects duplicates, `modify` rejects unknown IDs. Trait/modifier events also provide `remove(id)`. Removed definitions no longer execute, while saved identifiers/state remain on existing equipment. Removed material definitions disable protection for affected armor. Restart after changing startup scripts.

## Add or change a modifier

```js
ArmoryEvents.modifyModifiers(event => {
  event.create('example:amethyst_guard', modifier => {
    modifier.maxLevel(3).pointsPerLevel(8).slotsPerLevel(1)
      .equipment('head', 'chest', 'legs', 'feet')
      .on('incomingDamage', ctx => { ctx.amount *= 0.96 })
  })
  event.modify('conarm:speedy', modifier => {
    modifier.maxLevel(4)
  })
})
```

Additional controls: `conflicts(...ids)`, `slotsOnce(true)`, `accessory(true)`, `on(hook, callback)`, `clear(hook)`. Trait stacking is `max`, `unique` or `sum`, capped at `maxLevel`. Short trait/modifier IDs use namespace `conarm`; full IDs are recommended.

`ctx.amount` is the mutable value for damage/repair/fall/knockback/experience hooks. `ctx.stats` is present only for `modifyStats`. `ctx.wearer` and `ctx.source` can be null for stat calculation and repair previews. Persist independent state with `ctx.state.putInt(...)` etc., followed by `ctx.save()`. See the [complete hook table](API.md#callback-contract) before using a hook.

Stat and preview callbacks must be pure: no randomness, inventory changes, packets, saved state or world changes. They run more than once. Runtime hooks may mutate world state on the server. New callback names are validated; unsupported names throw instead of being silently ignored.

## Native modifier recipes

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'conarm:modifiers/diamond' })

  event.custom({
    type: 'conarm:armor_modifier',
    modifier: 'example:amethyst_guard',
    points: 2,
    ingredients: [
      { ingredient: { item: 'minecraft:amethyst_shard' }, count: 1 },
      { ingredient: { tag: 'c:ingots/iron' }, count: 2 }
    ]
  }).id('example:armory/amethyst_guard')
})
```

The identical object works as a datapack JSON recipe under `data/example/recipe/armory/amethyst_guard.json`. `points` defaults to 1. `ingredients` has 1–6 entries; each entry has a native nonempty `ingredient` and `count` (default 1, 1–384). The armor occupies station input 0, so at most five ingredient stacks can be placed in the forge at once. The ordinary station exposes only its first three inputs.

Ingredients may use item tags, alternatives or NeoForge component predicates. Overlapping requirements and split stacks are allocated together. An unrelated extra input rejects the operation. Ingredients are consumed only when the output is taken; crafting remaining items and surplus byproducts are returned. The result is derived from the source armor, so a fixed `result` field is not used. The modifier must exist and pass slot, equipment, maximum-point, conflict and accessory checks. A recipe that would exceed the maximum points is rejected rather than consuming excess material.

Builtin IDs are `conarm:modifiers/<name>`, including `diamond`, `emerald`, `reinforced`, `speedy`, `speedy_block`, `shulkerweight`, `resistant`, `fire_resistant`, `blast_resistant`, `projectile_resistant`, `powerful`, `dexterous`, `telekinetic`, `amphibious`, `frostwalker`, `high_stride`, `glowing`, `parasitic`, `mending`, `soulbound`, and traveller accessory names. Inspect `src/main/resources/data/conarm/recipe/modifiers/` for exact inputs. Normal item/block crafting uses standard Minecraft recipe types and can be removed or replaced normally.

## Casting and language

```js
ArmoryEvents.modifyMaterials(event => {
  // The underlying material must also be castable. This binds an existing source fluid.
  event.castingFluid('example:alloy', 'tconstruct:cobalt')
})

ClientEvents.lang('zh_cn', event => {
  event.add('trait.example.resolve.name', '坚毅')
  event.add('trait.example.resolve.desc', '提升护甲韧性。')
  event.add('modifier.example.amethyst_guard.name', '紫晶守护')
  event.add('modifier.example.amethyst_guard.desc', '减轻受到的伤害。')
})
```

Use the casting example only after creating `example:alloy`; it does not register a material or fluid. Casting bindings need a restart. The fluid's registry ID is used, not a bucket/block ID (`tconstruct:cobalt` is the source fluid; `tconstruct:molten_cobalt` is its block).

Names and descriptions use `trait.<namespace>.<path>.name/.desc` and `modifier.<namespace>.<path>.name/.desc`. Material names/colors are supplied by the base Tinkers material. The book reads current definitions automatically.

## Diagnosis

Inspect the current launch's `logs/kubejs/startup.log`, `logs/kubejs/server.log` and `logs/latest.log`. Successful script parsing alone does not prove a registration callback ran. Check the creative catalog/manual or a built item's resulting statistics, and use the opt-in [server fixture checks](../VERIFICATION.md) when changing the integration. Keep client/server startup definitions synchronized. A recipe-only `/reload` does not rerun material or callback registration.

Addon-owned runtime state and slot rewards can use `Java.loadClass('com.gctn.tinkersarmorynirvana.api.ArmorExtensions')`. These operations must run on the server outside pure stat/preview callbacks; they accept native `CompoundTag` objects. They do not implement experience or levels. See [the extension guide](EXTENSIONS.md).

Material-trait display colors resolve the current `color` of the first source material. Mixed armor resolves core/plates/trim separately, then embossment; duplicate traits keep the first source color. This also applies to KubeJS-created materials and traits without a separate text-color setting.
