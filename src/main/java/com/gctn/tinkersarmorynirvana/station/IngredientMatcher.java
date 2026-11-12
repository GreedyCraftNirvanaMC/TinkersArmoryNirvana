/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.station;

import net.minecraft.world.Container;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/** Bounded ingredient allocation, including overlapping item tags and split stacks. */
final class IngredientMatcher {
    private IngredientMatcher() {}
    static int[] allocate(Container input, List<IngredientCost> ingredients) {
        int sink = ingredients.size() + 6, size = sink + 1;
        int[][] capacity = new int[size][size];
        int total = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            var cost = ingredients.get(i); total += cost.count(); capacity[0][i + 1] = cost.count();
            for (int slot = 1; slot < 6; slot++) {
                if (cost.ingredient().test(input.getItem(slot))) { capacity[i + 1][ingredients.size() + slot] = 384; }
            }
        }
        for (int slot = 1; slot < 6; slot++) { capacity[ingredients.size() + slot][sink] = input.getItem(slot).getCount(); }
        int flow = 0;
        // 原料标签可能重叠，贪心扣料会误判。此网络最多 13 个节点，增广路径保证合法分配且可预测地终止。
        while (flow < total) {
            int[] parent = new int[size]; Arrays.fill(parent, -1); parent[0] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(0);
            while (!queue.isEmpty() && parent[sink] < 0) {
                int node = queue.removeFirst();
                for (int next = 1; next < size; next++) {
                    if (parent[next] < 0 && capacity[node][next] > 0) { parent[next] = node; queue.add(next); }
                }
            }
            if (parent[sink] < 0) { return null; }
            int amount = total - flow;
            for (int node = sink; node != 0; node = parent[node]) { amount = Math.min(amount, capacity[parent[node]][node]); }
            for (int node = sink; node != 0; node = parent[node]) { capacity[parent[node]][node] -= amount; capacity[node][parent[node]] += amount; }
            flow += amount;
        }
        int[] result = new int[6]; result[0] = 1;
        for (int slot = 1; slot < 6; slot++) {
            result[slot] = capacity[sink][ingredients.size() + slot];
            if (!input.getItem(slot).isEmpty() && result[slot] == 0) { return null; }
        }
        return result;
    }
}
