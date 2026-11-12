/* Behaviors adapted from Construct's Armory, Copyright (c) 2018-2020 C4.
 * SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.api.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BuiltinModifiers {
    private static boolean initialized;
    private BuiltinModifiers() {}
    private static ArmorModifier modifier(String id) {
        ArmorModifier definition = new ArmorModifier("conarm:" + id);
        ArmoryRegistry.registerModifier(definition); return definition;
    }
    public static void initialize() {
        if (initialized) { return; } initialized = true;
        modifier("diamond").on("modifyStats", ctx -> { ctx.stats.durability += 150; ctx.stats.defense += 4; ctx.stats.toughness += 2; });
        modifier("emerald").on("modifyStats", ctx -> { ctx.stats.durability += ctx.stats.baseDurability / 2; ctx.stats.toughness = Math.max(2, ctx.stats.toughness); });
        modifier("speedy").maxLevel(3).pointsPerLevel(50).on("modifyStats", ctx -> ctx.stats.speed += .05F * ArmorData.points(ctx.armor, ctx.id) / 50F);
        modifier("reinforced").maxLevel(5).on("durabilityDamage", ctx -> { if (ctx.wearer.getRandom().nextFloat() < .2F * ctx.level) { ctx.amount = 0; } });
        modifier("soulbound").slotsPerLevel(0);
        modifier("concealed").slotsPerLevel(0).conflicts("travel_sneak");
        modifier("creative").slotsPerLevel(0).maxLevel(1024).on("modifyStats", ctx -> ctx.stats.modifiers += ctx.level);
        modifier("extramodifier").slotsPerLevel(0).on("modifyStats", ctx -> ctx.stats.modifiers += ctx.level);
        modifier("incognito").slotsPerLevel(0);
        modifier("powerful").equipment("chest").conflicts("dexterous", "telekinetic").on("modifyStats", ctx -> ctx.stats.attack += .15F);
        modifier("dexterous").equipment("chest").conflicts("powerful", "telekinetic").on("modifyStats", ctx -> ctx.stats.attackSpeed += .15F);
        modifier("telekinetic").equipment("chest").conflicts("powerful", "dexterous").on("modifyStats", ctx -> ctx.stats.reach += 2);
        modifier("sticky").on("incomingDamage", ctx -> {
            if (ctx.source.getDirectEntity() instanceof LivingEntity target) { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)); }
        });
        modifier("shulkerweight").pointsPerLevel(20);
        modifier("high_stride").equipment("feet").maxLevel(2);
        modifier("parasitic").on("armorTick", ctx -> {
            if (ctx.wearer instanceof Player player && ctx.armor.getDamageValue() > 0 && player.getFoodData().getFoodLevel() > 1) {
                player.causeFoodExhaustion(.08F);
                if (player.tickCount % 100 == 0) { ArmorData.repair(ctx.armor, player, 1); }
            }
        });
        modifier("mending").maxLevel(3).on("inventoryTick", ctx -> {
            if (ArmorData.broken(ctx.armor) || ctx.armor.getDamageValue() <= 0 || ctx.wearer.level().getGameTime() - ctx.state.getLong("LastHeal") < 600) { return; }
            int xp = ctx.state.getInt("Experience");
            if (xp > 0) {
                ctx.state.putInt("Experience", xp - 1); ctx.state.putLong("LastHeal", ctx.wearer.level().getGameTime()); ctx.save();
                ArmorData.repair(ctx.armor, ctx.wearer, 2 + ctx.level);
            }
        });
        String[] protections = {"resistant", "fire_resistant", "blast_resistant", "projectile_resistant"};
        for (String id : protections) {
            modifier(id).maxLevel(8).slotsOnce(true).conflicts(java.util.Arrays.stream(protections).filter(name -> !name.equals(id)).toArray(String[]::new))
                    .on("incomingDamage", ctx -> {
                        boolean matches = switch (id) {
                            case "fire_resistant" -> ctx.source.is(DamageTypeTags.IS_FIRE);
                            case "blast_resistant" -> ctx.source.is(DamageTypeTags.IS_EXPLOSION);
                            case "projectile_resistant" -> ctx.source.is(DamageTypeTags.IS_PROJECTILE);
                            default -> true;
                        };
                        if (matches && !ctx.source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { ctx.amount *= Math.max(0, 1 - ctx.level * (id.equals("resistant") ? .02F : .04F)); }
                    });
        }
        modifier("amphibious").equipment("head").on("armorTick", ctx -> {
            int oxygen = ctx.state.getInt("Oxygen");
            if (ctx.wearer.isInWater()) {
                if (oxygen > 0 && ctx.wearer.getAirSupply() < ctx.wearer.getMaxAirSupply()) {
                    oxygen--; ctx.wearer.setAirSupply(ctx.wearer.getAirSupply() + 1);
                }
            } else { oxygen = Math.min(1200, oxygen + 2); }
            if (oxygen != ctx.state.getInt("Oxygen")) { ctx.state.putInt("Oxygen", oxygen); ctx.save(); }
        });
        modifier("frostwalker").equipment("feet").maxLevel(2).conflicts("autoforge")
                .on("armorTick", FrostWalking::tick).on("incomingDamage", ctx -> {
                    if (ctx.source.is(DamageTypes.HOT_FLOOR)) { ctx.amount = 0; ArmorData.damage(ctx.armor, ctx.wearer, ctx.source, 3); }
                });
        modifier("glowing").equipment("feet").on("armorTick", ctx -> {
            if (ctx.wearer.tickCount % 10 != 0 || ctx.wearer.level().getMaxLocalRawBrightness(ctx.wearer.blockPosition()) >= 8) { return; }
            var level = ctx.wearer.level(); var pos = ctx.wearer.blockPosition();
            for (var candidate : java.util.List.of(pos, pos.above(), pos.north(), pos.east(), pos.south(), pos.west(), pos.below())) {
                var glow = com.gctn.tconstruct.gadgets.GadgetModule.GLOW.get();
                if (level.isEmptyBlock(candidate) && glow.defaultBlockState().canSurvive(level, candidate)) {
                    level.setBlockAndUpdate(candidate, glow.defaultBlockState()); ArmorData.damage(ctx.armor, ctx.wearer, ctx.wearer.damageSources().generic(), 1); break;
                }
            }
        });
        modifier("travel_belt").equipment("legs");
        modifier("potion_belt").equipment("legs");
        modifier("travel_sack").equipment("chest");
        modifier("travel_goggles").equipment("head").conflicts("night_vision", "soul_sight");
        modifier("night_vision").equipment("head").conflicts("travel_goggles", "soul_sight").on("armorTick", ctx -> {
            if (ArmorData.enabled(ctx.armor, ctx.id)) {
                ArmorEffects.apply(ctx.wearer, MobEffects.NIGHT_VISION, 220); drain(ctx);
            }
        });
        modifier("soul_sight").equipment("head").conflicts("travel_goggles", "night_vision").on("armorTick", ctx -> { if (ArmorData.enabled(ctx.armor, ctx.id)) { drain(ctx); } });
        modifier("travel_sneak").equipment("chest").conflicts("travel_slowfall", "concealed").on("armorTick", ctx -> {
            if (ArmorData.enabled(ctx.armor, ctx.id)) {
                ArmorEffects.apply(ctx.wearer, MobEffects.INVISIBILITY, 25); drain(ctx);
            }
        });
        modifier("travel_slowfall").equipment("chest").conflicts("travel_sneak").on("armorTick", ctx -> {
            if (ArmorData.enabled(ctx.armor, ctx.id)) {
                var motion = ctx.wearer.getDeltaMovement();
                if (motion.y < 0) { ctx.wearer.setDeltaMovement(motion.x, motion.y / 1.5, motion.z); ctx.wearer.fallDistance = 0; ctx.wearer.hurtMarked = true; }
                drain(ctx);
            }
        });
        for (String id : java.util.List.of("travel_belt", "potion_belt", "travel_sack", "travel_goggles", "night_vision", "soul_sight", "travel_sneak", "travel_slowfall")) {
            ArmoryRegistry.modifier(id).accessory(true);
        }
    }
    private static void drain(ArmorContext ctx) {
        if (ctx.wearer.tickCount % 100 == 0) { ArmorData.damage(ctx.armor, ctx.wearer, ctx.wearer.damageSources().generic(), 1); }
    }
}
