/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana;

import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArmoryContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("conarm");
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("conarm");
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "conarm");
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "conarm");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> ARMOR_DATA =
            COMPONENTS.registerComponentType("armor", builder -> builder.persistent(CustomData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> ARMOR_STORAGE =
            COMPONENTS.registerComponentType("storage", builder -> builder.persistent(CustomData.CODEC));
    public static final Holder<ArmorMaterial> ARMOR = ARMOR_MATERIALS.register("modular", () -> new ArmorMaterial(Map.of(), 0,
            SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.EMPTY,
            com.gctn.tinkersarmorynirvana.api.ArmorVisuals.layers(), 0, 0));
    public static final Map<String, DeferredItem<ModularArmorItem>> ARMORS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<ArmorPartItem>> PARTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final DeferredItem<ArmoryPatternItem> PATTERN = ITEMS.register("pattern", ArmoryPatternItem::new);
    public static final DeferredBlock<ArmoryStationBlock> STATION = BLOCKS.register("armorstation", () ->
            new ArmoryStationBlock(Block.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ArmoryStationBlock> FORGE = BLOCKS.register("armorforge", () ->
            new ArmoryStationBlock(Block.Properties.of().strength(5).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> SOFT_OBSIDIAN = BLOCKS.register("soft_obsidian", () -> new SoftObsidianBlock(Block.Properties.of().strength(.5F).sound(SoundType.STONE).lightLevel(state -> 3).randomTicks().noLootTable()));
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "conarm");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmoryStationBlockEntity>> STATION_ENTITY =
            ENTITIES.register("armor_station", () -> BlockEntityType.Builder.of(ArmoryStationBlockEntity::new, STATION.get(), FORGE.get()).build(null));
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "conarm");
    public static final DeferredHolder<MenuType<?>, MenuType<ArmoryMenu>> STATION_MENU = MENUS.register("armor_station", () -> new MenuType<>(ArmoryMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<AccessoryMenu>> ACCESSORY_MENU = MENUS.register("accessory", () -> new MenuType<>(AccessoryMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "conarm");

    static {
        for (ArmorItem.Type type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
            ARMORS.put(type.getName(), ITEMS.register(type.getName(), () -> new ModularArmorItem(type)));
        }
        String[] parts = {"helmet_core", "chest_core", "leggings_core", "boots_core", "armor_plate", "armor_trim", "polishing_kit"};
        int[] costs = {4, 6, 5, 4, 3, 1, 2};
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i]; int cost = costs[i];
            PARTS.put(name, ITEMS.register(name, () -> new ArmorPartItem(name, cost)));
            MATERIAL_ITEMS.put("cast_" + name, ITEMS.registerSimpleItem("cast_" + name));
            MATERIAL_ITEMS.put("clay_cast_" + name, ITEMS.registerSimpleItem("clay_cast_" + name));
        }
        for (String name : List.of("invisible_ink", "resist_mat", "resist_mat_fire", "resist_mat_proj", "resist_mat_blast",
                "gauntlet_mat", "gauntlet_mat_attack", "gauntlet_mat_speed", "gauntlet_mat_reach", "frosty_soles",
                "travel_cloak", "travel_belt_base", "travel_belt", "travel_potion", "travel_sack", "travel_goggles_base",
                "travel_goggles", "travel_night", "travel_soul", "travel_sneak", "travel_slowfall")) {
            MATERIAL_ITEMS.put(name, ITEMS.register(name, ArmoryIngredientItem::new));
        }
        ITEMS.register("book", ArmoryBookItem::new);
        ITEMS.registerSimpleBlockItem(STATION); ITEMS.registerSimpleBlockItem(FORGE); ITEMS.registerSimpleBlockItem(SOFT_OBSIDIAN);
        TABS.register("armory", () -> CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.conarm"))
                .icon(() -> ArmorData.create(ARMORS.get("helmet").get(), "iron", "iron", "iron"))
                .displayItems((parameters, output) -> {
                    output.accept(STATION); output.accept(FORGE); output.accept(SOFT_OBSIDIAN); output.accept(PATTERN);
                    output.accept(ITEMS.getEntries().stream().filter(item -> item.getId().getPath().equals("book")).findFirst().orElseThrow().get());
                    MATERIAL_ITEMS.values().forEach(output::accept);
                    for (var material : com.gctn.tconstruct.library.TinkerMaterials.materials) {
                        if (material.isHidden() || com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.material(material.identifier) == null) { continue; }
                        for (var part : PARTS.values()) { output.accept(part.get().create(material.identifier)); }
                        for (var armor : ARMORS.values()) { output.accept(ArmorData.create(armor.get(), material.identifier, material.identifier, material.identifier)); }
                    }
                }).build());
    }
    private ArmoryContent() {}
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("conarm", path); }
    public static void register(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus); COMPONENTS.register(bus); ARMOR_MATERIALS.register(bus);
        ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
        ArmoryRecipes.register(bus); ArmoryNetwork.register(bus);
    }
}
