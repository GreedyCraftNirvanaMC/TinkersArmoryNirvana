/* Behaviors adapted from Construct's Armory, Copyright (c) 2018-2020 C4.
 * SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** Builtins use the same callbacks exposed to addons. Persistent counters live on each armor item. */
public final class BuiltinTraits {
    private static boolean initialized;
    private BuiltinTraits() {}
    private static ArmorTrait trait(String name) {
        ArmorTrait trait = new ArmorTrait("conarm:" + name); ArmoryRegistry.registerTrait(trait); return trait;
    }
    private static boolean chance(ArmorContext ctx, float chance) { return ctx.wearer.getRandom().nextFloat() < chance; }
    private static LivingEntity attacker(ArmorContext ctx) {
        return ctx.source != null && ctx.source.getDirectEntity() instanceof LivingEntity living ? living : null;
    }
    private static void thorns(ArmorContext ctx, LivingEntity target, float damage) {
        if (target != null && damage > 0) { target.hurt(ctx.wearer.damageSources().thorns(ctx.wearer), damage); }
    }
    private static void repair(ArmorContext ctx, int amount) { ArmorData.repair(ctx.armor, ctx.wearer, amount); }

    public static void initialize() {
        if (initialized) { return; } initialized = true;
        trait("writable").maxLevel(3).stacking("unique").on("modifyStats", ctx -> ctx.stats.modifiers += ctx.level);
        trait("cheapskate").on("modifyStats", ctx -> ctx.stats.durability = Math.max(1, ctx.stats.durability * 80 / 100));
        trait("skeletal").on("modifyStats", ctx -> ctx.stats.toughness += ctx.stats.baseToughness * .5F);
        trait("indomitable").on("modifyStats", ctx -> {
            ctx.stats.defense += ctx.stats.baseDefense * .2F; ctx.stats.toughness += ctx.stats.baseToughness * .2F;
        });
        trait("lightweight").on("modifyStats", ctx -> ctx.stats.speed += .05F);
        trait("heavy").on("modifyStats", ctx -> ctx.stats.knockbackResistance += .25F);
        trait("steady").on("modifyStats", ctx -> ctx.stats.knockbackResistance += .1F).on("incomingDamage", ctx -> {
            var target = attacker(ctx);
            if (target != null) { target.knockback(.2, Math.sin(ctx.wearer.getYRot() * Math.PI / 180), -Math.cos(ctx.wearer.getYRot() * Math.PI / 180)); }
        });
        trait("invigorating").on("modifyStats", ctx -> ctx.stats.health += 4);
        trait("aquaspeed").on("modifyStats", ctx -> ctx.stats.swimSpeed += .675F).on("armorTick", ctx -> {
            if (ctx.wearer.isInWater() && ctx.wearer.isShiftKeyDown()) { ctx.wearer.setDeltaMovement(ctx.wearer.getDeltaMovement().multiply(1, 1.1, 1)); }
        });
        trait("cheap").on("repair", ctx -> ctx.amount *= 1.05F);
        trait("ecological").on("inventoryTick", ctx -> { if (ctx.wearer.getRandom().nextInt(2400) == 0) { repair(ctx, 1); } });
        trait("duritos_ranch").on("durabilityDamage", ctx -> {
            float roll = ctx.wearer.getRandom().nextFloat(); if (roll < .1F) { ctx.amount *= 2; } else if (roll < .5F) { ctx.amount = 0; }
        });
        trait("dense").on("durabilityDamage", ctx -> {
            float lost = (float) ctx.armor.getDamageValue() / ctx.armor.getMaxDamage();
            if (chance(ctx, (float) Math.pow(.75F * lost, 3))) { ctx.amount -= Math.max(1, ctx.amount / 2); }
        });
        trait("subterranean").on("durabilityDamage", ctx -> {
            double depth = ctx.wearer.level().getSeaLevel() - ctx.wearer.getY();
            if (depth > 0 && chance(ctx, (float) depth / 100)) { ctx.amount = 0; }
        });
        trait("autoforge").on("armorTick", ctx -> {
            if (ctx.wearer.tickCount % 20 == 0) {
                if (ctx.wearer.isInLava()) { repair(ctx, 3); }
                else if (ctx.wearer.isOnFire()) { repair(ctx, 1); }
            }
        }).on("durabilityDamage", ctx -> { if (ctx.source != null && ctx.source.is(DamageTypeTags.IS_FIRE)) { ctx.amount = 0; } });
        trait("superhot").on("armorTick", ctx -> {
            if (ctx.wearer.isOnFire()) {
                GameplayEvents.attributeMultiplier(ctx.wearer, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                        "superhot", .15F * GameplayEvents.ability(ctx.wearer, "superhot"));
            }
        });
        trait("tasty").on("armorTick", ctx -> {
            if (!(ctx.wearer instanceof Player player) || !player.getFoodData().needsFood()) { return; }
            float chance = player.getHealth() < player.getMaxHealth() ? .01F : .002F;
            if (player.getFoodData().getFoodLevel() < 10) { chance += (10 - player.getFoodData().getFoodLevel()) * .0025F - player.getFoodData().getSaturationLevel() * .005F; }
            if (chance(ctx, chance) && ctx.armor.getMaxDamage() - ctx.armor.getDamageValue() >= 5) {
                player.getFoodData().eat(1, 0); ArmorData.damage(ctx.armor, player, player.damageSources().starve(), 5);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EAT, player.getSoundSource(), .8F, 1);
            }
        });
        trait("magnetic").maxLevel(3).stacking("unique");
        trait("ambitious").on("experience", ctx -> {
            if (ctx.amount > 0) { ctx.amount += ctx.wearer.getRandom().nextInt(3) + 2; }
        });
        trait("calcic").on("milk", ctx -> { repair(ctx, 10); ctx.wearer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0)); });
        trait("featherweight").on("incomingDamage", ctx -> { if (ctx.source.is(DamageTypes.FALL)) { ctx.amount *= .9F; } });
        trait("shielding").on("incomingDamage", ctx -> {
            if (ctx.source.is(DamageTypes.MAGIC) || ctx.source.is(DamageTypes.INDIRECT_MAGIC) || ctx.source.is(DamageTypes.WITHER)) { ctx.amount *= .9F; }
        });
        trait("blessed").on("incomingDamage", ctx -> {
            var target = attacker(ctx);
            if (target != null && target.getType().is(EntityTypeTags.UNDEAD)) { target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)); }
        });
        trait("spiny").on("afterDamage", ctx -> {
            var target = attacker(ctx);
            if (target != null && chance(ctx, .3F)) {
                thorns(ctx, target, 1 + ctx.wearer.getRandom().nextInt(4));
                ArmorData.damage(ctx.armor, ctx.wearer, ctx.source, 3);
            }
        });
        trait("rough").on("incomingDamage", ctx -> {
            double lost = (double) ctx.armor.getDamageValue() / ctx.armor.getMaxDamage();
            thorns(ctx, attacker(ctx), (float) Math.log(lost * 14.875 + 1) * 2);
        });
        trait("vengeful").on("incomingDamage", ctx -> {
            if (ctx.source.getEntity() instanceof LivingEntity target && chance(ctx, .15F)) {
                var effects = List.of(MobEffects.POISON, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WITHER, MobEffects.WEAKNESS);
                target.addEffect(new MobEffectInstance(effects.get(ctx.wearer.getRandom().nextInt(4)), 100, 0));
                ArmorData.damage(ctx.armor, ctx.wearer, ctx.source, 3);
            }
        });
        trait("baconlicious").on("afterDamage", ctx -> {
            if (chance(ctx, .025F)) { ctx.wearer.spawnAtLocation(item("tconstruct:bacon")); }
        });
        for (String color : List.of("green", "blue")) {
            trait("slimey_" + color).on("afterDamage", ctx -> {
                if (!chance(ctx, .01F)) { return; }
                EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                        net.minecraft.resources.ResourceLocation.parse(color.equals("blue") ? "tconstruct:blueslime" : "minecraft:slime"));
                if (type.create(ctx.wearer.level()) instanceof Slime slime) {
                    slime.setSize(1, true); slime.moveTo(ctx.wearer.getX() + .5, ctx.wearer.getY(), ctx.wearer.getZ() + .5);
                    ctx.wearer.level().addFreshEntity(slime);
                }
            });
        }
        trait("bouncy").on("knockback", ctx -> ctx.amount *= 1.5F).on("fall", ctx -> {
            if (((ModularArmorItem) ctx.armor.getItem()).getEquipmentSlot() != EquipmentSlot.FEET) { return; }
            if (ctx.wearer.isShiftKeyDown()) { ctx.amount *= .2F; return; }
            if (ctx.amount > 2) {
                double speed = Math.min(3, Math.sqrt(ctx.amount * .08)) * .9;
                ctx.wearer.setDeltaMovement(ctx.wearer.getDeltaMovement().x, speed, ctx.wearer.getDeltaMovement().z);
                ctx.wearer.hurtMarked = true; ctx.wearer.fallDistance = 0; ctx.amount = 0;
                if (com.gctn.tinkersarmorynirvana.ArmoryConfig.SERVER.isLoaded() && com.gctn.tinkersarmorynirvana.ArmoryConfig.BOUNCE_DURABILITY.get()) {
                    ArmorData.damage(ctx.armor, ctx.wearer, ctx.wearer.damageSources().fall(), 1);
                }
                ctx.wearer.level().playSound(null, ctx.wearer.blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_SQUISH, ctx.wearer.getSoundSource(), 1, 1);
            }
        });
        trait("dramatic").on("beforeDamage", ctx -> {
            if (ctx.wearer.getHealth() <= ctx.amount - ctx.wearer.getAbsorptionAmount() && chance(ctx, .1F)) { ctx.wearer.heal(2); ctx.amount = 0; }
        });
        trait("enderport").on("incomingDamage", ctx -> {
            if (!ctx.source.is(DamageTypes.LAVA) && !ctx.source.is(DamageTypes.DROWN) && !ctx.source.is(DamageTypes.IN_WALL)) { return; }
            if (!chance(ctx, .25F)) { return; }
            for (int i = 0; i < 64; i++) {
                var random = ctx.wearer.getRandom();
                if (ctx.wearer.randomTeleport(ctx.wearer.getX() + (random.nextDouble() - .5) * 64,
                        ctx.wearer.getY() + random.nextInt(128) - 64, ctx.wearer.getZ() + (random.nextDouble() - .5) * 64, true)) { break; }
            }
        });
        trait("combustible").on("incomingDamage", ctx -> {
            if (!ctx.source.is(DamageTypeTags.IS_FIRE) && !ctx.source.is(DamageTypeTags.IS_EXPLOSION)) { return; }
            for (var target : ctx.wearer.level().getEntitiesOfClass(LivingEntity.class, ctx.wearer.getBoundingBox().inflate(1.5), entity -> entity != ctx.wearer)) {
                if (!target.fireImmune()) { thorns(ctx, target, 1); target.igniteForSeconds(2); }
            }
        });
        trait("voltaic").on("incomingDamage", ctx -> {
            float charge = ctx.state.getFloat("Charge");
            if (charge >= 25) {
                ctx.state.putFloat("Charge", 0);
                for (var target : ctx.wearer.level().getEntitiesOfClass(LivingEntity.class, ctx.wearer.getBoundingBox().inflate(2), entity -> entity != ctx.wearer)) { thorns(ctx, target, 2); }
                ctx.wearer.level().playSound(null, ctx.wearer.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, ctx.wearer.getSoundSource(), .6F, 1.2F);
            } else {
                ctx.state.putFloat("Charge", Math.min(25, charge + (ctx.source.is(DamageTypes.LIGHTNING_BOLT) ? 25 : ctx.amount * .25F)));
            }
            if (ctx.source.is(DamageTypes.LIGHTNING_BOLT)) { ctx.amount *= .75F; }
            ctx.save();
        });
        trait("alien").on("inventoryTick", ctx -> {
            if (ctx.wearer.tickCount % 72 != 0) { return; }
            if (!ctx.state.contains("PoolDurability")) {
                int[] pool = new int[3]; for (int i = 0; i < 800; i++) { pool[ctx.wearer.getRandom().nextInt(3)]++; }
                ctx.state.putInt("PoolDurability", pool[0]); ctx.state.putInt("PoolToughness", pool[1]); ctx.state.putInt("PoolDefense", pool[2]);
            }
            String key = ctx.wearer.tickCount % 216 == 0 ? "Defense" : ctx.wearer.tickCount % 144 == 0 ? "Toughness" : "Durability";
            if (ctx.state.getInt(key) < ctx.state.getInt("Pool" + key)) { ctx.state.putInt(key, ctx.state.getInt(key) + 1); }
            ctx.save(); ArmorData.rebuild(ctx.armor);
        }).on("modifyStats", ctx -> {
            ctx.stats.durability += ctx.state.getInt("Durability"); ctx.stats.toughness += ctx.state.getInt("Toughness") * .01F; ctx.stats.defense += ctx.state.getInt("Defense") * .02F;
        });
        trait("absorbent").on("armorTick", ctx -> conditionalArmor(ctx, ctx.wearer.isInWaterOrRain()));
        trait("depth_explorer").on("armorTick", ctx -> conditionalArmor(ctx, ctx.wearer.getY() < 40));
        trait("desperation").on("armorTick", ctx -> conditionalArmor(ctx, ctx.wearer.getHealth() <= ctx.wearer.getMaxHealth() / 2));
        trait("mundane").maxLevel(3).stacking("unique").on("armorEffectiveness", ctx -> {
            var target = attacker(ctx); if (target != null && target.getMainHandItem().isEmpty()) { effectiveness(ctx, .2F * ctx.level); }
        });
        trait("infernal").on("armorEffectiveness", ctx -> {
            var target = ctx.source.getEntity();
            boolean netherMob = target != null && ctx.wearer.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .getTag(net.minecraft.tags.BiomeTags.IS_NETHER).stream().flatMap(holders -> holders.stream())
                    .anyMatch(biome -> biome.value().getMobSettings().getMobs(target.getType().getCategory()).unwrap().stream().anyMatch(entry -> entry.type == target.getType()));
            if (!netherMob) { effectiveness(ctx, .2F); }
        });
        trait("aridiculous").on("armorEffectiveness", ctx -> {
            var biome = ctx.wearer.level().getBiome(ctx.wearer.blockPosition()).value();
            float rain = biome.getModifiedClimateSettings().downfall();
            float dryness = (float) (Math.pow(1.25, 3 * (.5F + biome.getBaseTemperature() - rain)) - 1.25) - (ctx.wearer.level().isRaining() ? rain / 2 : 0);
            effectiveness(ctx, dryness / 10);
        });
        trait("petravidity").on("armorEffectiveness", ctx -> {
            BlockPos pos = ctx.wearer.blockPosition();
            for (BlockPos point : List.of(pos, pos.above(), pos.below(), pos.north(), pos.east(), pos.south(), pos.west())) {
                if (ctx.wearer.level().getBlockState(point).is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                    effectiveness(ctx, .2F); if (chance(ctx, .1F)) { repair(ctx, 3); } break;
                }
            }
        });
        trait("prideful").on("armorEffectiveness", ctx -> {
            if (ctx.wearer.getLastDamageSource() != null) { effectiveness(ctx, .1F); ArmorData.damage(ctx.armor, ctx.wearer, ctx.source, 3); }
        });
    }

    private static void effectiveness(ArmorContext ctx, float bonus) { ctx.amount += bonus; }
    private static void conditionalArmor(ArmorContext ctx, boolean active) {
        // 用瞬态原生属性表示环境加成；每个词条、装备槽使用稳定且独立的 ID，脱装时统一清理。
        var slot = ((ModularArmorItem) ctx.armor.getItem()).getEquipmentSlot();
        String prefix = ctx.id.substring(ctx.id.indexOf(':') + 1) + "/" + slot.getName();
        GameplayEvents.attribute(ctx.wearer, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, prefix + "/armor", active ? new int[]{2, 3, 4, 2}[slot.getIndex()] : 0);
        GameplayEvents.attribute(ctx.wearer, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, prefix + "/toughness", active ? 2 : 0);
    }
    static ItemStack item(String id) {
        var key = net.minecraft.resources.ResourceLocation.parse(id);
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key));
    }
}
