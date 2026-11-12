/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/** Pure calculation event on NeoForge.EVENT_BUS, on both physical sides and without a wearer. */
public final class ArmorStatsEvent extends Event {
    private final ItemStack armor;
    private final ArmorStats stats;
    public ArmorStatsEvent(ItemStack armor, ArmorStats stats) {
        this.armor = armor.copy(); this.stats = stats;
    }
    /** Independent snapshot; modifying it does not change the original armor. */
    public ItemStack getArmor() { return armor.copy(); }
    /** Mutable result after builtin calculations and before defense allocation and final validation. */
    public ArmorStats getStats() { return stats; }
}
