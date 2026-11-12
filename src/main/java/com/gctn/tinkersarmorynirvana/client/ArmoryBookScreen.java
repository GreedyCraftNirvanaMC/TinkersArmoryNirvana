/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Mantle's MIT-licensed book textures and 206x200 leaf layout are retained; see licenses/Mantle-MIT.txt. */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.armor.ModularArmorItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import static com.gctn.tinkersarmorynirvana.client.ArmoryBookContents.*;

/** Mantle-style cover, spreads and chapter navigation backed by live armor definitions and recipes. */
public final class ArmoryBookScreen extends Screen {
    private static final ResourceLocation BOOK = ArmoryContent.id("textures/gui/book/mantle_book.png");
    private static final ResourceLocation COVER = ArmoryContent.id("textures/gui/book/mantle_bookfront.png");
    private static final int BOOK_WIDTH = 412, BOOK_HEIGHT = 200, INK = 0xff3b2c20;
    private static String lastPage = "";
    private final ArmoryBookDocument document;
    private final ArrayDeque<Integer> history = new ArrayDeque<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Mannequin> previews = new ArrayList<>();
    private int spread = -1;
    private float scale = 1, left, top;
    private BookButton previous, next, index, back, open;
    private RemotePlayer mannequin;
    private record Hit(int x, int y, int width, int height, Component label, ItemStack item, String target) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private record Mannequin(int x, int y, List<ItemStack> armor) {}

