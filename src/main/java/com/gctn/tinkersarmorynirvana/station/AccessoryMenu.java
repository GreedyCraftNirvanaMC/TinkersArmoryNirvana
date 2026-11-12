/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.armor.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The inventory remains attached to the exact equipped stack; no client-provided item data is accepted. */
public class AccessoryMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final ItemStack armor;
    private final EquipmentSlot equipment;
    private final String accessory;
    private final SimpleContainer storage;
    private final ContainerData data = new SimpleContainerData(1);
    private boolean loading;
    public AccessoryMenu(int id, Inventory inventory) { this(id, inventory, null, ""); }
    public AccessoryMenu(int id, Inventory inventory, EquipmentSlot equipment, String accessory) {
        super(ArmoryContent.ACCESSORY_MENU.get(), id);
        this.inventory = inventory; this.equipment = equipment; this.accessory = accessory;
        armor = equipment == null ? ItemStack.EMPTY : inventory.player.getItemBySlot(equipment);
        storage = new SimpleContainer(27) { @Override public void setChanged() { super.setChanged(); save(); } };
        data.set(0, accessory.equals("potion_belt") ? 1 : 0); addDataSlots(data);
        if (equipment != null) { load(); }
        for (int i = 0; i < 27; i++) { addSlot(storageSlot(i, 8 + (i % 9) * 18, 18 + (i / 9) * 18)); }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18)); } }
        for (int i = 0; i < 9; i++) { addSlot(new Slot(inventory, i, 8 + i * 18, 143)); }
        updateSlotPositions();
    }
    private Slot storageSlot(int index, int x, int y) {
        return new Slot(storage, index, x, y) {
                @Override public boolean isActive() { return !potions() || index < 7; }
                @Override public boolean mayPlace(ItemStack stack) {
                    if (!isActive() || !stack.canFitInsideContainerItems() || stack.getItem() instanceof ModularArmorItem) { return false; }
                    return !potions() || stack.getItem() instanceof net.minecraft.world.item.PotionItem;
                }
            };
    }
    @Override public void setData(int id, int value) { super.setData(id, value); updateSlotPositions(); }
    private void updateSlotPositions() {
        if (slots.size() != 63) { return; }
        for (int i = 0; i < 63; i++) {
            var current = slots.get(i); Slot moved;
            if (i < 27) { moved = storageSlot(i, potions() ? 26 + i * 18 : 8 + i % 9 * 18, potions() ? 20 : 18 + i / 9 * 18); }
            else if (i < 54) { int index = i - 27; moved = new Slot(inventory, 9 + index, 8 + index % 9 * 18, (potions() ? 51 : 85) + index / 9 * 18); }
            else { int index = i - 54; moved = new Slot(inventory, index, 8 + index * 18, potions() ? 109 : 143); }
            moved.index = current.index; slots.set(i, moved);
        }
    }
    public boolean potions() { return data.get(0) != 0; }
    private void load() {
        loading = true;
        try {
            var stored = ArmorStorage.read(armor).getCompound("Inventories").getCompound(accessory);
            for (int i = 0; i < 27; i++) { storage.setItem(i, ItemStack.parseOptional(inventory.player.registryAccess(), stored.getCompound(Integer.toString(i)))); }
        } finally { loading = false; }
    }
    private void save() {
        if (loading || armor.isEmpty() || inventory.player.level().isClientSide) { return; }
        CompoundTag tag = ArmorStorage.read(armor), inventories = tag.getCompound("Inventories"), saved = new CompoundTag();
        for (int i = 0; i < 27; i++) { if (!storage.getItem(i).isEmpty()) { saved.put(Integer.toString(i), storage.getItem(i).save(inventory.player.registryAccess())); } }
        inventories.put(accessory, saved); tag.put("Inventories", inventories); ArmorStorage.write(armor, tag);
    }
    @Override public boolean stillValid(Player player) {
        return equipment == null || player.isAlive() && player.getItemBySlot(equipment) == armor && !ArmorData.broken(armor) && ArmorData.level(armor, accessory) > 0;
    }
    @Override public void removed(Player player) { save(); super.removed(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index); if (!slot.hasItem() || !slot.isActive()) { return ItemStack.EMPTY; }
        ItemStack raw = slot.getItem(), original = raw.copy();
        if (index < 27 ? !moveItemStackTo(raw, 27, 63, true) : !moveItemStackTo(raw, 0, potions() ? 7 : 27, false)) { return ItemStack.EMPTY; }
        if (raw.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, raw); save(); return original;
    }
    public static void swapHotbar(Player player, ItemStack armor) {
        CompoundTag tag = ArmorStorage.read(armor), inventories = tag.getCompound("Inventories"), saved = inventories.getCompound("travel_belt");
        CompoundTag replacement = new CompoundTag();
        for (int i = 0; i < 9; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current.getItem() instanceof ModularArmorItem || !current.canFitInsideContainerItems()) { return; }
            if (!current.isEmpty()) { replacement.put(Integer.toString(i), current.save(player.registryAccess())); }
        }
        for (int i = 0; i < 9; i++) { player.getInventory().setItem(i, ItemStack.parseOptional(player.registryAccess(), saved.getCompound(Integer.toString(i)))); }
        inventories.put("travel_belt", replacement); tag.put("Inventories", inventories); ArmorStorage.write(armor, tag);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
    }
}
