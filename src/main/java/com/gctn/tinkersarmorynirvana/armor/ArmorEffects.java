/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Tracks only effects owned by this mod, so removing an accessory does not clear somebody else's potion. */
public final class ArmorEffects {
    private record Applied(MobEffectInstance instance, int duration) {}
    private static final Map<LivingEntity, Map<Holder<MobEffect>, Applied>> OWNED = new WeakHashMap<>();
    private static final ThreadLocal<Set<Holder<MobEffect>>> TOUCHED = new ThreadLocal<>();
    private ArmorEffects() {}
    public static void begin() { TOUCHED.set(new HashSet<>()); }
    public static void apply(LivingEntity entity, Holder<MobEffect> type, int duration) {
        var touched = TOUCHED.get(); if (touched != null) { touched.add(type); }
        var owned = OWNED.computeIfAbsent(entity, ignored -> new HashMap<>());
        var current = entity.getEffect(type); var previous = owned.get(type);
        if (current != null && (previous == null || current != previous.instance() || current.getAmplifier() > 0 || current.getDuration() > previous.duration())) { return; }
        entity.addEffect(new MobEffectInstance(type, duration, 0, false, false));
        owned.put(type, new Applied(entity.getEffect(type), duration));
    }
    public static void end(LivingEntity entity) {
        var touched = TOUCHED.get(); TOUCHED.remove();
        var owned = OWNED.get(entity); if (owned == null) { return; }
        var iterator = owned.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); if (touched != null && touched.contains(entry.getKey())) { continue; }
            var current = entity.getEffect(entry.getKey()); var applied = entry.getValue();
            if (current != null && current == applied.instance() && current.getAmplifier() == 0 && current.getDuration() <= applied.duration()) { entity.removeEffect(entry.getKey()); }
            iterator.remove();
        }
        if (owned.isEmpty()) { OWNED.remove(entity); }
    }
    public static void clear() { OWNED.clear(); TOUCHED.remove(); }
}
