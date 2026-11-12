/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Native configurable progression; the optional legacy Tool Leveling mod is not required. */
public final class ArmorLeveling {
    private ArmorLeveling() {}
    public static int required(int level) {
        if (!ArmoryConfig.SERVER.isLoaded()) { return 100; }
        return (int) Math.min(Integer.MAX_VALUE, ArmoryConfig.BASE_XP.get() * Math.pow(ArmoryConfig.LEVEL_MULTIPLIER.get(), Math.max(0, level)));
    }
    public static void damaged(ItemStack armor, Player player, float damage, net.minecraft.world.damagesource.DamageSource source) {
        if (!ArmoryConfig.leveling() || damage <= 0 || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) { return; }
        var tag = ArmorData.read(armor);
        int level = tag.getInt("Level"), max = ArmoryConfig.MAX_LEVELS.get();
        if (max > 0 && level >= max) { return; }
        long xp = (long) tag.getInt("Experience") + Math.max(1, Math.min(ArmoryConfig.XP_CAP.get(), (int) (damage * ArmoryConfig.DAMAGE_TO_XP.get())));
        int old = level;
        while (xp >= required(level) && (max == 0 || level < max) && level < 1000) { xp -= required(level); level++; }
        tag.putInt("Experience", (int) Math.min(Integer.MAX_VALUE, xp)); tag.putInt("Level", level); ArmorData.write(armor, tag);
        if (level != old) {
            ArmorData.rebuild(armor); player.displayClientMessage(Component.translatable("message.conarm.level_up", armor.getHoverName(), level), false);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, player.getSoundSource(), .6F, 1.3F);
        }
    }
}
