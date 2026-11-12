/* Armor values adapted from Construct's Armory, Copyright (c) 2018-2020 C4.
 * SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.api.ArmorMaterialDefinition;

public final class BuiltinMaterials {
    private static boolean initialized;
    private BuiltinMaterials() {}
    public static void initialize() {
        if (initialized) { return; }
        initialized = true;
        new ArmorMaterialDefinition("wood").core(2.5F, 3).plates(1, 1, 0).trim(0.5F)
                .trait("core", "ecological", 1)
                .trait("plates", "ecological", 1)
                .trait("trim", "ecological", 1)
                .install();
        new ArmorMaterialDefinition("stone").core(8.7F, 5).plates(0.5F, -3.5F, 0).trim(0.75F)
                .trait("core", "cheapskate", 1)
                .trait("plates", "cheap", 1)
                .trait("trim", "cheap", 1)
                .install();
        new ArmorMaterialDefinition("flint").core(10, 4.5F).plates(0.6F, -5, 1).trim(2.6F)
                .trait("core", "mundane", 2)
                .trait("plates", "mundane", 1)
                .trait("trim", "mundane", 1)
                .install();
        new ArmorMaterialDefinition("cactus").core(12.5F, 10).plates(0.85F, 0.75F, 0).trim(3.5F)
                .trait("core", "spiny", 1)
                .trait("plates", "spiny", 1)
                .trait("trim", "spiny", 1)
                .install();
        new ArmorMaterialDefinition("obsidian").core(9.5F, 16.2F).plates(0.9F, -8, 3.5F).trim(7)
                .trait("core", "duritos_ranch", 1)
                .trait("plates", "duritos_ranch", 1)
                .trait("trim", "duritos_ranch", 1)
                .install();
        new ArmorMaterialDefinition("prismarine").core(16, 18.8F).plates(0.6F, -10, 2).trim(8)
                .trait("core", "aquaspeed", 1)
                .trait("plates", "aquaspeed", 1)
                .trait("trim", "aquaspeed", 1)
                .trait("core", "rough", 1)
                .install();
        new ArmorMaterialDefinition("endstone").core(15.5F, 9).plates(0.85F, 0, 1).trim(2.7F)
                .trait("core", "alien", 1)
                .trait("plates", "enderport", 1)
                .trait("trim", "enderport", 1)
                .install();
        new ArmorMaterialDefinition("paper").core(0.4F, 0.5F).plates(0.1F, 0.3F, 0).trim(0.5F)
                .trait("core", "writable", 2)
                .install();
        new ArmorMaterialDefinition("sponge").core(21, 1).plates(1.2F, 13, 5).trim(13)
                .trait("core", "absorbent", 1)
                .trait("plates", "absorbent", 1)
                .trait("trim", "absorbent", 1)
                .install();
        new ArmorMaterialDefinition("bone").core(12, 4).plates(1.10F, 3.5F, 1).trim(5.3F)
                .trait("core", "calcic", 1)
                .trait("plates", "skeletal", 1)
                .trait("trim", "skeletal", 1)
                .install();
        new ArmorMaterialDefinition("bloodbone").core(12, 4).plates(1.10F, 3.5F, 1).trim(5.3F)
                .trait("core", "desperation", 1)
                .trait("plates", "desperation", 1)
                .trait("trim", "desperation", 1)
                .trait("core", "calcic", 1)
                .trait("plates", "skeletal", 1)
                .trait("trim", "skeletal", 1)
                .install();
        new ArmorMaterialDefinition("slime").core(20.7F, 2).plates(0.7F, 0, 2).trim(14.5F)
                .trait("core", "slimey_green", 1)
                .trait("plates", "slimey_green", 1)
                .trait("trim", "slimey_green", 1)
                .trait("core", "bouncy", 1)
                .install();
        new ArmorMaterialDefinition("blueslime").core(19.5F, 1.8F).plates(1.3F, -3.5F, 2.25F).trim(12)
                .trait("core", "slimey_blue", 1)
                .trait("plates", "slimey_blue", 1)
                .trait("trim", "slimey_blue", 1)
                .trait("core", "bouncy", 1)
                .install();
        new ArmorMaterialDefinition("knightslime").core(20, 17.1F).plates(0.5F, 16.7F, 1).trim(9)
                .trait("core", "dramatic", 1)
                .trait("plates", "invigorating", 1)
                .trait("trim", "invigorating", 1)
                .install();
        new ArmorMaterialDefinition("magmaslime").core(18, 19.4F).plates(0.85F, -12, 2.5F).trim(10)
                .trait("core", "autoforge", 1)
                .trait("plates", "superhot", 1)
                .trait("trim", "superhot", 1)
                .install();
        new ArmorMaterialDefinition("netherrack").core(13.5F, 5.3F).plates(0.85F, -10, 0).trim(5.5F)
                .trait("core", "aridiculous", 1)
                .trait("core", "infernal", 1)
                .trait("plates", "infernal", 1)
                .trait("trim", "infernal", 1)
                .install();
        new ArmorMaterialDefinition("cobalt").core(19.5F, 15.6F).plates(0.9F, 8, 0).trim(14)
                .trait("core", "featherweight", 1)
                .trait("plates", "lightweight", 1)
                .trait("trim", "lightweight", 1)
                .install();
        new ArmorMaterialDefinition("ardite").core(20.5F, 13).plates(1.4F, -12, 4).trim(16.2F)
                .trait("core", "subterranean", 1)
                .trait("plates", "petravidity", 1)
                .trait("trim", "petravidity", 1)
                .install();
        new ArmorMaterialDefinition("manyullyn").core(19.7F, 20).plates(1.0F, 13, 3).trim(3.5F)
                .trait("core", "vengeful", 1)
                .trait("plates", "prideful", 1)
                .trait("trim", "prideful", 1)
                .install();
        new ArmorMaterialDefinition("firewood").core(17, 17.5F).plates(1.0F, -12, 0.5F).trim(10)
                .trait("core", "combustible", 1)
                .trait("plates", "combustible", 1)
                .trait("trim", "combustible", 1)
                .install();
        new ArmorMaterialDefinition("iron").core(12, 15).plates(0.85F, 5, 0).trim(3.5F)
                .trait("core", "magnetic", 2)
                .trait("plates", "magnetic", 1)
                .trait("trim", "magnetic", 1)
                .install();
        new ArmorMaterialDefinition("alubrass").core(15, 15.5F).plates(1.0F, 7, 1).trim(8.5F)
                .trait("core", "depth_explorer", 1)
                .trait("plates", "depth_explorer", 1)
                .trait("trim", "depth_explorer", 1)
                .install();
        new ArmorMaterialDefinition("alumite").core(18.0F, 17.0F).plates(1.0F, 0, 2.5F).trim(9.5F)
                .trait("core", "duritos_ranch", 1)
                .trait("plates", "duritos_ranch", 1)
                .trait("trim", "duritos_ranch", 1)
                .install();
        new ArmorMaterialDefinition("pigiron").core(15, 16.7F).plates(1.2F, 0, 1).trim(10.5F)
                .trait("core", "baconlicious", 1)
                .trait("core", "tasty", 1)
                .trait("plates", "tasty", 1)
                .trait("trim", "tasty", 1)
                .install();
        new ArmorMaterialDefinition("copper").core(12, 8).plates(1.05F, 2, 0).trim(8)
                .trait("core", "ambitious", 1)
                .trait("plates", "ambitious", 1)
                .trait("trim", "ambitious", 1)
                .install();
        new ArmorMaterialDefinition("bronze").core(16, 12).plates(1.10F, 5.5F, 1.25F).trim(6.5F)
                .trait("core", "dense", 1)
                .trait("plates", "dense", 1)
                .trait("trim", "dense", 1)
                .install();
        new ArmorMaterialDefinition("lead").core(16, 11).plates(0.7F, -3.5F, 2).trim(8)
                .trait("core", "heavy", 1)
                .trait("plates", "heavy", 1)
                .trait("trim", "heavy", 1)
                .trait("core", "shielding", 1)
                .trait("plates", "shielding", 1)
                .trait("trim", "shielding", 1)
                .install();
        new ArmorMaterialDefinition("silver").core(13, 17).plates(0.95F, 3.5F, 2).trim(10)
                .trait("core", "blessed", 1)
                .trait("plates", "blessed", 1)
                .trait("trim", "blessed", 1)
                .install();
        new ArmorMaterialDefinition("electrum").core(3.5F, 8.1F).plates(1.1F, -1, 0).trim(13)
                .trait("core", "voltaic", 1)
                .trait("plates", "voltaic", 1)
                .trait("trim", "voltaic", 1)
                .install();
        new ArmorMaterialDefinition("steel").core(17, 18.4F).plates(0.9F, 10, 4.5F).trim(1)
                .trait("core", "steady", 1)
                .trait("plates", "indomitable", 1)
                .trait("trim", "indomitable", 1)
                .install();
    }
}
