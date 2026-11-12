# Original UI source mapping

This port follows the supplied 1.12.2 sources and their resource layouts. Client acceptance is manual; compiling the renderer or running dedicated-server assertions does not verify its appearance.

| Original source | Modern implementation | Retained behavior |
| --- | --- | --- |
| Armory `GuiArmorStation`, `GuiArmorForge`, `GuiButtonsArmorStation`, `GuiButtonArmorRepair`, `ArmorBuildGuiInfo` | `client/ArmoryScreen`, `station/ArmoryStationLayout` | 176×174 center, five tabs, repair/armor modes, original slot coordinates and UVs, wood/metal decoration, arrows and rename field |
| Armory `GuiPreviewPanel`, `PreviewPlayer` | `ArmoryScreen.PreviewPlayer` | Real player skin/model flags, equipped previews, original scale 60, original eye target and foot baseline adapted to modern entity rendering |
| TinkersAntique `GuiInfoPanel` | `client/ArmoryInfoPanel` | Tiled borders, 126×83 right panels, captions, 9.5-pixel line spacing, trait hover help and scrollbar |
| TinkersAntique `GuiOutlineTexture`, `ExtraUtilityTexture` | `client/ArmoryPartOutline` | Alpha threshold, four-neighbor silhouette edge, dark outline, resource-reload cleanup |
| Armory `ContainerArmorStation`, `SlotArmorStationIn/Out`, `TileArmorStation` | `station/ArmoryMenu`, `ArmoryStationBlockEntity` | Selection-aware inputs, repair versus forge operations, pure previews, deconstruction cancellation and first-part commit |
| Mantle `GuiBook`, `GuiArrow`, `ElementSection`, `IndexTransformer` | `client/ArmoryBookScreen`, `ArmoryBookDocument` | 206×200 leaves, 182×176 content, first index on the right, dark inside cover, original section cells, cover/paper/arrow sprites and page placement |
| Armory `ArmoryBook` and section transformers | `client/ArmoryBookContents` | Original five sections, source JSON and localized language files, native material order and live modifier definitions |
| Armory `ContentArmorMaterial`, `ContentArmor`, `ContentArmorModifier` and polished variant | `ArmoryBookDocument` | Original title, side item column, stat groups, trait tooltips, flavor text, property lists and diagrams |
| TinkersAntique `ContentListing`, `ContentPageIconList` | `ArmoryBookDocument` | Listing columns, icon cell fitting and page entry links |
| Armory accessory GUIs and key handler | `client/AccessoryScreen`, `ArmoryClient`, `station/AccessoryMenu` | Potion-belt texture, chest storage texture, original slot positions and G/H/J by equipment slot |

Paths in the modern column are below `src/main/java/com/gctn/tinkersarmorynirvana/`. Original Armory classes are under `c4.conarm`, TinkersAntique classes under `slimeknights.tconstruct`, and Mantle classes under `slimeknights.mantle` in the supplied projects.

## API adaptations

- Minecraft 1.21.1 slot coordinates are immutable. The menu replaces slot view objects with the same underlying container/index when selection changes; it does not move or duplicate their stacks. The client and server use the same layout constants.
- Repair input 0 is `(33,42)`; the other five are `(15,62)`, `(11,37)`, `(33,19)`, `(55,37)`, `(51,62)`. Selected armor inputs are `(29,52)`, `(9,32)`, `(49,32)`. The result remains `(124,38)`.
- The actual armor is persisted while its deconstructed parts are virtual. Taking the first part consumes that source once; returning the source cancels. Break/save/load logic never serializes or drops duplicate preview parts.
- The original displayed defense is retained by `ArmorData.displayStats`. Native protection uses `ArmorData.stats` after slot allocation. GUI display values must not be written back as native attributes.
- Native pattern processing uses `AntiqueParts.registerExternal` from TinkersNirvana 1.21.1-1.0.2. Existing armor items remain the source of rendering and data components. Live material predicates keep startup-script edits visible to the processing system.
- Book page construction reads original fixed templates and bounds. Reopen after resource/recipe reloads. Bookmarks are stored in item `CUSTOM_DATA` with a bounded server-validated payload; they are not a global client-session field.
- Native 1.21.1 texture/item/font batches reset render state. Transparent GUI layers explicitly restore alpha blending; atlas-collected item textures and direct GUI textures follow their separate loading paths.

The original Armory LGPL grant remains in the root `LICENSE`. See [NOTICE](../NOTICE.md) for Mantle MIT and TinkersAntique LGPL source/asset attribution. No 1.12 JAR is shipped or required at runtime.
