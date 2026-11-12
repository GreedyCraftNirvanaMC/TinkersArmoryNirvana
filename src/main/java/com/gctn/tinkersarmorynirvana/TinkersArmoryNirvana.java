/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(TinkersArmoryNirvana.MODID)
public class TinkersArmoryNirvana {
    public static final String MODID = "conarm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TinkersArmoryNirvana(IEventBus modEventBus, ModContainer modContainer) {
        ArmoryContent.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, ArmoryConfig.SERVER);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ArmoryConfig.CLIENT);
        modEventBus.addListener(this::setup);
    }

    private void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            armorDefaults();
            com.gctn.tconstruct.tables.recipe.StationProcessingRecipes.register("conarm:armor",
                    stack -> stack.getItem() instanceof com.gctn.tinkersarmorynirvana.armor.ModularArmorItem
                            || stack.getItem() instanceof com.gctn.tinkersarmorynirvana.armor.ArmoryPatternItem,
                    com.gctn.tinkersarmorynirvana.station.ArmoryWorkshop::nativePlan);
        });
    }

    public static void armorDefaults() {
        com.gctn.tinkersarmorynirvana.armor.BuiltinTraits.initialize();
        com.gctn.tinkersarmorynirvana.armor.BuiltinModifiers.initialize();
        com.gctn.tinkersarmorynirvana.armor.BuiltinMaterials.initialize();
    }
}
