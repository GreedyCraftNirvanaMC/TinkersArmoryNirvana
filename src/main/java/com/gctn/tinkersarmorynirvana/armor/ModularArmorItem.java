/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.List;

public class ModularArmorItem extends ArmorItem {
    public ModularArmorItem(Type type) { super(ArmoryContent.ARMOR, type, new Properties().durability(1)); }
    @Override public ItemAttributeModifiers getDefaultAttributeModifiers() { return ItemAttributeModifiers.EMPTY; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack armor, ItemStack repair) { return false; }
    @Override public boolean isRepairable(ItemStack stack) { return false; }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) { return false; }
    @Override public boolean supportsEnchantment(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) { return false; }
    @Override public int getEnchantmentValue() { return 0; }
    @Override public boolean isBarVisible(ItemStack stack) { return !ArmorData.broken(stack) && super.isBarVisible(stack); }
    @Override public boolean isFoil(ItemStack stack) {
        return ArmorData.read(stack).getCompound("State").getCompound("conarm:voltaic").getFloat("Charge") >= 25;
    }
    @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
        int index = getMaterial().value().layers().indexOf(layer);
        if (index >= 3) {
            var overlay = com.gctn.tinkersarmorynirvana.api.ArmorVisuals.entries().get(index - 3);
            var texture = net.minecraft.resources.ResourceLocation.parse(overlay.texture());
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
        }
        String name = index == 1 ? "armor_plates" : index == 2 ? "armor_trim" : "armor_core";
        String key = index == 1 ? "Plates" : index == 2 ? "Trim" : "Core";
        String material = ArmorData.read(stack).getString(key);
        String suffix = material.equals("paper") || material.equals("cactus") ? "_" + material : "";
        return ArmoryContent.id("textures/models/armor/" + name + suffix + ".png");
    }
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<Item> onBroken) {
        ArmorData.damage(stack, entity, GameplayEvents.currentDamage(entity), amount);
        return 0;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof LivingEntity living && ArmorData.initialized(stack)) {
            if (living.tickCount % 20 == 0) { ArmorData.rebuild(stack); }
            ArmorData.invoke(stack, living, "inventoryTick", null, 0, null);
        }
    }
    @Override public Component getName(ItemStack stack) {
        if (!ArmorData.initialized(stack)) { return super.getName(stack); }
        return Component.translatable("item.conarm.material_part",
                com.gctn.tconstruct.library.materials.Material.nameOf(ArmorData.read(stack).getString("Core")), super.getName(stack));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!ArmorData.initialized(stack)) { tooltip.add(Component.translatable("tooltip.conarm.unbuilt")); return; }
        var stats = ArmorData.displayStats(stack);
        if (ArmorData.broken(stack)) { tooltip.add(Component.translatable("tooltip.conarm.broken").withStyle(ChatFormatting.RED)); }
        tooltip.add(Component.translatable("tooltip.conarm.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
        tooltip.add(Component.translatable("tooltip.conarm.defense", stats.defense, stats.toughness));
        tooltip.add(Component.translatable("tooltip.conarm.modifiers", ArmorData.freeSlots(stack)));
        ArmorData.traits(stack).forEach((id, level) -> tooltip.add(Component.translatable("trait." + id.replace(':', '.') + ".name")
                .append(level > 1 ? " " + level : "").withColor(ArmorData.traitColor(stack, id))));
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            int points = ArmorData.points(stack, modifier.id);
            if (points > 0) { tooltip.add(Component.translatable("modifier." + modifier.id.replace(':', '.') + ".name")
                    .append(" " + points + "/" + modifier.pointsPerLevel() * modifier.maxLevel()).withStyle(ChatFormatting.GOLD)); }
        }
        String key = switch (getEquipmentSlot()) { case HEAD -> "key.conarm.toggle_helm.desc"; case CHEST -> "key.conarm.toggle_chest.desc"; default -> "key.conarm.toggle_leg.desc"; };
        if (getEquipmentSlot() != net.minecraft.world.entity.EquipmentSlot.FEET) { tooltip.add(Component.translatable("tooltip.conarm.accessories", Component.keybind(key)).withStyle(ChatFormatting.GRAY)); }
    }
}
