/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Armor calculations adapted from Construct's Armory, Copyright (c) 2018-2020 C4. */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArmorData {
    private static final float[] DEFENSE = {.14F, .30F, .40F, .16F};
    private static final int[] DURABILITY = {13, 15, 16, 11};
    private ArmorData() {}
    public static CompoundTag read(ItemStack stack) { return stack.getOrDefault(ArmoryContent.ARMOR_DATA.get(), CustomData.EMPTY).copyTag(); }
    public static void write(ItemStack stack, CompoundTag tag) {
        // 只移除废弃内置升级的根字段；附属 Extensions/Data 内的同名数据由各自所有者管理。
        var data = tag.copy(); data.remove("Level"); data.remove("Experience");
        stack.set(ArmoryContent.ARMOR_DATA.get(), CustomData.of(data));
    }
    public static boolean initialized(ItemStack stack) { return stack.getItem() instanceof ModularArmorItem && read(stack).contains("Core"); }
    public static boolean broken(ItemStack stack) {
        if (!initialized(stack) || stack.getDamageValue() >= stack.getMaxDamage()) { return true; }
        var data = read(stack);
        return ArmoryRegistry.material(data.getString("Core")) == null || ArmoryRegistry.material(data.getString("Plates")) == null || ArmoryRegistry.material(data.getString("Trim")) == null;
    }
    public static int points(ItemStack stack, String id) { return read(stack).getCompound("Modifiers").getInt(ArmoryRegistry.id(id)); }
    public static int level(ItemStack stack, String id) {
        var modifier = ArmoryRegistry.modifier(id);
        int points = points(stack, id);
        return modifier == null || points <= 0 ? 0 : Math.min(modifier.maxLevel(), 1 + (points - 1) / modifier.pointsPerLevel());
    }
    public static boolean enabled(ItemStack stack, String id) { return !read(stack).getCompound("Disabled").getBoolean(ArmoryRegistry.id(id)); }
    public static void toggle(ItemStack stack, String id) {
        CompoundTag tag = read(stack), disabled = tag.getCompound("Disabled");
        disabled.putBoolean(ArmoryRegistry.id(id), !disabled.getBoolean(ArmoryRegistry.id(id)));
        tag.put("Disabled", disabled); write(stack, tag);
    }

    public static ItemStack create(ModularArmorItem item, String core, String plates, String trim) {
        core = ArmoryRegistry.materialId(core); plates = ArmoryRegistry.materialId(plates); trim = ArmoryRegistry.materialId(trim);
        for (String material : new String[]{core, plates, trim}) { if (ArmoryRegistry.material(material) == null) { throw new IllegalArgumentException("Unknown armor material: " + material); } }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1); tag.putString("Core", core); tag.putString("Plates", plates); tag.putString("Trim", trim);
        write(stack, tag); rebuild(stack); return stack;
    }

    public static Map<String, Integer> traits(ItemStack stack) {
        CompoundTag tag = read(stack);
        Map<String, Integer> result = new LinkedHashMap<>();
        Map<String, java.util.Set<Integer>> distinct = new LinkedHashMap<>();
        String[] keys = {"Core", "Plates", "Trim"};
        for (int i = 0; i < keys.length; i++) {
            var material = ArmoryRegistry.material(tag.getString(keys[i]));
            if (material == null) { continue; }
            for (var trait : material.traits(i)) {
                // 同一种非等级词条在三种部件上重复出现时只生效一次；等级词条取最大初始等级。
                distinct.computeIfAbsent(trait.id(), ignored -> new java.util.LinkedHashSet<>()).add(trait.level());
                var definition = ArmoryRegistry.trait(trait.id());
                if (definition != null && definition.stacking().equals("sum")) { result.merge(trait.id(), trait.level(), Integer::sum); }
                else { result.merge(trait.id(), trait.level(), Math::max); }
            }
        }
        CompoundTag embossed = tag.getCompound("EmbossedTraits");
        for (String id : embossed.getAllKeys()) {
            distinct.computeIfAbsent(id, ignored -> new java.util.LinkedHashSet<>()).add(embossed.getInt(id));
            var definition = ArmoryRegistry.trait(id);
            result.merge(id, embossed.getInt(id), definition != null && definition.stacking().equals("sum") ? Integer::sum : Math::max);
        }
        result.entrySet().removeIf(entry -> ArmoryRegistry.trait(entry.getKey()) == null);
        result.replaceAll((id, level) -> ArmoryRegistry.trait(id).stacking().equals("unique") ? distinct.get(id).stream().mapToInt(Integer::intValue).sum() : level);
        result.replaceAll((id, level) -> Math.min(level, ArmoryRegistry.trait(id).maxLevel()));
        return result;
    }

    /** First material granting this trait, following core/plates/trim and then embossment; empty when unknown. */
    public static String traitMaterial(ItemStack stack, String id) {
        String key = ArmoryRegistry.id(id);
        if (ArmoryRegistry.trait(key) == null) { return ""; }
        var data = read(stack); String[] parts = {"Core", "Plates", "Trim"};
        // 原版只在第一次加入词条时写入材料色；重复贡献可以改变等级，但不会覆盖首次来源的颜色。
        for (int i = 0; i < parts.length; i++) {
            var material = ArmoryRegistry.material(data.getString(parts[i]));
            if (material == null) { continue; }
            if (material.traits(i).stream().anyMatch(trait -> trait.id().equals(key))) { return material.id; }
        }
        return data.getCompound("EmbossedTraits").contains(key) ? data.getString("EmbossedMaterial") : "";
    }
    /** Current RGB color of the source material; neutral gray if old data does not retain its source. */
    public static int traitColor(ItemStack stack, String id) {
        var material = com.gctn.tconstruct.library.TinkerMaterials.getMaterial(traitMaterial(stack, id));
        return material == null ? 0xaaaaaa : material.color & 0xffffff;
    }

    public static void invoke(ItemStack stack, LivingEntity wearer, String hook, net.minecraft.world.damagesource.DamageSource source, float amount, ArmorStats stats) {
        for (var entry : traits(stack).entrySet()) {
            ArmoryRegistry.trait(entry.getKey()).invoke(hook, new ArmorContext(stack, wearer, entry.getKey(), entry.getValue(), source, amount, stats));
        }
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            int level = level(stack, modifier.id);
            if (level > 0) { modifier.invoke(hook, new ArmorContext(stack, wearer, modifier.id, level, source, amount, stats)); }
        }
    }

    public static float transform(ItemStack stack, LivingEntity wearer, String hook, net.minecraft.world.damagesource.DamageSource source, float amount) {
        for (var entry : traits(stack).entrySet()) {
            ArmorContext ctx = new ArmorContext(stack, wearer, entry.getKey(), entry.getValue(), source, amount, null);
            ArmoryRegistry.trait(entry.getKey()).invoke(hook, ctx); amount = ctx.amount;
        }
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            int level = level(stack, modifier.id);
            if (level <= 0) { continue; }
            ArmorContext ctx = new ArmorContext(stack, wearer, modifier.id, level, source, amount, null);
            modifier.invoke(hook, ctx); amount = ctx.amount;
        }
        return Float.isFinite(amount) ? Math.max(0, amount) : 0;
    }

    public static ArmorStats stats(ItemStack stack) {
        return stats(stack, true);
    }
    /** Original armor-information value, before the slot's native attribute allocation. */
    public static ArmorStats displayStats(ItemStack stack) {
        return stats(stack, false);
    }
    private static ArmorStats stats(ItemStack stack, boolean allocateDefense) {
        if (!(stack.getItem() instanceof ModularArmorItem item)) { throw new IllegalArgumentException("Expected modular armor"); }
        CompoundTag tag = read(stack);
        var core = ArmoryRegistry.material(tag.getString("Core"));
        var plates = ArmoryRegistry.material(tag.getString("Plates"));
        var trim = ArmoryRegistry.material(tag.getString("Trim"));
        if (core == null || plates == null || trim == null) { var missing = new ArmorStats(Math.max(1, stack.getMaxDamage()), 0, 0); missing.modifiers = 0; return missing; }
        int index = item.getEquipmentSlot().getIndex();
        int durability = Math.max(1, Math.round((Math.round(core.coreDurability * DURABILITY[index])
                + Math.round(trim.trimDurability * DURABILITY[index])) * plates.plateMultiplier)
                + Math.round(plates.plateDurability * DURABILITY[index]));
        ArmorStats stats = new ArmorStats(durability, core.defense, plates.toughness);
        stats.modifiers += ArmorExtensions.totalModifierSlots(stack);
        invoke(stack, null, "modifyStats", null, 0, stats);
        if (tag.contains("Polished")) {
            var polish = ArmoryRegistry.material(tag.getString("Polished"));
            if (polish != null) { stats.toughness = polish.toughness; }
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ArmorStatsEvent(stack, stats));
        // 先拒绝 NaN 等非法附属结果；Math.round(NaN) 会变成 0，不能在取整之后才检查。
        stats.validate();
        stats.defense = Math.round(stats.defense * (allocateDefense ? DEFENSE[index] : 1) * 100) / 100F;
        return stats;
    }

    public static int usedSlots(ItemStack stack) {
        int used = 0;
        for (var modifier : ArmoryRegistry.modifiers().values()) { used += modifier.slotCost(level(stack, modifier.id)); }
        return used;
    }
    public static int freeSlots(ItemStack stack) { return Math.max(0, stats(stack).modifiers - usedSlots(stack)); }

    public static void rebuild(ItemStack stack) {
        if (!initialized(stack)) { return; }
        var stored = read(stack);
        if (stored.contains("Level") || stored.contains("Experience")) { write(stack, stored); }
        int damage = stack.getDamageValue();
        ArmorStats stats = stats(stack);
        stack.set(DataComponents.MAX_DAMAGE, stats.durability);
        stack.setDamageValue(Math.min(damage, stats.durability));
        ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder();
        var item = (ModularArmorItem) stack.getItem();
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(item.getEquipmentSlot());
        if (!broken(stack)) {
            add(attributes, Attributes.ARMOR, "armor", stats.defense, slot, false);
            add(attributes, Attributes.ARMOR_TOUGHNESS, "toughness", stats.toughness, slot, false);
            add(attributes, Attributes.MOVEMENT_SPEED, "speed", stats.speed, slot, true);
            add(attributes, Attributes.KNOCKBACK_RESISTANCE, "knockback", stats.knockbackResistance, slot, false);
            add(attributes, Attributes.MAX_HEALTH, "health", stats.health, slot, false);
            add(attributes, net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, "swim", stats.swimSpeed, slot, false);
            add(attributes, Attributes.ATTACK_DAMAGE, "attack", stats.attack, slot, true);
            add(attributes, Attributes.ATTACK_SPEED, "attack_speed", stats.attackSpeed, slot, true);
            add(attributes, Attributes.ENTITY_INTERACTION_RANGE, "entity_reach", stats.reach, slot, false);
            add(attributes, Attributes.BLOCK_INTERACTION_RANGE, "block_reach", stats.reach, slot, false);
            add(attributes, Attributes.STEP_HEIGHT, "step_height", stats.stepHeight, slot, false);
        }
        var built = attributes.build();
        if (!built.equals(stack.get(DataComponents.ATTRIBUTE_MODIFIERS))) { stack.set(DataComponents.ATTRIBUTE_MODIFIERS, built); }
    }

    private static void add(ItemAttributeModifiers.Builder builder, Holder<Attribute> attribute, String name,
                            double amount, EquipmentSlotGroup slot, boolean multiply) {
        if (amount != 0) { builder.add(attribute, new AttributeModifier(ArmoryContent.id(name + "/" + slot.getSerializedName()), amount,
                multiply ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE), slot); }
    }

    public static void damage(ItemStack stack, LivingEntity wearer, net.minecraft.world.damagesource.DamageSource source, int amount) {
        if (!initialized(stack) || broken(stack) || amount <= 0) { return; }
        int value = Math.round(transform(stack, wearer, "durabilityDamage", source, amount));
        stack.setDamageValue((int) Math.min(stack.getMaxDamage(), (long) stack.getDamageValue() + value));
        rebuild(stack);
    }
    public static void repair(ItemStack stack, LivingEntity wearer, int amount) {
        if (!initialized(stack)) { return; }
        int healed = Math.round(transform(stack, wearer, "repair", null, amount));
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - healed)); rebuild(stack);
    }
}
