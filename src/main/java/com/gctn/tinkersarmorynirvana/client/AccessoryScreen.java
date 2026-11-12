/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.station.AccessoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    private static final net.minecraft.resources.ResourceLocation CHEST = net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final net.minecraft.resources.ResourceLocation POTIONS = com.gctn.tinkersarmorynirvana.ArmoryContent.id("textures/gui/potionbelt.png");
    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 168; inventoryLabelY = 74; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        if (menu.potions()) { graphics.blit(POTIONS, leftPos, topPos, 0, 0, 176, 133); }
        else { graphics.blit(CHEST, leftPos, topPos, 0, 0, 176, 71); graphics.blit(CHEST, leftPos, topPos + 71, 0, 126, 176, 96); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int expected = menu.potions() ? 133 : 168;
        if (imageHeight != expected) { imageHeight = expected; inventoryLabelY = expected - 94; init(minecraft, width, height); }
        super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (ArmoryClient.accessoryKey(menu.potions() ? net.minecraft.world.entity.EquipmentSlot.LEGS : net.minecraft.world.entity.EquipmentSlot.CHEST).matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
