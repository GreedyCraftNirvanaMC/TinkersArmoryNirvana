/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public final class FrostWalking {
    private FrostWalking() {}
    public static void tick(ArmorContext ctx) {
        if (!ctx.wearer.onGround() || ctx.wearer.isShiftKeyDown()) { return; }
        BlockPos origin = ctx.wearer.blockPosition();
        if (ctx.state.getLong("Position") == origin.asLong()) { return; }
        ctx.state.putLong("Position", origin.asLong()); ctx.save();
        int radius = ctx.level + 2;
        var level = ctx.wearer.level();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, -1, radius))) {
            if (pos.distToCenterSqr(ctx.wearer.position()) > radius * radius || !level.isEmptyBlock(pos.above())) { continue; }
            var fluid = level.getFluidState(pos);
            if (!level.getBlockState(pos).is(Blocks.WATER) && !level.getBlockState(pos).is(Blocks.LAVA)) { continue; }
            if (!fluid.isSource()) { continue; }
            var frozen = fluid.is(Fluids.WATER) ? Blocks.FROSTED_ICE : ctx.level > 1 && fluid.is(Fluids.LAVA) ? ArmoryContent.SOFT_OBSIDIAN.get() : null;
            if (frozen == null || !frozen.defaultBlockState().canSurvive(level, pos)) { continue; }
            if (!net.neoforged.neoforge.event.EventHooks.onBlockPlace(ctx.wearer,
                    net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos), net.minecraft.core.Direction.UP)) {
                level.setBlockAndUpdate(pos, frozen.defaultBlockState());
                level.scheduleTick(pos.immutable(), frozen, 60 + ctx.wearer.getRandom().nextInt(61));
            }
        }
    }
}
