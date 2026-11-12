/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArmoryStationBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    public ArmoryStationBlockEntity(BlockPos pos, BlockState state) { super(ArmoryContent.STATION_ENTITY.get(), pos, state); }
    public boolean forge() { return getBlockState().is(ArmoryContent.FORGE.get()); }
    @Override public int getContainerSize() { return 6; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable(forge() ? "block.conarm.armorforge" : "block.conarm.armorstation"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ArmoryMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); items = NonNullList.withSize(6, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, registries); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < (forge() ? 6 : 3); }
}
