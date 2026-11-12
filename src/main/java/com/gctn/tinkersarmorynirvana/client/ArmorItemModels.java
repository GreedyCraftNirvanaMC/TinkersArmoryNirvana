/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmorVisuals;
import com.gctn.tinkersarmorynirvana.armor.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.util.*;

/** Combine original material and modifier sprites at render time; cached models are cleared on resource reload. */
@EventBusSubscriber(modid = "conarm", value = Dist.CLIENT)
public final class ArmorItemModels {
    private static final String[] VARIANTS = {"", "_paper", "_cactus", "_contrast"};
    private ArmorItemModels() {}
    private static ModelResourceLocation model(String path) { return ModelResourceLocation.standalone(ArmoryContent.id("item/dynamic/" + path)); }
    @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event) {
        for (String armor : ArmoryContent.ARMORS.keySet()) {
            for (String part : List.of("core", "plate", "trim", "broken_core")) { for (String variant : VARIANTS) { event.register(model(armor + "/" + part + variant)); } }
            for (var overlay : ArmorVisuals.entries()) {
                if (overlay.id().startsWith("conarm:")) { event.register(model(armor + "/mod_" + overlay.id().substring(7))); }
            }
        }
    }
    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event) {
        for (String armor : ArmoryContent.ARMORS.keySet()) {
            ModelResourceLocation key = new ModelResourceLocation(ArmoryContent.id(armor), "inventory");
            BakedModel original = event.getModels().get(key);
            if (original != null) { event.getModels().put(key, new Dynamic(original, armor, event.getModels())); }
        }
    }
    private static final class Dynamic extends BakedModelWrapper<BakedModel> {
        private final String armor;
        private final Map<ModelResourceLocation, BakedModel> models;
        private final Map<String, BakedModel> cache = new LinkedHashMap<>();
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel base, ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) { return resolveArmor(stack); }
        };
        private Dynamic(BakedModel original, String armor, Map<ModelResourceLocation, BakedModel> models) { super(original); this.armor = armor; this.models = models; }
        @Override public ItemOverrides getOverrides() { return overrides; }
        private BakedModel resolveArmor(ItemStack stack) {
            var data = ArmorData.read(stack); List<Layer> layers = new ArrayList<>(); StringBuilder key = new StringBuilder();
            String[] parts = {ArmorData.broken(stack) ? "broken_core" : "core", "plate", "trim"};
            String[] materials = {data.getString("Core"), data.getString("Plates"), data.getString("Trim")};
            for (int i = 0; i < 3; i++) {
                if (ArmorData.broken(stack) && i > 0) { break; }
                String suffix = materials[i].equals("paper") || materials[i].equals("cactus") ? "_" + materials[i] : "";
                String path = armor + "/" + parts[i] + suffix; key.append(path).append(';');
                BakedModel model = models.get(model(path)); if (model != null) { layers.add(new Layer(model, i)); }
            }
            var overlays = ArmorVisuals.entries();
            if (!ArmorData.broken(stack) && ArmorData.level(stack, "incognito") == 0) {
                for (int i = 0; i < overlays.size(); i++) {
                    var overlay = overlays.get(i);
                    if (ArmorData.level(stack, overlay.id()) == 0 && !(overlay.id().equals("conarm:polished") && data.contains("Polished"))) { continue; }
                    String name = overlay.id().substring(overlay.id().indexOf(':') + 1); key.append(name).append(';');
                    BakedModel model = models.get(model(armor + "/mod_" + name)); if (model != null) { layers.add(new Layer(model, i + 3)); }
                }
            }
            if (cache.size() >= 512) { cache.clear(); }
            return cache.computeIfAbsent(key.toString(), ignored -> new Composite(originalModel, List.copyOf(layers)));
        }
    }
    private record Layer(BakedModel model, int tint) {}
    private static final class Composite extends BakedModelWrapper<BakedModel> {
        private final List<Layer> layers;
        private Composite(BakedModel original, List<Layer> layers) { super(original); this.layers = layers; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
        @Override public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) { originalModel.applyTransform(context, pose, leftHand); return this; }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return getQuads(state, side, random, ModelData.EMPTY, null); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            List<BakedQuad> quads = new ArrayList<>();
            for (var layer : layers) {
                for (var quad : layer.model().getQuads(state, side, random, data, renderType)) {
                    quads.add(new BakedQuad(quad.getVertices(), layer.tint(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
                }
            }
            return quads;
        }
    }
}
