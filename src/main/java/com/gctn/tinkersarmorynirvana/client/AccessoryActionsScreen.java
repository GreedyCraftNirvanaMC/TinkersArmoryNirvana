/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import com.gctn.tinkersarmorynirvana.armor.*;
import com.gctn.tinkersarmorynirvana.station.ArmoryNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class AccessoryActionsScreen extends Screen {
    public AccessoryActionsScreen() { super(Component.translatable("gui.conarm.accessories")); }
    @Override protected void init() {
        if (minecraft.player == null) { return; }
        int row = 0;
        for (String id : List.of("travel_belt", "potion_belt", "travel_sack", "travel_goggles", "night_vision", "soul_sight", "travel_sneak", "travel_slowfall")) {
            boolean installed = GameplayEvents.armor(minecraft.player).stream().anyMatch(stack -> ArmorData.level(stack, id) > 0);
            Button button = Button.builder(Component.translatable("modifier.conarm." + id + ".name"), ignored -> {
                minecraft.setScreen(null); net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArmoryNetwork.Action(id));
            }).bounds(width / 2 - 100, height / 2 - 88 + row++ * 22, 200, 20).build();
            button.active = installed; addRenderableWidget(button);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial); graphics.drawCenteredString(font, title, width / 2, height / 2 - 108, 0xfff0dfbd);
    }
}
