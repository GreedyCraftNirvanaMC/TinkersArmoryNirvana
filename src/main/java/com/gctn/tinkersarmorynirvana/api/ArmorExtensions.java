/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import com.gctn.tinkersarmorynirvana.armor.ArmorData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Namespaced addon state and passive modifier-slot contributions. No experience or leveling rules. */
public final class ArmorExtensions {
    private static final String EXTENSIONS = "Extensions";
    private ArmorExtensions() {}
    private static String key(ItemStack armor, String id) {
        if (!ArmorData.initialized(armor)) { throw new IllegalArgumentException("Expected initialized modular armor"); }
        if (id.indexOf(':') <= 0) { throw new IllegalArgumentException("Armor extensions require a namespaced identifier: " + id); }
        return ResourceLocation.parse(id).toString();
    }
    public static CompoundTag read(ItemStack armor, String id) {
        return ArmorData.read(armor).getCompound(EXTENSIONS).getCompound(key(armor, id)).getCompound("Data").copy();
    }
    public static int modifierSlots(ItemStack armor, String id) {
        return ArmorData.read(armor).getCompound(EXTENSIONS).getCompound(key(armor, id)).getInt("ModifierSlots");
    }
    public static int totalModifierSlots(ItemStack armor) {
        var entries = ArmorData.read(armor).getCompound(EXTENSIONS);
        long total = 0;
        for (String id : entries.getAllKeys()) { total += Math.max(0, entries.getCompound(id).getInt("ModifierSlots")); }
        if (total > 8192) { throw new IllegalArgumentException("Too many addon modifier slots"); }
        return (int) total;
    }
    public static void write(ItemStack armor, String id, CompoundTag data) {
        update(armor, id, data, modifierSlots(armor, id));
    }
    public static void setModifierSlots(ItemStack armor, String id, int slots) {
        update(armor, id, read(armor, id), slots);
    }
    /** Save both values together and rebuild immediately; an invalid update leaves the original unchanged. */
    public static void update(ItemStack armor, String id, CompoundTag data, int slots) {
        String key = key(armor, id); Objects.requireNonNull(data);
        if (slots < 0 || slots > 8192) { throw new IllegalArgumentException("Invalid addon modifier-slot contribution: " + slots); }
        var root = ArmorData.read(armor); var entries = root.getCompound(EXTENSIONS);
        if (data.isEmpty() && slots == 0) { entries.remove(key); }
        else {
            var entry = new CompoundTag(); entry.put("Data", data.copy()); entry.putInt("ModifierSlots", slots); entries.put(key, entry);
        }
        if (entries.isEmpty()) { root.remove(EXTENSIONS); } else { root.put(EXTENSIONS, entries); }
        // 先在独立副本上完成属性计算及槽位上限校验。失败时不写回，避免半次更新污染装备或其他附属的数据。
        var candidate = armor.copy(); ArmorData.write(candidate, root); ArmorData.rebuild(candidate);
        armor.applyComponents(candidate.getComponentsPatch());
    }
    /** Remove only this owner's state and slot contribution, preserving every other owner and installed modifier. */
    public static void remove(ItemStack armor, String id) { update(armor, id, new CompoundTag(), 0); }
}
