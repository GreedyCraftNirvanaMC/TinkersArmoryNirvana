/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArmoryConfig {
    public static final ModConfigSpec SERVER;
    public static final ModConfigSpec.BooleanValue SPAWN_WITH_BOOK, BOUNCE_DURABILITY;
    public static final ModConfigSpec CLIENT;
    public static final ModConfigSpec.BooleanValue COMPACT_VIEW;
    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SPAWN_WITH_BOOK = server.comment("Give the Armory Addendum to players joining this world for the first time.").define("spawnWithBook", true);
        BOUNCE_DURABILITY = server.comment("Bounces spend one durability point, as in the original mod.").define("bounceDurability", true);
        SERVER = server.build();
        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        COMPACT_VIEW = client.comment("Hide the armor mannequin beside the station interface.").define("compactView", false);
        CLIENT = client.build();
    }
    private ArmoryConfig() {}
}
