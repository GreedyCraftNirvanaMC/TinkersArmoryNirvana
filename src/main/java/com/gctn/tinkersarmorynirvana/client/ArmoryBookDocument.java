/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.gctn.tinkersarmorynirvana.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.gctn.tinkersarmorynirvana.client.ArmoryBookContents.*;

/** Measure and paginate before rendering; long descriptions and addon entries must never be clipped away. */
final class ArmoryBookDocument {
    static final int WIDTH = 178, BOTTOM = 160;
    record Text(FormattedCharSequence line) {}
    record Placed(int y, Object content) {}
    record Leaf(String id, Component title, List<Placed> contents) {}
    final List<Leaf> leaves = new ArrayList<>();
    final Map<String, Integer> locations = new LinkedHashMap<>();
    private final Font font;
    private List<Placed> current;
    private Page source;
    private int y, continuation;

    ArmoryBookDocument(Font font, List<Page> pages) {
        this.font = font;
        for (var page : pages) {
            source = page; current = new ArrayList<>(); y = 20; continuation = 0;
            locations.putIfAbsent("section:" + page.section(), leaves.size());
            for (var content : page.contents()) { append(content); }
            finish();
        }
    }
    private void finish() {
        String id = source.id() + (continuation == 0 ? "" : "/" + continuation);
        locations.putIfAbsent(id, leaves.size());
        Component title = continuation == 0 ? source.title() : text("continued", source.title());
        leaves.add(new Leaf(id, title, List.copyOf(current))); current.clear(); continuation++; y = 20;
    }
    private void place(Object content, int height) {
        if (y + height > BOTTOM) { finish(); }
        current.add(new Placed(y, content)); y += height;
    }
    private void append(Content content) {
        if (content instanceof Paragraph paragraph) {
            // 按字体实际宽度逐行分页。不能在 render 中截取固定行数，否则长描述会永久丢失。
            for (var line : font.split(paragraph.text(), WIDTH)) { place(new Text(line), 10); }
            y += 4;
        } else if (content instanceof Icons icons) {
            for (int start = 0; start < icons.items().size(); start += 7) { place(new Icons(icons.items().subList(start, Math.min(start + 7, icons.items().size()))), 25); }
        } else if (content instanceof Grid grid) {
            for (int start = 0; start < grid.entries().size(); start += 25) {
                var batch = grid.entries().subList(start, Math.min(start + 25, grid.entries().size()));
                place(new Grid(batch), ((batch.size() + 4) / 5) * 27 + 4);
            }
        } else if (content instanceof Link) { place(content, 19); }
        else if (content instanceof Recipe) { place(content, 76); }
        else if (content instanceof Preview) { place(content, 83); }
    }
    int find(String target) { return locations.getOrDefault(target, 0); }
}
