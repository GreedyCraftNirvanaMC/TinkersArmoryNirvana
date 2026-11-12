# Changelog

## 1.21.1-1.0.1

- Register the retained `items/` and `blocks/` sprite directories in Minecraft's blocks atlas, fixing missing item, armor icon and workstation textures.
- Replace the text-only manual with Mantle's original cover/paper/navigation textures, two-page spreads, chapter indexes, linked material/trait references, armor previews and live workshop recipe diagrams.
- Paginate complete descriptions and addon entries instead of discarding lines beyond a fixed display limit. Add keyboard navigation and session-local reading position.
- Extend the static audit to detect PNGs that exist but are absent from the target atlas; include the new book translations and retain Mantle's MIT notice.
- Real-client visual acceptance of this version remains pending with the user.

## 1.21.1-1.0.0

- Initial NeoForge 1.21.1 source port with armor workshops, material traits, modifiers, traveller accessories, addon APIs and optional KubeJS integration.
