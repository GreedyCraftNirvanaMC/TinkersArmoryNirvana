/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Register definitions during common setup or ArmoryEvents startup events. */
public final class ArmoryRegistry {
    private static final Map<String, ArmorMaterialDefinition> MATERIALS = new LinkedHashMap<>();
    private static final Map<String, ArmorTrait> TRAITS = new LinkedHashMap<>();
    private static final Map<String, ArmorModifier> MODIFIERS = new LinkedHashMap<>();
    private static long revision;
    private ArmoryRegistry() {}

    public static String id(String value) {
        return ResourceLocation.parse(value.indexOf(':') < 0 ? "conarm:" + value : value).toString();
    }
    public static long revision() { return revision; }
    public static String materialId(String id) {
        var material = com.gctn.tconstruct.library.TinkerMaterials.getMaterial(id); return material == null ? id : material.identifier;
    }
    public static void changed() { revision++; }
    public static void material(ArmorMaterialDefinition material) { material.validate(); MATERIALS.put(material.id, material); changed(); }
    public static ArmorMaterialDefinition material(String id) { return MATERIALS.get(materialId(id)); }
    public static void removeMaterial(String id) { MATERIALS.remove(materialId(id)); changed(); }
    public static Map<String, ArmorMaterialDefinition> materials() { return Collections.unmodifiableMap(MATERIALS); }
    public static void registerTrait(ArmorTrait trait) {
        if (TRAITS.putIfAbsent(trait.id, trait) != null) { throw new IllegalArgumentException("Duplicate armor trait: " + trait.id); }
        changed();
    }
    public static void replaceTrait(ArmorTrait trait) { TRAITS.put(trait.id, trait); changed(); }
    public static ArmorTrait trait(String id) { return TRAITS.get(id(id)); }
    public static Map<String, ArmorTrait> traits() { return Collections.unmodifiableMap(TRAITS); }
    public static void removeTrait(String id) { TRAITS.remove(id(id)); changed(); }
    public static void registerModifier(ArmorModifier modifier) {
        if (MODIFIERS.putIfAbsent(modifier.id, modifier) != null) { throw new IllegalArgumentException("Duplicate armor modifier: " + modifier.id); }
        changed();
    }
    public static ArmorModifier modifier(String id) { return MODIFIERS.get(id(id)); }
    public static Map<String, ArmorModifier> modifiers() { return Collections.unmodifiableMap(MODIFIERS); }
    public static void removeModifier(String id) { MODIFIERS.remove(id(id)); changed(); }
}
