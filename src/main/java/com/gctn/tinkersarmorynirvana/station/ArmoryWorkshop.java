/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.*;
import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.library.materials.MaterialInputs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Pure preview planners. Only Plan.commit changes the inventory. */
public final class ArmoryWorkshop {
    private static final java.util.Map<net.minecraft.resources.ResourceLocation, java.util.function.BiFunction<Container, Level, Plan>> PLANNERS = new java.util.LinkedHashMap<>();
    private ArmoryWorkshop() {}
    /** Register during startup. A planner returns null to defer; it must not mutate inputs or the world. */
    public static void registerPlanner(String id, java.util.function.BiFunction<Container, Level, Plan> planner) {
        var key = net.minecraft.resources.ResourceLocation.parse(id);
        if (PLANNERS.putIfAbsent(key, java.util.Objects.requireNonNull(planner)) != null) { throw new IllegalArgumentException("Duplicate armor station planner: " + id); }
    }
    public record Plan(ItemStack result, int[] consumed, List<ItemStack> remainders, List<ItemStack> snapshot) {
        public Plan {
            result = result.copy(); consumed = consumed.clone();
            remainders = remainders.stream().map(ItemStack::copy).toList();
            snapshot = snapshot.stream().map(ItemStack::copy).toList();
            if (result.isEmpty() || consumed.length != 6 || snapshot.size() != 6) { throw new IllegalArgumentException("Invalid workshop plan"); }
            int total = 0;
            for (int i = 0; i < 6; i++) {
                if (consumed[i] < 0 || consumed[i] > snapshot.get(i).getCount()) { throw new IllegalArgumentException("Invalid workshop ingredient count"); }
                total += consumed[i];
            }
            if (total == 0) { throw new IllegalArgumentException("Workshop plans must consume an ingredient"); }
        }
        @Override public ItemStack result() { return result.copy(); }
        @Override public int[] consumed() { return consumed.clone(); }
        @Override public List<ItemStack> remainders() { return remainders.stream().map(ItemStack::copy).toList(); }
        @Override public List<ItemStack> snapshot() { return snapshot.stream().map(ItemStack::copy).toList(); }
        public boolean valid(Container input) {
            for (int i = 0; i < 6; i++) {
                if (!ItemStack.matches(input.getItem(i), snapshot.get(i)) || input.getItem(i).getCount() < consumed[i]) { return false; }
            }
            return true;
        }
        public List<ItemStack> commit(Container input) {
            if (!valid(input)) { throw new IllegalStateException("Workshop inputs changed before commit"); }
            var returned = new ArrayList<>(remainders());
            for (int i = 0; i < 6; i++) {
                ItemStack source = input.getItem(i).copyWithCount(1);
                for (int count = 0; count < consumed[i]; count++) {
                    ItemStack remainder = source.getCraftingRemainingItem(); if (!remainder.isEmpty()) { returned.add(remainder); }
                }
                input.removeItem(i, consumed[i]);
            }
            return returned;
        }
    }
    private static Plan plan(Container input, ItemStack output, int[] counts, List<ItemStack> remainders) {
        return new Plan(output, counts, remainders, java.util.stream.IntStream.range(0, 6).mapToObj(input::getItem).toList());
    }

    public static Plan plan(Container input, Level level) {
        return plan(input, level, true);
    }
    public static Plan plan(Container input, Level level, boolean allowModifiers) {
        for (var planner : List.copyOf(PLANNERS.values())) { Plan plan = planner.apply(input, level); if (plan != null) { return plan; } }
        if (input.getItem(0).getItem() instanceof ArmoryPatternItem) { return part(input); }
        if (!ArmorData.initialized(input.getItem(0))) { return assemble(input); }
        Plan plan = allowModifiers ? polish(input) : null;
        if (plan == null && allowModifiers) { plan = emboss(input); }
        if (plan == null) { plan = replace(input); }
        if (plan == null) { plan = repair(input); }
        if (plan == null && allowModifiers && level != null) {
            for (var recipe : level.getRecipeManager().getAllRecipesFor(ArmoryRecipes.MODIFIER_TYPE.get()).stream()
                    .sorted(java.util.Comparator.comparing(holder -> holder.id().toString())).toList()) {
                int[] counts = recipe.value().allocation(new ArmoryInput(input));
                if (counts == null) { continue; }
                ItemStack result = applyModifier(input.getItem(0), recipe.value().modifier(), recipe.value().points());
                if (!result.isEmpty()) { return plan(input, result, counts, List.of()); }
            }
        }
        return plan;
    }

