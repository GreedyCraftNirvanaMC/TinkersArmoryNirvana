/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.armor.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class KitRepairRecipe extends CustomRecipe {
    public KitRepairRecipe(CraftingBookCategory category) { super(category); }
    private ItemStack repair(CraftingInput input) {
        SimpleContainer container = new SimpleContainer(6); int kits = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) { continue; }
            if (stack.getItem() instanceof ModularArmorItem) {
                if (!container.getItem(0).isEmpty()) { return ItemStack.EMPTY; } container.setItem(0, stack.copyWithCount(1));
            } else if (stack.getItem() instanceof ArmorPartItem part && part.part.equals("polishing_kit")) {
                if (kits == 0) { container.setItem(1, stack.copyWithCount(1)); }
                else if (!ArmorPartItem.material(container.getItem(1)).equals(ArmorPartItem.material(stack))) { return ItemStack.EMPTY; }
                else { container.getItem(1).grow(1); }
                kits++;
            } else { return ItemStack.EMPTY; }
        }
        var plan = kits > 0 ? ArmoryWorkshop.repair(container) : null; return plan == null ? ItemStack.EMPTY : plan.result();
    }
    @Override public boolean matches(CraftingInput input, Level level) { return !repair(input).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return repair(input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }
    @Override public RecipeSerializer<?> getSerializer() { return ArmoryRecipes.KIT_REPAIR.get(); }
}
