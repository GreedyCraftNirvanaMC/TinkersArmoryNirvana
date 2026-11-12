/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

public record ArmoryModifierRecipe(String modifier, int points, List<IngredientCost> ingredients) implements Recipe<ArmoryInput> {
    public ArmoryModifierRecipe {
        modifier = ArmoryRegistry.id(modifier); ingredients = List.copyOf(ingredients);
        if (points < 1 || points > 1000000 || ingredients.isEmpty() || ingredients.size() > 6) { throw new IllegalArgumentException("Invalid armor modifier recipe"); }
    }
    public int[] allocation(ArmoryInput input) { return IngredientMatcher.allocate(input.container(), ingredients); }
    @Override public boolean matches(ArmoryInput input, Level level) { return allocation(input) != null; }
    @Override public ItemStack assemble(ArmoryInput input, HolderLookup.Provider registries) { return ArmoryWorkshop.applyModifier(input.getItem(0), modifier, points); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ArmoryRecipes.MODIFIER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ArmoryRecipes.MODIFIER_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<ArmoryModifierRecipe> {
        private static final MapCodec<ArmoryModifierRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("modifier").forGetter(ArmoryModifierRecipe::modifier),
                Codec.intRange(1, 1000000).optionalFieldOf("points", 1).forGetter(ArmoryModifierRecipe::points),
                IngredientCost.CODEC.listOf(1, 6).fieldOf("ingredients").forGetter(ArmoryModifierRecipe::ingredients)).apply(instance, ArmoryModifierRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ArmoryModifierRecipe> STREAM = StreamCodec.of((buffer, recipe) -> {
            buffer.writeUtf(recipe.modifier); buffer.writeVarInt(recipe.points); buffer.writeVarInt(recipe.ingredients.size());
            for (var cost : recipe.ingredients) { Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, cost.ingredient()); buffer.writeVarInt(cost.count()); }
        }, buffer -> {
            String id = buffer.readUtf(); int points = buffer.readVarInt(); int count = buffer.readVarInt();
            if (count < 1 || count > 6) { throw new IllegalArgumentException("Invalid ingredient list length"); }
            var costs = new java.util.ArrayList<IngredientCost>();
            for (int i = 0; i < count; i++) { costs.add(new IngredientCost(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt())); }
            return new ArmoryModifierRecipe(id, points, costs);
        });
        @Override public MapCodec<ArmoryModifierRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ArmoryModifierRecipe> streamCodec() { return STREAM; }
    }
}
