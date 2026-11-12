/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.gctn.tconstruct.library.materials.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

/** Live registry pages ensure addon materials and changed modifiers are represented in the manual. */
public final class ArmoryBookScreen extends Screen {
    private final List<Component> pages = new ArrayList<>();
    private int page;
    private ArmoryBookScreen() { super(Component.translatable("item.conarm.book")); }
    public static void open() { Minecraft.getInstance().setScreen(new ArmoryBookScreen()); }
    @Override protected void init() {
        pages.clear();
        for (int i = 1; i <= 5; i++) { pages.add(Component.translatable("book.conarm.intro" + i)); }
        for (var material : ArmoryRegistry.materials().values()) {
            pages.add(Component.translatable("book.conarm.material", Material.nameOf(material.id),
                    material.coreDurability, material.defense, material.plateMultiplier, material.plateDurability, material.toughness, material.trimDurability));
        }
        for (var trait : ArmoryRegistry.traits().values()) {
            pages.add(Component.translatable("trait." + trait.id.replace(':', '.') + ".name").copy().append("\n\n")
                    .append(Component.translatable("trait." + trait.id.replace(':', '.') + ".desc")));
        }
        for (var modifier : ArmoryRegistry.modifiers().values()) {
            pages.add(Component.translatable("modifier." + modifier.id.replace(':', '.') + ".name").copy().append("\n\n")
                    .append(Component.translatable("modifier." + modifier.id.replace(':', '.') + ".desc"))
                    .append(Component.translatable("book.conarm.modifier", modifier.maxLevel(), modifier.pointsPerLevel(), modifier.slotsPerLevel())));
        }
        addRenderableWidget(Button.builder(Component.literal("←"), button -> page = Math.max(0, page - 1)).bounds(width / 2 - 115, height / 2 + 91, 35, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), button -> page = Math.min(pages.size() - 1, page + 1)).bounds(width / 2 + 80, height / 2 + 91, 35, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        int x = width / 2 - 130, y = height / 2 - 120;
        graphics.fill(x, y, x + 260, y + 205, 0xffe8dbb9);
        graphics.drawCenteredString(font, title, width / 2, y + 8, 0xff44382c);
        List<FormattedCharSequence> lines = font.split(pages.get(Math.min(page, pages.size() - 1)), 232);
        for (int i = 0; i < Math.min(17, lines.size()); i++) { graphics.drawString(font, lines.get(i), x + 14, y + 28 + i * 10, 0xff44382c, false); }
        graphics.drawCenteredString(font, (page + 1) + " / " + pages.size(), width / 2, y + 214, 0xffe8dbb9);
    }
    @Override public boolean isPauseScreen() { return false; }
}
