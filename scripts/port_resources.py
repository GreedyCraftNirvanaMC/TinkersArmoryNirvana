"""Convert AntiqueArmory's LGPL resources without editing the original checkout.

Usage: python -X utf8 scripts/port_resources.py --legacy /path/to/AntiqueArmory-1.12.2
The community language modpack is not copied: its separate license is not LGPL.
"""
from pathlib import Path
import argparse
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
PARTS = ["helmet_core", "chest_core", "leggings_core", "boots_core", "armor_plate", "armor_trim", "polishing_kit"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]


def write(relative, value):
    target = RESOURCES / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def language(path):
    result = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                result[key] = value.replace("\\n", "\n")
    return result


def convert_ingredient(value):
    if isinstance(value, list):
        return [convert_ingredient(entry) for entry in value]
    ores = {
        "workbench": {"item": "minecraft:crafting_table"}, "ingotIron": {"tag": "c:ingots/iron"},
        "leather": {"tag": "c:leathers"}, "blockQuartz": {"item": "minecraft:quartz_block"},
        "blockLapis": {"item": "minecraft:lapis_block"}, "blockRedstone": {"item": "minecraft:redstone_block"},
        "gemLapis": {"item": "minecraft:lapis_lazuli"}, "blockGlass": {"tag": "c:glass_blocks"},
        "enderpearl": {"tag": "c:ender_pearls"}, "obsidian": {"item": "minecraft:obsidian"},
        "blockIron": {"tag": "c:storage_blocks/iron"}, "chestWood": {"tag": "c:chests/wooden"},
        "ingotAlubrass": {"tag": "c:ingots/alubrass"}, "ingotAluminum": {"tag": "c:ingots/aluminum"},
        "wool": {"tag": "minecraft:wool"}, "lilypad": {"item": "minecraft:lily_pad"},
        "feather": {"tag": "c:feathers"}, "dustGlowstone": {"tag": "c:dusts/glowstone"},
        "blockSlimeCongealed": [{"item": "tconstruct:slime_congealed_" + color} for color in ["green", "blue", "purple", "magma", "blood"]],
    }
    if value.get("type") == "forge:ore_dict":
        return ores[value["ore"]]
    item = value["item"]
    if item == "#REINFORCEMENT_CENTER":
        return {"tag": "conarm:casts"}
    return {"item": {"minecraft:brick_block": "minecraft:bricks"}.get(item, item)}


def modifier_recipe(name, modifier, ingredients, points=1):
    write("data/conarm/recipe/modifiers/" + name + ".json", {
        "type": "conarm:armor_modifier", "modifier": "conarm:" + modifier, "points": points,
        "ingredients": [{"ingredient": {"tag" if item.startswith("#") else "item": item.lstrip("#")}, "count": count}
                        for item, count in ingredients],
    })


