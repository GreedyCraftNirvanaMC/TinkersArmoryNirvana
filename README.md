# TinkersArmoryNirvana

An LGPL-3.0-or-later port of **AntiqueArmory / Construct's Armory** for **Minecraft 1.21.1**, **NeoForge 21.1.265+** and **TinkersNirvana 1.21.1-1.0.2+**. The mod ID and content namespace are `conarm`.

[简体中文](README.zh-CN.md) · [Developer API](docs/en/API.md) · [KubeJS](docs/en/KUBEJS.md) · [Configuration](docs/en/CONFIGURATION.md) · [Material table](docs/MATERIALS.md) · [Verification](docs/VERIFICATION.md) · [License and credits](NOTICE.md)

The port includes four modular armor pieces, seven material-dependent parts, the original thirty armor-material stat sets, material traits, armor modifiers, polishing, embossment, repair, part replacement, armor stations/forges, smeltery casting and traveller accessories. Broken armor remains available for repair. Knapsack, potion-belt and hotbar-belt contents stay on the owning armor item. Original armor/accessory geometry and sprites are used by the modern rendering implementation.

## Installation

Install Java 21, Minecraft 1.21.1 and NeoForge 21.1.265 or newer within the 21.1 line. Put both the TinkersNirvana and TinkersArmoryNirvana runtime JARs in the client and server `mods` directories. Do not install sources JARs as mods. The 1.12 Forge, TinkersAntique and Mantle JARs cannot be used on this version.

KubeJS is optional. The integration is verified against `kubejs-neoforge:2101.7.2-build.381` and its declared dependencies. Install matching startup scripts on both the client and server. The mod also loads without KubeJS.

JEI integration is deferred: the supplied TinkersNirvana port has no implemented JEI integration. The manual and native recipes remain available. Legacy CraftTweaker/ContentTweaker scripting is replaced by the Java and KubeJS APIs described below. Native armor leveling is configurable and disabled by default, matching the original mod without its optional Tool Leveling dependency.

## Playing

1. Make an Armor Station using a crafting table and four blank Tinkers patterns. Use the native **Stencil Table** to select an armor-part pattern, then use the native **Part Builder** with craftable raw materials. Patterns are reusable. Material-unit costs are helmet core 4, chest core 6, leggings core 5, boots core 4, plates 3, trim 1 and polishing kit 2.
2. Select the helmet, chestplate, leggings or boots tab at the Armor Station. Put the matching core in the lower middle slot, plates on the left and trim on the right. Materials may differ. The left panel previews the armor on your player; the right panels show its stats and traits. Take the result to consume the parts.
3. Castable materials use the shared TinkersNirvana smeltery. Pour gold or aluminum brass over a part to make its metal cast; the dependency's configurable clay-cast path makes a single-use cast.
4. Select **Repair & Modify** to use the six radial slots. Put armor in the center and its core material or matching polishing kits in the other slots to repair it. New parts replace corresponding components. The name field renames the result. Repairs also work with polishing kits in a 3×3 crafting grid.
5. The ordinary station assembles, repairs, replaces parts and renames. Upgrade to an **Armor Forge** to apply modifiers, polish or emboss. Polish with armor, a polishing kit and sand. Emboss with armor, a part, green/blue/magma slime crystals and a gold block. Only one embossment is allowed.
6. The original accessory bindings are **G** for helmet, **H** for chestplate and **J** for leggings. They directly toggle the installed goggles/cloak, open the knapsack/potion belt, or exchange the hotbar belt. Configure each binding in Minecraft Controls. Equipment must be worn and unbroken.

Only one traveller accessory can be installed on each armor item. Gauntlets require a chestplate and are mutually exclusive. Frostwalking/high stride/glowing require boots; amphibious requires a helmet. Resistance types conflict; additional levels of the selected resistance type consume no additional slots.

With TinkersNirvana's `deconstructTools` setting enabled, place intact, undamaged armor in the right-hand result slot while the repair tab's inputs are empty. Its three parts are initially a preview. Taking the armor back cancels; taking any part commits deconstruction and loses installed modifiers. Empty accessory inventories first. Breaking the table before committing drops the original armor once.

The Armory Addendum uses the original five chapters: Introduction, Armor, Armor Materials, Modifiers and Accessories. Opening the cover shows the directory on the right and the dark inside cover on the left. Material pages retain the original side item column, two stat columns, trait hover text and flavor text. Modifier diagrams read current workshop recipes, including datapack/KubeJS changes. Use arrows, the wheel or Left/Right (A/D) to turn spreads; Home opens the directory and Backspace returns after a linked entry. The reading position is stored on the book item. Reopen after recipe or resource reloads.

Saved legacy `conarm:pattern` items remain recognized; right-click converts one to the corresponding native selected pattern, preserving its count. Newly created armor patterns come from the native stencil table. The material registry, part chest and smeltery are shared with TinkersNirvana.

## Building

Build TinkersNirvana **1.21.1-1.0.2 or newer with `AntiqueParts.registerExternal`** first. The required API changes are in [PR #10](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/pull/10), pending merge to Stable, and available on [codex/armory-parts-api](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/tree/codex/armory-parts-api). Point this project's `tinkersNirvanaJar` Gradle property at the resulting JAR; the default is the sibling project's `build/libs/TinkersNirvana-1.21.1-1.0.2.jar`.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana-1.21.1-1.0.2.jar'
```

On Linux/macOS use `./gradlew` instead of `gradlew.bat`. No dependency JARs are committed. Gradle resolves NeoForge, mappings and the optional KubeJS compile API. Generated game assets and local game worlds are excluded from version control.

See [original UI source mapping](docs/UI_PORT.md) for the source classes and modern API adaptations.

The runtime and sources JARs appear in `build/libs/`. `-PwithKubeJS` also loads KubeJS in development run configurations. See [verification](docs/VERIFICATION.md) for the opt-in server checks, test fixture preparation and manual client checklist.

## Extensions and customization

Java addons use `com.gctn.tinkersarmorynirvana.api`. Register armor statistics for an existing Tinkers material, add or modify callback traits and modifiers, register pure workshop planners, or supply armor overlays. No KubeJS dependency is needed for Java addons.

KubeJS provides `ArmoryEvents.modifyTraits`, `ArmoryEvents.modifyModifiers` and `ArmoryEvents.modifyMaterials` in startup scripts. Standard `ServerEvents.recipes` can add or remove `conarm:armor_modifier` recipes. Runnable examples are in [`examples/kubejs`](examples/kubejs). See the detailed [Java API](docs/en/API.md) and [KubeJS guide](docs/en/KUBEJS.md).

This is a source/API port, not binary compatibility with 1.12 addons or an automatic 1.12 world/NBT converter. Minecraft 1.21's native armor and attribute pipeline replaces Forge 1.12 `ISpecialArmor`. Old scripting files need migration to the documented APIs.
