/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class ArmoryIngredientItem extends Item {
    public ArmoryIngredientItem() { super(new Properties()); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String text = Component.translatableWithFallback(getDescriptionId() + ".tooltip", "").getString();
        if (!text.isBlank()) { for (String line : text.split("\n")) { tooltip.add(Component.literal(line)); } }
    }
}
