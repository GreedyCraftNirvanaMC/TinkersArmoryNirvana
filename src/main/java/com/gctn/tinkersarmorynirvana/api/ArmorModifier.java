/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import net.minecraft.world.entity.EquipmentSlot;
import java.util.Set;

public class ArmorModifier extends ArmorTrait {
    private int pointsPerLevel = 1;
    private int slotsPerLevel = 1;
    private boolean slotsOnce;
    private boolean accessory;
    private Set<EquipmentSlot> slots = Set.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private Set<String> conflicts = Set.of();

    public ArmorModifier(String id) { super(id); }
    @Override public ArmorModifier maxLevel(int level) { super.maxLevel(level); return this; }
    @Override public ArmorModifier on(String hook, java.util.function.Consumer<ArmorContext> action) { super.on(hook, action); return this; }
    public ArmorModifier pointsPerLevel(int points) {
        if (points < 1 || points > 1000000) { throw new IllegalArgumentException("Invalid modifier points"); }
        pointsPerLevel = points; return this;
    }
    public ArmorModifier slotsPerLevel(int slots) {
        if (slots < 0 || slots > 1024) { throw new IllegalArgumentException("Invalid modifier slot cost"); }
        slotsPerLevel = slots; return this;
    }
    public ArmorModifier equipment(String... names) {
        slots = java.util.Arrays.stream(names).map(name -> EquipmentSlot.valueOf(name.toUpperCase(java.util.Locale.ROOT)))
                .collect(java.util.stream.Collectors.toUnmodifiableSet()); return this;
    }
    public ArmorModifier conflicts(String... ids) {
        conflicts = java.util.Arrays.stream(ids).map(ArmoryRegistry::id).collect(java.util.stream.Collectors.toUnmodifiableSet()); return this;
    }
    public int pointsPerLevel() { return pointsPerLevel; }
    public ArmorModifier slotsOnce(boolean value) { slotsOnce = value; return this; }
    public ArmorModifier accessory(boolean value) { accessory = value; return this; }
    public boolean accessory() { return accessory; }
    public int slotCost(int level) { return (slotsOnce && level > 0 ? 1 : level) * slotsPerLevel; }
    public int slotsPerLevel() { return slotsPerLevel; }
    public boolean allows(EquipmentSlot slot) { return slots.contains(slot); }
    public Set<String> conflicts() { return conflicts; }
}
