/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Selection, dormant slots and deconstruction behavior adapted from C4's ContainerArmorStation. */
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
    private int positionedSelection = -1;
    public ArmoryMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(6), null, new SimpleContainerData(4)); }
    public ArmoryMenu(int id, Inventory inventory, ArmoryStationBlockEntity station) {
        this(id, inventory, station, station, new SimpleContainerData(4));
    }
    private ArmoryMenu(int id, Inventory inventory, Container input, ArmoryStationBlockEntity station, ContainerData data) {
        super(ArmoryContent.STATION_MENU.get(), id);
        this.input = input; this.station = station; this.inventory = inventory; this.data = data;
        addDataSlots(data);
        addSlot(new Slot(output, 0, 124, 38) {
            @Override public boolean mayPlace(ItemStack stack) { return station != null ? station.canDeconstruct(stack) : selection() == 0 && emptyInputs() && stack.getItem() instanceof com.gctn.tinkersarmorynirvana.armor.ModularArmorItem && !stack.isDamaged(); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPickup(Player player) { return station == null ? hasItem() : deconstructing() ? !station.source().isEmpty() : plan != null && plan.valid(input) && hasItem(); }
            @Override public void set(ItemStack stack) {
                if (station != null && !stack.isEmpty() && station.beginDeconstruction(stack)) { refresh(); return; }
                if (station != null && stack.isEmpty() && deconstructing()) { station.cancelDeconstruction(); }
                super.set(stack);
            }
            @Override public ItemStack remove(int count) {
                if (station != null && deconstructing()) { var source = station.cancelDeconstruction(); refresh(); return source; }
                return super.remove(count);
            }
            @Override public void onTake(Player player, ItemStack stack) {
                if (station != null && plan != null && plan.valid(input)) {
                    var returned = plan.commit(input); plan = null;
                    for (var remainder : returned) { if (!inventory.add(remainder)) { player.drop(remainder, false); } }
                    station.setChanged();
                    player.level().playSound(null, station.getBlockPos(), forge() ? net.minecraft.sounds.SoundEvents.ANVIL_USE : ArmoryContent.SAW.get(), player.getSoundSource(), forge() ? .9F : .8F, forge() ? .95F + player.getRandom().nextFloat() * .2F : .8F + player.getRandom().nextFloat() * .4F);
                }
                refresh(); super.onTake(player, stack);
            }
        });
        for (int i = 0; i < 6; i++) { addSlot(inputSlot(i, 0, 0)); }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 92 + row * 18)); } }
        for (int i = 0; i < 9; i++) { addSlot(new Slot(inventory, i, 8 + i * 18, 150)); }
        refresh(); updateSlotPositions();
    }
    private Slot inputSlot(int slot, int x, int y) {
        return new Slot(input, slot, x, y) {
            @Override public ItemStack getItem() { return station != null && station.deconstructing() ? station.preview(slot) : super.getItem(); }
            @Override public boolean isActive() { return slot < ArmoryStationLayout.activeSlots(selection()) || hasItem(); }
            @Override public boolean mayPlace(ItemStack stack) {
                if (deconstructing() || slot >= ArmoryStationLayout.activeSlots(selection())) { return false; }
                var required = ArmoryStationLayout.requiredPart(selection(), slot); return required == null || stack.is(required);
            }
            @Override public ItemStack remove(int count) {
                if (station != null && station.deconstructing()) { station.takeDeconstructedParts(); refresh(); }
                return super.remove(count);
            }
            @Override public void setChanged() { super.setChanged(); refresh(); }
        };
    }
    public boolean forge() { return station == null ? data.get(0) != 0 : station.forge(); }
    public int selection() { return station == null ? data.get(1) : station.selection(); }
    public boolean deconstructing() { return station == null ? data.get(2) != 0 : station.deconstructing(); }
    public boolean needsForge() { return data.get(3) != 0; }
    public boolean emptyInputs() { for (int i = 0; i < 6; i++) { if (!input.getItem(i).isEmpty()) { return false; } } return true; }
    public ItemStack shownArmor() { var result = getSlot(0).getItem(); return result.isEmpty() ? getSlot(1).getItem() : result; }
    public void rename(String value) { name = net.minecraft.util.StringUtil.filterText(value).strip(); if (name.length() > 50) { name = name.substring(0, 50); } refresh(); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (station == null || !stillValid(player) || !ArmoryStationLayout.valid(button) || deconstructing()) { return false; }
        station.selection(button); refresh(); broadcastChanges(); return true;
    }
    public void updateSlotPositions() {
        if (slots.size() < 7) { return; }
        int dormant = 0;
        for (int i = 0; i < 6; i++) {
            var current = slots.get(i + 1);
            int[] position = ArmoryStationLayout.position(selection(), i, dormant);
            if (i >= ArmoryStationLayout.activeSlots(selection()) && current.hasItem()) { dormant++; }
            if (current.x != position[0] || current.y != position[1]) {
                // Slot.x/y 在 1.21 中是 final；用同容器、同索引的新 Slot 替换，避免 AT 或反射。
                var moved = inputSlot(i, position[0], position[1]); moved.index = current.index; slots.set(i + 1, moved);
            }
        }
        positionedSelection = selection();
    }
    @Override public void setData(int id, int value) { super.setData(id, value); if (id == 1) { updateSlotPositions(); } }
    private void refresh() {
        if (station == null || inventory.player.level().isClientSide) { return; }
        data.set(0, station.forge() ? 1 : 0); data.set(1, station.selection()); data.set(2, station.deconstructing() ? 1 : 0); data.set(3, 0);
        if (station.deconstructing()) { plan = null; output.setItem(0, station.source()); return; }
        plan = ArmoryWorkshop.plan(input, inventory.player.level(), forge());
        if (plan == null && !forge() && ArmoryWorkshop.plan(input, inventory.player.level(), true) != null) { data.set(3, 1); }
        if (plan == null) { plan = ArmoryWorkshop.rename(input, name); }
        ItemStack result = plan == null ? ItemStack.EMPTY : plan.result();
        if (!name.isBlank() && !result.isEmpty()) { result.set(DataComponents.CUSTOM_NAME, Component.literal(name)); }
        output.setItem(0, result);
        if (positionedSelection != selection()) { updateSlotPositions(); }
    }
    @Override public void broadcastChanges() { refresh(); updateSlotPositions(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return station == null || input.stillValid(player); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index != 0 && !deconstructing() && super.canTakeItemForPickAll(stack, slot); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        if (station != null && deconstructing() && index > 0 && index < 7) {
            ItemStack part = slot.getItem(); boolean room = false;
            for (int i = 7; i < 43; i++) {
                var target = getSlot(i).getItem();
                if (target.isEmpty() || ItemStack.isSameItemSameComponents(target, part) && target.getCount() < target.getMaxStackSize()) { room = true; break; }
            }
            if (!room) { return ItemStack.EMPTY; }
            station.takeDeconstructedParts(); refresh(); slot = slots.get(index);
        }
        ItemStack raw = slot.getItem(), original = raw.copy();
        if (index < 7) {
            if (!moveItemStackTo(raw, 7, 43, true)) { return ItemStack.EMPTY; }
            if (index == 0) { slot.onQuickCraft(raw, original); }
        } else if (!moveItemStackTo(raw, 1, 7, false)) { return ItemStack.EMPTY; }
        if (raw.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        if (raw.getCount() == original.getCount()) { return ItemStack.EMPTY; }
        slot.onTake(player, raw); return original;
    }
}