    public static Plan rename(Container input, String name) {
        if (name.isBlank() || !ArmorData.initialized(input.getItem(0)) || name.equals(input.getItem(0).getHoverName().getString())) { return null; }
        for (int i = 1; i < 6; i++) { if (!input.getItem(i).isEmpty()) { return null; } }
        var result = input.getItem(0).copyWithCount(1);
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
        return plan(input, result, new int[]{1, 0, 0, 0, 0, 0}, List.of());
    }

    public static com.gctn.tconstruct.tables.recipe.StationProcessingRecipes.Plan nativePlan(Container input) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        Plan plan = plan(input, server == null ? null : server.overworld());
        if (plan == null) { return null; }
        int[] counts = plan.consumed(); var returns = new ArrayList<>(plan.remainders());
        if (counts[0] == 0) { counts[0] = 1; returns.add(input.getItem(0).copyWithCount(1)); }
        return new com.gctn.tconstruct.tables.recipe.StationProcessingRecipes.Plan(plan.result(), counts, returns);
    }

    private static Plan assemble(Container input) {
        if (!(input.getItem(0).getItem() instanceof ArmorPartItem core)) { return null; }
        String armor = switch (core.part) {
            case "helmet_core" -> "helmet";
            case "chest_core" -> "chestplate";
            case "leggings_core" -> "leggings";
            case "boots_core" -> "boots";
            default -> null;
        };
        if (armor == null || !input.getItem(1).is(ArmoryContent.PARTS.get("armor_plate").get())
                || !input.getItem(2).is(ArmoryContent.PARTS.get("armor_trim").get())) { return null; }
        for (int i = 3; i < 6; i++) { if (!input.getItem(i).isEmpty()) { return null; } }
        String[] materials = new String[3];
        for (int i = 0; i < 3; i++) {
            materials[i] = ArmorPartItem.material(input.getItem(i));
            if (ArmoryRegistry.material(materials[i]) == null) { return null; }
        }
        return plan(input, ArmorData.create(ArmoryContent.ARMORS.get(armor).get(), materials[0], materials[1], materials[2]), new int[]{1, 1, 1, 0, 0, 0}, List.of());
    }

    private static Plan part(Container input) {
        ArmorPartItem part = ArmoryContent.PARTS.get(ArmoryPatternItem.selected(input.getItem(0))).get();
        com.gctn.tconstruct.library.materials.Material material = null;
        int[] counts = new int[6]; float value = 0;
        for (int slot = 1; slot < 6; slot++) {
            ItemStack stack = input.getItem(slot); if (stack.isEmpty()) { continue; }
            var match = MaterialInputs.match(stack);
            if (match == null || !match.material().isCraftable() || match.material().isHidden() || ArmoryRegistry.material(match.material().identifier) == null
                    || material != null && match.material() != material) { return null; }
            material = match.material();
            while (counts[slot] < stack.getCount() && value + .0001F < part.cost) { value += match.value(); counts[slot]++; }
        }
        if (material == null || value + .0001F < part.cost) { return null; }
        float surplus = value - part.cost; int shards = Math.round(surplus * 2);
        if (shards > 64 || Math.abs(surplus - shards * .5F) > .001F) { return null; }
        var returned = new ArrayList<ItemStack>();
        if (shards > 0) {
            var shard = com.gctn.tconstruct.library.toolparts.AntiqueParts.get("shard").create(material);
            if (shard.isEmpty()) { return null; } shard.setCount(shards); returned.add(shard);
        }
        return plan(input, part.create(material.identifier), counts, returned);
    }

    public static ItemStack applyModifier(ItemStack source, String id, int points) {
        if (!ArmorData.initialized(source)) { return ItemStack.EMPTY; }
        var modifier = ArmoryRegistry.modifier(id); var armor = (ModularArmorItem) source.getItem();
        if (modifier == null || !modifier.allows(armor.getEquipmentSlot())) { return ItemStack.EMPTY; }
        int current = ArmorData.points(source, id);
        if (points < 1 || points > modifier.maxLevel() * modifier.pointsPerLevel() - current) { return ItemStack.EMPTY; }
        for (String other : modifier.conflicts()) {
            if (ArmorData.level(source, other) > 0 || ArmorData.traits(source).containsKey(other)) { return ItemStack.EMPTY; }
        }
        for (var other : ArmoryRegistry.modifiers().values()) {
            if (modifier.accessory() && other.accessory() && !modifier.id.equals(other.id) && ArmorData.level(source, other.id) > 0) { return ItemStack.EMPTY; }
            if (ArmorData.level(source, other.id) > 0 && other.conflicts().contains(modifier.id)) { return ItemStack.EMPTY; }
        }
        ItemStack result = source.copyWithCount(1); CompoundTag tag = ArmorData.read(result), applied = tag.getCompound("Modifiers");
        applied.putInt(modifier.id, current + points); tag.put("Modifiers", applied);
        if (current == 0 && List.of("conarm:night_vision", "conarm:soul_sight", "conarm:travel_goggles", "conarm:travel_sneak", "conarm:travel_slowfall").contains(modifier.id)) {
            CompoundTag disabled = tag.getCompound("Disabled"); disabled.putBoolean(modifier.id, true); tag.put("Disabled", disabled);
        }
        ArmorData.write(result, tag);
        if (ArmorData.usedSlots(result) > ArmorData.stats(result).modifiers) { return ItemStack.EMPTY; }
        ArmorData.rebuild(result); return result;
    }

    private static Plan replace(Container input) {
        ItemStack result = input.getItem(0).copyWithCount(1); CompoundTag tag = ArmorData.read(result);
        var armor = (ModularArmorItem) result.getItem();
        String core = switch (armor.getEquipmentSlot()) { case HEAD -> "helmet_core"; case CHEST -> "chest_core"; case LEGS -> "leggings_core"; default -> "boots_core"; };
        int[] counts = new int[6]; counts[0] = 1; var used = new java.util.HashSet<String>();
        for (int i = 1; i < 6; i++) {
            if (input.getItem(i).isEmpty()) { continue; }
            if (!(input.getItem(i).getItem() instanceof ArmorPartItem part)) { return null; }
            String key = part.part.equals(core) ? "Core" : part.part.equals("armor_plate") ? "Plates" : part.part.equals("armor_trim") ? "Trim" : null;
            String material = ArmorPartItem.material(input.getItem(i));
            if (key == null || !used.add(key) || material.equals(tag.getString(key)) || ArmoryRegistry.material(material) == null) { return null; }
            tag.putString(key, material); counts[i] = 1;
        }
        if (used.isEmpty()) { return null; }
        ArmorData.write(result, tag);
        if (ArmorData.usedSlots(result) > ArmorData.stats(result).modifiers || result.getDamageValue() >= ArmorData.stats(result).durability) { return null; }
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            if (ArmorData.level(result, modifier.id) <= 0) { continue; }
            for (String conflict : modifier.conflicts()) { if (ArmorData.traits(result).containsKey(conflict)) { return null; } }
        }
        ArmorData.rebuild(result); return plan(input, result, counts, List.of());
    }

    public static Plan repair(Container input) {
        ItemStack source = input.getItem(0);
        if (!ArmorData.initialized(source) || source.getDamageValue() <= 0) { return null; }
        String materialId = ArmorData.read(source).getString("Core"); var material = ArmoryRegistry.material(materialId);
        if (material == null) { return null; }
        int[] counts = new int[6]; counts[0] = 1; float value = 0;
        int multiplier = new int[]{13, 15, 16, 11}[((ModularArmorItem) source.getItem()).getEquipmentSlot().getIndex()];
        for (int i = 1; i < 6; i++) {
            ItemStack ingredient = input.getItem(i); if (ingredient.isEmpty()) { continue; }
            float worth;
            if (ingredient.getItem() instanceof ArmorPartItem part && part.part.equals("polishing_kit")) {
                if (!ArmorPartItem.material(ingredient).equals(materialId)) { return null; } worth = part.cost;
            } else {
                var match = MaterialInputs.match(ingredient); if (match == null || !match.material().identifier.equals(materialId)) { return null; } worth = match.value();
            }
            // 只消耗实际用于修复的数量，多余同材质物品保留在原槽。
            while (counts[i] < ingredient.getCount() && repairAmount(source, material, multiplier, value) < source.getDamageValue()) { value += worth; counts[i]++; }
        }
        if (value <= 0) { return null; }
        ItemStack result = source.copyWithCount(1); CompoundTag data = ArmorData.read(result);
        int amount = repairAmount(source, material, multiplier, value);
        if (amount <= 0) { return null; }
        data.putInt("RepairCount", data.getInt("RepairCount") + 1); ArmorData.write(result, data);
        result.setDamageValue(Math.max(0, result.getDamageValue() - amount)); ArmorData.rebuild(result);
        return plan(input, result, counts, List.of());
    }
    private static int repairAmount(ItemStack source, ArmorMaterialDefinition material, int multiplier, float value) {
        float diminishing = Math.max(.5F, (100 - ArmorData.read(source).getInt("RepairCount") / 2) / 100F);
        ArmorStats stats = ArmorData.stats(source);
        int amount = (int) Math.ceil(value * material.coreDurability * multiplier * Math.min(10, (float) stats.durability / stats.baseDurability) * diminishing);
        return Math.round(ArmorData.transform(source, null, "repair", null, amount));
    }

    private static Plan polish(Container input) {
        if (!(input.getItem(1).getItem() instanceof ArmorPartItem part) || !part.part.equals("polishing_kit") || !input.getItem(2).is(net.minecraft.tags.ItemTags.SAND)) { return null; }
        for (int i = 3; i < 6; i++) { if (!input.getItem(i).isEmpty()) { return null; } }
        String material = ArmorPartItem.material(input.getItem(1)); if (ArmoryRegistry.material(material) == null) { return null; }
        ItemStack result = input.getItem(0).copyWithCount(1); CompoundTag tag = ArmorData.read(result);
        if (material.equals(tag.getString("Polished"))) { return null; }
        tag.putString("Polished", material); ArmorData.write(result, tag); ArmorData.rebuild(result);
        return plan(input, result, new int[]{1, 1, 1, 0, 0, 0}, List.of());
    }

    private static Plan emboss(Container input) {
        if (!(input.getItem(1).getItem() instanceof ArmorPartItem part) || input.getItem(2).isEmpty()) { return null; }
        String[] extras = {"tconstruct:slimecrystal_green", "tconstruct:slimecrystal_blue", "tconstruct:slimecrystal_magma", "minecraft:gold_block"};
        for (int i = 0; i < 4; i++) { if (!input.getItem(i + 2).is(BuiltinItems.get(extras[i]))) { return null; } }
        int index = part.part.equals("armor_plate") ? 1 : part.part.equals("armor_trim") ? 2 : part.part.endsWith("_core") ? 0 : -1;
        if (index == 0) {
            String required = switch (((ModularArmorItem) input.getItem(0).getItem()).getEquipmentSlot()) {
                case HEAD -> "helmet_core"; case CHEST -> "chest_core"; case LEGS -> "leggings_core"; default -> "boots_core";
            };
            if (!part.part.equals(required)) { return null; }
        }
        var material = ArmoryRegistry.material(ArmorPartItem.material(input.getItem(1)));
        if (index < 0 || material == null || material.traits(index).isEmpty()) { return null; }
        ItemStack result = input.getItem(0).copyWithCount(1); CompoundTag tag = ArmorData.read(result);
        if (tag.contains("EmbossedTraits")) { return null; }
        CompoundTag traits = new CompoundTag();
        for (var trait : material.traits(index)) { traits.putInt(trait.id(), trait.level()); }
        tag.put("EmbossedTraits", traits); tag.putString("EmbossedMaterial", material.id); ArmorData.write(result, tag);
        if (ArmorData.usedSlots(result) > ArmorData.stats(result).modifiers) { return null; }
        ArmorData.rebuild(result); return plan(input, result, new int[]{1, 1, 1, 1, 1, 1}, List.of());
    }

    private static final class BuiltinItems {
        static net.minecraft.world.item.Item get(String id) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)); }
    }
}
