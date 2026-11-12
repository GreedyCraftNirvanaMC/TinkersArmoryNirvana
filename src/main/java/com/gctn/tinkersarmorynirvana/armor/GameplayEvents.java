/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid = "conarm")
public final class GameplayEvents {
    private static final Map<LivingEntity, Map<Holder<Attribute>, Set<ResourceLocation>>> ACTIVE = new WeakHashMap<>();
    private static final ThreadLocal<Map<Holder<Attribute>, Set<ResourceLocation>>> DESIRED = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SECONDARY_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final Map<LivingEntity, net.minecraft.world.damagesource.DamageSource> CURRENT_DAMAGE = new WeakHashMap<>();
    private GameplayEvents() {}
    public static net.minecraft.world.damagesource.DamageSource currentDamage(LivingEntity entity) { return CURRENT_DAMAGE.getOrDefault(entity, entity.getLastDamageSource()); }
    public static List<ItemStack> armor(LivingEntity entity) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : entity.getArmorSlots()) { if (ArmorData.initialized(stack) && !ArmorData.broken(stack)) { stacks.add(stack); } }
        return stacks;
    }
    public static int ability(LivingEntity entity, String id) {
        int result = 0; String key = com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.id(id);
        for (var stack : armor(entity)) { result += ArmorData.traits(stack).getOrDefault(key, 0) + ArmorData.level(stack, key); }
        return result;
    }
    public static void attribute(LivingEntity entity, Holder<Attribute> attribute, String name, double amount) {
        attribute(entity, attribute, name, amount, AttributeModifier.Operation.ADD_VALUE);
    }
    public static void attributeMultiplier(LivingEntity entity, Holder<Attribute> attribute, String name, double amount) {
        attribute(entity, attribute, name, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    private static void attribute(LivingEntity entity, Holder<Attribute> attribute, String name, double amount, AttributeModifier.Operation operation) {
        var instance = entity.getAttribute(attribute); if (instance == null) { return; }
        ResourceLocation id = ArmoryContent.id("conditional/" + name);
        var desired = DESIRED.get();
        if (desired != null && amount != 0) { desired.computeIfAbsent(attribute, ignored -> new HashSet<>()).add(id); }
        var old = instance.getModifier(id);
        if (old != null && old.amount() == amount && old.operation() == operation) { return; }
        instance.removeModifier(id);
        if (amount != 0) { instance.addTransientModifier(new AttributeModifier(id, amount, operation)); }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity(); if (player.level().isClientSide) { return; }
        Map<Holder<Attribute>, Set<ResourceLocation>> desired = new HashMap<>(); DESIRED.set(desired); ArmorEffects.begin();
        try {
            for (var stack : armor(player)) {
                ArmorData.invoke(stack, player, "armorTick", null, 0, null);
                int stride = player.isShiftKeyDown() ? 0 : ArmorData.level(stack, "high_stride");
                if (stride > 0) { attribute(player, net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT, "high_stride", stride); }
            }
        } finally { DESIRED.remove(); ArmorEffects.end(player); }
        var previous = ACTIVE.put(player, desired);
        if (previous != null) {
            previous.forEach((attribute, ids) -> {
                var instance = player.getAttribute(attribute); if (instance == null) { return; }
                for (var id : ids) { if (!desired.getOrDefault(attribute, Set.of()).contains(id)) { instance.removeModifier(id); } }
            });
        }
        if (player.getHealth() > player.getMaxHealth()) { player.setHealth(player.getMaxHealth()); }
        if (player.tickCount % 2 == 0 && !player.isShiftKeyDown()) {
            int magnetic = ability(player, "magnetic");
            if (magnetic > 0) {
                double range = Math.min(32, 1.8 + (magnetic - 1) * 3);
                var items = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(range),
                        item -> item.isAlive() && !item.hasPickUpDelay());
                for (int i = 0; i < Math.min(200, items.size()); i++) {
                    var item = items.get(i); var vector = player.position().subtract(item.position());
                    if (vector.lengthSqr() > .05) { item.setDeltaMovement(item.getDeltaMovement().add(vector.normalize().scale(.07))); }
                }
            }
        }
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) { return; }
        CURRENT_DAMAGE.put(event.getEntity(), event.getSource());
        if (SECONDARY_DAMAGE.get()) { return; }
        SECONDARY_DAMAGE.set(true);
        try {
            float amount = event.getAmount();
            for (var stack : armor(event.getEntity())) { amount = ArmorData.transform(stack, event.getEntity(), "incomingDamage", event.getSource(), amount); }
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                for (var stack : armor(attacker)) { amount = ArmorData.transform(stack, attacker, "outgoingDamage", event.getSource(), amount); }
            }
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, (container, reduction) -> {
                double total = event.getEntity().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                if (total <= 0) { return reduction; }
                float bonus = 0;
                for (var stack : armor(event.getEntity())) {
                    float factor = ArmorData.transform(stack, event.getEntity(), "armorEffectiveness", event.getSource(), 1);
                    bonus += (factor - 1) * ArmorData.stats(stack).defense / total;
                }
                return Math.max(0, Math.min(container.getNewDamage() * .8F, reduction * (1 + bonus)));
            });
            event.setAmount(amount); if (amount <= 0) { event.setCanceled(true); }
        } finally { SECONDARY_DAMAGE.remove(); }
    }
    @SubscribeEvent public static void beforeDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || SECONDARY_DAMAGE.get()) { return; }
        float amount = event.getNewDamage();
        for (var stack : armor(event.getEntity())) { amount = ArmorData.transform(stack, event.getEntity(), "beforeDamage", event.getSource(), amount); }
        event.setNewDamage(amount);
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide || event.getNewDamage() <= 0 || SECONDARY_DAMAGE.get()) { return; }
        SECONDARY_DAMAGE.set(true);
        try {
            for (var stack : armor(event.getEntity())) {
                ArmorData.invoke(stack, event.getEntity(), "afterDamage", event.getSource(), event.getNewDamage(), null);
            }
        }
        finally { SECONDARY_DAMAGE.remove(); }
    }
    @SubscribeEvent public static void fall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) { return; }
        float distance = event.getDistance();
        for (var stack : armor(event.getEntity())) { distance = ArmorData.transform(stack, event.getEntity(), "fall", null, distance); }
        event.setDistance(distance); if (distance == 0) { event.setDamageMultiplier(0); }
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent event) {
        float strength = event.getStrength();
        for (var stack : armor(event.getEntity())) { strength = ArmorData.transform(stack, event.getEntity(), "knockback", null, strength); }
        event.setStrength(strength);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        float bonus = 0;
        for (var stack : armor(event.getEntity())) { bonus += .4F * ArmorData.points(stack, "shulkerweight") / 20F; }
        if (bonus > 0) { event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().add(0, bonus, 0)); event.getEntity().hurtMarked = true; }
        for (var stack : armor(event.getEntity())) { ArmorData.invoke(stack, event.getEntity(), "jump", null, bonus, null); }
    }
    @SubscribeEvent public static void milk(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !event.getItem().is(net.minecraft.world.item.Items.MILK_BUCKET)) { return; }
        for (var stack : armor(event.getEntity())) { ArmorData.invoke(stack, event.getEntity(), "milk", null, 0, null); }
    }
    @SubscribeEvent public static void experience(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() == null) { return; }
        event.setDroppedExperience(xp(event.getAttackingPlayer(), event.getDroppedExperience()));
    }
    @SubscribeEvent public static void blockExperience(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.getBreaker() instanceof Player player) { event.setDroppedExperience(xp(player, event.getDroppedExperience())); }
    }
    private static int xp(Player player, int xp) {
        float result = xp; for (var stack : armor(player)) { result = ArmorData.transform(stack, player, "experience", null, result); } return Math.round(result);
    }
    @SubscribeEvent public static void pickupXp(PlayerXpEvent.PickupXp event) {
        if (event.getEntity().level().isClientSide) { return; }
        for (var stack : armor(event.getEntity())) {
            int level = ArmorData.level(stack, "mending"); if (level == 0) { continue; }
            var ctx = new com.gctn.tinkersarmorynirvana.api.ArmorContext(stack, event.getEntity(), "conarm:mending", level, null, 0, null);
            int capacity = 30 * (int) Math.pow(3, level - 1);
            int stored = Math.min(event.getOrb().value, Math.max(0, capacity - ctx.state.getInt("Experience")));
            ctx.state.putInt("Experience", ctx.state.getInt("Experience") + stored); ctx.save(); event.getOrb().value -= stored;
        }
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) { return; }
        ListTag saved = new ListTag();
        event.getDrops().removeIf(entity -> {
            var stack = entity.getItem();
            if (ArmorData.initialized(stack) && ArmorData.level(stack, "soulbound") > 0) { saved.add(stack.save(player.registryAccess())); return true; }
            return false;
        });
        if (!saved.isEmpty()) { player.getPersistentData().put("conarm:Soulbound", saved); }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) { return; }
        var original = event.getOriginal(); var player = event.getEntity();
        var saved = original.getPersistentData().getList("conarm:Soulbound", 10);
        for (int i = 0; i < saved.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), saved.getCompound(i));
            if (!player.getInventory().add(stack) && !stack.isEmpty()) { player.drop(stack, false); }
        }
        original.getPersistentData().remove("conarm:Soulbound");
    }
    @SubscribeEvent public static void serverStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) { ArmoryCasting.install(); com.gctn.tinkersarmorynirvana.dev.ArmorySelfTest.run(event.getServer()); }
    @SubscribeEvent public static void serverStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) { ACTIVE.clear(); CURRENT_DAMAGE.clear(); ArmorEffects.clear(); }
    @SubscribeEvent public static void equipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide) { return; }
        if (ArmorData.initialized(event.getFrom())) { ArmorData.invoke(event.getFrom(), event.getEntity(), "unequip", null, 0, null); }
        if (ArmorData.initialized(event.getTo())) { ArmorData.invoke(event.getTo(), event.getEntity(), "equip", null, 0, null); }
    }
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean("conarm:ReceivedBook")) {
            persisted.putBoolean("conarm:ReceivedBook", true); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            if (com.gctn.tinkersarmorynirvana.ArmoryConfig.SPAWN_WITH_BOOK.get()) {
                var book = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ArmoryContent.id("book")));
                if (!player.getInventory().add(book)) { player.drop(book, false); }
            }
        }
    }
    @SubscribeEvent public static void invulnerability(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item && ArmorData.initialized(item.getItem())
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) { event.setInvulnerable(true); }
    }
    @SubscribeEvent public static void expire(net.neoforged.neoforge.event.entity.item.ItemExpireEvent event) {
        if (ArmorData.initialized(event.getEntity().getItem())) { event.getEntity().setUnlimitedLifetime(); event.addExtraLife(6000); }
    }
    @SubscribeEvent public static void anvil(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        if (ArmorData.initialized(event.getLeft()) && !event.getRight().isEmpty()) { event.setCanceled(true); }
    }
}
