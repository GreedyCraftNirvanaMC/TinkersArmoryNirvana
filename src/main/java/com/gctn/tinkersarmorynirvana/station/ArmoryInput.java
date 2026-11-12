/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ArmoryInput(Container container) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return container.getItem(index); }
    @Override public int size() { return 6; }
}
