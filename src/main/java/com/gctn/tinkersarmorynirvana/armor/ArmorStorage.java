/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Storage has its own component so frequent stat/trait reads never copy a loaded knapsack. */
public final class ArmorStorage {
    private ArmorStorage() {}
    public static CompoundTag read(ItemStack stack) { return stack.getOrDefault(ArmoryContent.ARMOR_STORAGE.get(), CustomData.EMPTY).copyTag(); }
    public static void write(ItemStack stack, CompoundTag tag) { stack.set(ArmoryContent.ARMOR_STORAGE.get(), CustomData.of(tag)); }
}
