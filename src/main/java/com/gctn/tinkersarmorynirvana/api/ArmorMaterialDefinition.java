/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import com.gctn.tconstruct.library.TinkerMaterials;
import java.util.ArrayList;
import java.util.List;

/** Armor statistics augment a TinkersNirvana material without changing its tool statistics. */
public final class ArmorMaterialDefinition {
    public record Trait(String id, int level) {
        public Trait {
            id = ArmoryRegistry.id(id);
            if (level < 1) { throw new IllegalArgumentException("Invalid material trait level"); }
        }
    }

    public final String id;
    public float coreDurability, defense, plateMultiplier = 1, plateDurability, toughness, trimDurability;
    private final List<List<Trait>> traits = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    public ArmorMaterialDefinition(String id) { this.id = ArmoryRegistry.materialId(id); }
    public ArmorMaterialDefinition(ArmorMaterialDefinition previous) {
        this(previous.id);
        core(previous.coreDurability, previous.defense);
        plates(previous.plateMultiplier, previous.plateDurability, previous.toughness);
        trim(previous.trimDurability);
        for (int i = 0; i < 3; i++) { traits.get(i).addAll(previous.traits(i)); }
    }

    public ArmorMaterialDefinition core(float durability, float defense) {
        this.coreDurability = durability; this.defense = defense; return this;
    }
    public ArmorMaterialDefinition plates(float multiplier, float durability, float toughness) {
        plateMultiplier = multiplier; plateDurability = durability; this.toughness = toughness; return this;
    }
    public ArmorMaterialDefinition trim(float durability) { trimDurability = durability; return this; }
    public ArmorMaterialDefinition trait(String part, String id, int level) {
        traits.get(partIndex(part)).add(new Trait(id, level)); return this;
    }
    public ArmorMaterialDefinition clearTraits(String part) { traits.get(partIndex(part)).clear(); return this; }
    public List<Trait> traits(int part) { return List.copyOf(traits.get(part)); }
    public ArmorMaterialDefinition install() { ArmoryRegistry.material(this); return this; }

    public void validate() {
        if (TinkerMaterials.getMaterial(id) == null) { throw new IllegalArgumentException("Unknown TinkersNirvana material: " + id); }
        for (float value : new float[]{coreDurability, defense, plateMultiplier, plateDurability, toughness, trimDurability}) {
            if (!Float.isFinite(value)) { throw new IllegalArgumentException("Non-finite armor material stat: " + id); }
        }
        if (coreDurability <= 0 || defense < 0 || plateMultiplier <= 0 || toughness < 0 || trimDurability < 0) {
            throw new IllegalArgumentException("Invalid armor material stats: " + id);
        }
        for (var list : traits) {
            for (var trait : list) {
                ArmorTrait definition = ArmoryRegistry.trait(trait.id());
                if (definition == null || trait.level() > definition.maxLevel()) {
                    throw new IllegalArgumentException("Unknown trait or excessive level: " + trait.id());
                }
            }
        }
    }

    private static int partIndex(String part) {
        return switch (part) {
            case "core" -> 0;
            case "plates" -> 1;
            case "trim" -> 2;
            default -> throw new IllegalArgumentException("Expected core, plates or trim: " + part);
        };
    }
}
