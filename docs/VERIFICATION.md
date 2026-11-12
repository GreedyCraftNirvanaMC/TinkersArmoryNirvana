# Verification / 验证说明

The runtime targets Java 21, Minecraft 1.21.1 and NeoForge 21.1.265. The baseline dependency is the locally built `TinkersNirvana-1.21.1-1.0.2.jar`. Client visual/interaction acceptance is intentionally left to the user, as requested. A dedicated-server check does not establish how the real client looks, sounds or responds to input.

The 1.21.1-1.0.9 update adds source-material colors to trait displays and saves new embossment provenance. The source-compatible book chapter hover implementation remains as in TinkersNirvana. Client colors and hover appearance remain pending manual acceptance.

## Recorded results — 1.21.1-1.0.9, 2026-11-12

| Check | Result |
| --- | --- |
| Gradle build, runtime and sources JARs | Passed |
| Dedicated server without KubeJS | 369 assertions passed |
| Dedicated server with KubeJS build.381 and the supplied example scripts | 385 assertions passed; startup/server scripts loaded without errors or warnings |
| Static resource audit | 525 JSON files and 218 required English/Chinese translation keys checked; passed |
| Missing-atlas regression check | Removing the atlas declaration makes the audit fail on 211 affected model/texture references; restoring it passes |
| Missing display-transform/texture-variable regressions | Removing the forge display parent or the inherited table leg-bottom binding is rejected by the audit |
| Real-client rendering, screens, input and audio | Pending manual acceptance by the user |

Checks cover source-material trait colors in actual item tooltips, mixed/duplicate sources, current color edits, new embossment previews/save round trips, neutral fallback for missing old provenance, KubeJS material colors, and material representative selection, visible-material coverage, explicit addon overrides, independent stacks and actual KubeJS ingredient selection. Addon API checks cover independent data/slot owners, disk/network round trips, part replacement, snapshot isolation, stat-event unregistration, atomic rejection of negative/excessive contributions and invalid stats, and removal of old root progression fields. Workstation geometry/UVs were compared against the original source, accounting for the dependency's renamed `leg_bottom` texture variable. Render depth ordering and nameplate handling were checked against the exact local Minecraft/NeoForge 1.21.1 sources; they still need real-client acceptance.

Generated server reports are `build/reports/armory-selftest-without-kubejs.md` and `build/reports/armory-selftest-with-kubejs.md`. Reports and game logs are local build artifacts and are not committed. No Minecraft client was launched for these results.

## Build

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana-1.21.1-1.0.2.jar'
```

This compiles both common and client code and packages runtime/source JARs. It does not launch Minecraft. Optional KubeJS classes are isolated from ordinary startup when KubeJS is absent.

## Opt-in dedicated-server checks

`ArmorySelfTest` is disabled during ordinary gameplay. `-ParmorySelfTest` enables transaction/serialization checks in the development server, writes a Markdown report under `build/reports/`, and stops that server when successful. Failures throw an assertion and are visible in the current run's logs/crash report. Do not use a personal game world for these checks: they may create and remove a temporary station and use a fake player.

Prepare an isolated `run` directory and accept the Minecraft EULA yourself if needed. A low-resource test configuration can use a flat world, loopback binding, a free local port, and view/simulation distances of 2. Supply valid flat-world generator settings, rather than an empty JSON object.

```powershell
# No KubeJS runtime:
.\gradlew.bat runServer -ParmorySelfTest '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana.jar'

