/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.dev;

import com.gctn.tinkersarmorynirvana.*;
import com.gctn.tinkersarmorynirvana.api.*;
import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** Opt-in development checks for transaction and serialization risks; never run during ordinary gameplay. */
public final class ArmorySelfTest {
    private final MinecraftServer server;
    private final java.util.ArrayList<String> checks = new java.util.ArrayList<>();
    private ArmorySelfTest(MinecraftServer server) { this.server = server; }
    private void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks.add(message);
    }
    private ItemStack armor(String type, String material) { return ArmorData.create(ArmoryContent.ARMORS.get(type).get(), material, material, material); }
    private ArmoryWorkshop.Plan plan(SimpleContainer input) { return ArmoryWorkshop.plan(input, server.overworld()); }

    public static void run(MinecraftServer server) {
        if (!Boolean.getBoolean("conarm.selftest")) { return; }
        ArmorySelfTest suite = new ArmorySelfTest(server);
        try {
            java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("conarm.selftestReport", "armory-selftest.md"));
            java.nio.file.Files.deleteIfExists(path);
            suite.execute();
            String report = "# Armory server verification\n\nMinecraft 1.21.1 / NeoForge 21.1.265\n\n" + String.join("\n", suite.checks.stream().map(check -> "- PASS: " + check).toList()) + "\n";
            java.nio.file.Files.createDirectories(path.toAbsolutePath().getParent()); java.nio.file.Files.writeString(path, report);
            java.nio.file.Files.writeString(path.toAbsolutePath().resolveSibling("armory-item-registry.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).sorted().toList()));
            TinkersArmoryNirvana.LOGGER.info("ARMORY_SELFTEST_PASS: {} checks", suite.checks.size());
        } catch (Exception exception) { throw new IllegalStateException("Armory verification failed", exception); }
        server.halt(false);
    }

    private void execute() {
        TinkersArmoryNirvana.LOGGER.info("Armory fixture diagnostics: material definitions={}, installed example={}, armor example={}, custom trait={}",
                com.gctn.tconstruct.api.ContentRegistries.MATERIALS.size(), com.gctn.tconstruct.library.TinkerMaterials.getMaterial("example:amethyst") != null,
                ArmoryRegistry.material("example:amethyst") != null, ArmoryRegistry.trait("example:resolve") != null);
        if (Boolean.getBoolean("conarm.kubejsFixture")) {
            check(ArmoryRegistry.material("example:amethyst") != null, "KubeJS startup scripts install the example armor material");
            check(ArmoryRegistry.trait("example:resolve") != null && ArmoryRegistry.modifier("example:amethyst_guard") != null, "KubeJS startup scripts install callback traits and modifiers");
            check(ArmoryRegistry.material("wood").coreDurability == 6, "KubeJS can edit an existing armor material");
            check(server.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("example:armory/amethyst_guard")).isPresent(), "KubeJS recipe events install a native armor modifier recipe");
        }
        check(ArmoryRegistry.materials().size() >= 30, "All thirty original armor materials are registered");
        for (String material : ArmoryRegistry.materials().keySet()) {
            for (String type : ArmoryContent.ARMORS.keySet()) {
                ItemStack armor = armor(type, material);
                check(ArmorData.initialized(armor) && armor.getMaxDamage() > 0 && !ArmorData.broken(armor), "Build " + material + " " + type);
                check(!ArmorData.traits(armor).isEmpty(), "Resolve material traits for " + material + " " + type);
            }
        }
        if (Boolean.getBoolean("conarm.kubejsFixture")) {
            var custom = armor("helmet", "example:amethyst");
            check(ArmorData.stats(custom).toughness == 3.5F, "KubeJS custom stat callbacks execute during armor calculation");
            var modified = ArmoryWorkshop.applyModifier(custom, "example:amethyst_guard", 2);
            check(!modified.isEmpty() && Math.abs(ArmorData.transform(modified, null, "incomingDamage", null, 100) - 96) < .001F,
                    "KubeJS custom combat callbacks execute outside startup event dispatch");
        }
        ItemStack iron = armor("helmet", "iron");
        check(iron.getMaxDamage() == 200 && ArmorData.stats(iron).defense == 2.4F, "Original iron helmet durability and defense calculation");
        check(ArmorData.traits(iron).get("conarm:magnetic") == 3, "Unique initial levels 2 and 1 combine; repeated level 1 is counted once");
        ItemStack diamond = ArmoryWorkshop.applyModifier(iron, "diamond", 1);
        check(!diamond.isEmpty() && diamond.getMaxDamage() == 350 && ArmorData.freeSlots(diamond) == 2, "Diamond applies stats and uses one slot");
        check(ArmoryWorkshop.applyModifier(diamond, "diamond", 1).isEmpty(), "Duplicate single-level modifier is rejected");
        var extra = ArmoryWorkshop.applyModifier(iron, "extramodifier", 1);
        check(!extra.isEmpty() && ArmorData.freeSlots(extra) == 4 && ArmoryWorkshop.applyModifier(extra, "extramodifier", 1).isEmpty(), "Dragon-head extra modifier grants one free slot and cannot be repeated");
        check(!iron.isRepairable() && !iron.isBookEnchantable(new ItemStack(Items.ENCHANTED_BOOK)), "Vanilla repair combination and book enchanting are disabled");
        check(ArmoryWorkshop.applyModifier(iron, "telekinetic", 1).isEmpty(), "Chest-only gauntlet cannot be installed on a helmet");
        ItemStack resistance = iron.copy();
        for (int i = 0; i < 8; i++) { resistance = ArmoryWorkshop.applyModifier(resistance, "resistant", 1); }
        check(!resistance.isEmpty() && ArmorData.usedSlots(resistance) == 1, "Eight resistance levels use only the first modifier slot");
        check(ArmoryWorkshop.applyModifier(resistance, "fire_resistant", 1).isEmpty(), "Different resistance types are mutually exclusive");
        ItemStack chest = armor("chestplate", "iron");
        chest = ArmoryWorkshop.applyModifier(chest, "travel_sack", 1);
        check(!chest.isEmpty() && ArmoryWorkshop.applyModifier(chest, "travel_slowfall", 1).isEmpty(), "One travelling accessory per armor piece");
        ItemStack source = diamond.copy(); source.setDamageValue(200);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("事务测试"));
        SimpleContainer input = new SimpleContainer(6); input.setItem(0, source.copy()); input.setItem(1, ArmoryContent.PARTS.get("polishing_kit").get().create("iron"));
        var preview = plan(input);
        check(preview != null && preview.result().getDamageValue() < 200, "Polishing kit repairs damaged armor");
        check(input.getItem(0).getDamageValue() == 200 && input.getItem(1).getCount() == 1, "Repair previews do not consume or mutate inputs");
        var snapshot = input.getItem(0).copy(); preview.commit(input);
        check(input.getItem(0).isEmpty() && input.getItem(1).isEmpty(), "Repair commit consumes exactly one armor and one kit");
        check(!preview.valid(input), "A committed transaction cannot be replayed");
        input.setItem(0, snapshot); input.setItem(1, ArmoryContent.PARTS.get("polishing_kit").get().create("wood"));
        check(plan(input) == null, "Wrong-material polishing kits are rejected");
        input.setItem(1, ArmoryContent.PARTS.get("polishing_kit").get().create("iron")); preview = plan(input);
        input.getItem(1).grow(1); check(!preview.valid(input), "Concurrent inventory changes invalidate an older preview");
        input.clearContent(); input.setItem(0, armor("helmet", "iron"));
        input.setItem(1, ArmoryContent.PARTS.get("armor_trim").get().create("cobalt")); preview = plan(input);
        check(preview != null && ArmorData.read(preview.result()).getString("Trim").equals("cobalt"), "Part replacement selects the correct material position");
        input.setItem(1, ArmoryContent.PARTS.get("chest_core").get().create("wood")); check(plan(input) == null, "Wrong armor core type is rejected");
        input.clearContent(); input.setItem(0, new ItemStack(ArmoryContent.PATTERN.get())); input.setItem(1, new ItemStack(Items.OAK_PLANKS, 8));
        preview = plan(input); check(preview != null && preview.consumed()[1] == 4 && preview.consumed()[0] == 0, "Part crafting consumes the original cost and retains the pattern");
        preview.commit(input); check(input.getItem(0).getCount() == 1 && input.getItem(1).getCount() == 4, "Pattern and surplus raw material survive part crafting");
        input.clearContent(); input.setItem(0, ArmoryContent.PARTS.get("helmet_core").get().create("iron"));
        input.setItem(1, ArmoryContent.PARTS.get("armor_plate").get().create("cobalt")); input.setItem(2, ArmoryContent.PARTS.get("armor_trim").get().create("wood"));
        preview = plan(input); check(preview != null && ArmorData.initialized(preview.result()), "Mixed-material armor assembly");
        input.setItem(4, new ItemStack(Items.DIRT)); check(plan(input) == null, "Unrelated extra assembly ingredients are rejected");
        input.clearContent(); input.setItem(0, iron.copy()); input.setItem(1, new ItemStack(Items.IRON_INGOT)); input.setItem(2, new ItemStack(Items.GOLD_INGOT));
        var overlap = new ArmoryModifierRecipe("diamond", 1, List.of(new IngredientCost(Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT), 1), new IngredientCost(Ingredient.of(Items.IRON_INGOT), 1)));
        int[] allocation = overlap.allocation(new ArmoryInput(input));
        check(allocation != null && allocation[1] == 1 && allocation[2] == 1, "Overlapping ingredients are allocated without greedy matching failures");
        input.clearContent(); input.setItem(0, iron.copy()); input.setItem(1, new ItemStack(Items.DIAMOND));
        preview = plan(input); check(preview != null && ArmorData.level(preview.result(), "diamond") == 1, "Datapack armor modifier recipes are loaded by the native recipe manager");
        check(server.getRecipeManager().byKey(ArmoryContent.id("travel_night")).isPresent() && server.getRecipeManager().byKey(ArmoryContent.id("travel_sneak")).isPresent(), "Potion-component accessory recipes decode successfully");
        ItemStack paper = ArmorData.create(ArmoryContent.ARMORS.get("helmet").get(), "paper", "iron", "iron");
        for (int i = 0; i < 5; i++) { paper = ArmoryWorkshop.applyModifier(paper, "reinforced", 1); }
        check(!paper.isEmpty() && ArmorData.level(paper, "reinforced") == 5, "Paper permits five reinforcement levels");
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("c332714b-8e78-4622-a6c3-40efa4e39900"), "ArmoryPortTest"));
        ArmorData.damage(paper, fake, fake.damageSources().generic(), 10000);
        check(paper.getDamageValue() == 0, "Fully reinforced armor rejects durability damage");
        ItemStack broken = armor("helmet", "iron"); ArmorData.damage(broken, fake, fake.damageSources().generic(), 10000);
        check(broken.getCount() == 1 && ArmorData.broken(broken) && broken.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().isEmpty(), "Broken armor remains intact and loses its attributes");
        ArmorData.repair(broken, fake, 30); check(!ArmorData.broken(broken), "Repair restores broken armor");
        ArmorEffects.begin(); ArmorEffects.apply(fake, net.minecraft.world.effect.MobEffects.NIGHT_VISION, 220); ArmorEffects.end(fake);
        check(fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "Accessory night vision is applied on the server");
        ArmorEffects.begin(); ArmorEffects.end(fake);
        check(!fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "Owned accessory effects are removed when no longer active");
        fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 3600, 0));
        ArmorEffects.begin(); ArmorEffects.apply(fake, net.minecraft.world.effect.MobEffects.NIGHT_VISION, 220); ArmorEffects.end(fake);
        ArmorEffects.begin(); ArmorEffects.end(fake);
        check(fake.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION).getDuration() == 3600, "Accessory cleanup preserves an independent night-vision potion");
        fake.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        boolean previousLeveling = ArmoryConfig.LEVELING.get();
        try {
            ArmoryConfig.LEVELING.set(true);
            var progressing = armor("helmet", "iron");
            ArmorLeveling.damaged(progressing, fake, 400, fake.damageSources().generic());
            check(ArmorData.read(progressing).getInt("Experience") == 0 && ArmorData.read(progressing).getInt("Level") == 0, "Armor-bypassing damage does not grant armor experience");
            ArmorLeveling.damaged(progressing, fake, 400, fake.damageSources().playerAttack(fake));
            check(ArmorData.read(progressing).getInt("Level") == 1 && ArmorData.freeSlots(progressing) == 4, "Enabled native leveling awards a persisted level and modifier slot");
        } finally { ArmoryConfig.LEVELING.set(previousLeveling); }
        fake.setItemSlot(EquipmentSlot.CHEST, chest);
        AccessoryMenu bag = new AccessoryMenu(3, fake.getInventory(), EquipmentSlot.CHEST, "travel_sack");
        bag.getSlot(0).set(new ItemStack(Items.DIAMOND, 12));
        check(ArmorStorage.read(chest).getCompound("Inventories").getCompound("travel_sack").contains("0"), "Knapsack changes persist immediately on its equipped armor");
        check(!bag.getSlot(1).mayPlace(iron), "Armor storage rejects recursive armor inventories");
        fake.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); check(!bag.stillValid(fake), "Unequipping the owning armor closes access to its storage");
        ItemStack saved = ItemStack.parseOptional(server.registryAccess(), (CompoundTag) chest.save(server.registryAccess()));
        check(ItemStack.isSameItemSameComponents(chest, saved), "Disk serialization preserves material, modifiers and accessory contents");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), server.registryAccess());
        try { ItemStack.STREAM_CODEC.encode(buffer, chest); ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer); check(ItemStack.isSameItemSameComponents(chest, decoded), "Network serialization preserves armor data components"); }
        finally { buffer.release(); }
        check(com.gctn.tconstruct.smeltery.recipe.SmelteryRecipes.CASTING.stream().anyMatch(recipe -> recipe.output().is(ArmoryContent.PARTS.get("helmet_core").get())), "Armor parts are available through native smeltery casting");
        check(com.gctn.tconstruct.smeltery.recipe.SmelteryRecipes.CASTING.stream().anyMatch(recipe -> recipe.output().is(ArmoryContent.MATERIAL_ITEMS.get("clay_cast_polishing_kit").get())), "Single-use clay casting includes polishing kits");
        check(ArmoryContent.PARTS.get("helmet_core").get().create("tconstruct:iron").is(ArmoryContent.PARTS.get("helmet_core").get()), "Qualified builtin material identifiers are accepted by the part API");
        menuTransaction(fake);
    }

    private void menuTransaction(net.minecraft.server.level.ServerPlayer player) {
        var level = server.overworld(); var pos = level.getSharedSpawnPos().above(3);
        var previous = level.getBlockState(pos);
        check(level.getBlockEntity(pos) == null, "Temporary menu test does not replace an existing block entity");
        try {
            level.setBlockAndUpdate(pos, ArmoryContent.FORGE.get().defaultBlockState());
            player.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 0, 0);
            var station = (ArmoryStationBlockEntity) level.getBlockEntity(pos);
            station.setItem(0, armor("helmet", "iron")); station.setItem(1, new ItemStack(Items.DIAMOND));
            var menu = new ArmoryMenu(12, player.getInventory(), station);
            check(menu.getSlot(0).hasItem() && menu.stillValid(player), "Actual station menu exposes a valid preview");
            var moved = menu.quickMoveStack(player, 0);
            check(!moved.isEmpty() && ArmorData.level(moved, "diamond") == 1 && station.getItem(0).isEmpty() && station.getItem(1).isEmpty(), "Shift-taking a menu result consumes its source and ingredient once");
            check(menu.quickMoveStack(player, 0).isEmpty(), "Shift-taking the old result again cannot duplicate it");
            menu.removed(player);
        } finally { level.setBlockAndUpdate(pos, previous); }
    }
}