    private ArmoryBookScreen() {
        super(Component.translatable("item.conarm.book"));
        document = new ArmoryBookDocument(Minecraft.getInstance().font, ArmoryBookContents.pages());
        if (!lastPage.isEmpty()) { spread = document.find(lastPage) / 2; }
    }
    public static void open() { Minecraft.getInstance().setScreen(new ArmoryBookScreen()); }
    @Override protected void init() {
        scale = Math.min(1, Math.min((width - 24F) / BOOK_WIDTH, (height - 24F) / BOOK_HEIGHT));
        left = (width - BOOK_WIDTH * scale) / 2; top = (height - BOOK_HEIGHT * scale) / 2;
        previous = button(Kind.PREVIOUS, 22, 177, "previous", () -> turn(-1));
        next = button(Kind.NEXT, 371, 177, "next", () -> turn(1));
        index = button(Kind.INDEX, -9, 0, "contents", () -> navigate("index"));
        back = button(Kind.BACK, 197, 190, "back", this::back);
        open = button(Kind.COVER, 103, 0, "open", () -> turn(1));
        if (minecraft.level != null && minecraft.player != null) { mannequin = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile()); }
        updateButtons();
    }
    private int px(float value) { return Math.round(left + value * scale); }
    private int py(float value) { return Math.round(top + value * scale); }
    private BookButton button(Kind kind, int x, int y, String key, Runnable action) {
        return addRenderableWidget(new BookButton(kind, x, y, text(key), action));
    }
    private void updateButtons() {
        previous.visible = spread >= 0; next.visible = spread < (document.leaves.size() - 1) / 2;
        next.setPosition(px(spread < 0 ? 286 : 371), py(177));
        index.visible = spread >= 0; back.visible = !history.isEmpty(); open.visible = spread < 0;
    }
    private void turn(int direction) {
        spread = Math.max(-1, Math.min((document.leaves.size() - 1) / 2, spread + direction)); updateButtons();
    }
    private void navigate(String target) {
        int destination = document.find(target) / 2;
        if (destination != spread) { history.addLast(spread); spread = destination; }
        updateButtons();
    }
    private void back() { if (!history.isEmpty()) { spread = history.removeLast(); updateButtons(); } }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics, mouseX, mouseY, partial); hits.clear(); previews.clear();
        float mx = (mouseX - left) / scale, my = (mouseY - top) / scale;
        var pose = graphics.pose(); pose.pushPose(); pose.translate(left, top, 0); pose.scale(scale, scale, 1);
        if (spread < 0) { renderCover(graphics); }
        else {
            graphics.blit(BOOK, 0, 0, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, 512, 512);
            for (int side = 0; side < 2; side++) {
                graphics.blit(BOOK, side * 206, 0, side * 206, 200, 206, 200, 512, 512);
                int page = spread * 2 + side;
                if (page < document.leaves.size()) { renderLeaf(graphics, document.leaves.get(page), side, page, mx, my); }
            }
        }
        graphics.flush(); pose.popPose();
        // 原版人偶方法内部使用屏幕坐标裁剪。必须在退出书本的缩放矩阵之后调用，
        // 否则 GUI 缩放或窗口较小时，预览会被错误的裁剪区域完全遮住。
        if (mannequin != null) {
            for (var preview : previews) {
                for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) { mannequin.setItemSlot(slot, ItemStack.EMPTY); }
                for (var stack : preview.armor) {
                    if (stack.getItem() instanceof ModularArmorItem armor) { mannequin.setItemSlot(armor.getEquipmentSlot(), stack); }
                }
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, px(preview.x), py(preview.y), px(preview.x + ArmoryBookDocument.WIDTH), py(preview.y + 78),
                        Math.max(1, Math.round(36 * scale)), .0625F, mouseX, mouseY, mannequin);
            }
        }
        super.render(graphics, mouseX, mouseY, partial);
        for (int i = hits.size() - 1; i >= 0; i--) {
            var hit = hits.get(i); if (!hit.contains(mx, my)) { continue; }
            if (!hit.item.isEmpty()) { graphics.renderTooltip(font, hit.item, mouseX, mouseY); }
            else { graphics.renderTooltip(font, hit.label, mouseX, mouseY); }
            break;
        }
    }
    private void renderCover(GuiGraphics graphics) {
        graphics.blit(COVER, 103, 0, 0, 0, 206, 200, 512, 512);
        graphics.blit(COVER, 103, 0, 0, 200, 206, 200, 512, 512);
        var lines = font.split(text("cover.title"), 72);
        var pose = graphics.pose(); pose.pushPose(); pose.translate(206, 74 - lines.size() * 4, 1); pose.scale(2, 2, 1);
        for (int i = 0; i < lines.size(); i++) { graphics.drawString(font, lines.get(i), -font.width(lines.get(i)) / 2, i * 11, 0xffbd8c16, true); }
        pose.popPose();
        graphics.drawCenteredString(font, text("cover.subtitle"), 212, 174, 0xffbd8c16);
    }
    private void renderLeaf(GuiGraphics graphics, ArmoryBookDocument.Leaf leaf, int side, int number, float mouseX, float mouseY) {
        int x = side == 0 ? 16 : 214, y = 12;
        float titleScale = Math.min(1, ArmoryBookDocument.WIDTH / (float) Math.max(1, font.width(leaf.title())));
        var pose = graphics.pose(); pose.pushPose(); pose.translate(x + ArmoryBookDocument.WIDTH / 2F, y, 0); pose.scale(titleScale, titleScale, 1);
        graphics.drawString(font, leaf.title(), -font.width(leaf.title()) / 2, 0, INK, false); pose.popPose();
        hits.add(new Hit(x, y, ArmoryBookDocument.WIDTH, 11, leaf.title(), ItemStack.EMPTY, null));
        graphics.fill(x, y + 13, x + ArmoryBookDocument.WIDTH, y + 14, 0x554c3724);
        for (var placed : leaf.contents()) {
            int cy = y + placed.y(); Object content = placed.content();
            if (content instanceof ArmoryBookDocument.Text line) { graphics.drawString(font, line.line(), x, cy, INK, false); }
            else if (content instanceof Icons icons) {
                int start = x + (ArmoryBookDocument.WIDTH - icons.items().size() * 24) / 2;
                for (int i = 0; i < icons.items().size(); i++) { renderItem(graphics, icons.items().get(i), start + i * 24 + 4, cy + 2, null); }
            } else if (content instanceof Link link) { renderLink(graphics, link, x, cy, mouseX, mouseY); }
            else if (content instanceof Grid grid) {
                for (int i = 0; i < grid.entries().size(); i++) {
                    var link = grid.entries().get(i); int tx = x + 12 + i % 5 * 32, ty = cy + i / 5 * 27;
                    if (mouseX >= tx && mouseX < tx + 25 && mouseY >= ty && mouseY < ty + 25) { graphics.fill(tx, ty, tx + 25, ty + 25, 0x33694a27); }
                    renderItem(graphics, link.icon(), tx + 4, ty + 4, null);
                    hits.add(new Hit(tx, ty, 25, 25, link.label(), ItemStack.EMPTY, link.target()));
                }
            } else if (content instanceof Recipe recipe) { renderRecipe(graphics, recipe, x, cy); }
            else if (content instanceof Preview preview) { previews.add(new Mannequin(x, cy, preview.armor())); }
        }
        graphics.drawCenteredString(font, Integer.toString(number + 1), side * 206 + 103, 182, 0xff795c3b);
    }
    private void renderLink(GuiGraphics graphics, Link link, int x, int y, float mouseX, float mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + ArmoryBookDocument.WIDTH && mouseY >= y && mouseY < y + 18;
        int offset = link.icon().isEmpty() ? 0 : 21;
        if (!link.icon().isEmpty()) { renderItem(graphics, link.icon(), x, y, null); }
        var label = net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(link.label(), ArmoryBookDocument.WIDTH - offset));
        graphics.drawString(font, label, x + offset, y + 4, hovered ? 0xffa34e22 : INK, false);
        if (hovered) { graphics.fill(x + offset, y + 14, x + offset + font.width(label), y + 15, 0xffa34e22); }
        hits.add(new Hit(x, y, ArmoryBookDocument.WIDTH, 18, link.label(), ItemStack.EMPTY, link.target()));
    }
    private void renderRecipe(GuiGraphics graphics, Recipe display, int x, int y) {
        graphics.drawString(font, text("recipe.points", display.recipe().points()), x, y, INK, false);
        renderItem(graphics, display.input(), x, y + 27, null);
        graphics.drawString(font, "+", x + 22, y + 31, INK, false);
        long frame = minecraft.level == null ? 0 : minecraft.level.getGameTime() / 20;
        var ingredients = display.recipe().ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            var cost = ingredients.get(i); var choices = cost.ingredient().getItems(); int ix = x + 40 + i % 3 * 22, iy = y + 16 + i / 3 * 22;
            graphics.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0x225e4026);
            if (choices.length == 0) {
                graphics.drawString(font, "?", ix + 5, iy + 4, 0xffa33020, false);
                hits.add(new Hit(ix, iy, 16, 16, text("recipe.missing"), ItemStack.EMPTY, null));
            } else { renderItem(graphics, choices[(int) (frame % choices.length)], ix, iy, cost.count() > 1 ? Integer.toString(cost.count()) : null); }
        }
        graphics.drawString(font, "→", x + 113, y + 31, INK, false);
        renderItem(graphics, display.output(), x + 139, y + 27, null);
    }
    private void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y, String count) {
        if (stack.isEmpty()) { return; }
        graphics.renderFakeItem(stack, x, y); graphics.renderItemDecorations(font, stack, x, y, count);
        hits.add(new Hit(x, y, 16, 16, stack.getHoverName(), stack, null));
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) { return true; }
        if (button != 0) { return false; }
        double mx = (mouseX - left) / scale, my = (mouseY - top) / scale;
        for (int i = hits.size() - 1; i >= 0; i--) {
            var hit = hits.get(i);
            if (hit.target != null && hit.contains(mx, my)) { navigate(hit.target); return true; }
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy != 0) { turn(dy < 0 ? 1 : -1); return true; }
        return super.mouseScrolled(x, y, dx, dy);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { turn(1); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { turn(-1); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) { navigate("index"); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !history.isEmpty()) { back(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void removed() { lastPage = spread < 0 ? "" : document.leaves.get(spread * 2).id(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }

    private enum Kind {
        NEXT(412, 0, 18, 10), PREVIOUS(412, 10, 18, 10), BACK(412, 30, 18, 10), INDEX(412, 40, 18, 18), COVER(0, 0, 206, 200);
        final int u, v, width, height;
        Kind(int u, int v, int width, int height) { this.u = u; this.v = v; this.width = width; this.height = height; }
    }
    private final class BookButton extends AbstractButton {
        private final Kind kind;
        private final Runnable action;
        private BookButton(Kind kind, int x, int y, Component label, Runnable action) {
            super(px(x), py(y), Math.max(1, Math.round(kind.width * scale)), Math.max(1, Math.round(kind.height * scale)), label);
            this.kind = kind; this.action = action; setTooltip(Tooltip.create(label));
        }
        @Override public void onPress() { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (kind == Kind.COVER) { return; }
            int color = isHoveredOrFocused() ? 0xffd98a31 : 0xffe6d2ae;
            RenderSystem.setShaderColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(BOOK, getX(), getY(), getWidth(), getHeight(), kind.u, kind.v, kind.width, kind.height, 512, 512);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
