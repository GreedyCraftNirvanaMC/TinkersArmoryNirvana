/* SPDX-License-Identifier: LGPL-3.0-only
 * Ports the original GuiInfoPanel border, caption, text spacing and scrollbar from TinkersAntique (LGPL-3.0-only). */
package com.gctn.tinkersarmorynirvana.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

final class ArmoryInfoPanel {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tconstruct", "textures/gui/panel.png");
    record Row(Component text, Component tooltip) { Row(Component text) { this(text, null); } }
    private record Line(FormattedCharSequence text, Component tooltip) {}
    private final List<Line> lines = new ArrayList<>();
    private int x, y, width, height, scroll, maximum, theme;
    private Component caption = Component.empty();
    private boolean dragging;
    void place(int x, int y, int width, int height, boolean metal) { this.x = x; this.y = y; this.width = width; this.height = height; theme = metal ? 83 : 0; }
    void content(Font font, Component caption, List<Row> rows) {
        this.caption = caption; lines.clear();
        int available = height - 10 - (caption.getString().isEmpty() ? 0 : 12);
        int estimated = rows.stream().mapToInt(row -> Math.max(1, font.split(row.text, width - 8).size())).sum();
        int textWidth = width - 8 - (estimated * 9.5F > available ? 6 : 0);
        for (var row : rows) { for (var line : font.split(row.text, textWidth)) { lines.add(new Line(line, row.tooltip)); } }
        maximum = Math.max(0, lines.size() - (int) (available / 9.5F)); scroll = Math.min(scroll, maximum);
    }
    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        int u = 126, v = theme;
        tile(graphics, x + 4, y + 4, width - 8, height - 8, 4, 4, 118, 75);
        tile(graphics, x + 4, y, width - 8, 4, u + 4, v, 118, 4);
        tile(graphics, x + 4, y + height - 4, width - 8, 4, u + 4, v + 79, 118, 4);
        tile(graphics, x, y + 4, 4, height - 8, u, v + 4, 4, 75);
        tile(graphics, x + width - 4, y + 4, 4, height - 8, u + 122, v + 4, 4, 75);
        graphics.blit(TEXTURE, x, y, u, v, 4, 4); graphics.blit(TEXTURE, x + width - 4, y, u + 122, v, 4, 4);
        graphics.blit(TEXTURE, x, y + height - 4, u, v + 79, 4, 4); graphics.blit(TEXTURE, x + width - 4, y + height - 4, u + 122, v + 79, 4, 4);
        float textY = y + 5;
        if (!caption.getString().isEmpty()) {
            graphics.drawCenteredString(font, caption.copy().withStyle(net.minecraft.ChatFormatting.UNDERLINE), x + width / 2, (int) textY, 0xfff0f0f0); textY += 12;
        }
        if (lines.stream().anyMatch(line -> line.tooltip != null)) { graphics.drawString(font, "?", x + width - 6, y + 5, 0xff5f5f5f, false); }
        for (int i = scroll; i < lines.size() && textY + 9 <= y + height - 5; i++) {
            graphics.drawString(font, lines.get(i).text, x + 5F, textY, 0xfff0f0f0, true); textY += 9.5F;
        }
        if (maximum > 0) {
            graphics.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            int sx = x + width - 9, sy = y + 16, travel = height - 25, shift = theme == 83 ? 12 : 6;
            tile(graphics, sx, sy + 4, 3, travel - 3, shift, 88, 3, 8);
            graphics.blit(TEXTURE, sx, sy, shift + 3, 88, 3, 4); graphics.blit(TEXTURE, sx, sy + travel, shift + 3, 92, 3, 4);
            int thumbY = sy + scroll * Math.max(1, travel - 1) / maximum;
            boolean hover = mouseX >= sx && mouseX < sx + 3 && mouseY >= thumbY && mouseY < thumbY + 5;
            graphics.blit(TEXTURE, sx, thumbY, shift + (hover || dragging ? 3 : 0), 83, 3, 5);
        }
    }
    void tooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) { return; }
        if (mouseX >= x + width - 8 && mouseY >= y + 5 && mouseY < y + 14 && lines.stream().anyMatch(line -> line.tooltip != null)) {
            graphics.renderTooltip(font, Component.translatable("gui.general.hover"), mouseX, mouseY); return;
        }
        int start = y + 5 + (caption.getString().isEmpty() ? 0 : 12);
        int index = scroll + (int) ((mouseY - start) / 9.5F);
        if (mouseY >= start && index >= 0 && index < lines.size() && lines.get(index).tooltip != null) {
            graphics.renderTooltip(font, font.split(lines.get(index).tooltip, 200), mouseX, mouseY);
        }
    }
    boolean scroll(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height || maximum == 0) { return false; }
        scroll = Math.max(0, Math.min(maximum, scroll - (int) Math.signum(amount))); return true;
    }
    boolean click(double mouseX, double mouseY) {
        dragging = maximum > 0 && mouseX >= x + width - 12 && mouseX < x + width && mouseY >= y + 16 && mouseY < y + height - 5;
        if (dragging) { drag(mouseY); } return dragging;
    }
    void drag(double mouseY) { if (dragging) { scroll = Math.max(0, Math.min(maximum, (int) Math.round((mouseY - y - 16) * maximum / Math.max(1, height - 26)))); } }
    void release() { dragging = false; }
    private static void tile(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int tileWidth, int tileHeight) {
        // GuiElementScalable 原实现逐块平铺，不能拉伸木纹／金属边框。
        for (int dy = 0; dy < height; dy += tileHeight) {
            for (int dx = 0; dx < width; dx += tileWidth) { graphics.blit(TEXTURE, x + dx, y + dy, u, v, Math.min(tileWidth, width - dx), Math.min(tileHeight, height - dy)); }
        }
    }
}
