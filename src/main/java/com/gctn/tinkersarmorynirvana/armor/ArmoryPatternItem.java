/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import java.util.ArrayList;

public class ArmoryPatternItem extends Item {
    public ArmoryPatternItem() { super(new Properties()); }
    public static String selected(ItemStack stack) {
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Part");
        return ArmoryContent.PARTS.containsKey(name) ? name : "helmet_core";
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.conarm.pattern_for", Component.translatable("item.conarm." + selected(stack)));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var names = new ArrayList<>(ArmoryContent.PARTS.keySet());
            String name = names.get(Math.floorMod(names.indexOf(selected(stack)) + (player.isShiftKeyDown() ? -1 : 1), names.size()));
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putString("Part", name); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(getName(stack), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
