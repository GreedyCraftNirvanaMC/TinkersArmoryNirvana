/* SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only AND MIT
 * Page builders port C4's ContentArmorMaterial, ContentArmor and ContentArmorModifier.
 * Mantle's section/text elements and TinkersAntique's listing/icon layouts retain their source attribution. */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.library.materials.Material;
import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.*;
import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.*;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.*;

final class ArmoryBookDocument {
    static final int WIDTH = 182, HEIGHT = 176;
    interface Element {}
    record Text(int x, int y, FormattedCharSequence line, float scale, boolean shadow, Component tooltip) implements Element {}
    record Icon(int x, int y, List<ItemStack> items, float scale, Component tooltip) implements Element {}
    record Image(int x, int y, int width, int height, ResourceLocation texture, int u, int v, int uw, int vh, int tw, int th, int color) implements Element {}
    record Link(int x, int y, int width, Component label, String target) implements Element {}
    record Section(int x, int y, Component label, String target, ItemStack icon) implements Element {}
    record Tile(int x, int y, int size, float scale, Component label, String target, ItemStack icon) implements Element {}
    record Leaf(String id, List<Element> left, List<Element> right) { List<Element> elements(boolean rightSide) { return rightSide ? right : left; } }
    private static final ResourceLocation MODIFY = ArmoryContent.id("textures/gui/book/modify.png");
    final List<Leaf> leaves = new ArrayList<>();
    final Map<String, Integer> locations = new LinkedHashMap<>();
    final ArmoryBookContents book;
    private final Font font;
    private List<Element> elements;

