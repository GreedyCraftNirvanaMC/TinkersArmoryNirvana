/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class ArmoryMenu extends AbstractContainerMenu {
    private final Container input;
    private final ResultContainer output = new ResultContainer();
    private final ArmoryStationBlockEntity station;
    private final Inventory inventory;
    private final ContainerData data;
    private ArmoryWorkshop.Plan plan;
    private String name = "";
    public ArmoryMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(6), null, new SimpleContainerData(1)); }
    public ArmoryMenu(int id, Inventory inventory, ArmoryStationBlockEntity station) {
        this(id, inventory, station, station, new SimpleContainerData(1)); data.set(0, station.forge() ? 1 : 0);
    }
    private ArmoryMenu(int id, Inventory inventory, Container input, ArmoryStationBlockEntity station, ContainerData data) {
        super(ArmoryContent.STATION_MENU.get(), id); this.input = input; this.station = station; this.inventory = inventory; this.data = data;
        addDataSlots(data);
        addSlot(new Slot(output, 0, 143, 47) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return station == null ? hasItem() : plan != null && plan.valid(input) && !getItem().isEmpty(); }
            @Override public void onTake(Player player, ItemStack stack) {
                if (station != null && plan != null && plan.valid(input)) {
                    var returned = plan.commit(input); plan = null;
                    for (var remainder : returned) { if (!inventory.add(remainder)) { player.drop(remainder, false); } }
                    station.setChanged(); refresh();
                }
                super.onTake(player, stack);
            }
        });
        for (int i = 0; i < 6; i++) {
            final int index = i;
            addSlot(new Slot(input, i, 17 + (i % 3) * 22, 35 + (i / 3) * 22) {
                @Override public boolean isActive() { return index < (forge() ? 6 : 3); }
                @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
                @Override public void setChanged() { super.setChanged(); refresh(); }
            });
        }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 100 + row * 18)); } }
        for (int i = 0; i < 9; i++) { addSlot(new Slot(inventory, i, 8 + i * 18, 158)); }
        refresh();
    }
    public boolean forge() { return data.get(0) != 0; }
    public void rename(String value) { name = net.minecraft.util.StringUtil.filterText(value).strip(); if (name.length() > 50) { name = name.substring(0, 50); } refresh(); }
    private void refresh() {
        if (station == null || inventory.player.level().isClientSide) { return; }
        plan = ArmoryWorkshop.plan(input, inventory.player.level());
        ItemStack result = plan == null ? ItemStack.EMPTY : plan.result();
        if (!name.isBlank() && !result.isEmpty()) { result.set(DataComponents.CUSTOM_NAME, Component.literal(name)); }
        output.setItem(0, result);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return station == null || input.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        ItemStack raw = slot.getItem(), original = raw.copy();
        if (index == 0) {
            if (!moveItemStackTo(raw, 7, 43, true)) { return ItemStack.EMPTY; }
            slot.onQuickCraft(raw, original);
        } else if (index < 7) {
            if (!moveItemStackTo(raw, 7, 43, true)) { return ItemStack.EMPTY; }
        } else if (!moveItemStackTo(raw, 1, forge() ? 7 : 4, false)) { return ItemStack.EMPTY; }
        if (raw.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        if (raw.getCount() == original.getCount()) { return ItemStack.EMPTY; }
        slot.onTake(player, raw); return original;
    }
}
