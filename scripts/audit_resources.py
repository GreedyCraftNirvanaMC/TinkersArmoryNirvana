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
            parent = data.get("parent", "")
            if ":" in parent:
                namespace, name = parent.split(":", 1)
                if namespace in owners and not (owners[namespace] / f"assets/{namespace}/models/{name}.json").is_file():
                    failures.append(f"{path.relative_to(ROOT)}: missing parent {parent}")
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
                        if not key.endswith(".") and key != "book.conarm.intro" and key.startswith(("item.conarm.", "gui.conarm.", "tooltip.conarm.", "message.conarm.", "book.conarm.")))
    required.update(f"book.conarm.intro{index}" for index in range(1, 6))
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
