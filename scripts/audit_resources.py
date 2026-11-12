"""Check converted resources without launching a client. Uses a built dependency's assets."""
from pathlib import Path
import argparse
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"


def main(tinkers):
    owners = {"conarm": RES, "tconstruct": tinkers / "src/main/resources"}
    failures = []
    # 检查图集中的逻辑 sprite ID，而不只检查 PNG 是否存在；旧版复数路径不会自动拼入新版图集。
    atlas_sprites = set()
    sources = [{"type": "minecraft:directory", "source": name, "prefix": name + "/"} for name in ["item", "block"]]
    for owner in owners.values():
        atlas = owner / "assets/minecraft/atlases/blocks.json"
        if atlas.exists():
            sources.extend(json.loads(atlas.read_text(encoding="utf-8"))["sources"])
    for source in sources:
        if source["type"] == "minecraft:single":
            atlas_sprites.add(source.get("sprite", source["resource"]))
        elif source["type"] == "minecraft:directory":
            for namespace, owner in owners.items():
                directory = owner / f"assets/{namespace}/textures" / source["source"]
                for png in directory.rglob("*.png"):
                    atlas_sprites.add(namespace + ":" + source["prefix"] + png.relative_to(directory).with_suffix("").as_posix())
    count = 0
    for path in RES.rglob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (ValueError, UnicodeError) as error:
            failures.append(f"{path.relative_to(ROOT)}: {error}")
            continue
        count += 1
        if path.is_relative_to(RES / "assets/conarm/models"):
            for value in data.get("textures", {}).values():
                if value.startswith("#"):
                    continue
                namespace, name = value.split(":", 1) if ":" in value else ("minecraft", value)
                if namespace in owners and not (owners[namespace] / f"assets/{namespace}/textures/{name}.png").is_file():
                    failures.append(f"{path.relative_to(ROOT)}: missing texture {value}")
                if namespace == "conarm" and value not in atlas_sprites:
                    failures.append(f"{path.relative_to(ROOT)}: texture is not collected by the blocks atlas: {value}")
            parent = data.get("parent", "")
            if ":" in parent:
                namespace, name = parent.split(":", 1)
                if namespace in owners and not (owners[namespace] / f"assets/{namespace}/models/{name}.json").is_file():
                    failures.append(f"{path.relative_to(ROOT)}: missing parent {parent}")
    # 3D 工作台不能只检查 PNG：父链还必须继承物品显示变换，否则手持模型会以整块大小贴到玩家前方。
    for table in ["armorstation", "armorforge"]:
        parent = f"conarm:item/{table}"
        visited = set()
        textures, elements = {}, None
        while parent != "minecraft:block/block" and parent not in visited:
            visited.add(parent)
            namespace, name = parent.split(":", 1)
            path = owners.get(namespace, RES) / f"assets/{namespace}/models/{name}.json"
            if not path.exists():
                failures.append(f"{table}: missing item model parent {parent}")
                break
            model = json.loads(path.read_text(encoding="utf-8"))
            for key, value in model.get("textures", {}).items():
                textures.setdefault(key, value)
            if elements is None and "elements" in model:
                elements = model["elements"]
            if all(key in model.get("display", {}) for key in ["gui", "thirdperson_righthand", "firstperson_righthand"]):
                break
            parent = model.get("parent", "")
            if ":" not in parent:
                failures.append(f"{table}: missing inherited 3D item display transforms")
                break
        else:
            if parent != "minecraft:block/block":
                failures.append(f"{table}: cyclic item model parent chain")
        for element in elements or []:
            for face in element.get("faces", {}).values():
                reference = face.get("texture", "")
                if reference.startswith("#") and reference[1:] not in textures:
                    failures.append(f"{table}: unbound inherited model texture {reference}")
    registry = ROOT / "build/reports/armory-item-registry.json"
    known_items = set(json.loads(registry.read_text(encoding="utf-8"))) if registry.exists() else None
    for path in (RES / "data/conarm/recipe").rglob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        def visit(value):
            if isinstance(value, dict):
                for key, entry in value.items():
                    if key in ("item", "id") and isinstance(entry, str) and ":" in entry:
                        if known_items is not None and entry not in known_items:
                            failures.append(f"{path.relative_to(ROOT)}: unknown recipe item {entry}")
                        elif known_items is None:
                            namespace, name = entry.split(":", 1)
                            if namespace in owners and not (owners[namespace] / f"assets/{namespace}/models/item/{name}.json").exists():
                                failures.append(f"{path.relative_to(ROOT)}: no item model for {entry}")
                    visit(entry)
            elif isinstance(value, list):
                for entry in value:
                    visit(entry)
        visit(data)
    java = ROOT / "src/main/java/com/gctn/tinkersarmorynirvana"
    required = set()
    for path in java.rglob("*.java"):
        required.update(key for key in re.findall(r'Component\.translatable\("([^"]+)"', path.read_text(encoding="utf-8"))
                        if not key.endswith(".") and key != "book.conarm.intro" and key.startswith(("item.conarm.", "gui.", "stat.", "tooltip.conarm.", "message.conarm.", "book.conarm.", "modifier.")))
    required.update(f"book.conarm.intro{index}" for index in range(1, 6))
    for path in (java / "client").glob("ArmoryBook*.java"):
        required.update("book.conarm." + key for key in re.findall(r'\btext\("([^"]+)"', path.read_text(encoding="utf-8")) if not key.endswith("."))
    required.update("book.conarm.section." + section for section in ["welcome", "armory", "materials", "traits", "modifiers", "accessories"])
    required.update("book.conarm.part." + part for part in ["core", "plates", "trim"])
    required.update("book.conarm." + key for key in ["previous", "next", "contents", "back", "open"])
    required.update("stat." + role + ".name" for role in ["core", "plates", "trim"])
    for stat in ["core.durability", "core.defense", "plates.modifier", "plates.durability", "plates.toughness", "trim.durability"]:
        required.update("stat." + stat + suffix for suffix in [".name", ".desc"])
    required.update("item.conarm." + armor + ".desc" for armor in ["helmet", "chestplate", "leggings", "boots"])
    required.update("key.conarm." + key for key in ["toggle_helm.desc", "toggle_chest.desc", "toggle_leg.desc", "category"])
    traits_source = (java / "armor/BuiltinTraits.java").read_text(encoding="utf-8")
    modifiers_source = (java / "armor/BuiltinModifiers.java").read_text(encoding="utf-8")
    traits = set(re.findall(r'trait\("([a-z_]+)"\)', traits_source)) | {"slimey_green", "slimey_blue"}
    modifiers = set(re.findall(r'modifier\("([a-z_]+)"\)', modifiers_source)) | {"resistant", "fire_resistant", "blast_resistant", "projectile_resistant"}
    for kind, identifiers in [("trait", traits), ("modifier", modifiers)]:
        for identifier in identifiers:
            required.update(f"{kind}.conarm.{identifier}.{suffix}" for suffix in ["name", "desc"])
    for lang in ["en_us", "zh_cn"]:
        data = json.loads((RES / f"assets/conarm/lang/{lang}.json").read_text(encoding="utf-8"))
        for key in sorted(required - data.keys()):
            failures.append(f"{lang}: missing translation {key}")
        if lang == "zh_cn":
            for key in sorted(required):
                if key.startswith(("trait.", "modifier.")) and key in data and not re.search(r"[\u4e00-\u9fff]", data[key]):
                    failures.append(f"{lang}: untranslated builtin {key} = {data[key]}")
        language = (RES / f"assets/conarm/book/{lang}/language.lang").read_text(encoding="utf-8")
        labels = {line.split("=", 1)[0] for line in language.splitlines() if "=" in line}
        for label in ["welcome", "armory", "materials", "modifiers", "accessories", "armor.title", "armor.properties", "modifier.effect"]:
            if label not in labels:
                failures.append(f"{lang}: missing original book label {label}")
    sounds = json.loads((RES / "assets/conarm/sounds.json").read_text(encoding="utf-8"))
    for sound in sounds.values():
        for entry in sound["sounds"]:
            namespace, name = entry.split(":", 1)
            if namespace == "conarm" and not (RES / f"assets/conarm/sounds/{name}.ogg").is_file():
                failures.append(f"Missing original workshop sound: {entry}")
    print(f"Parsed {count} JSON resources; checked textures, parents, recipes and {len(required)} translation keys in two languages.")
    if failures:
        print("\n".join(failures))
        return 1
    print("Resource audit passed. Real-client resource baking and rendering remain manual acceptance checks.")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tinkers", type=Path, required=True)
    sys.exit(main(parser.parse_args().tinkers))
