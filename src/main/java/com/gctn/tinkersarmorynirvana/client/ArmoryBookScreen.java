/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Mantle GuiBook, GuiArrow and ElementSection presentation adapted to native 1.21.1 APIs (MIT). */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.station.ArmoryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.ArrayList;
import java.util.List;
import static com.gctn.tinkersarmorynirvana.client.ArmoryBookDocument.*;

/** The original 206x200 Mantle leaves, single right-hand index and source-defined page elements. */
public final class ArmoryBookScreen extends Screen {
    private static final ResourceLocation BOOK = ArmoryContent.id("textures/gui/book/mantle_book.png");
    private static final ResourceLocation FRONT = ArmoryContent.id("textures/gui/book/mantle_bookfront.png");
    private final ArmoryBookDocument document;
    private final InteractionHand hand;
    private final int slot;
    private final List<Hit> hits = new ArrayList<>();
    private int spread = -1, returnSpread = -2;
    private float scale = 1, left, top;
    private BookButton previous, next, index, back, open;
    private record Hit(float x, float y, float width, float height, Component label, ItemStack item, String target, boolean returner) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private ArmoryBookScreen(InteractionHand hand) {
        super(Component.translatable("item.conarm.book")); this.hand = hand;
        var minecraft = Minecraft.getInstance(); slot = hand == InteractionHand.MAIN_HAND ? minecraft.player.getInventory().selected : 40;
        document = new ArmoryBookDocument(minecraft.font, new ArmoryBookContents());
        String saved = minecraft.player.getItemInHand(hand).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("conarm:BookPage");
        if (!saved.isEmpty()) { spread = spreadFor(document.find(saved)); }
    }
    public static void open(InteractionHand hand) { Minecraft.getInstance().setScreen(new ArmoryBookScreen(hand)); }
    private static int spreadFor(int leaf) { return leaf == 0 ? 0 : (leaf + 1) / 2; }
    private int lastSpread() { return document.leaves.size() / 2; }
    private Leaf leaf(int side) {
        int page = spread == 0 ? side == 1 ? 0 : -1 : spread * 2 - 1 + side;
        return spread < 0 || page < 0 || page >= document.leaves.size() ? null : document.leaves.get(page);
    }
    @Override protected void init() {
        scale = Math.min(1, Math.min((width - 12F) / 412, (height - 28F) / 200));
        left = (float) Math.floor(width / 2F) - 206 * scale; top = (float) Math.floor(height / 2F) - 100 * scale;
        previous = button(Kind.PREVIOUS, 22, 175, "previous", () -> turn(-1));
        next = button(Kind.NEXT, 371, 175, "next", () -> turn(1));
        index = button(Kind.INDEX, -9, 0, "contents", () -> navigate("index", false));
        back = button(Kind.BACK, 197, 193, "back", this::back);
        open = button(Kind.COVER, 103, 0, "open", () -> turn(1)); updateButtons();
    }
    private int px(float value) { return Math.round(left + value * scale); }
    private int py(float value) { return Math.round(top + value * scale); }
    private BookButton button(Kind kind, int x, int y, String key, Runnable action) {
        return addRenderableWidget(new BookButton(kind, x, y, Component.translatable("book.conarm." + key), action));
    }
    private void updateButtons() {
        previous.visible = spread >= 0; next.visible = spread < lastSpread(); next.setPosition(px(spread < 0 ? 286 : 371), py(175));
        index.visible = spread > 0; back.visible = returnSpread >= -1; open.visible = spread < 0;
    }
    private void turn(int direction) { spread = Math.max(-1, Math.min(lastSpread(), spread + direction)); returnSpread = -2; updateButtons(); }
    private void navigate(String target, boolean returner) {
        int destination = spreadFor(document.find(target));
        if (destination != spread) { returnSpread = returner ? spread : -2; spread = destination; }
        updateButtons();
    }
    private void back() { if (returnSpread >= -1) { spread = returnSpread; } returnSpread = -2; updateButtons(); }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // Screen.render 会在绘制控件前调用此方法。书页已经画好，不能再执行原生的全屏模糊。
        // 旧 GuiBook 保留清晰的游戏背景，所以这里也不额外叠加背景效果。
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // GuiBook 原版不对世界加模糊或暗幕；只绘制书本与控件。
        hits.clear(); float mx = (mouseX - left) / scale, my = (mouseY - top) / scale;
        var pose = graphics.pose(); pose.pushPose(); pose.translate(left, top, 0); pose.scale(scale, scale, 1);
        int coverColor = color("coverColor", 0xffffff);
        if (spread < 0) {
            tint(graphics, coverColor); graphics.blit(FRONT, 103, 0, 0, 0, 206, 200, 512, 512); tint(graphics, 0xffffff);
            graphics.blit(FRONT, 103, 0, 0, 200, 206, 200, 512, 512);
            String title = ArmoryBookContents.string(document.book.appearance, "title", "Armory Addendum");
            float titleScale = font.width(title) <= 67 ? 2.5F : 2;
            pose.pushPose(); pose.translate(206 + 3 * titleScale, 95.5F - 4 * titleScale, 1); pose.scale(titleScale, titleScale, 1);
            graphics.drawString(font, title, -font.width(title) / 2, 0, 0xffae8000, true); pose.popPose();
            String subtitle = ArmoryBookContents.string(document.book.appearance, "subtitle", "by C4");
            pose.pushPose(); pose.translate(216.5F, 182, 1); pose.scale(1.5F, 1.5F, 1);
            graphics.drawString(font, subtitle, -font.width(subtitle) / 2, 0, 0xffae8000, true); pose.popPose();
        } else {
            tint(graphics, coverColor); graphics.blit(BOOK, 0, 0, 0, 0, 412, 200, 512, 512); tint(graphics, 0xffffff);
            for (int side = 0; side < 2; side++) {
                var leaf = leaf(side); if (leaf == null) { continue; }
                tint(graphics, 0xffffff);
                graphics.blit(BOOK, side * 206, 0, side * 206, 200, 206, 200, 512, 512);
                int x = side == 0 ? 16 : 214;
                for (var element : leaf.elements(side == 1)) { element(graphics, element, x, 12, mx, my); }
                int number = spread == 0 ? 1 : spread * 2 + side;
                graphics.drawString(font, Integer.toString(number), x + WIDTH / 2 - font.width(Integer.toString(number)) / 2, 12 + HEIGHT - 10, 0xffaaaaaa, false);
            }
        }
        graphics.flush(); pose.popPose(); super.render(graphics, mouseX, mouseY, partial);
        for (int i = hits.size() - 1; i >= 0; i--) {
            var hit = hits.get(i); if (!hit.contains(mx, my)) { continue; }
            if (!hit.item.isEmpty()) { graphics.renderTooltip(font, hit.item, mouseX, mouseY); }
            else if (hit.label != null) { graphics.renderTooltip(font, font.split(hit.label, Math.min(200, width - 24)), mouseX, mouseY); }
            break;
        }
    }
    private void element(GuiGraphics graphics, Element element, int x, int y, float mouseX, float mouseY) {
        if (element instanceof Text text) {
            var pose = graphics.pose(); pose.pushPose(); pose.translate(x + text.x(), y + text.y(), 0); pose.scale(text.scale(), text.scale(), 1);
            graphics.drawString(font, text.line(), 0, 0, 0xff000000, text.shadow()); pose.popPose();
            if (text.tooltip() != null) { hits.add(new Hit(x + text.x(), y + text.y(), font.width(text.line()) * text.scale(), 9 * text.scale(), text.tooltip(), ItemStack.EMPTY, null, false)); }
        } else if (element instanceof Image image) {
            tint(graphics, image.color()); graphics.blit(image.texture(), x + image.x(), y + image.y(), image.width(), image.height(), image.u(), image.v(), image.uw(), image.vh(), image.tw(), image.th()); tint(graphics, 0xffffff);
        } else if (element instanceof Icon icon) {
            ItemStack stack = choice(icon.items());
            item(graphics, stack, x + icon.x(), y + icon.y(), icon.scale());
            hits.add(new Hit(x + icon.x(), y + icon.y(), 16 * icon.scale(), 16 * icon.scale(), icon.tooltip(), icon.tooltip() == null ? stack : ItemStack.EMPTY, null, false));
        } else if (element instanceof Section section) {
            int tx = x + section.x(), ty = y + section.y(); boolean hover = mouseX >= tx && mouseX < tx + 42 && mouseY >= ty && mouseY < ty + 42;
            if (hover) { graphics.fill(tx + 5, ty + 5, tx + 37, ty + 37, 0x77ee541c); }
            // 物品各渲染层处理全局 alpha 的方式不同，不能用旧版的半透明色值控制目录图标。
            // 图标和文字两种状态均使用固定的不透明颜色，悬停只绘制背景高亮。
            graphics.setColor(1, 1, 1, 1); item(graphics, section.icon(), tx + 5, ty + 5, 2); graphics.setColor(1, 1, 1, 1);
            graphics.drawString(font, section.label(), tx + 21 - font.width(section.label()) / 2, ty + 38, 0xff7f7f7f, false);
            hits.add(new Hit(tx, ty, 42, 42, section.label(), ItemStack.EMPTY, section.target(), false));
        } else if (element instanceof Tile tile) {
            int tx = x + tile.x(), ty = y + tile.y();
            item(graphics, tile.icon(), tx + (tile.size() - 16 * tile.scale()) / 2, ty + (tile.size() - 16 * tile.scale()) / 2, tile.scale());
            hits.add(new Hit(tx, ty, tile.size(), tile.size(), tile.label(), ItemStack.EMPTY, tile.target(), true));
        } else if (element instanceof Link link) {
            int tx = x + link.x(), ty = y + link.y(); boolean hover = mouseX >= tx && mouseX < tx + link.width() && mouseY >= ty && mouseY < ty + 9;
            var label = Component.literal(hover ? " > " : "- ").append(link.label()).withColor(hover ? 0x800000 : 0x000000);
            graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(label, link.width())), tx, ty, 0xff000000, false);
            hits.add(new Hit(tx, ty, link.width(), 9, link.label(), ItemStack.EMPTY, link.target(), true));
        }
    }
    private ItemStack choice(List<ItemStack> items) { return items.isEmpty() ? ItemStack.EMPTY : items.get((int) ((minecraft.level == null ? 0 : minecraft.level.getGameTime() / 20) % items.size())); }
    private static void item(GuiGraphics graphics, ItemStack stack, float x, float y, float scale) {
        if (stack.isEmpty()) { return; }
        var pose = graphics.pose(); pose.pushPose(); pose.translate(x, y, 0); pose.scale(scale, scale, 1); graphics.renderFakeItem(stack, 0, 0); pose.popPose();
    }
    private int color(String key, int fallback) { return document.book.appearance.has(key) ? document.book.appearance.get(key).getAsInt() : fallback; }
    private static void tint(GuiGraphics graphics, int color) {
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        // 物品与字体的批次提交会重置混合状态；每次画透明书页／箭头都显式恢复。
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) { return true; }
        double mx = (x - left) / scale, my = (y - top) / scale;
        for (int i = hits.size() - 1; i >= 0; i--) { var hit = hits.get(i); if (button == 0 && hit.target != null && hit.contains(mx, my)) { navigate(hit.target, hit.returner); return true; } }
        return false;
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { if (dy != 0) { turn(dy < 0 ? 1 : -1); return true; } return super.mouseScrolled(x, y, dx, dy); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 262 || key == 68) { turn(1); return true; }
        if (key == 263 || key == 65) { turn(-1); return true; }
        if (key == 268) { navigate("index", false); return true; }
        if (key == 259 && returnSpread >= -1) { back(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void removed() {
        var current = leaf(spread == 0 ? 1 : 0);
        if (minecraft.getConnection() != null) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArmoryNetwork.BookPage(hand, slot, current == null ? "" : current.id())); }
        super.removed();
    }
    @Override public boolean isPauseScreen() { return false; }
    private enum Kind {
        NEXT(412, 0, 18, 10), PREVIOUS(412, 10, 18, 10), BACK(412, 30, 18, 10), INDEX(412, 40, 18, 18), COVER(0, 0, 206, 200);
        final int u, v, width, height; Kind(int u, int v, int width, int height) { this.u = u; this.v = v; this.width = width; this.height = height; }
    }
    private final class BookButton extends AbstractButton {
        private final Kind kind;
        private final Runnable action;
        BookButton(Kind kind, int x, int y, Component label, Runnable action) {
            super(px(x), py(y), Math.max(1, Math.round(kind.width * scale)), Math.max(1, Math.round(kind.height * scale)), label);
            this.kind = kind; this.action = action; setTooltip(Tooltip.create(label));
        }
        @Override public void onPress() { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (kind == Kind.COVER) { return; }
            tint(graphics, isHoveredOrFocused() ? color("arrowColorHover", 0xff541c) : color("arrowColor", 0xffffd3));
            graphics.blit(BOOK, getX(), getY(), getWidth(), getHeight(), kind.u, kind.v, kind.width, kind.height, 512, 512); tint(graphics, 0xffffff);
        }
    }
}
