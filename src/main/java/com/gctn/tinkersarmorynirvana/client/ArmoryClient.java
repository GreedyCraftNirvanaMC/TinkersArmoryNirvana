/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.armor.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid = "conarm", value = Dist.CLIENT)
public final class ArmoryClient {
    private static final KeyMapping HELMET = new KeyMapping("key.conarm.toggle_helm.desc", org.lwjgl.glfw.GLFW.GLFW_KEY_G, "key.conarm.category");
    private static final KeyMapping CHEST = new KeyMapping("key.conarm.toggle_chest.desc", org.lwjgl.glfw.GLFW.GLFW_KEY_H, "key.conarm.category");
    private static final KeyMapping LEGS = new KeyMapping("key.conarm.toggle_leg.desc", org.lwjgl.glfw.GLFW.GLFW_KEY_J, "key.conarm.category");
    private ArmoryClient() {}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(ArmoryContent.STATION_MENU.get(), ArmoryScreen::new); event.register(ArmoryContent.ACCESSORY_MENU.get(), AccessoryScreen::new);
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(HELMET); event.register(CHEST); event.register(LEGS); }
    @SubscribeEvent public static void previewName(RenderNameTagEvent event) {
        // 预览复用玩家皮肤，但它不是世界中的玩家；禁止名字与计分板文字进入预览视口。
        if (event.getEntity() instanceof ArmoryScreen.PreviewPlayer) { event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE); }
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> ArmoryContent.PARTS.values().forEach(item -> net.minecraft.client.renderer.item.ItemProperties.register(item.get(), ArmoryContent.id("material_style"),
                (stack, level, entity, seed) -> switch (ArmorPartItem.material(stack)) { case "paper" -> 1; case "cactus" -> 2; default -> 0; })));
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        EquipmentSlot slot = HELMET.consumeClick() ? EquipmentSlot.HEAD : CHEST.consumeClick() ? EquipmentSlot.CHEST : LEGS.consumeClick() ? EquipmentSlot.LEGS : null;
        if (slot == null || minecraft.player == null || minecraft.screen != null) { return; }
        var armor = minecraft.player.getItemBySlot(slot);
        if (ArmorData.broken(armor)) { return; }
        for (var modifier : com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.modifiers().values()) {
            if (modifier.id.startsWith("conarm:") && modifier.accessory() && ArmorData.level(armor, modifier.id) > 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.gctn.tinkersarmorynirvana.station.ArmoryNetwork.Action(modifier.id.substring(modifier.id.indexOf(':') + 1))); break;
            }
        }
    }
    public static KeyMapping accessoryKey(EquipmentSlot slot) { return slot == EquipmentSlot.HEAD ? HELMET : slot == EquipmentSlot.CHEST ? CHEST : LEGS; }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        ArmoryContent.PARTS.values().forEach(item -> event.register((stack, tint) -> ArmorPartItem.color(stack), item.get()));
        ArmoryContent.MATERIAL_ITEMS.forEach((name, item) -> {
            if (name.startsWith("cast_") || name.startsWith("clay_cast_")) { event.register((stack, tint) -> name.startsWith("clay_") ? 0xffb7a28e : 0xffffc84b, item.get()); }
        });
        ArmoryContent.ARMORS.values().forEach(item -> event.register((stack, tint) -> color(stack, tint), item.get()));
    }
    private static int color(ItemStack stack, int layer) {
        if (layer >= 3) {
            var overlay = com.gctn.tinkersarmorynirvana.api.ArmorVisuals.entries().get(layer - 3);
            if (overlay.id().equals("conarm:polished")) {
                var polished = com.gctn.tconstruct.library.TinkerMaterials.getMaterial(ArmorData.read(stack).getString("Polished"));
                return polished == null ? 0 : polished.color | 0xff000000;
            }
            return overlay.color();
        }
        String key = layer == 1 ? "Plates" : layer == 2 ? "Trim" : "Core";
        var material = com.gctn.tconstruct.library.TinkerMaterials.getMaterial(ArmorData.read(stack).getString(key));
        return material == null ? 0xffaaaaaa : material.color | 0xff000000;
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        ArmoryContent.ARMORS.values().forEach(item -> event.registerItem(new IClientItemExtensions() {
            @Override public int getArmorLayerTintColor(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, ArmorMaterial.Layer layer, int index, int fallback) {
                if (ArmorData.broken(stack) || ArmorData.level(stack, "concealed") > 0 || ArmorData.level(stack, "travel_sneak") > 0 && ArmorData.enabled(stack, "travel_sneak")) { return 0; }
                LegacyArmorModel.select(((ModularArmorItem) stack.getItem()).getEquipmentSlot(), index);
                if (index >= 3) {
                    var overlay = com.gctn.tinkersarmorynirvana.api.ArmorVisuals.entries().get(index - 3);
                    if (!overlay.id().equals("conarm:polished") && ArmorData.level(stack, overlay.id()) <= 0) { return 0; }
                    if (ArmorData.level(stack, "incognito") > 0 && overlay.geometry().equals("armor")) { return 0; }
                }
                return color(stack, index);
            }
            @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity living, ItemStack stack, EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                return LegacyArmorModel.model(slot, original);
            }
        }, item.get()));
    }
    @SubscribeEvent public static void fov(ComputeFovModifierEvent event) {
        var helmet = event.getPlayer().getItemBySlot(EquipmentSlot.HEAD);
        if (!ArmorData.broken(helmet) && ArmorData.level(helmet, "travel_goggles") > 0 && ArmorData.enabled(helmet, "travel_goggles")) { event.setNewFovModifier(.3F); }
    }
    @SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (!net.minecraft.client.gui.screens.Screen.hasAltDown()) { return; }
        var stack = event.getItemStack();
        if (stack.getItem() instanceof ModularArmorItem && ArmorData.initialized(stack)) {
            for (String id : ArmorData.traits(stack).keySet()) { event.getToolTip().add(net.minecraft.network.chat.Component.translatable("trait." + id.replace(':', '.') + ".desc")); }
            for (var modifier : com.gctn.tinkersarmorynirvana.api.ArmoryRegistry.modifiers().values()) {
                if (ArmorData.level(stack, modifier.id) > 0) { event.getToolTip().add(net.minecraft.network.chat.Component.translatable("modifier." + modifier.id.replace(':', '.') + ".desc")); }
            }
        }
    }
}
