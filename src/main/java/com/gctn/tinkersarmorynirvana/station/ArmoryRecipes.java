/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ArmoryRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "conarm");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "conarm");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ArmoryModifierRecipe>> MODIFIER_TYPE = TYPES.register("armor_modifier", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArmoryModifierRecipe>> MODIFIER_SERIALIZER = SERIALIZERS.register("armor_modifier", ArmoryModifierRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KitRepairRecipe>> KIT_REPAIR = SERIALIZERS.register("kit_repair", () -> new SimpleCraftingRecipeSerializer<>(KitRepairRecipe::new));
    private ArmoryRecipes() {}
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
}
