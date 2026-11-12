/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Direct layout port of C4's GuiArmorStation / GuiArmorForge, Copyright 2018-2020 C4. */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ArmoryScreen extends AbstractContainerScreen<ArmoryMenu> {
    private static final ResourceLocation BACKGROUND = ArmoryContent.id("textures/gui/armorforge.png");
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("tconstruct", "textures/gui/icons.png");
    private static final ResourceLocation ARMOR_ICONS = ArmoryContent.id("textures/gui/icons.png");
    private final ArmoryInfoPanel armorInfo = new ArmoryInfoPanel(), traitInfo = new ArmoryInfoPanel(), preview = new ArmoryInfoPanel();
    private net.minecraft.client.player.RemotePlayer mannequin;
    private EditBox name;
    private boolean compact;
    public ArmoryScreen(ArmoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 174; inventoryLabelY = 80;
    }
    @Override protected void init() {
        super.init(); topPos += 4;
        compact = com.gctn.tinkersarmorynirvana.ArmoryConfig.CLIENT.isLoaded() && com.gctn.tinkersarmorynirvana.ArmoryConfig.COMPACT_VIEW.get();
        name = new EditBox(font, leftPos + 80, topPos + 7, 82, 12, Component.translatable("gui.conarm.name"));
        name.setBordered(false); name.setMaxLength(50); name.setTextColor(0xff404040);
        name.setResponder(value -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArmoryNetwork.Name(menu.containerId, value)));
        addRenderableWidget(name);
        for (int i = 0; i < 5; i++) { addRenderableWidget(new ArmorButton(i)); }
        if (!compact && minecraft.player != null && minecraft.level != null) { mannequin = new PreviewPlayer(minecraft.level, minecraft.player); }
    }
    public static ItemStack demo(String type) {
        String core = ArmoryRegistry.material("wood") != null ? "wood" : ArmoryRegistry.materials().keySet().stream().findFirst().orElse(null);
        if (core == null) { return ItemStack.EMPTY; }
        return ArmorData.create(ArmoryContent.ARMORS.get(type).get(), core, ArmoryRegistry.material("cobalt") == null ? core : "cobalt", ArmoryRegistry.material("ardite") == null ? core : "ardite");
    }
    private void display() {
        boolean metal = menu.forge(); int beam = 7, rightX = leftPos + 178;
        armorInfo.place(rightX, topPos + beam + 4, 126, 83, metal);
        traitInfo.place(rightX, topPos + beam + 4 + 87, 126, 83, metal);
        if (!compact) { preview.place(leftPos - 108, topPos + 31, 106, 150, metal); preview.content(font, Component.translatable("gui.armorstation.preview"), List.of()); }
        ItemStack shown = menu.shownArmor(); List<ArmoryInfoPanel.Row> rows = new ArrayList<>(), traits = new ArrayList<>();
        if (mannequin != null && menu.emptyInputs() && shown.isEmpty()) { mannequin.getInventory().clearContent(); }
        if (menu.needsForge()) {
            armorInfo.content(font, Component.translatable("gui.error"), List.of(new ArmoryInfoPanel.Row(Component.translatable("gui.error.modifier_on_station"))));
            traitInfo.content(font, Component.empty(), List.of()); return;
        }
        if (ArmorData.initialized(shown)) {
            var stats = ArmorData.displayStats(shown);
            rows.add(new ArmoryInfoPanel.Row(Component.translatable("stat.core.durability.name").append(": ").append(Component.literal((shown.getMaxDamage() - shown.getDamageValue()) + "/" + shown.getMaxDamage()).withColor(0x47cc47))));
            rows.add(new ArmoryInfoPanel.Row(Component.translatable("stat.core.defense.name").append(": ").append(Component.literal(number(stats.defense)).withColor(0xd76464))));
            rows.add(new ArmoryInfoPanel.Row(Component.translatable("stat.plates.toughness.name").append(": ").append(Component.literal(number(stats.toughness)).withColor(0x78a0cd))));
            if (ArmorData.freeSlots(shown) > 0) { rows.add(new ArmoryInfoPanel.Row(Component.translatable("gui.conarm.remaining_modifiers", ArmorData.freeSlots(shown)))); }
            ArmorData.traits(shown).forEach((id, level) -> {
                int color = ArmorData.traitColor(shown, id);
                var name = Component.translatable("trait." + id.replace(':', '.') + ".name").append(level > 1 ? " " + level : "").withColor(color);
                var description = Component.translatable("trait." + id.replace(':', '.') + ".desc").withColor(color);
                traits.add(new ArmoryInfoPanel.Row(name, description));
            });
            for (var modifier : ArmoryRegistry.modifiers().values()) {
                if (ArmorData.level(shown, modifier.id) > 0) { traits.add(new ArmoryInfoPanel.Row(Component.translatable("modifier." + modifier.id.replace(':', '.') + ".name").withColor(0xa065cf), Component.translatable("modifier." + modifier.id.replace(':', '.') + ".desc"))); }
            }
            if (traits.isEmpty()) { traits.add(new ArmoryInfoPanel.Row(Component.translatable("gui.toolstation.noTraits"))); }
            armorInfo.content(font, Component.translatable(shown.getItem().getDescriptionId()), rows); traitInfo.content(font, Component.translatable("gui.toolstation.traits"), traits);
            if (mannequin != null && shown.getItem() instanceof ModularArmorItem armor) { mannequin.setItemSlot(armor.getEquipmentSlot(), shown.copy()); }
        } else if (menu.selection() == 0) {
            if (mannequin != null) { mannequin.getInventory().clearContent(); }
            armorInfo.content(font, Component.translatable("gui.toolstation.repair"), List.of());
            if (com.gctn.tconstruct.Config.DECONSTRUCT_TOOLS.get()) {
                traitInfo.content(font, Component.translatable("gui.armorstation.deconstruct"), List.of(new ArmoryInfoPanel.Row(Component.translatable("gui.armorstation.deconstruct.info"))));
            } else { traitInfo.content(font, Component.empty(), List.of()); }
        } else {
            String armor = ArmoryStationLayout.ARMOR[menu.selection() - 1];
            armorInfo.content(font, Component.translatable("item.conarm." + armor), List.of(new ArmoryInfoPanel.Row(Component.translatable("item.conarm." + armor + ".desc"))));
            for (int i = 0; i < 3; i++) {
                var part = ArmoryStationLayout.requiredPart(menu.selection(), i);
                var label = Component.literal(" * ").append(Component.translatable(part.getDescriptionId()));
                if (!menu.getSlot(i + 1).getItem().is(part)) { label.withStyle(net.minecraft.ChatFormatting.RED); }
                rows.add(new ArmoryInfoPanel.Row(label));
            }
            traitInfo.content(font, Component.translatable("gui.toolstation.components"), rows);
        }
    }
    private static String number(float value) { return java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable(menu.forge() ? "gui.armorforge.name" : "gui.armorstation.name"), 8, 6, 0xff404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 80, 0xff404040, false);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        menu.updateSlotPositions(); display();
        blend();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, 176, 174);
        if (name.isFocused()) { graphics.blit(BACKGROUND, leftPos + 78, topPos + 6, 0, 210, 92, 12); }
        var pose = graphics.pose(); pose.pushPose(); pose.translate(10, 22, 0); pose.scale(3.7F, 3.7F, 1);
        if (menu.selection() == 0) { graphics.blit(ICONS, (int) (leftPos / 3.7F), (int) (topPos / 3.7F), 54, 0, 18, 18); }
        else { graphics.renderFakeItem(demo(ArmoryStationLayout.ARMOR[menu.selection() - 1]), (int) (leftPos / 3.7F), (int) (topPos / 3.7F)); }
        pose.popPose(); overlay(graphics, .82F); graphics.blit(BACKGROUND, leftPos + 7, topPos + 18, 176, 18, 80, 64); overlay(graphics, 1);
        if (menu.selection() == 0 && (menu.deconstructing() || menu.emptyInputs())) { graphics.blit(BACKGROUND, leftPos + 82, topPos + 38, 8, 241, 8, 15); }
        if (!menu.deconstructing()) { graphics.blit(BACKGROUND, leftPos + 104, topPos + 38, 0, 241, 8, 15); }
        // 先画原版半透明槽底，再画不透明边框；两层不能合并成只画边框。
        for (int i = 0; i < ArmoryStationLayout.activeSlots(menu.selection()); i++) {
            var slot = menu.getSlot(i + 1);
            overlay(graphics, menu.deconstructing() && !slot.hasItem() ? .58F : .28F);
            graphics.blit(BACKGROUND, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
        }
        overlay(graphics, 1);
        for (int i = 0; i < 6; i++) {
            var slot = menu.getSlot(i + 1); if (!slot.isActive()) { continue; }
            graphics.blit(BACKGROUND, leftPos + slot.x - 1, topPos + slot.y - 1, 194, 0, 18, 18);
            if (slot.hasItem() || menu.deconstructing()) { continue; }
            if (menu.selection() == 0) { graphics.blit(ARMOR_ICONS, leftPos + slot.x - 1, topPos + slot.y - 1, i * 18, 234, 18, 18); }
            else if (i < 3) { ArmoryPartOutline.draw(graphics, ArmoryStationLayout.requiredPart(menu.selection(), i), leftPos + slot.x, topPos + slot.y); }
        }
        int beamV = menu.forge() ? 187 : 180, columns = compact ? 1 : 5, buttonsX = leftPos - columns * 18 - (columns - 1) * 4 - 2;
        beam(graphics, buttonsX - 2, topPos, columns * 18 + (columns - 1) * 4 + 4, beamV);
        beam(graphics, leftPos + 176, topPos, 130, beamV);
        armorInfo.render(graphics, font, mouseX, mouseY); traitInfo.render(graphics, font, mouseX, mouseY);
        int decoration = menu.forge() ? 36 : 18;
        for (int panelY : new int[]{topPos + 11, topPos + 98}) {
            graphics.blit(BACKGROUND, leftPos + 183, panelY - 4, decoration, 174, 5, 4);
            graphics.blit(BACKGROUND, leftPos + 290, panelY - 4, decoration + 9, 174, 9, 4);
        }
        if (mannequin != null && !compact) {
            preview.render(graphics, font, mouseX, mouseY);
            // 新接口以包围框中心定位。保留旧 GuiPreviewPanel 的脚底 y+135 和视线 (x+50,y+30)。
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                    leftPos - 104, topPos + 50, leftPos - 6, topPos + 174, 60, 0,
                    (float) Math.atan((leftPos - 58F - mouseX) / 40F),
                    (float) Math.atan((topPos + 61F - mouseY) / 40F), mannequin);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }
    private static void beam(GuiGraphics graphics, int x, int y, int width, int v) {
        blend();
        graphics.blit(BACKGROUND, x, y, 0, v, 2, 7);
        for (int i = 2; i < width - 2; i += 129) { graphics.blit(BACKGROUND, x + i, y, 2, v, Math.min(129, width - 2 - i), 7); }
        graphics.blit(BACKGROUND, x + width - 2, y, 131, v, 2, 7);
    }
    private static void blend() { com.mojang.blaze3d.systems.RenderSystem.enableBlend(); com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc(); }
    private static void overlay(GuiGraphics graphics, float alpha) {
        graphics.setColor(1, 1, 1, alpha);
        blend();
        // renderFakeItem 写入比 GUI 更靠前的深度；而 setColor/flush 又会启用深度测试。
        // 对照原 GuiArmorStation，遮罩、槽底和边框都在关闭深度测试后按绘制顺序覆盖图标。
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
    }
    static final class PreviewPlayer extends net.minecraft.client.player.RemotePlayer {
        PreviewPlayer(net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.client.player.AbstractClientPlayer source) {
            super(level, source.getGameProfile());
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, source.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
        armorInfo.tooltip(graphics, font, mouseX, mouseY); traitInfo.tooltip(graphics, font, mouseX, mouseY);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return armorInfo.scroll(x, y, dy) || traitInfo.scroll(x, y, dy) || super.mouseScrolled(x, y, dx, dy); }
    @Override public boolean mouseClicked(double x, double y, int button) { if (button == 0 && (armorInfo.click(x, y) || traitInfo.click(x, y))) { return true; } return super.mouseClicked(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { armorInfo.drag(y); traitInfo.drag(y); return super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int button) { armorInfo.release(); traitInfo.release(); return super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        // 命名时将字母键交给输入框，避免 E 等物品栏快捷键提前关闭工作台；Esc 仍正常退出。
        if (name.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { name.keyPressed(key, scan, modifiers); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        int minX = compact ? leftPos - 20 : leftPos - 110;
        return x < minX || x >= leftPos + 306 || y < topPos || y >= topPos + 181;
    }
    private final class ArmorButton extends AbstractButton {
        private final int mode;
        ArmorButton(int mode) {
            super(compact ? leftPos - 20 : leftPos - 108 + mode * 22, compact ? topPos + 9 + mode * 22 : topPos + 9, 18, 18,
                    Component.translatable(mode == 0 ? "gui.toolstation.repair" : "item.conarm." + ArmoryStationLayout.ARMOR[mode - 1]));
            this.mode = mode; setTooltip(Tooltip.create(getMessage()));
        }
        @Override public void onPress() { if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode); } }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            blend();
            int v = menu.forge() ? 198 : 180, u = menu.selection() == mode ? 144 : isHoveredOrFocused() ? 216 : 180;
            graphics.blit(ICONS, getX(), getY(), u, v, 18, 18);
            if (mode == 0) { graphics.blit(ICONS, getX(), getY(), 54, 0, 18, 18); }
            else { graphics.renderFakeItem(demo(ArmoryStationLayout.ARMOR[mode - 1]), getX() + 1, getY() + 1); }
            int theme = menu.forge() ? 36 : 18;
            blend();
            graphics.blit(BACKGROUND, getX(), getY() - 2, theme, 176, 18, 2); graphics.blit(BACKGROUND, getX(), getY() + 18, theme, 174, 18, 2);
            graphics.blit(BACKGROUND, getX(), getY() + 20, theme, 176, 18, 2);
        }
    }
}
