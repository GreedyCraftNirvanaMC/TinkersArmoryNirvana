/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.crafting.Ingredient;

public record IngredientCost(Ingredient ingredient, int count) {
    public static final Codec<IngredientCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(IngredientCost::ingredient),
            Codec.intRange(1, 384).optionalFieldOf("count", 1).forGetter(IngredientCost::count)).apply(instance, IngredientCost::new));
    public IngredientCost {
        if (count < 1 || count > 384) { throw new IllegalArgumentException("Ingredient count must be 1..384"); }
    }
}
