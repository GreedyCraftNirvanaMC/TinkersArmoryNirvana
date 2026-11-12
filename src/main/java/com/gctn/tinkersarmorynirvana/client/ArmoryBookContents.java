/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Ports ArmoryBook's file repository and ArmorMaterial/Modifier/AccessorySectionTransformer, Copyright C4. */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tconstruct.library.TinkerMaterials;
import com.gctn.tconstruct.library.materials.Material;
import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.TinkersArmoryNirvana;
import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Loads the original index, section files, localized pages and book strings, without inventing a new chapter structure. */
final class ArmoryBookContents {
    record Section(String id, Component title, ItemStack icon, List<Page> pages) {}
    record Page(String id, String section, String type, Component title, JsonObject data, String definition) {}
    final List<Section> sections = new ArrayList<>();
    final Map<String, String> strings = new LinkedHashMap<>();
    final JsonObject appearance;
    private final String language = Minecraft.getInstance().getLanguageManager().getSelected();

    ArmoryBookContents() {
        readLanguage("en_us"); if (!language.equals("en_us")) { readLanguage(language); }
        appearance = read("appearance.json").getAsJsonObject();
        var localized = optional(language + "/appearance.json");
        if (localized != null) { localized.getAsJsonObject().entrySet().forEach(entry -> appearance.add(entry.getKey(), entry.getValue())); }
        for (var entry : read("index.json").getAsJsonArray()) {
            var definition = entry.getAsJsonObject(); String id = string(definition, "name", "");
            List<Page> pages = new ArrayList<>();
            if (id.equals("materials")) {
                for (Material material : TinkerMaterials.materials) {
                    if (!material.isHidden() && ArmoryRegistry.material(material.identifier) != null) {
                        pages.add(new Page(id + "." + material.identifier, id, "armormaterial", material.getDisplayName().copy().withColor(material.color), new JsonObject(), material.identifier));
                    }
                }
            } else if (definition.has("data")) {
                for (var source : read(definition.get("data").getAsString()).getAsJsonArray()) {
                    var page = source.getAsJsonObject(); String path = page.get("data").getAsString();
                    var data = localized(path);
                    String name = string(page, "name", path.substring(path.lastIndexOf('/') + 1, path.length() - 5));
                    String type = page.get("type").getAsString(), modifier = canonical(string(data, "armormodifier", name));
                    if (type.startsWith("armormodifier") && ArmoryRegistry.modifier(modifier) == null && !Set.of("conarm:polished", "conarm:extratrait").contains(modifier)) { continue; }
                    Component title = Component.literal(string(data, "title", ""));
                    if (type.startsWith("armormodifier")) { title = modifierTitle(modifier); }
                    if (type.equals("armor")) { title = label("armor.title"); }
                    pages.add(new Page(id + "." + name, id, type, title, data, modifier));
                }
            }
            if (id.equals("modifiers") || id.equals("accessories")) {
                for (var modifier : ArmoryRegistry.modifiers().values()) {
                    if (modifier.accessory() != id.equals("accessories") || pages.stream().anyMatch(page -> page.definition.equals(modifier.id))) { continue; }
                    var data = new JsonObject(); data.addProperty("descriptionKey", "modifier." + modifier.id.replace(':', '.') + ".desc");
                    pages.add(new Page(id + "." + modifier.id, id, "armormodifier", modifierTitle(modifier.id), data, modifier.id));
                }
            }
            String iconId = definition.getAsJsonObject("icon").getAsJsonObject("item").get("id").getAsString();
            sections.add(new Section(id, label(id), new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(iconId))), List.copyOf(pages)));
        }
    }
    Component label(String key) { return Component.literal(strings.getOrDefault(key, key)); }
    static String string(JsonObject data, String key, String fallback) { return data.has(key) ? data.get(key).getAsString() : fallback; }
    static String canonical(String id) {
        if (id.endsWith("_armor")) { id = id.substring(0, id.length() - 6); }
        id = switch (id) { case "travel_night" -> "night_vision"; case "travel_soul" -> "soul_sight"; default -> id; };
        return ArmoryRegistry.id(id);
    }
    private static Component modifierTitle(String id) {
        if (id.equals("conarm:polished")) { return Component.translatable("modifier.polished_armor.name"); }
        if (id.equals("conarm:extratrait")) { return Component.translatable("modifier.extratrait_armor.name"); }
        return Component.translatable("modifier." + id.replace(':', '.') + ".name");
    }
    JsonObject localized(String path) {
        var localized = optional(language + "/" + path);
        if (localized == null) { localized = read("en_us/" + path); }
        return localized.getAsJsonObject();
    }
    private void readLanguage(String locale) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(ArmoryContent.id("book/" + locale + "/language.lang"));
        if (resource.isEmpty()) { return; }
        try (var reader = new java.io.BufferedReader(resource.get().openAsReader())) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("//") || line.startsWith("#") || !line.contains("=")) { continue; }
                int split = line.indexOf('='); strings.put(line.substring(0, split), line.substring(split + 1).replace("\\n", "\n"));
            }
        } catch (java.io.IOException error) { TinkersArmoryNirvana.LOGGER.error("Cannot load Armory book language {}", locale, error); }
    }
    private static JsonElement optional(String path) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(ArmoryContent.id("book/" + path));
        if (resource.isEmpty()) { return null; }
        try (var reader = resource.get().openAsReader()) { return JsonParser.parseReader(reader); }
        catch (Exception error) { throw new IllegalStateException("Cannot load Armory book resource " + path, error); }
    }
    private static JsonElement read(String path) {
        var result = optional(path); if (result == null) { throw new IllegalStateException("Missing Armory book resource " + path); } return result;
    }
}