    ArmoryBookDocument(Font font, ArmoryBookContents book) {
        this.font = font; this.book = book;
        // Mantle IndexTransformer: first leaf is a section list on the right; its left side stays the inside cover.
        for (int start = 0; start < book.sections.size(); start += 9) {
            List<Element> index = new ArrayList<>();
            for (int count = 0; count < Math.min(9, book.sections.size() - start); count++) {
                var section = book.sections.get(start + count); int ox = (WIDTH - 136) / 2, oy = (HEIGHT - 136) / 2;
                index.add(new Section(ox + count % 3 * 47, oy + count / 3 * 47, section.title(), "section:" + section.id(), section.icon()));
            }
            add(start == 0 ? "index" : "index." + start / 9, index, index);
        }
        for (var section : book.sections) {
            locations.put("section:" + section.id(), leaves.size());
            if (section.id().equals("materials")) { materialIndex(section); }
            else if (section.id().equals("modifiers") || section.id().equals("accessories")) { listing(section); }
            for (var page : section.pages()) {
                var left = build(page, false); var right = page.type().equals("armormaterial") ? build(page, true) : left;
                add(page.id(), left, right);
            }
        }
    }
    private void add(String id, List<Element> left, List<Element> right) { locations.put(id, leaves.size()); leaves.add(new Leaf(id, List.copyOf(left), List.copyOf(right))); }
    int find(String target) { return locations.getOrDefault(target, 0); }
    private void materialIndex(ArmoryBookContents.Section section) {
        // ContentPageIconList: 20-pixel cells, 15-pixel margins and the original quarter-step fitting scale.
        for (int start = 0; start < section.pages().size(); start += 49) {
            elements = new ArrayList<>(); title(section.title(), false);
            int count = Math.min(49, section.pages().size() - start), size = 20; float scale = section.pages().size() > 49 ? 1 : 2.5F;
            while (scale > 1) { scale -= .25F; size = (int) (20 * scale); if ((WIDTH - 30) / size * ((HEIGHT - 20) / size) >= count) { break; } }
            int columns = (WIDTH - 30) / size;
            for (int i = 0; i < count; i++) {
                var page = section.pages().get(start + i); var material = TinkerMaterials.getMaterial(page.definition());
                var icon = material.getRepresentativeItem(); if (icon.isEmpty()) { icon = armor("helmet", material.identifier); }
                elements.add(new Tile(15 + i % columns * size, 20 + i / columns * size, size, scale, page.title(), page.id(), icon));
            }
            add(section.id() + ".overview" + start / 49, elements, elements);
        }
    }
    private void listing(ArmoryBookContents.Section section) {
        for (int start = 0; start < section.pages().size(); start += 30) {
            elements = new ArrayList<>(); title(section.title(), false);
            int count = Math.min(30, section.pages().size() - start), width = count * 9 + 20 > HEIGHT - 30 ? WIDTH / 2 : WIDTH;
            for (int i = 0; i < count; i++) {
                var page = section.pages().get(start + i); elements.add(new Link(i / 15 * width, 20 + i % 15 * 9, width, page.title(), page.id()));
            }
            add(section.id() + ".listing" + start / 30, elements, elements);
        }
    }
    private List<Element> build(ArmoryBookContents.Page page, boolean rightSide) {
        elements = new ArrayList<>();
        switch (page.type()) {
            case "armormaterial" -> material(page, rightSide);
            case "armor" -> armorPage(page);
            case "armormodifier", "armormodifier_polished" -> modifier(page);
            default -> textPage(page);
        }
        return List.copyOf(elements);
    }
    private void title(Component title, boolean shadow) {
        Component styled = title.copy().withStyle(ChatFormatting.UNDERLINE);
        elements.add(new Text((WIDTH - (int) Math.ceil(font.width(styled) * 1.2F)) / 2, 0, styled.getVisualOrderText(), 1.2F, shadow, null));
    }
    private void material(ArmoryBookContents.Page page, boolean rightSide) {
        // ContentArmorMaterial.build: preserve x/y/w, side column, stat order and trim/flavour baseline.
        var material = TinkerMaterials.getMaterial(page.definition()); var stats = ArmoryRegistry.material(page.definition());
        title(page.title(), true);
        int x = (rightSide ? 0 : 22) + 10, y = 25, width = WIDTH / 2 - 10;
        group(stats, 0, x, y, width); group(stats, 1, x + width, y, width - 10);
        y += 55 + 10 * stats.traits(0).size(); group(stats, 2, x, y, width - 10);
        String flavour = book.strings.get(material.identifier + ".flavour");
        if (flavour != null) { text(List.of(Component.literal("\"" + flavour + "\"").withStyle(ChatFormatting.ITALIC)), x + width, y, width - 16, 60); }
        int edge = rightSide ? WIDTH - 18 : 0; y = 10;
        List<Icon> display = new ArrayList<>();
        if (!material.getRepresentativeItem().isEmpty()) { display.add(new Icon(edge, y, List.of(material.getRepresentativeItem()), 1, null)); }
        if (material.isCraftable()) { display.add(new Icon(edge, y, List.of(new ItemStack(com.gctn.tconstruct.tables.WorkshopModule.BLOCKS.get(com.gctn.tconstruct.tables.WorkshopModule.Kind.PART_BUILDER).get())), 1, book.label("material.craft_partbuilder"))); }
        if (material.isCastable()) { display.add(new Icon(edge, y, List.of(new ItemStack(com.gctn.tconstruct.smeltery.SmelteryModule.BASIN.get())), 1, Component.literal(book.label("material.craft_casting").getString().replace("%s", material.getDisplayName().getString())))); }
        for (String type : ArmoryStationLayout.ARMOR) { display.add(new Icon(edge, y, List.of(armor(type, material.identifier)), 1, null)); }
        for (var icon : display) { elements.add(new Icon(edge, y, icon.items, 1, icon.tooltip)); y += 16; }
    }
    private void group(ArmorMaterialDefinition material, int part, int x, int y, int width) {
        String role = new String[]{"core", "plates", "trim"}[part];
        List<ItemStack> parts = new ArrayList<>();
        for (String id : part == 0 ? List.of("helmet_core", "chest_core", "leggings_core", "boots_core") : part == 1 ? List.of("armor_plate") : List.of("armor_trim")) { parts.add(ArmoryContent.PARTS.get(id).get().create(material.id)); }
        elements.add(new Icon(x, y + 1, parts, .5F, null));
        text(List.of(Component.translatable("stat." + role + ".name").withStyle(ChatFormatting.UNDERLINE)), x + 10, y, width - 10, 10);
        List<Component> rows = new ArrayList<>();
        if (part == 0) { rows.add(number("core.durability", material.coreDurability, 0x47cc47)); rows.add(number("core.defense", material.defense, 0xd76464)); }
        else if (part == 1) { rows.add(number("plates.modifier", material.plateMultiplier, 0xb9b95a)); rows.add(number("plates.durability", material.plateDurability, 0x47cc47)); rows.add(number("plates.toughness", material.toughness, 0x78a0cd)); }
        else { rows.add(number("trim.durability", material.trimDurability, 0x47cc47)); }
        for (var trait : material.traits(part)) {
            if (ArmoryRegistry.trait(trait.id()) == null) { continue; }
            rows.add(Component.translatable("trait." + trait.id().replace(':', '.') + ".name").append(trait.level() > 1 ? " " + trait.level() : "")
                    .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withUnderlined(true).withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("trait." + trait.id().replace(':', '.') + ".desc").withColor(TinkerMaterials.getMaterial(material.id).color)))));
        }
        text(rows, x, y + 12, width, HEIGHT);
    }
    private static Component number(String key, float value, int color) {
        return Component.translatable("stat." + key + ".name").append(": ").append(Component.literal(java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()).withColor(color))
                .withStyle(style -> style.withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("stat." + key + ".desc"))));
    }
    private void textPage(ArmoryBookContents.Page page) {
        int y = page.title().getString().isEmpty() ? 0 : 16;
        if (y != 0) { text(List.of(page.title().copy().withStyle(ChatFormatting.UNDERLINE)), 0, 0, WIDTH, 9); }
        if (page.type().equals("imageText2") && page.data().has("image")) {
            var image = page.data().getAsJsonObject("image"); int width = image.get("width").getAsInt(), height = image.get("height").getAsInt();
            elements.add(new Image((HEIGHT - width) / 2, y, width, height, ResourceLocation.parse(image.get("file").getAsString()), 0, 0, 256, 256, 256, 256, 0xffffff)); y += height + 5;
        }
        text(paragraphs(page.data(), "text"), 0, y, WIDTH, HEIGHT - y);
    }
    private void armorPage(ArmoryBookContents.Page page) {
        title(page.title(), false); text(paragraphs(page.data(), "text"), 5, 16, WIDTH - 10, WIDTH / 3 - 10);
        int imageX = WIDTH - 72 - 8, imageY = HEIGHT - 72 - 16, armorX = imageX + 28, armorY = imageY + 28;
        text(List.of(book.label("armor.properties").copy().withStyle(ChatFormatting.UNDERLINE)), 5, imageY - 6, 81, 10);
        text(bullets(page.data(), "properties"), 5, imageY + 4, WIDTH / 2 + 5, HEIGHT - imageY);
        image(imageX + 15, imageY + 28, 214, 0, 42, 46, 0xffffff); image(imageX, imageY, 0, 0, 72, 72, slotColor());
        List<ItemStack> armor = Arrays.stream(ArmoryStationLayout.ARMOR).map(ArmoryScreen::demo).toList();
        elements.add(new Icon(armorX, armorY, armor, 1, null)); image(armorX - 3, armorY - 3, 0, 75, 22, 22, 0xffffff);
        String material = exampleMaterial(); if (material == null) { return; }
        elements.add(new Icon(armorX - 21, armorY + 22, Arrays.stream(ArmoryStationLayout.CORES).map(part -> ArmoryContent.PARTS.get(part).get().create(material)).toList(), 1, null));
        elements.add(new Icon(armorX - 25, armorY - 4, List.of(ArmoryContent.PARTS.get("armor_plate").get().create(preferred("cobalt", material))), 1, null));
        elements.add(new Icon(armorX, armorY - 25, List.of(ArmoryContent.PARTS.get("armor_trim").get().create(preferred("ardite", material))), 1, null));
    }
    private void modifier(ArmoryBookContents.Page page) {
        int color = ArmorVisuals.entries().stream().filter(entry -> entry.id().equals(page.definition())).mapToInt(ArmorVisuals.Overlay::color).findFirst().orElse(0xdddddd);
        title(page.title().copy().withColor(color), true);
        List<Component> description = page.data().has("descriptionKey") ? List.of(Component.translatable(page.data().get("descriptionKey").getAsString())) : paragraphs(page.data(), "text");
        int h = WIDTH / 3 - 10; text(description, 10, 20, WIDTH - 20, h);
        text(List.of(book.label("modifier.effect").copy().withStyle(ChatFormatting.UNDERLINE)), 10, 20 + h, WIDTH / 2 - 5, 10);
        text(bullets(page.data(), "effects"), 10, 30 + h, WIDTH / 2 + 5, HEIGHT - h - 20);
        var inputs = ingredients(page); int count = Math.max(1, inputs.size()), imageWidth = count == 1 ? 22 : count == 2 ? 40 : 58, imageHeight = count < 4 ? 22 : 41;
        int v = count == 1 ? 75 : count == 2 ? 97 : count == 3 ? 119 : 141;
        int x = WIDTH / 2 + 20 + 29 - imageWidth / 2, y = HEIGHT / 2 + 30 + 20 - imageHeight / 2;
        image(x + (imageWidth - 42) / 2, y - 24, 214, 0, 42, 46, 0xffffff); image(x, y, 0, v, imageWidth, imageHeight, slotColor());
        String type = "chestplate";
        if (page.data().has("demoArmor") && !page.data().getAsJsonArray("demoArmor").isEmpty()) { type = ResourceLocation.parse(page.data().getAsJsonArray("demoArmor").get(0).getAsString()).getPath(); }
        ItemStack demo = ArmoryScreen.demo(type);
        if (ArmoryRegistry.modifier(page.definition()) != null && !demo.isEmpty()) {
            var changed = ArmoryWorkshop.applyModifier(demo, page.definition(), 1); if (!changed.isEmpty()) { demo = changed; }
        }
        elements.add(new Icon(x + (imageWidth - 16) / 2, y - 24, List.of(demo), 1, null)); image(x + (imageWidth - 22) / 2, y - 27, 0, 75, 22, 22, 0xffffff);
        int[] slotX = {3, 21, 39, 12, 30}, slotY = {3, 3, 3, 22, 22};
        for (int i = 0; i < inputs.size() && i < 5; i++) { elements.add(new Icon(x + slotX[i], y + slotY[i], inputs.get(i), 1, null)); }
    }
    private List<List<ItemStack>> ingredients(ArmoryBookContents.Page page) {
        List<List<ItemStack>> result = new ArrayList<>(); var level = Minecraft.getInstance().level;
        if (level != null) {
            for (var holder : level.getRecipeManager().getAllRecipesFor(ArmoryRecipes.MODIFIER_TYPE.get()).stream().sorted(Comparator.comparing(value -> value.id().toString())).toList()) {
                if (!holder.value().modifier().equals(page.definition())) { continue; }
                for (int i = 0; i < holder.value().ingredients().size(); i++) {
                    while (result.size() <= i) { result.add(new ArrayList<>()); }
                    var ingredient = holder.value().ingredients().get(i);
                    for (var item : ingredient.ingredient().getItems()) { result.get(i).add(item.copyWithCount(Math.min(64, ingredient.count()))); }
                }
            }
        }
        if (page.definition().equals("conarm:polished")) {
            result.add(ArmoryRegistry.materials().keySet().stream().map(id -> ArmoryContent.PARTS.get("polishing_kit").get().create(id)).toList()); result.add(List.of(new ItemStack(net.minecraft.world.item.Items.SAND)));
        } else if (page.definition().equals("conarm:extratrait")) {
            String material = exampleMaterial();
            if (material != null) { result.add(List.of(ArmoryContent.PARTS.get("armor_plate").get().create(material))); }
            for (String color : List.of("green", "blue", "magma")) { result.add(List.of(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("tconstruct:slime_crystal_" + color))))); }
            result.add(List.of(new ItemStack(net.minecraft.world.item.Items.GOLD_BLOCK)));
        }
        return result;
    }
    private int slotColor() { return book.appearance.has("slotColor") ? book.appearance.get("slotColor").getAsInt() : 0xff844c; }
    private void image(int x, int y, int u, int v, int width, int height, int color) { elements.add(new Image(x, y, width, height, MODIFY, u, v, width, height, 256, 256, color)); }
    private void text(List<Component> paragraphs, int x, int y, int width, int height) {
        int bottom = y + height;
        for (var paragraph : paragraphs) {
            var hover = paragraph.getStyle().getHoverEvent(); Component tooltip = hover == null ? null : hover.getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);
            for (var line : font.split(paragraph, width)) {
                if (y + 9 > bottom) { return; }
                elements.add(new Text(x, y, line, 1, false, tooltip)); y += 9;
            }
        }
    }
    private static List<Component> paragraphs(JsonObject data, String key) {
        if (!data.has(key)) { return List.of(); }
        var combined = Component.empty();
        for (var element : data.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) { combined.append(element.getAsString()); continue; }
            var value = element.getAsJsonObject(); if (!value.has("text")) { continue; }
            if (value.has("paragraph") && value.get("paragraph").getAsBoolean()) { combined.append("\n\n"); }
            var text = Component.literal(value.get("text").getAsString());
            for (var format : List.of("italic", "bold", "underlined")) {
                if (value.has(format) && value.get(format).getAsBoolean()) { text.withStyle(format.equals("italic") ? ChatFormatting.ITALIC : format.equals("bold") ? ChatFormatting.BOLD : ChatFormatting.UNDERLINE); }
            }
            combined.append(text);
        }
        return List.of(combined);
    }
    private static List<Component> bullets(JsonObject data, String key) {
        if (!data.has(key)) { return List.of(); }
        List<Component> result = new ArrayList<>(); for (var row : data.getAsJsonArray(key)) { result.add(Component.literal("● " + row.getAsString())); } return result;
    }
    private static String exampleMaterial() { return ArmoryRegistry.material("wood") == null ? ArmoryRegistry.materials().keySet().stream().findFirst().orElse(null) : "wood"; }
    private static String preferred(String id, String fallback) { return ArmoryRegistry.material(id) == null ? fallback : id; }
    private static ItemStack armor(String type, String material) { return ArmorData.create(ArmoryContent.ARMORS.get(type).get(), material, material, material); }
}