def main(legacy):
    source = legacy / "src/main/resources/assets/conarm"
    assets = RESOURCES / "assets/conarm"
    for directory in ["textures", "book"]:
        shutil.copytree(source / directory, assets / directory, dirs_exist_ok=True)
    # 1.21 默认图集只收集 item/block；保留旧资源 ID 时必须显式收集 items/blocks。
    write("assets/minecraft/atlases/blocks.json", {"sources": [
        {"type": "minecraft:directory", "source": "items", "prefix": "items/"},
        {"type": "minecraft:directory", "source": "blocks", "prefix": "blocks/"},
    ]})
    for book_json in (assets / "book").rglob("*.json"):
        text = re.sub(r'("coverColor"\s*:\s*)0x([0-9a-fA-F]+)', lambda match: match[1] + str(int(match[2], 16)), book_json.read_text(encoding="utf-8"))
        book_json.write_text(json.dumps(json.loads(text), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for model in (source / "models/item").glob("*.json"):
        write("assets/conarm/models/item/" + model.name, json.loads(model.read_text(encoding="utf-8")))
    for part in PARTS:
        write(f"assets/conarm/models/item/{part}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"conarm:items/parts/{part}"},
              "overrides": [{"predicate": {"conarm:material_style": style}, "model": f"conarm:item/{part}_{name}"} for name, style in [("paper", 1), ("cactus", 2)]]})
        for name in ["paper", "cactus"]:
            write(f"assets/conarm/models/item/{part}_{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"conarm:items/parts/{part}_{name}"}})
        for cast in ["cast", "clay_cast"]:
            write(f"assets/conarm/models/item/{cast}_{part}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"conarm:items/parts/{part}"}})
    write("assets/conarm/models/item/pattern.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "tconstruct:item/toolsupplies/pattern"}})
    for armor in ARMOR:
        write(f"assets/conarm/models/item/{armor}.json", {"parent": "minecraft:item/generated", "textures": {
            "layer0": f"conarm:items/{armor}/core", "layer1": f"conarm:items/{armor}/plate", "layer2": f"conarm:items/{armor}/trim"}})
        for part in ["core", "plate", "trim", "broken_core"]:
            for suffix in ["", "_paper", "_cactus", "_contrast"]:
                write(f"assets/conarm/models/item/dynamic/{armor}/{part}{suffix}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"conarm:items/{armor}/{part}{suffix}"}})
        visuals = ["diamond", "emerald", "reinforced", "resistant", "fire_resistant", "blast_resistant", "projectile_resistant", "speedy", "shulkerweight",
                   "mending", "parasitic", "sticky", "amphibious", "glowing", "high_stride", "frostwalker", "powerful", "dexterous", "telekinetic", "soulbound", "polished",
                   "travel_belt", "potion_belt", "travel_sack", "travel_goggles", "night_vision", "soul_sight", "travel_sneak", "travel_slowfall"]
        aliases = {"potion_belt": "travel_belt", "night_vision": "travel_goggles", "soul_sight": "travel_goggles", "travel_sneak": "travel_cloak", "travel_slowfall": "travel_cloak"}
        for visual in visuals:
            texture = f"items/{armor}/mod_{aliases.get(visual, visual)}_armor"
            textures = {"layer0": "conarm:" + texture} if (source / f"textures/{texture}.png").exists() else {}
            write(f"assets/conarm/models/item/dynamic/{armor}/mod_{visual}.json", {"parent": "minecraft:item/generated", "textures": textures})
    # 直接保留原工作台网格，只转换新版纹理路径与方块状态格式。
    for name in ["armorstation", "armorforge"]:
        model = json.loads((source / f"models/block/{name}.json").read_text(encoding="utf-8"))
        if name == "armorstation":
            # 木台保留原有匠魂桌面与桌腿，不复用更薄的金属锻造台网格。
            model["textures"].update({"side": "tconstruct:block/table_side", "leg": "tconstruct:block/table_side",
                                      "bottom": "minecraft:block/oak_planks", "legBottom": "minecraft:block/oak_planks"})
            # 新依赖将桌腿底部纹理变量改成了 snake_case；父模型中的 #leg_bottom 必须实际绑定。
            model["textures"]["leg_bottom"] = model["textures"].pop("legBottom")
        else:
            # 原 forge:default-block 由旧 blockstate 注入；新版通过父模型继承物品的 GUI／手持变换。
            model["parent"] = "minecraft:block/block"
            model["textures"].update({key: "minecraft:block/iron_block" for key in ["bottom", "leg", "legBottom"]})
        write(f"assets/conarm/models/block/{name}.json", model)
        write(f"assets/conarm/models/item/{name}.json", {"parent": f"conarm:block/{name}"})
        write(f"assets/conarm/blockstates/{name}.json", {"variants": {f"facing={direction}": {"model": f"conarm:block/{name}", "y": angle}
                                                                       for direction, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}})
        write(f"data/conarm/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "entries": [{"type": "minecraft:item", "name": f"conarm:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write("assets/conarm/blockstates/soft_obsidian.json", {"variants": {f"age={age}": {"model": f"conarm:block/soft_obsidian_{age}"} for age in range(4)}})
    for age in range(4):
        write(f"assets/conarm/models/block/soft_obsidian_{age}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"conarm:blocks/soft_obsidian_{age}" if age else "minecraft:block/obsidian"}})
    write("assets/conarm/models/block/soft_obsidian.json", {"parent": "conarm:block/soft_obsidian_0"})
    write("assets/conarm/models/item/soft_obsidian.json", {"parent": "conarm:block/soft_obsidian_0"})
    write("data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ["conarm:armorstation"]})
    write("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": ["conarm:armorforge", "conarm:soft_obsidian"]})
    write("data/conarm/tags/item/casts.json", {"replace": False, "values": [f"conarm:cast_{part}" for part in PARTS] +
          ["tconstruct:cast_blank", "tconstruct:cast_ingot", "tconstruct:cast_nugget", "tconstruct:cast_gem"]})
    for recipe in (source / "recipes").glob("*.json"):
        if recipe.name.startswith("_"):
            continue
        data = json.loads(recipe.read_text(encoding="utf-8"))
        data["type"] = "minecraft:crafting_shaped" if "pattern" in data else "minecraft:crafting_shapeless"
        if "key" in data:
            data["key"] = {key: convert_ingredient(value) for key, value in data["key"].items()}
        else:
            data["ingredients"] = [convert_ingredient(value) for value in data["ingredients"]]
        data["result"] = {"id": data["result"]["item"], "count": data["result"].get("count", 1)}
        write("data/conarm/recipe/" + recipe.name, data)
    for metal in ["iron", "gold", "copper", "cobalt", "ardite", "manyullyn", "bronze", "steel", "aluminum", "lead", "silver", "electrum"]:
        write(f"data/conarm/recipe/armorforge_{metal}.json", {"type": "minecraft:crafting_shaped", "pattern": ["BBB", "MTM", "M M"],
              "key": {"B": {"item": "tconstruct:seared_bricks"}, "M": {"tag": f"c:storage_blocks/{metal}"}, "T": {"item": "conarm:armorstation"}}, "result": {"id": "conarm:armorforge"}})
    write("data/conarm/recipe/kit_repair.json", {"type": "conarm:kit_repair", "category": "equipment"})
    single = {"diamond": "minecraft:diamond", "emerald": "minecraft:emerald", "soulbound": "minecraft:nether_star", "reinforced": "tconstruct:reinforcement",
              "parasitic": "tconstruct:necrotic_bone", "mending": "tconstruct:mending_moss", "concealed": "conarm:invisible_ink", "sticky": "minecraft:cobweb",
              "resistant": "conarm:resist_mat", "fire_resistant": "conarm:resist_mat_fire", "blast_resistant": "conarm:resist_mat_blast", "projectile_resistant": "conarm:resist_mat_proj",
              "powerful": "conarm:gauntlet_mat_attack", "dexterous": "conarm:gauntlet_mat_speed", "telekinetic": "conarm:gauntlet_mat_reach", "frostwalker": "conarm:frosty_soles",
              "travel_belt": "conarm:travel_belt", "potion_belt": "conarm:travel_potion", "travel_sack": "conarm:travel_sack", "travel_goggles": "conarm:travel_goggles",
              "night_vision": "conarm:travel_night", "soul_sight": "conarm:travel_soul", "travel_sneak": "conarm:travel_sneak", "travel_slowfall": "conarm:travel_slowfall",
              "creative": "tconstruct:creative_modifier", "endearment": "minecraft:dragon_head", "incognito": "minecraft:wet_sponge"}
    for modifier, item in single.items():
        modifier_recipe(modifier, "extramodifier" if modifier == "endearment" else modifier, [(item, 1)])
    modifier_recipe("speedy", "speedy", [("minecraft:redstone", 1)])
    modifier_recipe("speedy_block", "speedy", [("minecraft:redstone_block", 1)], 9)
    modifier_recipe("shulkerweight", "shulkerweight", [("minecraft:popped_chorus_fruit", 1)])
    modifier_recipe("high_stride", "high_stride", [("minecraft:piston", 2)])
    modifier_recipe("glowing", "glowing", [("minecraft:glowstone_dust", 2), ("minecraft:ender_eye", 1)])
    modifier_recipe("amphibious", "amphibious", [("minecraft:glass", 2), ("minecraft:prismarine_crystals", 1)])
    # Potion ingredients compare the actual data component, rather than accepting every potion.
    for name, potion, items in [("travel_night", "night_vision", ["minecraft:golden_carrot", "minecraft:flint_and_steel", "conarm:travel_goggles_base"]),
                                ("travel_sneak", "invisibility", ["minecraft:golden_carrot", "minecraft:ender_eye", "minecraft:fermented_spider_eye", "conarm:travel_cloak"])]:
        write(f"data/conarm/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [
            {"type": "neoforge:components", "items": "minecraft:potion", "components": {"minecraft:potion_contents": {"potion": f"minecraft:{potion}"}}, "strict": False}
        ] + [{"item": item} for item in items], "result": {"id": f"conarm:{name}"}})
    generate_languages(source)
    licenses = ROOT / "licenses"
    licenses.mkdir(exist_ok=True)
    shutil.copyfile(legacy / "LICENSE", ROOT / "LICENSE")
    shutil.copyfile(legacy / "docs/licenses/GPL", licenses / "GPL-3.0.txt")
    shutil.copyfile(legacy / "docs/licenses/GPL.LESSER", licenses / "LGPL-3.0.txt")


def generate_languages(source):
    dependency = source.parents[5] / "NeoForge/TinkersNirvana-1.21.1/src/main/resources/assets/tconstruct/lang"
    traits = set(re.findall(r'trait\("([a-z_]+)"\)', (ROOT / "src/main/java/com/gctn/tinkersarmorynirvana/armor/BuiltinTraits.java").read_text(encoding="utf-8")))
    traits.update(["slimey_green", "slimey_blue"])
    modifiers = set(re.findall(r'modifier\("([a-z_]+)"\)', (ROOT / "src/main/java/com/gctn/tinkersarmorynirvana/armor/BuiltinModifiers.java").read_text(encoding="utf-8")))
    modifiers.update(["resistant", "fire_resistant", "blast_resistant", "projectile_resistant"])
    for path in (source / "lang").glob("*.lang"):
        data = language(path)
        result = {key: value for key, value in data.items() if not key.startswith(("conarm.general.leveling.", "tooltip.level", "tooltip.xp"))}
        for key, value in data.items():
            if key.startswith("item.conarm.") and key.endswith(".name"):
                result[key[:-5]] = value
            if key.startswith("tile.conarm.") and key.endswith(".name"):
                result[key.replace("tile.", "block.")[:-5]] = value
        for kind, names in [("trait", traits), ("modifier", modifiers)]:
            for name in sorted(names):
                legacy = "slimey" if name.startswith("slimey_") else name
                for suffix in ["name", "desc"]:
                    value = data.get(f"modifier.{legacy}_armor.{suffix}", data.get(f"modifier.{legacy}.{suffix}", name.replace("_", " ").title()))
                    if value.startswith("modifier."):
                        value = data.get(value, name.replace("_", " ").title())
                    result[f"{kind}.conarm.{name}.{suffix}"] = value
        result.update(extra_translations(path.stem == "zh_cn"))
        names = {
            "writable": ("Writable", "铭刻"), "slimey_green": ("Slimey", "绿史莱姆"), "slimey_blue": ("Slimey", "蓝史莱姆"),
            "baconlicious": ("Baconlicious", "培根美味"), "cheapskate": ("Cheapskate", "吝啬"), "cheap": ("Cheap", "廉价"),
            "heavy": ("Heavy", "厚重"), "tasty": ("Tasty", "美味"), "alien": ("Alien", "异星"), "aridiculous": ("Aridiculous", "干旱"),
            "magnetic": ("Magnetic", "磁力"), "dense": ("Dense", "致密"), "lightweight": ("Lightweight", "轻盈"),
            "duritos_ranch": ("Duritae", "持久"), "ecological": ("Ecological", "生态"), "reinforced": ("Reinforced", "加固"),
            "emerald": ("Emerald", "绿宝石"), "mending": ("Mending", "修补"), "creative": ("Creative", "创造强化"),
            "soulbound": ("Soulbound", "灵魂绑定"), "diamond": ("Diamond", "钻石"), "extramodifier": ("Extra Modifier", "额外强化")}
        descriptions = {
            "writable": ("Provides additional modifier slots.", "增加可用强化槽。"),
            "slimey_green": ("Taking damage may spawn a small green slime.", "受伤时有机会出现小绿史莱姆。"),
            "slimey_blue": ("Taking damage may spawn a small blue slime.", "受伤时有机会出现小蓝史莱姆。"),
            "alien": ("Armor gradually develops additional durability, defense and toughness.", "护甲随使用逐渐获得额外耐久、防御和韧性。"),
            "ecological": ("Armor slowly repairs itself in your inventory.", "护甲会在物品栏中缓慢自我修复。"),
            "reinforced": ("Reduces durability consumption; full reinforcement prevents durability loss.", "减少耐久消耗，完全加固后不再损失耐久。"),
            "emerald": ("Increases durability and improves low toughness.", "提升耐久，并改善较低的韧性。"),
            "creative": ("Adds a modifier slot.", "增加可用强化槽。"),
            "diamond": ("Adds durability, defense and toughness.", "增加耐久、防御和韧性。"),
            "extramodifier": ("A dragon head grants one additional modifier slot, once per armor item.", "消耗龙首为这件护甲增加一个强化槽，仅可应用一次。")}
        for kind, identifiers in [("trait", traits), ("modifier", modifiers)]:
            for identifier in sorted(identifiers):
                if identifier in names:
                    result[f"{kind}.conarm.{identifier}.name"] = names[identifier][path.stem == "zh_cn"]
                if identifier in descriptions:
                    result[f"{kind}.conarm.{identifier}.desc"] = descriptions[identifier][path.stem == "zh_cn"]
        for part in PARTS:
            result.setdefault("item.conarm." + part, {"helmet_core": "头盔核心", "chest_core": "胸甲核心", "leggings_core": "护腿核心", "boots_core": "靴子核心", "armor_plate": "护甲板", "armor_trim": "护甲夹板", "polishing_kit": "抛光磨具"}.get(part, part) if path.stem == "zh_cn" else part.replace("_", " ").title())
            result[f"item.conarm.cast_{part}"] = result["item.conarm." + part] + ("铸模" if path.stem == "zh_cn" else " Cast")
            result[f"item.conarm.clay_cast_{part}"] = result["item.conarm." + part] + ("黏土铸模" if path.stem == "zh_cn" else " Clay Cast")
        for armor, chinese in zip(ARMOR, ["头盔", "胸甲", "护腿", "靴子"]):
            result["item.conarm." + armor] = chinese if path.stem == "zh_cn" else armor.title()
        # 旧版语言文件允许值引用其它键；原样转为 JSON 会把 stat.core.durability.name 显示给玩家。
        for key in list(result):
            seen = {key}
            while result[key] in result and result[key] not in seen:
                seen.add(result[key]); result[key] = result[result[key]]
        write(f"assets/conarm/lang/{path.stem}.json", result)


def extra_translations(chinese):
    pairs = {
        "itemGroup.conarm": ("Tinkers' Armory Nirvana", "匠魂护甲：涅槃"),
        "item.conarm.material_part": ("%s %s", "%s%s"), "item.conarm.pattern_for": ("%s Pattern", "%s模具"),
        "item.conarm.pattern": ("Armor Pattern", "护甲模具"),
        "tooltip.conarm.unbuilt": ("Assemble this armor at an Armor Station.", "请在装甲装配台组装护甲。"),
        "tooltip.conarm.broken": ("Broken — repair to restore protection.", "已损坏，修复后恢复防护。"),
        "tooltip.conarm.durability": ("Durability: %s / %s", "耐久：%s / %s"),
        "tooltip.conarm.defense": ("Armor: %s  Toughness: %s", "护甲：%s　韧性：%s"),
        "tooltip.conarm.modifiers": ("Free modifier slots: %s", "剩余强化槽：%s"),
        "tooltip.conarm.core_stats": ("Base durability: %s  Defense: %s", "基础耐久：%s　防御：%s"),
        "tooltip.conarm.plate_stats": ("Durability multiplier: %s  Bonus: %s  Toughness: %s", "耐久倍率：%s　附加耐久：%s　韧性：%s"),
        "tooltip.conarm.trim_stats": ("Extra durability: %s", "附加耐久：%s"),
        "tooltip.conarm.accessories": ("Press %s to use this armor's accessory.", "按%s使用这件护甲的配件。"),
        "gui.conarm.remaining_modifiers": ("Modifiers: %s", "剩余强化次数：%s"),
        "gui.error": ("ERROR", "错误"),
        "gui.toolstation.repair": ("Repair & Modify", "修复&强化"),
        "gui.toolstation.traits": ("Traits", "特性"),
        "gui.toolstation.components": ("Components", "组件"),
        "gui.toolstation.noTraits": ("None :[", "无 :["),
        "gui.general.hover": ("Hover over the entries for more info", "将指针悬停在条目上以获取更多信息"),
        "modifier.extratrait_armor.name": ("Embossment", "刻印"),
        "gui.armorstation.deconstruct": ("Deconstruct", "分解"),
        "gui.armorstation.deconstruct.info": ("Place intact, undamaged armor in the slot on the right to preview its parts. Take a part to deconstruct; modifiers are lost. Empty accessory inventories first.", "将完整、无损坏的护甲放入右侧槽位以预览部件。取出任一部件完成分解，强化不会保留。配件中的物品需要事先取空。"),
        "gui.conarm.name": ("Armor name", "护甲名称"), "gui.conarm.inputs": ("Materials", "投入材料"),
        "gui.conarm.help1": ("Assemble: core, plate, trim in the top row.", "组装：上排依次放入核心、护甲板、夹板。"),
        "gui.conarm.help2": ("Upgrade or repair: armor first, ingredients after it.", "强化或修复：首格放护甲，其余格放材料。"),
        "gui.conarm.help3": ("Parts: armor pattern first, then raw materials.", "部件：首格放护甲模具，其余格放原料。"),
        "gui.conarm.accessories": ("Armory accessories", "护甲配件"),
        "key.conarm.accessories": ("Open armor accessories", "打开护甲配件控制"), "key.categories.conarm": ("Tinkers' Armory", "匠魂护甲"),
        "message.conarm.enabled": ("%s enabled", "%s已启用"), "message.conarm.disabled": ("%s disabled", "%s已关闭"),
        "book.conarm.cover.title": ("Armory\nAddendum", "盔甲锻造\n手册"),
        "book.conarm.cover.subtitle": ("C4 · Nirvana", "C4 · 涅槃"),
        "book.conarm.contents": ("Contents", "目录"),
        "book.conarm.section.welcome": ("Welcome", "欢迎"),
        "book.conarm.section.armory": ("Making armor", "护甲锻造"),
        "book.conarm.section.materials": ("Armor materials", "护甲材料"),
        "book.conarm.section.traits": ("Material traits", "材料词条"),
        "book.conarm.section.modifiers": ("Armor modifiers", "护甲强化"),
        "book.conarm.section.accessories": ("Traveller accessories", "旅行配件"),
        "book.conarm.stations": ("Armor workshops", "护甲工作台"),
        "book.conarm.parts": ("Armor parts and casts", "部件与铸模"),
        "book.conarm.armor": ("Modular armor", "模块化护甲"),
        "book.conarm.repair": ("Repair and replacement", "修复与替换"),
        "book.conarm.finishing": ("Polishing and embossment", "抛光与浮雕"),
        "book.conarm.controls": ("Using accessories", "使用配件"),
        "book.conarm.assembly": ("A core, plates and trim form each armor piece. Put them in the top row of the Armor Station in that order. Each part can use a different material. Move the mouse over the preview to inspect the armor.", "每件护甲由核心、护甲板与夹板组成。在装甲装配台上排依次放入这三种部件即可组装，不同部件可以使用不同材料。移动鼠标可以观察预览人偶。"),
        "book.conarm.part.core": ("Core traits", "核心词条"),
        "book.conarm.part.plates": ("Plate traits", "护甲板词条"),
        "book.conarm.part.trim": ("Trim traits", "夹板词条"),
        "book.conarm.modifier.levels": ("Maximum level: %s\nPoints per level: %s", "最高等级：%s\n每级材料点数：%s"),
        "book.conarm.modifier.slots": ("Slots: %s for the first level; %s for each further level.", "首级消耗%s个强化槽；后续每级消耗%s个。"),
        "book.conarm.modifier.equipment": ("Equipment: %s", "可用护甲：%s"),
        "book.conarm.modifier.conflict": ("Incompatible: %s", "不兼容：%s"),
        "book.conarm.recipe.points": ("Workshop recipe · %s point(s)", "工作台配方 · 每次%s点"),
        "book.conarm.recipe.none": ("No workshop recipe is available in the current pack.", "当前整合包未提供此强化的工作台配方。"),
        "book.conarm.recipe.missing": ("No matching ingredient is available.", "没有可用的匹配原料。"),
        "book.conarm.accessory.controls": ("Install this accessory, equip the armor, then use the accessory key (G by default) to open its controls.", "安装配件并穿戴护甲后，按配件控制键（默认 G）打开操作菜单。"),
        "book.conarm.continued": ("%s (continued)", "%s（续）"),
        "book.conarm.previous": ("Previous spread", "上一页"),
        "book.conarm.next": ("Next spread", "下一页"),
        "book.conarm.back": ("Return to previous location", "返回跳转前的位置"),
        "book.conarm.open": ("Open the book", "打开手册"),
        "book.conarm.intro1": ("Welcome to the Armory Addendum. Make an Armor Station from a crafting table and four blank Tinkers patterns. Convert one blank pattern to an armor pattern in a crafting grid. Right-click the pattern to choose a part; sneak to cycle backwards.", "欢迎阅读《匠魂宝典：盔甲补充》。用工作台和四个匠魂空白模具制作装甲装配台。在合成栏将一个空白模具转换为护甲模具。手持护甲模具右键切换部件，潜行时反向切换。"),
        "book.conarm.intro2": ("Place an armor pattern in the first station slot and matching craftable raw materials after it. Core costs: helmet 4, chest 6, leggings 5, boots 4. Plates cost 3, trim 1, polishing kits 2. Castable metals use the smeltery: pour gold or aluminum brass over a part to make its cast.", "首格放护甲模具，其余格放可加工原料。头盔、胸甲、护腿、靴子的核心分别需4、6、5、4份材料；护甲板3份，夹板1份，抛光磨具2份。金属使用冶炼炉浇筑：在部件上浇注金或铝黄铜制作铸模。"),
        "book.conarm.intro3": ("Assemble armor with its core, plates and trim in the three top slots, in that order. Materials can differ. Repairs use the core material or a matching polishing kit. Replace parts by putting the armor first and the new parts after it. Broken armor stays intact and can be repaired.", "在上排依次放入核心、护甲板、夹板组装护甲，三种材料可以不同。首格放护甲，其余格放核心原料或同材质抛光磨具进行修复；放入新部件可替换材料。损坏的护甲会保留，修复后恢复防护。"),
        "book.conarm.intro4": ("Use an Armor Forge for operations requiring more than three inputs. Polish with armor, polishing kit and sand. Emboss with armor, a part, green/blue/magma slime crystals and a gold block. Modifiers use free slots and respect equipment restrictions and conflicts. Diamond and emerald improve durability.", "需要三个以上投入物的操作请使用装甲锻造台。抛光依次放入护甲、抛光磨具和沙子。浮雕依次放入护甲、部件、绿/蓝/熔岩史莱姆结晶、金块。强化需要剩余槽位，并遵守装备部位和互斥限制。钻石和绿宝石可以提升耐久。"),
        "book.conarm.intro5": ("Press the armor accessory key (G by default) to swap the belt hotbar, open the potion belt or knapsack, or toggle goggles and cloaks. Only installed, unbroken equipment can be used. Storage is saved on the armor. Addon and KubeJS definitions appear on the following pages.", "按护甲配件按键（默认G）可以交换腰带快捷栏、打开药水腰带或背包，以及切换护目镜与披风。只有已安装且未损坏的装备可以使用。库存保存在护甲上。后续页面显示当前材质和词条，包括附属与KubeJS新增内容。"),
        "book.conarm.material": ("%s\n\nCore durability: %s\nDefense: %s\nPlate multiplier: %s\nPlate durability: %s\nToughness: %s\nTrim durability: %s", "%s\n\n核心耐久：%s\n防御：%s\n护甲板倍率：%s\n护甲板耐久：%s\n韧性：%s\n夹板耐久：%s"),
        "book.conarm.modifier": ("\n\nMaximum level: %s\nPoints per level: %s\nSlots per level: %s", "\n\n最高等级：%s\n每级材料点数：%s\n每级强化槽：%s"),
    }
    return {key: value[1 if chinese else 0] for key, value in pairs.items()}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--legacy", required=True, type=Path)
    main(parser.parse_args().legacy)
