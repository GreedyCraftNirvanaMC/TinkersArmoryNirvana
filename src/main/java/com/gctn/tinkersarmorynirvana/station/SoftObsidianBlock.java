/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SoftObsidianBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty AGE = net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3;
    public SoftObsidianBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(AGE, 0)); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) { level.scheduleTick(pos, this, 60 + level.random.nextInt(61)); }
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0 || neighbors(level, pos) < 4) { soften(state, level, pos, random, true); }
        else { level.scheduleTick(pos, this, 20 + random.nextInt(21)); }
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { tick(state, level, pos, random); }
    private int neighbors(net.minecraft.world.level.Level level, BlockPos pos) {
        int count = 0; for (var direction : net.minecraft.core.Direction.values()) { if (level.getBlockState(pos.relative(direction)).is(this)) { count++; } } return count;
    }
    private void soften(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, boolean adjacent) {
        if (state.getValue(AGE) < 3) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2); level.scheduleTick(pos, this, 20 + random.nextInt(21));
        } else {
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            if (adjacent) {
                for (var direction : net.minecraft.core.Direction.values()) {
                    BlockPos target = pos.relative(direction); BlockState neighbor = level.getBlockState(target);
                    if (neighbor.is(this)) { soften(neighbor, level, target, random, false); }
                }
            }
        }
    }
    @Override protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && block == this && neighbors(level, pos) < 2) { level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState()); }
    }
    @Override public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        Blocks.MAGMA_BLOCK.stepOn(level, pos, Blocks.MAGMA_BLOCK.defaultBlockState(), entity);
    }
}
