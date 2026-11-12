/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArmoryConfig {
    public static final ModConfigSpec SERVER;
    public static final ModConfigSpec.BooleanValue SPAWN_WITH_BOOK, BOUNCE_DURABILITY, LEVELING;
    public static final ModConfigSpec.IntValue STARTING_SLOTS, MAX_LEVELS, BASE_XP, XP_CAP;
    public static final ModConfigSpec.DoubleValue LEVEL_MULTIPLIER, DAMAGE_TO_XP;
    public static final ModConfigSpec CLIENT;
    public static final ModConfigSpec.BooleanValue COMPACT_VIEW;
    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SPAWN_WITH_BOOK = server.comment("Give the Armory Addendum to players joining this world for the first time.").define("spawnWithBook", true);
        BOUNCE_DURABILITY = server.comment("Bounces spend one durability point, as in the original mod.").define("bounceDurability", true);
        server.push("leveling");
        LEVELING = server.comment("Native replacement for the optional 1.12 Tinkers' Tool Leveling integration.").define("enabled", false);
        STARTING_SLOTS = server.defineInRange("startingModifiers", 3, 0, 1024);
        MAX_LEVELS = server.comment("Zero uses the implementation ceiling of 1000 levels.").defineInRange("maximumLevels", 0, 0, 1000);
        BASE_XP = server.defineInRange("baseXp", 100, 1, 1000000000);
        LEVEL_MULTIPLIER = server.defineInRange("levelMultiplier", 2.0, 1.0, 100.0);
        XP_CAP = server.defineInRange("xpGainCap", 100, 1, 1000000);
        DAMAGE_TO_XP = server.defineInRange("damageToXp", .25, 0, 1000);
        server.pop(); SERVER = server.build();
        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        COMPACT_VIEW = client.comment("Hide the armor mannequin beside the station interface.").define("compactView", false);
        CLIENT = client.build();
    }
    private ArmoryConfig() {}
    public static boolean leveling() { return SERVER.isLoaded() && LEVELING.get(); }
    public static int startingSlots() { return leveling() ? STARTING_SLOTS.get() : 3; }
}
