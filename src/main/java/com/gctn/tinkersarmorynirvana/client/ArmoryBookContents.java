/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.library.materials.Material;
import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.TinkersArmoryNirvana;
import com.gctn.tinkersarmorynirvana.api.*;
import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.*;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Book content is resolved when opened, so resource packs, addon definitions and recipe reloads remain visible. */
final class ArmoryBookContents {
    interface Content {}
    record Paragraph(Component text) implements Content {}
    record Icons(List<ItemStack> items) implements Content {}
    record Link(Component label, String target, ItemStack icon) implements Content {}
    record Grid(List<Link> entries) implements Content {}
    record Preview(List<ItemStack> armor) implements Content {}
    record Recipe(ItemStack input, ItemStack output, ArmoryModifierRecipe recipe) implements Content {}
    record Page(String id, String section, Component title, List<Content> contents) {}
    private static final List<String> SECTIONS = List.of("welcome", "armory", "materials", "traits", "modifiers", "accessories");
    private ArmoryBookContents() {}

    static List<Page> pages() {
        List<Page> result = new ArrayList<>();
        List<Content> index = new ArrayList<>();
        for (String section : SECTIONS) { index.add(new Link(sectionTitle(section), "section:" + section, sectionIcon(section))); }
        result.add(new Page("index", "index", text("contents"), List.copyOf(index)));
        List<Content> welcome = legacyWelcome();
        welcome.add(new Icons(List.of(item("book"))));
        result.add(new Page("welcome", "welcome", sectionTitle("welcome"), List.copyOf(welcome)));
        workshop(result);
        materials(result);
        traits(result);
        modifiers(result, false);
        modifiers(result, true);
        return List.copyOf(result);
    }
    static Component text(String key, Object... args) { return Component.translatable("book.conarm." + key, args); }
    private static Component sectionTitle(String section) { return text("section." + section); }
    private static Component traitName(String id) { return Component.translatable("trait." + id.replace(':', '.') + ".name"); }
    private static Component modifierName(String id) { return Component.translatable("modifier." + id.replace(':', '.') + ".name"); }
    private static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.get(ArmoryContent.id(name))); }
    private static ItemStack armor(String type, String material) { return ArmorData.create(ArmoryContent.ARMORS.get(type).get(), material, material, material); }
    private static String exampleMaterial() {
        if (ArmoryRegistry.material("iron") != null) { return "iron"; }
        return ArmoryRegistry.materials().keySet().stream().findFirst().orElse(null);
    }
    private static ItemStack exampleArmor(String type) { String material = exampleMaterial(); return material == null ? ItemStack.EMPTY : armor(type, material); }
    private static ItemStack sectionIcon(String section) {
        return switch (section) {
            case "welcome" -> item("book");
            case "armory" -> new ItemStack(ArmoryContent.STATION.get());
            case "materials" -> new ItemStack(Items.IRON_INGOT);
            case "traits" -> exampleArmor("helmet");
            case "modifiers" -> new ItemStack(Items.DIAMOND);
            default -> item("travel_goggles");
        };
    }
    private static List<Content> legacyWelcome() {
        // 优先读取资源包中的当前语言，缺失时回退英文；保留原作者欢迎页的完整段落。
        var minecraft = Minecraft.getInstance();
        String language = minecraft.getLanguageManager().getSelected();
        for (String locale : language.equals("en_us") ? List.of("en_us") : List.of(language, "en_us")) {
            var resource = minecraft.getResourceManager().getResource(ArmoryContent.id("book/" + locale + "/intro/welcome.json"));
            if (resource.isEmpty()) { continue; }
            try (var reader = resource.get().openAsReader()) {
                List<Content> paragraphs = new ArrayList<>();
                for (var paragraph : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("text")) {
                    paragraphs.add(new Paragraph(Component.literal(paragraph.getAsJsonObject().get("text").getAsString())));
                }
                return paragraphs;
            } catch (Exception error) { TinkersArmoryNirvana.LOGGER.warn("Cannot read Armory welcome page for {}", locale, error); }
        }
        return new ArrayList<>(List.of(new Paragraph(text("intro1"))));
    }
    private static void workshop(List<Page> pages) {
        List<Page> chapter = new ArrayList<>();
        chapter.add(new Page("stations", "armory", text("stations"), List.of(
                new Icons(List.of(new ItemStack(ArmoryContent.STATION.get()), new ItemStack(ArmoryContent.FORGE.get()), new ItemStack(ArmoryContent.PATTERN.get()))),
                new Paragraph(text("intro1")))));
        String material = exampleMaterial();
        List<ItemStack> parts = material == null ? List.of() : ArmoryContent.PARTS.values().stream().map(part -> part.get().create(material)).toList();
        chapter.add(new Page("parts", "armory", text("parts"), List.of(new Icons(parts), new Paragraph(text("intro2")))));
        List<Content> assembly = new ArrayList<>();
        var suit = material == null ? List.<ItemStack>of() : ArmoryContent.ARMORS.keySet().stream().map(type -> armor(type, material)).toList();
        assembly.add(new Preview(suit)); assembly.add(new Icons(suit)); assembly.add(new Paragraph(text("assembly")));
        chapter.add(new Page("assembly", "armory", text("armor"), List.copyOf(assembly)));
        chapter.add(new Page("repair", "armory", text("repair"), List.of(new Icons(parts.isEmpty() ? List.of() : List.of(parts.getLast())), new Paragraph(text("intro3")))));
        chapter.add(new Page("finishing", "armory", text("finishing"), List.of(new Icons(List.of(new ItemStack(Items.SAND), new ItemStack(Items.GOLD_BLOCK))), new Paragraph(text("intro4")))));
        chapter.add(new Page("controls", "armory", text("controls"), List.of(new Icons(List.of(item("travel_belt"), item("travel_sack"), item("travel_goggles"))), new Paragraph(text("intro5")))));
        chapter(pages, "armory", chapter);
    }
    private static void materials(List<Page> pages) {
        List<Page> chapter = new ArrayList<>();
        for (var definition : ArmoryRegistry.materials().values()) {
            var base = TinkerMaterials.getMaterial(definition.id);
            if (base == null || base.isHidden()) { continue; }
            List<Content> contents = new ArrayList<>();
            var suit = ArmoryContent.ARMORS.keySet().stream().map(type -> armor(type, definition.id)).toList();
            contents.add(new Icons(suit));
            contents.add(new Paragraph(text("material", Material.nameOf(definition.id), definition.coreDurability, definition.defense,
                    definition.plateMultiplier, definition.plateDurability, definition.toughness, definition.trimDurability)));
            String[] names = {"core", "plates", "trim"};
            String[] parts = {"helmet_core", "armor_plate", "armor_trim"};
            for (int part = 0; part < 3; part++) {
                contents.add(new Paragraph(text("part." + names[part])));
                for (var trait : definition.traits(part)) {
                    if (ArmoryRegistry.trait(trait.id()) == null) { continue; }
                    contents.add(new Link(traitName(trait.id()).copy().append(trait.level() > 1 ? " " + trait.level() : ""),
                            "trait:" + trait.id(), ArmoryContent.PARTS.get(parts[part]).get().create(definition.id)));
                }
            }
            contents.add(new Preview(suit));
            chapter.add(new Page("material:" + definition.id, "materials", Material.nameOf(definition.id), List.copyOf(contents)));
        }
        chapter(pages, "materials", chapter);
    }
    private static void traits(List<Page> pages) {
        List<Page> chapter = new ArrayList<>();
        for (var trait : ArmoryRegistry.traits().values()) {
            List<Content> contents = new ArrayList<>();
            contents.add(new Paragraph(Component.translatable("trait." + trait.id.replace(':', '.') + ".desc")));
            List<ItemStack> examples = new ArrayList<>();
            for (var material : ArmoryRegistry.materials().values()) {
                var base = TinkerMaterials.getMaterial(material.id);
                if (base == null || base.isHidden()) { continue; }
                boolean applies = false;
                for (int part = 0; part < 3; part++) { if (material.traits(part).stream().anyMatch(value -> value.id().equals(trait.id))) { applies = true; } }
                if (applies) {
                    ItemStack example = armor("helmet", material.id); examples.add(example);
                    contents.add(new Link(Material.nameOf(material.id), "material:" + material.id, example));
                }
            }
            if (!examples.isEmpty()) { contents.addFirst(new Icons(examples)); }
            chapter.add(new Page("trait:" + trait.id, "traits", traitName(trait.id), List.copyOf(contents)));
        }
        chapter(pages, "traits", chapter);
    }
    private static void modifiers(List<Page> pages, boolean accessories) {
        List<Page> chapter = new ArrayList<>(); String section = accessories ? "accessories" : "modifiers";
        var level = Minecraft.getInstance().level;
        var recipes = level == null ? List.<net.minecraft.world.item.crafting.RecipeHolder<ArmoryModifierRecipe>>of()
                : level.getRecipeManager().getAllRecipesFor(ArmoryRecipes.MODIFIER_TYPE.get()).stream().sorted(java.util.Comparator.comparing(value -> value.id().toString())).toList();
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            if (modifier.accessory() != accessories) { continue; }
            List<Content> contents = new ArrayList<>();
            String type = ArmoryContent.ARMORS.keySet().stream().filter(name -> modifier.allows(ArmoryContent.ARMORS.get(name).get().getEquipmentSlot())).findFirst().orElse("helmet");
            ItemStack input = exampleArmor(type), output = input.isEmpty() ? ItemStack.EMPTY : ArmoryWorkshop.applyModifier(input, modifier.id, 1);
            contents.add(new Icons(output.isEmpty() ? List.of(input) : List.of(output)));
            contents.add(new Paragraph(Component.translatable("modifier." + modifier.id.replace(':', '.') + ".desc")));
            contents.add(new Paragraph(text("modifier.levels", modifier.maxLevel(), modifier.pointsPerLevel())));
            contents.add(new Paragraph(text("modifier.slots", modifier.slotCost(1), modifier.slotCost(2) - modifier.slotCost(1))));
            Component allowed = Component.empty();
            for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                if (!modifier.allows(slot)) { continue; }
                if (!allowed.getString().isEmpty()) { allowed = allowed.copy().append(" · "); }
                String armorType = switch (slot) { case HEAD -> "helmet"; case CHEST -> "chestplate"; case LEGS -> "leggings"; default -> "boots"; };
                allowed = allowed.copy().append(Component.translatable("item.conarm." + armorType));
            }
            contents.add(new Paragraph(text("modifier.equipment", allowed)));
            for (var holder : recipes) { if (holder.value().modifier().equals(modifier.id)) { contents.add(new Recipe(input, output, holder.value())); } }
            if (contents.stream().noneMatch(Recipe.class::isInstance)) { contents.add(new Paragraph(text("recipe.none"))); }
            if (accessories) { contents.add(new Paragraph(text("accessory.controls"))); }
            for (String conflict : modifier.conflicts().stream().sorted().toList()) {
                if (ArmoryRegistry.modifier(conflict) != null) { contents.add(new Link(text("modifier.conflict", modifierName(conflict)), "modifier:" + conflict, ItemStack.EMPTY)); }
            }
            chapter.add(new Page("modifier:" + modifier.id, section, modifierName(modifier.id), List.copyOf(contents)));
        }
        chapter(pages, section, chapter);
    }
    private static void chapter(List<Page> pages, String section, List<Page> chapter) {
        List<Link> entries = new ArrayList<>();
        for (var page : chapter) {
            ItemStack icon = page.contents.stream().filter(Icons.class::isInstance).map(Icons.class::cast).flatMap(icons -> icons.items.stream()).filter(stack -> !stack.isEmpty()).findFirst().orElse(sectionIcon(section));
            entries.add(new Link(page.title, page.id, icon));
        }
        pages.add(new Page("section:" + section, section, sectionTitle(section), List.of(new Grid(List.copyOf(entries)))));
        pages.addAll(chapter);
    }
}
