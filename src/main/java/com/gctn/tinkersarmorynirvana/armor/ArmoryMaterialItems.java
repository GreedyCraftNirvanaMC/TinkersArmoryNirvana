/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.armor;

import com.gctn.tconstruct.library.materials.Material;
import com.gctn.tconstruct.library.materials.MaterialInputs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.IdentityHashMap;
import java.util.Map;

/** A per-book snapshot of the same material-input selection used by TinkersNirvana's guide. */
public final class ArmoryMaterialItems {
    private final Map<Material, ItemStack> items = new IdentityHashMap<>();
    public ArmoryMaterialItems() {
        var priorities = new IdentityHashMap<Material, Integer>();
        // 每次打开书只扫描一次当前物品与标签。不要永久缓存，否则数据包/KubeJS 修改原料后图标会过期。
        for (var item : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            var match = MaterialInputs.match(stack);
            if (match == null) { continue; }
            boolean ingot = stack.getTags().anyMatch(tag -> tag.location().getNamespace().equals("c")
                    && (tag.location().getPath().equals("ingots") || tag.location().getPath().startsWith("ingots/")));
            int priority = ingot ? 0 : Math.abs(match.value() - 1) < .00001F ? 1 : 2;
            if (priority < priorities.getOrDefault(match.material(), Integer.MAX_VALUE)) {
                priorities.put(match.material(), priority); items.put(match.material(), stack);
            }
        }
    }
    /** Explicit addon/KubeJS representatives take priority; callers receive an independent single item. */
    public ItemStack representative(Material material) {
        var explicit = material.getRepresentativeItem();
        return explicit.isEmpty() ? items.getOrDefault(material, ItemStack.EMPTY).copy() : explicit;
    }
}
