# Changelog

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
