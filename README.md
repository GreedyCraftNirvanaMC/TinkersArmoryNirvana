# TinkersArmoryNirvana

An LGPL-3.0-or-later port of **AntiqueArmory / Construct's Armory** for **Minecraft 1.21.1**, **NeoForge 21.1.265+** and **TinkersNirvana 1.21.1-1.0.1+**. The mod ID and content namespace are `conarm`.

[简体中文](README.zh-CN.md) · [Developer API](docs/en/API.md) · [KubeJS](docs/en/KUBEJS.md) · [Configuration](docs/en/CONFIGURATION.md) · [Material table](docs/MATERIALS.md) · [Verification](docs/VERIFICATION.md) · [License and credits](NOTICE.md)

The port includes four modular armor pieces, seven material-dependent parts, the original thirty armor-material stat sets, material traits, armor modifiers, polishing, embossment, repair, part replacement, armor stations/forges, smeltery casting and traveller accessories. Broken armor remains available for repair. Knapsack, potion-belt and hotbar-belt contents stay on the owning armor item. Original armor/accessory geometry and sprites are used by the modern rendering implementation.

## Installation

Install Java 21, Minecraft 1.21.1 and NeoForge 21.1.265 or newer within the 21.1 line. Put both the TinkersNirvana and TinkersArmoryNirvana runtime JARs in the client and server `mods` directories. Do not install sources JARs as mods. The 1.12 Forge, TinkersAntique and Mantle JARs cannot be used on this version.

KubeJS is optional. The integration is verified against `kubejs-neoforge:2101.7.2-build.381` and its declared dependencies. Install matching startup scripts on both the client and server. The mod also loads without KubeJS.

JEI integration is deferred: the supplied TinkersNirvana port has no implemented JEI integration. The manual and native recipes remain available. Legacy CraftTweaker/ContentTweaker scripting is replaced by the Java and KubeJS APIs described below. Native armor leveling is configurable and disabled by default, matching the original mod without its optional Tool Leveling dependency.

## Playing

1. Make an Armor Station using a crafting table and four blank Tinkers patterns. Convert a blank Tinkers pattern into an armor pattern in a crafting grid. Right-click the armor pattern to cycle its part; sneak-right-click cycles backwards.
2. Put the pattern in the first station slot and craftable material ingredients in the remaining slots. Costs in material units: helmet core 4, chest core 6, leggings core 5, boots core 4, plates 3, trim 1, polishing kit 2. Patterns are reusable. Supported surplus is returned as Tinkers material shards.
3. Assemble armor with core, plates and trim in the three top slots. Materials may differ. Castable materials use the TinkersNirvana smeltery. Pour gold or aluminum brass over a part to create a metal cast; the configurable clay-cast path creates a single-use cast.
4. Put armor first to repair it using its core material or a matching polishing kit, replace its parts, or apply modifier ingredients. Rename the result using the station's text field. Repairs also work with polishing kits in a 3×3 crafting grid.
5. Upgrade the station to an Armor Forge for six-input operations. Polish with armor, polishing kit and sand. Emboss with armor, the selected part, green/blue/magma slime crystals and a gold block. Only one embossment is allowed.
6. Install a traveller accessory, then press **G** by default to open its controls. The menu enables only installed, unbroken equipment. It provides hotbar exchange, knapsack/potion storage and goggles/cloak toggles. Change the key in Minecraft controls if another mod uses G.

Only one traveller accessory can be installed on each armor item. Gauntlets require a chestplate and are mutually exclusive. Frostwalking/high stride/glowing require boots; amphibious requires a helmet. Resistance types conflict; additional levels of the selected resistance type consume no additional slots. The in-game Armory Addendum shows current material and modifier definitions, including addon content.

Open the Armory Addendum's cover to reach its chapter directory. Material, trait, modifier and accessory entries have clickable indexes, item tooltips and links between related pages. Modifier diagrams read the world's current workshop recipes, including datapack/KubeJS changes. Use the page arrows, mouse wheel or Left/Right keys to turn spreads, Home for the directory, and Backspace to return after following a link. The last spread is remembered for the current client session. Long entries continue on further pages.

In this port armor patterns are processed by the Armor Station/Forge, rather than the dependency's part-builder screen. Existing modular armor and armor patterns also work in the native Tinkers tool-station processing mode. The material registry and smeltery are shared with TinkersNirvana.

## Building

Build the supplied TinkersNirvana source first. Point this project's `tinkersNirvanaJar` Gradle property at the resulting JAR; the default is the sibling project's `build/libs/TinkersNirvana-1.21.1-1.0.1.jar`.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana-1.21.1-1.0.1.jar'
```

On Linux/macOS use `./gradlew` instead of `gradlew.bat`. No dependency JARs are committed. Gradle resolves NeoForge, mappings and the optional KubeJS compile API. Generated game assets and local game worlds are excluded from version control.

The runtime and sources JARs appear in `build/libs/`. `-PwithKubeJS` also loads KubeJS in development run configurations. See [verification](docs/VERIFICATION.md) for the opt-in server checks, test fixture preparation and manual client checklist.

## Extensions and customization

Java addons use `com.gctn.tinkersarmorynirvana.api`. Register armor statistics for an existing Tinkers material, add or modify callback traits and modifiers, register pure workshop planners, or supply armor overlays. No KubeJS dependency is needed for Java addons.

KubeJS provides `ArmoryEvents.modifyTraits`, `ArmoryEvents.modifyModifiers` and `ArmoryEvents.modifyMaterials` in startup scripts. Standard `ServerEvents.recipes` can add or remove `conarm:armor_modifier` recipes. Runnable examples are in [`examples/kubejs`](examples/kubejs). See the detailed [Java API](docs/en/API.md) and [KubeJS guide](docs/en/KUBEJS.md).

This is a source/API port, not binary compatibility with 1.12 addons or an automatic 1.12 world/NBT converter. Minecraft 1.21's native armor and attribute pipeline replaces Forge 1.12 `ISpecialArmor`. Old scripting files need migration to the documented APIs.
