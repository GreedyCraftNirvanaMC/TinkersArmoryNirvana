/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.compat.kubejs;

import com.gctn.tinkersarmorynirvana.TinkersArmoryNirvana;
import com.gctn.tinkersarmorynirvana.api.*;
import dev.latvian.mods.kubejs.event.*;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.function.Consumer;

public final class ArmoryKubeJSPlugin implements KubeJSPlugin {
    private static final EventGroup EVENTS = EventGroup.of("ArmoryEvents");
    private static final EventHandler TRAITS = EVENTS.startup("modifyTraits", () -> TraitsEvent.class);
    private static final EventHandler MODIFIERS = EVENTS.startup("modifyModifiers", () -> ModifiersEvent.class);
    private static final EventHandler MATERIALS = EVENTS.startup("modifyMaterials", () -> MaterialsEvent.class);
    @Override public void registerEvents(EventGroupRegistry registry) { registry.register(EVENTS); }
    @Override public void afterInit() {
        // neoforge.mods.toml 将 conarm 排在 tconstruct 后面；复用 KubeJS 的正式插件生命周期，
        // 此时依赖插件已经安装启动注册表中的材料，Java 附属的 common setup 也已执行。
        TinkersArmoryNirvana.armorDefaults();
        TRAITS.post(ScriptType.STARTUP, new TraitsEvent());
        MODIFIERS.post(ScriptType.STARTUP, new ModifiersEvent());
        MATERIALS.post(ScriptType.STARTUP, new MaterialsEvent());
        ArmoryRegistry.changed();
        TinkersArmoryNirvana.LOGGER.info("Installed ArmoryEvents startup definitions");
    }
    public static final class TraitsEvent implements KubeEvent {
        public void create(String id, Consumer<ArmorTrait> action) {
            ArmorTrait trait = new ArmorTrait(ArmoryRegistry.id(id)); action.accept(trait); ArmoryRegistry.registerTrait(trait);
        }
        public void modify(String id, Consumer<ArmorTrait> action) {
            var trait = ArmoryRegistry.trait(id); if (trait == null) { throw new IllegalArgumentException("Unknown armor trait: " + id); }
            action.accept(trait); ArmoryRegistry.changed();
        }
        public void remove(String id) { ArmoryRegistry.removeTrait(id); }
    }
    public static final class ModifiersEvent implements KubeEvent {
        public void create(String id, Consumer<ArmorModifier> action) {
            ArmorModifier modifier = new ArmorModifier(ArmoryRegistry.id(id)); action.accept(modifier); ArmoryRegistry.registerModifier(modifier);
        }
        public void modify(String id, Consumer<ArmorModifier> action) {
            var modifier = ArmoryRegistry.modifier(id); if (modifier == null) { throw new IllegalArgumentException("Unknown armor modifier: " + id); }
            action.accept(modifier); ArmoryRegistry.changed();
        }
        public void remove(String id) { ArmoryRegistry.removeModifier(id); }
    }
    public static final class MaterialsEvent implements KubeEvent {
        public void create(String id, Consumer<ArmorMaterialDefinition> action) {
            if (ArmoryRegistry.material(id) != null) { throw new IllegalArgumentException("Armor material already exists: " + id); }
            var material = new ArmorMaterialDefinition(id); action.accept(material); material.install();
        }
        public void modify(String id, Consumer<ArmorMaterialDefinition> action) {
            var previous = ArmoryRegistry.material(id); if (previous == null) { throw new IllegalArgumentException("Unknown armor material: " + id); }
            var material = new ArmorMaterialDefinition(previous); action.accept(material); material.install();
        }
        public void castingFluid(String material, String fluid) { com.gctn.tinkersarmorynirvana.armor.ArmoryCasting.bind(material, fluid); }
        public void remove(String id) { ArmoryRegistry.removeMaterial(id); }
    }
}
