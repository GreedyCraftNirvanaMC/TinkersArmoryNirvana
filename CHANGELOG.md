# Changelog

## 1.21.1-1.0.10

- Add an original transparent pixel-art armor emblem and use it in NeoForge mod metadata and both README files. Gameplay is unchanged.

## 1.21.1-1.0.9

- Color material-trait names and descriptions using their actual source material in armor/part tooltips, Alt descriptions and workstation trait panels, replacing fixed cyan/purple colors.
- Resolve duplicate-trait color provenance in core/plates/trim order, matching the original first-application behavior. Read current material colors so addon/KubeJS edits remain visible without a stale cache.
- Record `EmbossedMaterial` for new embossments, preserving their trait color through saves. Existing embossments without source information retain their traits and use neutral gray when no base-part source is available.
- Expose `ArmorData.traitMaterial` and `traitColor` for addon displays; trait levels, stacking and gameplay effects are unchanged.
- Build/resource checks passed; dedicated-server verification passed 369 checks without KubeJS and 385 with the example scripts, including mixed colors, actual item tooltips, embossment provenance and KubeJS material colors. Client appearance remains pending manual acceptance.

## 1.21.1-1.0.8

- Follow the supplied TinkersNirvana `BookScreen.section` and `BookContents.paper` hover implementation: use the `minecraft:uniform` font without added bold, retain its idle/hover text colors, highlight bounds and clickable caption extent, and keep icons opaque.

## 1.21.1-1.0.7

- Match the chapter-label hover reference: gray regular text when idle, black bold text when hovered, with full icon/text opacity in both states. Center labels using the styled text width and keep the stored label unchanged.

## 1.21.1-1.0.6

- Keep both chapter icons and labels fully opaque in idle and hovered states, with a fixed label color. Only the hover background changes.

## 1.21.1-1.0.5

- Render all five book chapter icons at full opacity, matching the dependency's guide. Remove the legacy idle alpha tint that faded some item render layers while leaving others opaque; retain hover backgrounds and label colors.
- Compilation and resource audit passed. Client appearance remains pending manual acceptance; common gameplay code is unchanged from 1.0.4.

## 1.21.1-1.0.4

- Remove the built-in armor-leveling implementation, all `[leveling]` configuration, automatic damage XP, level rewards, level messages/sounds and level tooltips. Restore three base modifier slots before traits/modifiers and addon contributions.
- Discard the old root `Level` and `Experience` fields on armor writes/rebuilds, without migration or compensation. Namespaced addon data remains independent.
- Add `ArmorExtensions` for namespaced state and absolute extra-slot contributions, with atomic validation, immediate rebuilding, independent owners and native disk/network persistence.
- Add a non-cancellable `ArmorStatsEvent` on the NeoForge game bus for global addon calculations without installing material traits. Keep gameplay triggers and progression rules in addons.
- Validate non-finite stats before defense rounding so invalid addon results cannot silently turn into zero or partially update an item.
- Document Java/KubeJS access, state ownership, stat-event purity, native damage/tooltip events and slot removal behavior in English and Chinese.
- Build and resource audit passed. Dedicated-server verification passed 359 checks without KubeJS and 374 with the supplied KubeJS scripts. TinkersNirvana remains at 1.21.1-1.0.2.

## 1.21.1-1.0.3

- Prevent the inherited screen background pass from blurring the already-rendered Armory book and leave the world background clear, matching the original book.
- Restore the original wood-station parent geometry and side textures. Inherit native block-item display transforms for both tables so GUI, first/third-person and dropped-item views use block scales and rotations.
- Draw the enlarged assembly emblem behind the original 0.82 cover and 0.28/0.58 slot backgrounds with the original depth-test ordering, preserving visible slot borders and outlines.
- Suppress nameplates and below-name scoreboard text only for the station's preview player.
- Use the same live material-input selection as TinkersNirvana's guide for material index icons and detail sidebars: explicit representative first, then ingots, then unit-value ingredients. Remove the armor-helmet fallback and retain addon/KubeJS representatives.
- Build, resource audit and dedicated-server checks passed: 339 without KubeJS and 354 with the example scripts, including representative items from real KubeJS material inputs.
- Keep the required TinkersNirvana version at 1.21.1-1.0.2; no dependency source or binary change is needed for these fixes. Client visual acceptance remains manual.

## 1.21.1-1.0.2

- Port the original station/forge layout: five armor/repair tabs, radial or selected-part input positions, outlined empty slots, player preview, wood/metal frames and two scrolling information panels.
- Restore six repair inputs on ordinary stations; require the forge for modifiers, polishing and embossment. Add original intact-armor deconstruction with cancellation, persisted source ownership and commit-on-first-part behavior.
- Port the original Mantle book directory and Armory page builders, retaining five chapters, material side icons, colored stat columns, trait tooltips, flavor text and original modifier diagrams. Store the bookmark on the book item.
- Restore G/H/J accessory bindings and the original potion-belt / knapsack screens. Remove the replacement accessory action menu.
- Register the seven real armor parts in the native stencil table and part builder through TinkersNirvana's new external-part API. Require **TinkersNirvana 1.21.1-1.0.2+**; both runtime JARs must be updated. Right-click migrates saved legacy armor-pattern items to native patterns.
- Preserve original unallocated defense in interface displays while retaining allocated native armor attributes. Restore workstation sounds and retain source license notices for the GUI helpers and assets.
- Build and dedicated-server verification passed: 332 checks without KubeJS, 346 with the supplied KubeJS scripts. Static resource audit passed on 525 JSON files and 220 required English/Chinese keys. Real-client visual/interaction acceptance remains manual and pending.

## 1.21.1-1.0.1

- Register the retained `items/` and `blocks/` sprite directories in Minecraft's blocks atlas, fixing missing item, armor icon and workstation textures.
- Replace the text-only manual with Mantle's original cover/paper/navigation textures, two-page spreads, chapter indexes, linked material/trait references, armor previews and live workshop recipe diagrams.
- Paginate complete descriptions and addon entries instead of discarding lines beyond a fixed display limit. Add keyboard navigation and session-local reading position.
- Extend the static audit to detect PNGs that exist but are absent from the target atlas; include the new book translations and retain Mantle's MIT notice.
- Real-client visual acceptance of this version remains pending with the user.

## 1.21.1-1.0.0

- Initial NeoForge 1.21.1 source port with armor workshops, material traits, modifiers, traveller accessories, addon APIs and optional KubeJS integration.
