/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.smeltery.SmelteryFluids;
import com.gctn.tconstruct.smeltery.recipe.SmelteryRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArmoryCasting {
    private static final Map<String, String> FLUIDS = new LinkedHashMap<>();
    private static boolean installed;
    private ArmoryCasting() {}
    public static void bind(String material, String fluid) {
        if (installed) { throw new IllegalStateException("Casting bindings require a game restart"); }
        FLUIDS.put(ArmoryRegistry.materialId(material), ResourceLocation.parse(fluid).toString());
    }
    public static void install() {
        if (installed) { return; } installed = true;
        SmelteryRecipes.initialize();
        for (var entry : ArmoryRegistry.materials().values()) {
            var material = TinkerMaterials.getMaterial(entry.id); if (material == null || !material.isCastable()) { continue; }
            var fluid = FLUIDS.containsKey(entry.id) ? BuiltInRegistries.FLUID.get(ResourceLocation.parse(FLUIDS.get(entry.id)))
                    : SmelteryFluids.ENTRIES.containsKey(entry.id) ? SmelteryFluids.get(entry.id) : net.minecraft.world.level.material.Fluids.EMPTY;
            if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
                if (FLUIDS.containsKey(entry.id)) { throw new IllegalArgumentException("Unknown armor casting fluid: " + FLUIDS.get(entry.id)); }
                continue;
            }
            for (var part : ArmoryContent.PARTS.values()) {
                ArmorPartItem item = part.get(); ItemStack result = item.create(material.identifier); int amount = item.cost * 144;
                SmelteryRecipes.MELTING.add(SmelteryRecipes.melting(stack -> stack.is(item) && ArmorPartItem.material(stack).equals(entry.id), fluid, amount));
                var cast = ArmoryContent.MATERIAL_ITEMS.get("cast_" + item.part).get();
                var clayCast = ArmoryContent.MATERIAL_ITEMS.get("clay_cast_" + item.part).get();
                SmelteryRecipes.CASTING.add(SmelteryRecipes.casting(false, stack -> stack.is(cast), fluid, amount, result, false, false));
                SmelteryRecipes.CASTING.add(SmelteryRecipes.casting(false, stack -> com.gctn.tconstruct.Config.CLAY_CASTS.get() && stack.is(clayCast), fluid, amount, result, true, false));
            }
        }
        for (var part : ArmoryContent.PARTS.values()) {
            ItemStack cast = new ItemStack(ArmoryContent.MATERIAL_ITEMS.get("cast_" + part.get().part).get());
            for (String fluid : new String[]{"gold", "alubrass", "brass"}) {
                if (!SmelteryFluids.ENTRIES.containsKey(fluid)) { continue; }
                SmelteryRecipes.CASTING.add(SmelteryRecipes.casting(false, stack -> stack.is(part.get()), SmelteryFluids.get(fluid),
                        fluid.equals("gold") ? 288 : 144, cast, true, true));
            }
            SmelteryRecipes.CASTING.add(SmelteryRecipes.casting(false, stack -> com.gctn.tconstruct.Config.CLAY_CASTS.get() && stack.is(part.get()),
                    SmelteryFluids.get("clay"), 288, new ItemStack(ArmoryContent.MATERIAL_ITEMS.get("clay_cast_" + part.get().part).get()), true, true));
        }
    }
}
