/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class ArmoryStationBlock extends BaseEntityBlock {
    public static final MapCodec<ArmoryStationBlock> CODEC = simpleCodec(ArmoryStationBlock::new);
    public ArmoryStationBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING))); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArmoryStationBlockEntity(pos, state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArmoryStationBlockEntity station) { player.openMenu(station); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ArmoryStationBlockEntity station) {
                Containers.dropContents(level, pos, station);
                var source = station.cancelDeconstruction();
                if (!source.isEmpty()) { Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), source); }
            }
            super.onRemove(state, level, pos, next, moving);
        }
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.or(box(0, 12, 0, 16, 16, 16), box(0, 0, 0, 4, 12, 4), box(12, 0, 0, 16, 12, 4), box(0, 0, 12, 4, 12, 16), box(12, 0, 12, 16, 12, 16));
    }
}
