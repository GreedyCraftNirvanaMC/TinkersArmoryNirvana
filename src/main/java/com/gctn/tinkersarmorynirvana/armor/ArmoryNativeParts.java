/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tinkersarmorynirvana.ArmoryContent;
import com.gctn.tinkersarmorynirvana.api.ArmoryRegistry;
import com.gctn.tconstruct.library.toolparts.AntiqueParts;
import net.minecraft.network.chat.Component;

/** Register the existing conarm items in the native stencil, part-builder and part-storage catalogs. */
public final class ArmoryNativeParts {
    private static boolean initialized;
    private ArmoryNativeParts() {}
    public static void initialize() {
        if (initialized) { return; } initialized = true;
        ArmoryContent.PARTS.forEach((id, item) -> {
            String role = id.equals("armor_plate") ? "plates" : id.equals("armor_trim") ? "trim" : "core";
            // 谓词与工厂延迟读取当前注册表，KubeJS 增删／修改材料后无需再复制一份护甲数值。
            AntiqueParts.registerExternal("conarm:" + id, item.get().cost, "conarm:" + role, item,
                    material -> !material.isHidden() && ArmoryRegistry.material(material.identifier) != null,
                    material -> item.get().create(material.identifier), () -> Component.translatable(item.get().getDescriptionId()));
        });
    }
}
