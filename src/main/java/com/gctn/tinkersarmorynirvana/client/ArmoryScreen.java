/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.station.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ArmoryScreen extends AbstractContainerScreen<ArmoryMenu> {
    private net.minecraft.client.player.RemotePlayer mannequin;
    public ArmoryScreen(ArmoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 182; inventoryLabelY = 89;
        if (!com.gctn.tinkersarmorynirvana.ArmoryConfig.CLIENT.isLoaded() || !com.gctn.tinkersarmorynirvana.ArmoryConfig.COMPACT_VIEW.get()) { imageWidth = 286; }
    }
    @Override protected void init() {
        super.init();
        EditBox name = new EditBox(font, leftPos + 86, topPos + 16, 80, 16, Component.translatable("gui.conarm.name"));
        name.setMaxLength(50); name.setHint(Component.translatable("gui.conarm.name"));
        name.setResponder(value -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArmoryNetwork.Name(menu.containerId, value)));
        addRenderableWidget(name);
        if (imageWidth > 176 && minecraft.player != null && minecraft.level != null) { mannequin = new net.minecraft.client.player.RemotePlayer(minecraft.level, minecraft.player.getGameProfile()); }
    }
    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xff35302c); graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xffd1c6b0);
    }
    static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff766d60); graphics.fill(x, y, x + 16, y + 16, 0xffa79b86);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) { if (slot.isActive()) { slot(graphics, leftPos + slot.x, topPos + slot.y); } }
        graphics.drawString(font, "→", leftPos + 105, topPos + 50, 0xff514236, false);
        graphics.drawString(font, Component.translatable("gui.conarm.inputs"), leftPos + 16, topPos + 22, 0xff514236, false);
        if (mannequin != null) {
            for (var equipment : java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)) {
                mannequin.setItemSlot(equipment, minecraft.player.getItemBySlot(equipment).copy());
            }
            var result = menu.getSlot(0).getItem();
            if (result.getItem() instanceof com.gctn.tinkersarmorynirvana.armor.ModularArmorItem armor) { mannequin.setItemSlot(armor.getEquipmentSlot(), result.copy()); }
            graphics.fill(leftPos + 181, topPos + 14, leftPos + 278, topPos + 174, 0xffb9ad97);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    leftPos + 184, topPos + 17, leftPos + 275, topPos + 171, 56, .0625F, mouseX, mouseY, mannequin);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 10 && mouseX <= leftPos + 85 && mouseY >= topPos + 20 && mouseY <= topPos + 32) {
            graphics.renderComponentTooltip(font, java.util.List.of(Component.translatable("gui.conarm.help1"), Component.translatable("gui.conarm.help2"), Component.translatable("gui.conarm.help3")), mouseX, mouseY);
        }
    }
}
