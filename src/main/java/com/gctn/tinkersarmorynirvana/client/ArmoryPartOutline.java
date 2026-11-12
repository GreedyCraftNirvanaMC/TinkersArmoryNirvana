/* SPDX-License-Identifier: LGPL-3.0-only
 * GuiOutlineTexture / ExtraUtilityTexture behavior from TinkersAntique (LGPL-3.0-only), Slime Knights. */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.TinkersArmoryNirvana;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "conarm", value = Dist.CLIENT)
public final class ArmoryPartOutline {
    private static final Map<Item, ResourceLocation> TEXTURES = new HashMap<>();
    private ArmoryPartOutline() {}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
            for (var texture : TEXTURES.values()) { if (texture.getNamespace().equals("conarm")) { Minecraft.getInstance().getTextureManager().release(texture); } }
            TEXTURES.clear();
        });
    }
    static void draw(GuiGraphics graphics, Item item, int x, int y) {
        var texture = TEXTURES.computeIfAbsent(item, ArmoryPartOutline::load);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }
    private static ResourceLocation load(Item item) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        var id = ArmoryContent.id("dynamic/part_outline/" + name);
        var source = ArmoryContent.id("textures/items/parts/" + name + ".png");
        try (var stream = Minecraft.getInstance().getResourceManager().open(source); var image = NativeImage.read(stream)) {
            int width = image.getWidth(), height = image.getHeight();
            boolean[] transparent = new boolean[width * height], edge = new boolean[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    edge[index] |= x == 0 || y == 0 || x == width - 1 || y == height - 1;
                    if ((image.getPixelRGBA(x, y) >>> 24) < 64) {
                        transparent[index] = true;
                        if (x > 0) { edge[index - 1] = true; } if (x < width - 1) { edge[index + 1] = true; }
                        if (y > 0) { edge[index - width] = true; } if (y < height - 1) { edge[index + width] = true; }
                    }
                }
            }
            var outline = new NativeImage(width, height, true);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    outline.setPixelRGBA(x, y, transparent[index] ? image.getPixelRGBA(x, y) : edge[index] ? 0xff323232 : 0);
                }
            }
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(outline)); return id;
        } catch (java.io.IOException error) { TinkersArmoryNirvana.LOGGER.error("Cannot create original GUI outline for {}", source, error); return net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation(); }
    }
}
