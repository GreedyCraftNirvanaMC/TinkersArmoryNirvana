/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Coordinates ported from GuiButtonArmorRepair and ArmorBuildGuiInfo.default3Part, Copyright 2018-2020 C4. */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.world.item.Item;

/** Common-side selection and coordinates keep server slot restrictions and the original GUI in agreement. */
public final class ArmoryStationLayout {
    public static final int REPAIR = 0;
    public static final String[] ARMOR = {"helmet", "chestplate", "leggings", "boots"};
    public static final String[] CORES = {"helmet_core", "chest_core", "leggings_core", "boots_core"};
    private static final int[][] REPAIR_POSITIONS = {{33, 42}, {15, 62}, {11, 37}, {33, 19}, {55, 37}, {51, 62}};
    private static final int[][] BUILD_POSITIONS = {{29, 52}, {9, 32}, {49, 32}};
    private ArmoryStationLayout() {}
    public static boolean valid(int selection) { return selection >= 0 && selection <= ARMOR.length; }
    public static int activeSlots(int selection) { return selection == REPAIR ? 6 : 3; }
    public static int[] position(int selection, int slot, int dormantIndex) {
        if (selection == REPAIR) { return REPAIR_POSITIONS[slot].clone(); }
        return slot < 3 ? BUILD_POSITIONS[slot].clone() : new int[]{87 + 20 * dormantIndex, 62};
    }
    public static Item requiredPart(int selection, int slot) {
        if (selection == REPAIR || slot >= 3) { return null; }
        return ArmoryContent.PARTS.get(slot == 0 ? CORES[selection - 1] : slot == 1 ? "armor_plate" : "armor_trim").get();
    }
}
