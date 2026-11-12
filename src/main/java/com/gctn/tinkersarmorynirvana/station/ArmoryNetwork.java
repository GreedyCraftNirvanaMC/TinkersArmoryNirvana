/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.armor.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.Map;

public final class ArmoryNetwork {
    private static final Map<String, EquipmentSlot> ACCESSORIES = Map.of("travel_belt", EquipmentSlot.LEGS,
            "potion_belt", EquipmentSlot.LEGS, "travel_sack", EquipmentSlot.CHEST, "travel_goggles", EquipmentSlot.HEAD,
            "night_vision", EquipmentSlot.HEAD, "soul_sight", EquipmentSlot.HEAD, "travel_sneak", EquipmentSlot.CHEST, "travel_slowfall", EquipmentSlot.CHEST);
    private ArmoryNetwork() {}
    public static void register(IEventBus bus) { bus.addListener(ArmoryNetwork::payloads); }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Action.TYPE, Action.STREAM, (payload, context) -> context.enqueueWork(() -> {
            var player = context.player(); EquipmentSlot slot = ACCESSORIES.get(payload.accessory);
            if (slot == null || !player.isAlive() || player.containerMenu != player.inventoryMenu) { return; }
            itemAction(player, slot, payload.accessory);
        }));
        registrar.playToServer(Name.TYPE, Name.STREAM, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ArmoryMenu menu && menu.containerId == payload.container && menu.stillValid(context.player())) { menu.rename(payload.name); }
        }));
    }
    private static void itemAction(net.minecraft.world.entity.player.Player player, EquipmentSlot slot, String id) {
        var armor = player.getItemBySlot(slot);
        if (!ArmorData.initialized(armor) || ArmorData.broken(armor) || ArmorData.level(armor, id) == 0) { return; }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("conarm:LastAction") == now) { return; }
        player.getPersistentData().putLong("conarm:LastAction", now);
        if (id.equals("travel_belt")) { AccessoryMenu.swapHotbar(player, armor); }
        else if (id.equals("potion_belt") || id.equals("travel_sack")) {
            player.openMenu(new SimpleMenuProvider((container, inventory, owner) -> new AccessoryMenu(container, inventory, slot, id), Component.translatable("modifier.conarm." + id + ".name")));
        } else {
            if (!ArmorData.enabled(armor, id) && !id.equals("travel_goggles")) { ArmorData.damage(armor, player, player.damageSources().generic(), 3); }
            ArmorData.toggle(armor, id);
            player.displayClientMessage(Component.translatable(ArmorData.enabled(armor, id) ? "message.conarm.enabled" : "message.conarm.disabled", Component.translatable("modifier.conarm." + id + ".name")), true);
        }
    }
    public record Action(String accessory) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ArmoryContent.id("accessory"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> STREAM = StreamCodec.of((buffer, payload) -> buffer.writeUtf(payload.accessory, 64), buffer -> new Action(buffer.readUtf(64)));
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Name(int container, String name) implements CustomPacketPayload {
        public static final Type<Name> TYPE = new Type<>(ArmoryContent.id("name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Name> STREAM = StreamCodec.of((buffer, payload) -> { buffer.writeVarInt(payload.container); buffer.writeUtf(payload.name, 50); }, buffer -> new Name(buffer.readVarInt(), buffer.readUtf(50)));
        @Override public Type<Name> type() { return TYPE; }
    }
}
