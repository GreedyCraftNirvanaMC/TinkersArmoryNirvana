/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.armor.ArmorData;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Local no-depth soul markers; no server entity flags or effects are modified. */
@EventBusSubscriber(modid = "conarm", value = Dist.CLIENT)
public final class SoulSightRenderer extends RenderType {
    private static final RenderType SOUL = create("conarm_soul", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(ArmoryContent.id("textures/particles/soul.png"), false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    private SoulSightRenderer() { super("conarm_soul", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, () -> {}, () -> {}); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) { return; }
        var minecraft = Minecraft.getInstance(); if (minecraft.player == null || minecraft.level == null) { return; }
        var helmet = minecraft.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (ArmorData.broken(helmet) || ArmorData.level(helmet, "soul_sight") == 0 || !ArmorData.enabled(helmet, "soul_sight")) { return; }
        var entities = minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                minecraft.player.getBoundingBox().inflate(20), entity -> entity != minecraft.player && entity.isAlive());
        var source = minecraft.renderBuffers().bufferSource(); var buffer = source.getBuffer(SOUL); var camera = event.getCamera();
        var pose = event.getPoseStack();
        for (int i = 0; i < Math.min(64, entities.size()); i++) {
            var entity = entities.get(i); var position = entity.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false)).subtract(camera.getPosition());
            pose.pushPose(); pose.translate(position.x, position.y + entity.getBbHeight() * .65, position.z); pose.mulPose(camera.rotation());
            var matrix = pose.last().pose();
            vertex(buffer, matrix, -.2F, -.2F, 0, 1); vertex(buffer, matrix, .2F, -.2F, 1, 1);
            vertex(buffer, matrix, .2F, .2F, 1, 0); vertex(buffer, matrix, -.2F, .2F, 0, 0);
            pose.popPose();
        }
        source.endBatch(SOUL);
    }
    private static void vertex(VertexConsumer buffer, org.joml.Matrix4f matrix, float x, float y, float u, float v) {
        buffer.addVertex(matrix, x, y, 0).setColor(255, 255, 255, 210).setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(0xf000f0).setNormal(0, 0, 1);
    }
}
