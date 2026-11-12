/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tconstruct.init.TinkerDataComponents;
import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.library.utils.Tags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ArmorPartItem extends Item {
    public final String part;
    public final int cost;
    public ArmorPartItem(String part, int cost) { super(new Properties()); this.part = part; this.cost = cost; }
    public ItemStack create(String material) {
        material = com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.materialId(material);
        if (com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.material(material) == null) { throw new IllegalArgumentException("Unknown armor part material: " + material); }
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag(); tag.putString(Tags.PART_MATERIAL, material);
        stack.set(TinkerDataComponents.TINKER_MATERIAL.get(), CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.conarm.material_part",
                com.gctn.tconstruct.library.materials.Material.nameOf(material), Component.translatable(getDescriptionId())));
        return stack;
    }
    public static String material(ItemStack stack) { return TinkerDataComponents.getMaterialTag(stack).getString(Tags.PART_MATERIAL); }
    public static int color(ItemStack stack) {
        var material = TinkerMaterials.getMaterial(material(stack)); return material == null ? 0xffaaaaaa : material.color | 0xff000000;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        var definition = com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.material(material(stack));
        if (definition == null) { return; }
        int type = part.equals("armor_trim") ? 2 : part.equals("armor_plate") || part.equals("polishing_kit") ? 1 : 0;
        if (type == 0) { tooltip.add(Component.translatable("tooltip.conarm.core_stats", definition.coreDurability, definition.defense)); }
        else if (type == 1) { tooltip.add(Component.translatable("tooltip.conarm.plate_stats", definition.plateMultiplier, definition.plateDurability, definition.toughness)); }
        else { tooltip.add(Component.translatable("tooltip.conarm.trim_stats", definition.trimDurability)); }
        for (var trait : definition.traits(type)) { tooltip.add(Component.translatable("trait." + trait.id().replace(':', '.') + ".name").append(trait.level() > 1 ? " " + trait.level() : "")); }
    }
}
