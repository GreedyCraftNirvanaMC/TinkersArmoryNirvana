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
    private ItemStack deconstructing = ItemStack.EMPTY;
    private final NonNullList<ItemStack> partPreview = NonNullList.withSize(6, ItemStack.EMPTY);
    private int selection;
    public ArmoryStationBlockEntity(BlockPos pos, BlockState state) { super(ArmoryContent.STATION_ENTITY.get(), pos, state); }
    public boolean forge() { return getBlockState().is(ArmoryContent.FORGE.get()); }
    @Override public int getContainerSize() { return 6; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable(forge() ? "block.conarm.armorforge" : "block.conarm.armorstation"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ArmoryMenu(id, inventory, this); }
    public int selection() { return selection; }
    public void selection(int value) { if (ArmoryStationLayout.valid(value) && !deconstructing()) { selection = value; setChanged(); } }
    public boolean deconstructing() { return !deconstructing.isEmpty(); }
    public ItemStack source() { return deconstructing; }
    public ItemStack preview(int slot) { return partPreview.get(slot); }
    public boolean canDeconstruct(ItemStack stack) {
        if (!com.gctn.tconstruct.Config.DECONSTRUCT_TOOLS.get() || selection != 0 || deconstructing() || !isEmpty()
                || !com.gctn.tinkersarmorynirvana.armor.ArmorData.initialized(stack)
                || com.gctn.tinkersarmorynirvana.armor.ArmorData.broken(stack) || stack.isDamaged()) { return false; }
        // 分解会丢弃强化；有实际配件库存的护甲必须先取空，避免把玩家的物品一起销毁。
        var inventories = com.gctn.tinkersarmorynirvana.armor.ArmorStorage.read(stack).getCompound("Inventories");
        for (String key : inventories.getAllKeys()) { if (!inventories.getCompound(key).isEmpty()) { return false; } }
        return true;
    }
    public boolean beginDeconstruction(ItemStack stack) {
        if (!canDeconstruct(stack)) { return false; }
        deconstructing = stack.copyWithCount(1); rebuildPreview(); setChanged(); return true;
    }
    private void rebuildPreview() {
        partPreview.clear();
        if (!com.gctn.tinkersarmorynirvana.armor.ArmorData.initialized(deconstructing)) { return; }
        var data = com.gctn.tinkersarmorynirvana.armor.ArmorData.read(deconstructing);
        var armor = (com.gctn.tinkersarmorynirvana.armor.ModularArmorItem) deconstructing.getItem();
        String core = switch (armor.getEquipmentSlot()) { case HEAD -> "helmet_core"; case CHEST -> "chest_core"; case LEGS -> "leggings_core"; default -> "boots_core"; };
        String[] parts = {core, "armor_plate", "armor_trim"}, keys = {"Core", "Plates", "Trim"};
        for (String key : keys) { if (com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.material(data.getString(key)) == null) { return; } }
        for (int i = 0; i < 3; i++) { partPreview.set(i, ArmoryContent.PARTS.get(parts[i]).get().create(data.getString(keys[i]))); }
    }
    public ItemStack cancelDeconstruction() {
        ItemStack result = deconstructing; deconstructing = ItemStack.EMPTY; partPreview.clear(); setChanged(); return result;
    }
    public void takeDeconstructedParts() {
        if (!deconstructing()) { return; }
        // 所有预览部件一次性实化，同时销毁唯一原件。其余槽位中的预览从来不是额外的真实库存。
        for (int i = 0; i < 6; i++) { items.set(i, partPreview.get(i).copy()); }
        deconstructing = ItemStack.EMPTY; partPreview.clear(); setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Selection", selection);
        if (!deconstructing.isEmpty()) { tag.put("DeconstructingStack", deconstructing.save(registries)); }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items = NonNullList.withSize(6, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, registries);
        selection = ArmoryStationLayout.valid(tag.getInt("Selection")) ? tag.getInt("Selection") : 0;
        deconstructing = ItemStack.parseOptional(registries, tag.getCompound("DeconstructingStack")); rebuildPreview();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 6 && !deconstructing(); }
}
