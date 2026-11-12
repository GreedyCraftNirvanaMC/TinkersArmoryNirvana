/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.api;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Common-side visual metadata; register addon overlays during mod construction, before armor materials register. */
public final class ArmorVisuals {
    public record Overlay(String id, String texture, int color, String geometry) {}
    private static final Map<String, Overlay> OVERLAYS = new LinkedHashMap<>();
    private static boolean frozen;
    private static List<Overlay> snapshot = List.of();
    static {
        String[] names = {"diamond", "emerald", "reinforced", "resistant", "fire_resistant", "blast_resistant", "projectile_resistant",
                "speedy", "shulkerweight", "mending", "parasitic", "sticky", "amphibious", "glowing", "high_stride", "frostwalker", "powerful", "dexterous", "telekinetic", "soulbound", "polished"};
        int[] colors = {0x8cf4e2, 0x41f384, 0x502e83, 0xfff6f6, 0xea9e32, 0x862d2d, 0x10574b,
                0x910000, 0xaaccff, 0x43ab32, 0x5e0000, 0xffffff, 0x00ccff, 0xffffaa, 0x8c8c8c, 0x009999, 0xfff6f6, 0x660000, 0xccccff, 0xf5fbac, 0xffffff};
        for (int i = 0; i < names.length; i++) { register("conarm:" + names[i], "conarm:models/modifiers/mod_" + names[i] + "_armor", colors[i], "armor"); }
        register("conarm:travel_belt", "conarm:models/accessories/travel_belt", 0xffffff, "belt");
        register("conarm:potion_belt", "conarm:models/accessories/travel_belt", 0xffffff, "belt");
        register("conarm:travel_sack", "conarm:models/accessories/travel_sack", 0xffffff, "sack");
        for (String id : List.of("travel_goggles", "night_vision", "soul_sight")) { register("conarm:" + id, "conarm:models/accessories/travel_goggles", 0xffffff, "goggles"); }
        for (String id : List.of("travel_sneak", "travel_slowfall")) { register("conarm:" + id, "conarm:models/accessories/travel_cloak", 0xffffff, "cloak"); }
    }
    private ArmorVisuals() {}
    public static void register(String modifier, String texture, int color, String geometry) {
        if (frozen) { throw new IllegalStateException("Register armor visuals before armor material registration"); }
        ResourceLocation.parse(texture);
        if (!List.of("armor", "belt", "sack", "goggles", "cloak").contains(geometry)) { throw new IllegalArgumentException("Unknown overlay geometry"); }
        String id = ArmoryRegistry.id(modifier);
        if (OVERLAYS.putIfAbsent(id, new Overlay(id, texture, color | 0xff000000, geometry)) != null) { throw new IllegalArgumentException("Duplicate armor visual: " + id); }
        snapshot = List.copyOf(OVERLAYS.values());
    }
    public static List<Overlay> entries() { return snapshot; }
    public static List<net.minecraft.world.item.ArmorMaterial.Layer> layers() {
        frozen = true;
        List<net.minecraft.world.item.ArmorMaterial.Layer> layers = new ArrayList<>();
        for (String name : List.of("armor_core", "armor_plates", "armor_trim")) { layers.add(new net.minecraft.world.item.ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("conarm", name), "", true)); }
        for (var overlay : OVERLAYS.values()) { layers.add(new net.minecraft.world.item.ArmorMaterial.Layer(ResourceLocation.parse(overlay.id()), "", true)); }
        return List.copyOf(layers);
    }
}
