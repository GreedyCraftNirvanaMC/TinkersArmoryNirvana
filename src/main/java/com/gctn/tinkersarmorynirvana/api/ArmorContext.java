/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import com.gctn.tinkersarmorynirvana.armor.ArmorData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Server callbacks may mutate state and explicitly call save(). Stat callbacks must be pure. */
public final class ArmorContext {
    public final ItemStack armor;
    public final LivingEntity wearer;
    public final String id;
    public final int level;
    public final CompoundTag state;
    public final DamageSource source;
    public final ArmorStats stats;
    public float amount;

    public ArmorContext(ItemStack armor, LivingEntity wearer, String id, int level,
                        DamageSource source, float amount, ArmorStats stats) {
        this.armor = armor;
        this.wearer = wearer;
        this.id = id;
        this.level = level;
        this.source = source;
        this.amount = amount;
        this.stats = stats;
        this.state = ArmorData.read(armor).getCompound("State").getCompound(id).copy();
    }

    public void save() {
        CompoundTag data = ArmorData.read(armor);
        CompoundTag states = data.getCompound("State");
        states.put(id, state.copy());
        data.put("State", states);
        ArmorData.write(armor, data);
    }
}
