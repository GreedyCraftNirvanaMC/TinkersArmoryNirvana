/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.station.AccessoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 168; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        ArmoryScreen.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { if (slot.isActive()) { ArmoryScreen.slot(graphics, leftPos + slot.x, topPos + slot.y); } }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) { super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY); }
}
