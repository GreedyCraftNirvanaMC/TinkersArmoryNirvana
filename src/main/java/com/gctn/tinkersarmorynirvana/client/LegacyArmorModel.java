/* Mesh adapted from C4's Tabula ModelConstructsArmor. Original UVs and cube geometry retained.
 * SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.EnumMap;

public final class LegacyArmorModel {
    private static final EnumMap<EquipmentSlot, HumanoidModel<?>> MODELS = new EnumMap<>(EquipmentSlot.class);
    private LegacyArmorModel() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static HumanoidModel<?> model(EquipmentSlot slot, HumanoidModel original) {
        HumanoidModel result = MODELS.computeIfAbsent(slot, key -> new EquippedModel(mesh(key).bakeRoot()));
        original.copyPropertiesTo(result);
        result.setAllVisible(false);
        switch (slot) {
            case HEAD -> result.head.visible = true;
            case CHEST -> { result.body.visible = true; result.leftArm.visible = true; result.rightArm.visible = true; }
            case LEGS -> { result.body.visible = true; result.leftLeg.visible = true; result.rightLeg.visible = true; }
            case FEET -> { result.leftLeg.visible = true; result.rightLeg.visible = true; }
            default -> { }
        }
        return result;
    }
    public static void select(EquipmentSlot slot, int layer) {
        if (MODELS.get(slot) instanceof EquippedModel model) {
            model.geometry = layer < 3 ? "armor" : com.gctn.tinkersarmorynirvana.api.ArmorVisuals.entries().get(layer - 3).geometry();
        }
    }
    private static final class EquippedModel extends HumanoidModel<net.minecraft.world.entity.LivingEntity> {
        private String geometry = "armor";
        private final java.util.Map<String, net.minecraft.client.model.geom.ModelPart> accessories = new java.util.HashMap<>();
        private EquippedModel(net.minecraft.client.model.geom.ModelPart root) {
            super(root);
            for (String name : java.util.List.of("belt", "goggles", "sack", "cloak")) { accessories.put(name, accessory(name)); }
        }
        @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
            if (geometry.equals("armor")) { super.renderToBuffer(pose, buffer, light, overlay, color); return; }
            var part = accessories.get(geometry);
            part.copyFrom(geometry.equals("goggles") ? head : body);
            part.render(pose, buffer, light, overlay, color);
        }
    }
    private static net.minecraft.client.model.geom.ModelPart accessory(String name) {
        MeshDefinition mesh = new MeshDefinition(); var root = mesh.getRoot(); int width = 32, height = 32;
        switch (name) {
            case "goggles" -> {
                width = 64;
                root.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5.5F, -5.4F, 10, 4, 10, new CubeDeformation(.7F)), PartPose.ZERO);
            }
            case "belt" -> {
                root.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0, -2.5F, 9, 2, 5, new CubeDeformation(.7F)), PartPose.offset(0, 9.5F, 0));
                root.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(0, 7).addBox(-2, -1, -3.3F, 4, 4, 1, new CubeDeformation(.4F)), PartPose.offset(0, 9.5F, 0));
            }
            case "sack" -> root.addOrReplaceChild("sack", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0, 4, 7, 9, 5, new CubeDeformation(.5F)), PartPose.ZERO);
            case "cloak" -> {
                width = 64; height = 64;
                root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 22).addBox(-5, -3.5F, -3, 10, 5, 13, new CubeDeformation(.45F)), PartPose.rotation(.13962634F, 0, 0));
                root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -3.5F, -4, 10, 12, 10, new CubeDeformation(.45F)), PartPose.offsetAndRotation(5, 0, 0, 0, -.13962634F, -.06981317F));
                root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 40).addBox(-5, -3.5F, -4, 10, 7, 10, new CubeDeformation(.45F)), PartPose.offsetAndRotation(-5, 0, 0, 0, .13962634F, .06981317F));
            }
            default -> throw new IllegalArgumentException("Unknown accessory geometry: " + name);
        }
        return LayerDefinition.create(mesh, width, height).bakeRoot();
    }
    private static LayerDefinition mesh(EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        var head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        var left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
        var right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
        var left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12, 0));
        var right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12, 0));
        if (slot == EquipmentSlot.HEAD) {
            var helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(32, 48).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.6F + 0.4F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, 0));
            var face = head.addOrReplaceChild("face", CubeListBuilder.create().texOffs(3, 10).addBox(-4.0F, -8.0F, -2.5F, 8, 8, 5, new CubeDeformation(0.6F + 0.7F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.5F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.CHEST) {
            var chestTop = body.addOrReplaceChild("chestTop", CubeListBuilder.create().texOffs(0, 23).addBox(-5.0F, 0.0F, -3.0F, 10, 6, 6, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, 0));
            var chestBack = chestTop.addOrReplaceChild("chestBack", CubeListBuilder.create().texOffs(50, 13).addBox(-3.0F, 0.0F, -0.5F, 6, 3, 1, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0.0F, 6.1F, 2.3F, 0, 0, 0));
            var chestBottom = chestTop.addOrReplaceChild("chestBottom", CubeListBuilder.create().texOffs(0, 55).addBox(-4.0F, 0.0F, -2.0F, 8, 5, 4, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0, 0, 0));
            var chestFront = chestTop.addOrReplaceChild("chestFront", CubeListBuilder.create().texOffs(50, 13).addBox(-3.0F, 0.0F, -0.5F, 6, 3, 1, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0.0F, 6.1F, -2.3F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.CHEST) {
            var shoulderLeft = left_arm.addOrReplaceChild("shoulderLeft", CubeListBuilder.create().texOffs(44, 28).addBox(-1.0F, -2.0F, -2.5F, 5, 4, 5, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.17453292519943295F));
            var shoulderLeftEx = left_arm.addOrReplaceChild("shoulderLeftEx", CubeListBuilder.create().texOffs(52, 20).addBox(-1.0F, 0.0F, -2.0F, 2, 4, 4, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(2.6F, -1.5F, 0.0F, 0.0F, 0.0F, -0.08726646259971647F));
            var gauntletLeft = left_arm.addOrReplaceChild("gauntletLeft", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -2.0F, -2.5F, 4, 5, 5, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(0.0F, 7.50F, 0.0F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.CHEST) {
            var shoulderRight = right_arm.addOrReplaceChild("shoulderRight", CubeListBuilder.create().texOffs(44, 28).mirror().addBox(-4.0F, -2.0F, -2.5F, 5, 4, 5, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.17453292519943295F));
            var shoulderRightEx = right_arm.addOrReplaceChild("shoulderRightEx", CubeListBuilder.create().texOffs(52, 20).mirror().addBox(-1.0F, 0.0F, -2.0F, 2, 4, 4, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(-2.6F, -1.5F, 0.0F, 0.0F, 0.0F, 0.08726646259971647F));
            var gauntletRight = right_arm.addOrReplaceChild("gauntletRight", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(-3.0F, -2.0F, -2.5F, 4, 5, 5, new CubeDeformation(0.6F + 0.2F)), PartPose.offsetAndRotation(0.0F, 7.50F, 0.0F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.LEGS) {
            var belt = body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -2.5F, 9, 2, 5, new CubeDeformation(0.6F - 0.2F)), PartPose.offsetAndRotation(0.0F, 9.5F, 0.0F, 0, 0, 0));
            var skirtBack = body.addOrReplaceChild("skirtBack", CubeListBuilder.create().texOffs(54, 0).addBox(-2.0F, 0.0F, -0.5F, 4, 6, 1, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, 2.5F, 0.08726646259971647F, 0.0F, 0.0F));
            var skirtFront = body.addOrReplaceChild("skirtFront", CubeListBuilder.create().texOffs(54, 7).addBox(-2.0F, 0.0F, -0.5F, 4, 5, 1, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(0.0F, 11.5F, -2.5F, -0.08726646259971647F, 0.0F, 0.0F));
            var skirtLeft = body.addOrReplaceChild("skirtLeft", CubeListBuilder.create().texOffs(36, 10).addBox(-1.5F, 0.0F, -2.5F, 2, 6, 5, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(4.5F, 11.5F, 0.0F, 0.0F, 0.0F, -0.20943951023931953F));
            var skirtRight = body.addOrReplaceChild("skirtRight", CubeListBuilder.create().texOffs(36, 10).mirror().addBox(-0.5F, 0.0F, -2.5F, 2, 6, 5, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(-4.5F, 11.5F, 0.0F, 0.0F, 0.0F, 0.20943951023931953F));
        }
        if (slot == EquipmentSlot.LEGS) {
            var legLeft = left_leg.addOrReplaceChild("legLeft", CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0, 0.0F, 0.0F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.LEGS) {
            var legRight = right_leg.addOrReplaceChild("legRight", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.6F + 0.1F)), PartPose.offsetAndRotation(0, 0.0F, 0.0F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.FEET) {
            var bootPlateLeft = left_leg.addOrReplaceChild("bootPlateLeft", CubeListBuilder.create().texOffs(38, 21).addBox(-2.5F, 0.0F, -1.0F, 5, 5, 2, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(0.1F, 5.1F, -2.0F, 0, 0, 0));
            var bootLeft = left_leg.addOrReplaceChild("bootLeft", CubeListBuilder.create().texOffs(16, 43).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F + 0.4F)), PartPose.offsetAndRotation(0, 6.0F, 0.0F, 0, 0, 0));
        }
        if (slot == EquipmentSlot.FEET) {
            var bootPlateRight = right_leg.addOrReplaceChild("bootPlateRight", CubeListBuilder.create().texOffs(38, 21).mirror().addBox(-2.5F, 0.0F, -1.0F, 5, 5, 2, new CubeDeformation(0.6F + 0.0F)), PartPose.offsetAndRotation(-0.1F, 5.1F, -2.0F, 0, 0, 0));
            var bootRight = right_leg.addOrReplaceChild("bootRight", CubeListBuilder.create().texOffs(16, 43).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F + 0.4F)), PartPose.offsetAndRotation(0, 6.0F, 0.0F, 0, 0, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
