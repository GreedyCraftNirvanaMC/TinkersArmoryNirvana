/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

/** Mutable calculation context. Attribute values are for one armor piece. */
public final class ArmorStats {
    public final int baseDurability;
    public final float baseDefense;
    public final float baseToughness;
    public int durability;
    public float defense;
    public float toughness;
    public int modifiers = 3;
    public float speed;
    public float knockbackResistance;
    public float health;
    public float swimSpeed;
    public float attack;
    public float attackSpeed;
    public float reach;
    public float stepHeight;

    public ArmorStats(int durability, float defense, float toughness) {
        this.baseDurability = durability;
        this.baseDefense = defense;
        this.baseToughness = toughness;
        this.durability = durability;
        this.defense = defense;
        this.toughness = toughness;
    }

    public void validate() {
        if (durability < 1 || modifiers < 0 || modifiers > 8192) {
            throw new IllegalArgumentException("Invalid armor durability or modifier slots");
        }
        for (float value : new float[]{defense, toughness, speed, knockbackResistance, health,
                swimSpeed, attack, attackSpeed, reach, stepHeight}) {
            if (!Float.isFinite(value)) { throw new IllegalArgumentException("Non-finite armor stat"); }
        }
        defense = Math.max(0, defense);
        toughness = Math.max(0, toughness);
    }
}
