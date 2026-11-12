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
        check(ArmorData.displayStats(iron).defense == 15 && ArmorData.stats(iron).defense == 2.4F, "Original information panels show the full armor defense while native attributes retain slot allocation");
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
        originalMenuInteractions(fake);
        nativePartBuilder(fake);
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

    private void originalMenuInteractions(net.minecraft.server.level.ServerPlayer player) {
        var level = server.overworld(); var pos = level.getSharedSpawnPos().above(3); var previous = level.getBlockState(pos);
        player.getInventory().clearContent();
        try {
            level.setBlockAndUpdate(pos, ArmoryContent.STATION.get().defaultBlockState());
            var station = (ArmoryStationBlockEntity) level.getBlockEntity(pos); var menu = new ArmoryMenu(21, player.getInventory(), station);
            check(menu.getSlot(1).x == 33 && menu.getSlot(1).y == 42 && menu.getSlot(6).isActive(), "Original repair selection exposes all six radial slots on the ordinary station");
            check(menu.clickMenuButton(player, 1) && menu.getSlot(1).x == 29 && menu.getSlot(1).y == 52 && menu.getSlot(2).x == 9 && menu.getSlot(3).x == 49,
                    "Original armor tab selects the core-bottom, plate-left and trim-right layout");
            check(!menu.getSlot(1).mayPlace(ArmoryContent.PARTS.get("chest_core").get().create("iron")) && !menu.getSlot(4).isActive(), "Armor tabs restrict the core type and hide empty dormant slots");
            station.setItem(5, new ItemStack(Items.DIRT)); menu.broadcastChanges();
            check(menu.getSlot(6).isActive() && !menu.getSlot(6).mayPlace(new ItemStack(Items.DIRT)) && menu.getSlot(6).x == 87, "Filled dormant slots remain visible and extraction-only");
            check(!menu.quickMoveStack(player, 6).isEmpty() && station.getItem(5).isEmpty(), "Dormant inventory can be recovered after switching tabs");
            station.setItem(0, ArmoryContent.PARTS.get("helmet_core").get().create("iron"));
            station.setItem(1, ArmoryContent.PARTS.get("armor_plate").get().create("iron")); station.setItem(2, ArmoryContent.PARTS.get("armor_trim").get().create("iron")); menu.broadcastChanges();
            check(menu.getSlot(0).hasItem() && !menu.quickMoveStack(player, 0).isEmpty() && station.isEmpty(), "Original assembly tab produces armor and consumes its three parts once");
            check(!menu.clickMenuButton(player, 99) && menu.selection() == 1, "Invalid selection packets cannot change the station mode");
            menu.clickMenuButton(player, 0);
            ItemStack damaged = armor("helmet", "iron"); damaged.setDamageValue(30); station.setItem(0, damaged);
            station.setItem(1, ArmoryContent.PARTS.get("polishing_kit").get().create("iron")); menu.broadcastChanges();
            check(menu.getSlot(0).hasItem(), "Ordinary Armor Station still repairs armor");
            station.clearContent(); station.setItem(0, armor("helmet", "iron")); station.setItem(1, new ItemStack(Items.DIAMOND)); menu.broadcastChanges();
            check(!menu.getSlot(0).hasItem() && menu.needsForge() && station.getItem(1).getCount() == 1, "Ordinary Armor Station rejects modifiers and requests an Armor Forge without consuming inputs");
            station.clearContent(); station.setItem(0, armor("helmet", "iron")); menu.rename("原版命名测试");
            check(menu.getSlot(0).getItem().getHoverName().getString().equals("原版命名测试") && !menu.quickMoveStack(player, 0).isEmpty() && station.isEmpty(), "Original standalone naming operation consumes only its source armor");
            menu.rename(""); menu.broadcastChanges();
            var source = ArmoryWorkshop.applyModifier(armor("helmet", "iron"), "diamond", 1);
            check(menu.getSlot(0).mayPlace(source), "Default selection accepts intact armor in the right-hand deconstruction slot");
            menu.setCarried(source.copy()); menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && station.deconstructing() && station.isEmpty() && menu.getSlot(1).getItem().is(ArmoryContent.PARTS.get("helmet_core").get()),
                    "Native output-slot click transfers the source and exposes virtual part previews");
            check(!menu.clickMenuButton(player, 2) && !menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT)), "Deconstruction prevents tab switching and insertion into preview slots");
            menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            check(ItemStack.isSameItemSameComponents(menu.getCarried(), source) && !station.deconstructing() && station.isEmpty() && !menu.getSlot(1).hasItem(), "Taking the original armor cancels deconstruction without producing parts");
            menu.setCarried(source.copy()); menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            menu.clicked(1, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            check(menu.getCarried().is(ArmoryContent.PARTS.get("helmet_core").get()) && menu.getCarried().getCount() == 1 && !station.deconstructing() && station.getItem(0).isEmpty()
                    && station.getItem(1).getCount() == 1 && station.getItem(2).getCount() == 1 && !menu.getSlot(0).hasItem(), "Taking the first virtual part consumes the unique source and materializes the other two exactly once");
            menu.setCarried(ItemStack.EMPTY); station.clearContent(); menu.broadcastChanges(); station.beginDeconstruction(source);
            var saved = station.saveWithFullMetadata(server.registryAccess());
            var restored = (ArmoryStationBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, station.getBlockState(), saved, server.registryAccess());
            check(restored != null && restored.deconstructing() && restored.isEmpty() && ItemStack.isSameItemSameComponents(restored.source(), source), "Pending deconstruction saves the source armor without saving duplicate preview parts");
            station.cancelDeconstruction();
            var bag = ArmoryWorkshop.applyModifier(armor("chestplate", "iron"), "travel_sack", 1);
            var storage = new CompoundTag(); var inventories = new CompoundTag(); var contents = new CompoundTag(); contents.put("0", new ItemStack(Items.DIAMOND).save(server.registryAccess())); inventories.put("travel_sack", contents); storage.put("Inventories", inventories); ArmorStorage.write(bag, storage);
            check(!station.canDeconstruct(bag), "Deconstruction rejects armor containing accessory inventory");
            source.setDamageValue(1); check(!station.canDeconstruct(source), "Damaged armor cannot be deconstructed"); source.setDamageValue(0);
            station.beginDeconstruction(source);
            var nearby = new net.minecraft.world.phys.AABB(pos).inflate(2);
            var before = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, nearby).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, nearby, item -> !before.contains(item.getUUID()));
            check(dropped.size() == 1 && ItemStack.isSameItemSameComponents(dropped.getFirst().getItem(), source), "Breaking a station during deconstruction drops the original armor once and no virtual parts");
            dropped.forEach(net.minecraft.world.entity.Entity::discard);
            var potionMenu = new AccessoryMenu(22, player.getInventory()); potionMenu.setData(0, 1);
            check(potionMenu.getSlot(0).x == 26 && potionMenu.getSlot(0).y == 20 && potionMenu.getSlot(27).y == 51 && potionMenu.getSlot(54).y == 109 && !potionMenu.getSlot(7).isActive(),
                    "Potion belt synchronizes its original seven-slot layout and player inventory positions");
            potionMenu.setData(0, 0);
            check(potionMenu.getSlot(0).x == 8 && potionMenu.getSlot(27).y == 85 && potionMenu.getSlot(54).y == 143 && potionMenu.getSlot(26).isActive(), "Knapsack restores the original chest layout after mode synchronization");
            menu.removed(player);
        } finally { level.setBlockAndUpdate(pos, previous); }
    }
    private void nativePartBuilder(net.minecraft.server.level.ServerPlayer player) {
        for (var entry : ArmoryContent.PARTS.entrySet()) {
            String id = "conarm:" + entry.getKey();
            var definition = com.gctn.tconstruct.library.toolparts.AntiqueParts.get(id);
            check(definition != null && definition.external() && definition.item().get() == entry.getValue().get()
                    && com.gctn.tconstruct.tables.menu.WorkshopMenu.patterns().contains(definition), "Native stencil catalog contains the existing " + id + " item");
            var pattern = new ItemStack(com.gctn.tconstruct.tools.PatternItem.PATTERN.get());
            var materialTag = new CompoundTag(); materialTag.putString("Pattern", id);
            pattern.set(com.gctn.tconstruct.init.TinkerDataComponents.TINKER_MATERIAL.get(), net.minecraft.world.item.component.CustomData.of(materialTag));
            var input = new SimpleContainer(7); input.setItem(0, pattern); input.setItem(1, new ItemStack(Items.OAK_PLANKS, 16));
            var preview = com.gctn.tconstruct.tables.recipe.PartBuilderRecipes.planWithRemainder(input);
            check(preview != null && preview.result().is(entry.getValue().get()) && ArmorPartItem.material(preview.result()).equals("wood")
                    && input.getItem(1).getCount() == 16, "Native part builder previews " + id + " without consuming its pattern or material");
            preview.consume(input);
            check(input.getItem(0).getCount() == 1 && input.getItem(1).getCount() == 16 - entry.getValue().get().cost, "Native part builder consumes the source cost once for " + id);
        }
        var nativeInput = new SimpleContainer(7); var pattern = new ItemStack(com.gctn.tconstruct.tools.PatternItem.PATTERN.get());
        var tag = new CompoundTag(); tag.putString("Pattern", "pickaxe_head"); pattern.set(com.gctn.tconstruct.init.TinkerDataComponents.TINKER_MATERIAL.get(), net.minecraft.world.item.component.CustomData.of(tag));
        nativeInput.setItem(0, pattern); nativeInput.setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
        var plan = com.gctn.tconstruct.tables.recipe.PartBuilderRecipes.planWithRemainder(nativeInput);
        check(plan != null && plan.result().is(com.gctn.tconstruct.library.toolparts.AntiqueParts.get("pickaxe_head").item().get()), "External-part API preserves native tool-part processing");
        var legacy = new ItemStack(ArmoryContent.PATTERN.get(), 2); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, legacy);
        ArmoryContent.PATTERN.get().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var converted = player.getMainHandItem();
        check(converted.is(com.gctn.tconstruct.tools.PatternItem.PATTERN.get()) && converted.getCount() == 2
                && com.gctn.tconstruct.tools.PatternItem.selected(converted).id().equals("conarm:helmet_core"), "Existing legacy armor patterns migrate to native selected patterns without losing their count");
    }
}