# With KubeJS: copy the example fixture first, then start the development server.
New-Item -ItemType Directory -Force run\kubejs | Out-Null
Copy-Item -Recurse -Force examples\kubejs\* run\kubejs\
.\gradlew.bat runServer -ParmorySelfTest -PwithKubeJS '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana.jar'
```

The KubeJS fixture is required for the second command's assertions. Ensure the example files end up directly in `run/kubejs/startup_scripts`, `server_scripts` and `client_scripts`, not an extra nested `kubejs` directory. Server-only tests do not execute the client language script.

Checks cover:

- All original material/armor combinations, source durability/defense, trait resolution and distinct-level stacking.
- Modifier slot costs, duplicate rejection, equipment restrictions, resistance conflicts and traveller accessory exclusivity.
- Repair and part replacement previews, unchanged inputs before commit, exact consumption, invalidated snapshots and rejection of unrelated inputs.
- Reusable patterns, material costs, mixed-material assembly, native recipe loading and overlapping ingredient allocation.
- Broken-item preservation, attribute removal/restoration and complete reinforcement.
- Accessory effect cleanup without removing independent potion effects; addon-owned data and slot contributions without builtin leveling.
- Immediate accessory storage persistence, recursive-storage rejection and access closure when armor is removed.
- Actual native part-builder previews and commits for all seven armor parts: pure previews, exact costs and retained patterns; native pickaxe-head processing remains valid.
- Original menu tab selection, radial and selected-part coordinates, filled inactive-slot recovery, station/forge permissions, rename-only plans and rejection of wrong core types.
- Actual menu click/shift-click consumption and replay rejection; preview-only deconstruction, cancellation, source persistence, committing one part, damaged/nonempty-storage rejection and breaking before commit.
- Original potion-belt/knapsack slot positions, raw display defense versus allocated native attributes, and migration of saved legacy armor patterns.
- Registry-aware disk and network component round trips, and native smeltery casting registration.
- With KubeJS: actual material/trait/modifier installation, editing a builtin, callback execution and custom recipe availability.

Use `logs/latest.log`, `logs/kubejs/startup.log` and `logs/kubejs/server.log` from that launch. `ARMORY_SELFTEST_PASS` records successful completion. Do not interpret Gradle's `test NO-SOURCE` as an executed game test.

## Static resource audit

The resource converter is reproducible from the supplied legacy source:

```powershell
python -X utf8 scripts/port_resources.py --legacy C:/path/to/AntiqueArmory-1.12.2
python -X utf8 scripts/audit_resources.py --tinkers C:/path/to/TinkersNirvana-1.21.1
```

The audit checks JSON, owned model/texture references, item recipe references against the built dependency, and required English/Chinese keys. This does not replace resource baking or screenshots from a real client.

Model textures must also be collected by the target atlas. Minecraft 1.21's default blocks atlas scans `textures/item/` and `textures/block/`, whereas this port retains legacy `textures/items/` and `textures/blocks/` resource IDs. `assets/minecraft/atlases/blocks.json` explicitly collects the retained directories. The audit now checks this relationship; a PNG's presence alone is insufficient. The original 1.0.0 client log confirmed missing atlas sprites even though the old file-existence check passed.

## 客户端手动验收

客户端与服务端使用 **TinkersArmoryNirvana 1.21.1-1.0.9** 和 **TinkersNirvana 1.21.1-1.0.2**，移除旧护甲 JAR 后重新启动。本次没有更新依赖 JAR。不要把源码 JAR 放进 `mods`。

1. 对照提供的原版截图检查手册：封面打开后左侧深色、右侧五个章节图标；材料页有侧边物品列、基底／护甲板两列、下方夹板与斜体风味文字。整本书和文字应保持清晰，五个章节图标保持不透明，文字使用与匠魂宝典相同的 uniform 字体及悬停色值，没有额外粗体；背景高亮位置与匠魂宝典一致，移开后恢复且文字始终居中；材料目录图标应是木板、燧石、金属锭等材料代表物品，详情侧栏首个图标应与之对应；不应整页显示头盔。检查彩色数值、带下划线的词条与悬停提示。翻页、目录、返回和书本重开后的阅读位置应正常，GUI 缩放后各项仍在书页内。
2. 创造栏、部件、护甲和工作台不应有紫黑贴图；日志不应再出现 `Missing textures in model conarm:`。在原生模具台选择七种护甲部件模具，检查两个工作台在创造栏、主／副手、第三人称与掉落状态下的旋转和大小，木台侧面及桌腿底部不应缺纹理。到原生部件加工台加工木部件。预览不扣料，取出才按成本扣料，模具保留。
3. 对照原版装配台／锻造台截图检查：五个标签、木／金属横梁、左侧跟随鼠标的本人皮肤预览、右侧上下两栏、原版环形六槽、命名框、箭头和部件空槽轮廓。切到胸甲标签后，应是下中核心、左护甲板、右夹板；右侧显示护甲属性与特性。用玛玉灵核心、骑士史莱姆护甲板、仙人掌夹板组装时，物品提示及右侧词条应分别显示对应的紫／粉／绿色，不能统一青色或紫色；部件提示和 Alt 描述也应采用材料色。放大的护甲图案应淡化到槽位之后，边框和空槽轮廓始终可见。持续把鼠标移到预览下方时不应显示玩家名字或计分板文字。测试面板滚动和悬停提示。
4. 测试组装、修复、换部件与命名。普通台不能应用强化、抛光或刻印，并给出锻造台提示；锻造台允许。错误核心、无关投入和冲突强化不应产生成品。关闭／取消不扣料，Shift 取出只扣一次。
5. `deconstructTools` 开启时，空输入的修复模式可把完好护甲放入右侧槽预览分解。取回原护甲应取消；取一件部件后才完成分解，不能再取回原护甲。预览时关闭重开、保存重载或拆台均不可复制物品。损坏护甲或配件内仍有物品时应拒绝。
6. G 操作头盔、H 操作胸甲、J 操作护腿的已安装配件，应直接切换能力或打开原版背包／药水腰带。测试存取、快捷栏交换、对应按键关闭、脱装、损坏和重新登录；库存应保留，不得将带库存的护甲套进自身。
7. 检查实际穿戴和配件模型、锯木／铁砧声音、夜视／缩放／灵魂视野、隐身／缓降开关。脱装、损坏和关闭功能后，只清理本模组自己的效果。测试弹跳、潜浮、高步、冰霜行者及多人同时操作同一工作台的表现。

These are the remaining manual acceptance checks, not claims of automated client verification.

## Addon API acceptance

The core has no `[leveling]` config, automatic armor XP, level rewards, level sounds or level tooltip. Rebuilding an old item clears its root `Level`/`Experience` fields. Installed modifiers remain; extra slots come only from ordinary traits/modifiers, namespaced contributions or explicit stat handlers. To test an external progression addon later, verify its own damage eligibility, level rules, tooltip, login/save/load and removal behavior against [the extension contract](en/EXTENSIONS.md).
