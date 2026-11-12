/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Java and KubeJS share this definition. Unknown hooks fail early instead of silently doing nothing. */
public class ArmorTrait {
    public static final List<String> HOOKS = List.of("modifyStats", "inventoryTick", "armorTick",
            "incomingDamage", "afterDamage", "durabilityDamage", "repair", "fall", "knockback", "milk", "experience",
            "jump", "outgoingDamage", "equip", "unequip", "beforeDamage", "armorEffectiveness");
    public final String id;
    private int maxLevel = 1;
    private String stacking = "max";
    private final Map<String, List<Consumer<ArmorContext>>> handlers = new LinkedHashMap<>();

    public ArmorTrait(String id) {
        this.id = ResourceLocation.parse(id).toString();
    }

    public ArmorTrait maxLevel(int level) {
        if (level < 1 || level > 1024) { throw new IllegalArgumentException("Invalid trait level"); }
        maxLevel = level;
        return this;
    }

    public int maxLevel() { return maxLevel; }
    public ArmorTrait stacking(String policy) {
        if (!List.of("max", "unique", "sum").contains(policy)) { throw new IllegalArgumentException("Expected max, unique or sum stacking"); }
        stacking = policy; return this;
    }
    public String stacking() { return stacking; }

    public ArmorTrait on(String hook, Consumer<ArmorContext> action) {
        if (!HOOKS.contains(hook)) { throw new IllegalArgumentException("Unknown armor hook: " + hook); }
        handlers.computeIfAbsent(hook, ignored -> new ArrayList<>()).add(java.util.Objects.requireNonNull(action));
        return this;
    }

    public ArmorTrait clear(String hook) {
        if (!HOOKS.contains(hook)) { throw new IllegalArgumentException("Unknown armor hook: " + hook); }
        handlers.remove(hook);
        return this;
    }

    public void invoke(String hook, ArmorContext context) {
        for (var handler : List.copyOf(handlers.getOrDefault(hook, List.of()))) { handler.accept(context); }
    }
}
