# Verification / 验证说明

The runtime targets Java 21, Minecraft 1.21.1 and NeoForge 21.1.265. The baseline dependency is the locally built `TinkersNirvana-1.21.1-1.0.1.jar`. Client visual/interaction acceptance is intentionally left to the user, as requested. A dedicated-server check does not establish how the real client looks, sounds or responds to input.

## Recorded results — 2026-11-12

| Check | Result |
| --- | --- |
| Gradle build, runtime and sources JARs | Passed |
| Dedicated server without KubeJS | 287 assertions passed |
| Dedicated server with KubeJS build.381 and the supplied example scripts | 301 assertions passed; startup/server scripts loaded without errors or warnings |
| Static resource audit | 524 JSON files and 177 required English/Chinese translation keys checked; passed |
| Real-client rendering, screens, input and audio | Pending manual acceptance by the user |

Generated server reports are `build/reports/armory-selftest-without-kubejs.md` and `build/reports/armory-selftest-with-kubejs.md`. Reports and game logs are local build artifacts and are not committed. No Minecraft client was launched for these results.

## Build

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build '-PtinkersNirvanaJar=C:/path/to/TinkersNirvana-1.21.1-1.0.1.jar'
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
- Accessory effect cleanup without removing independent potion effects; optional leveling rewards and rejection of armor-bypassing damage.
- Immediate accessory storage persistence, recursive-storage rejection and access closure when armor is removed.
- Actual station-menu shift-click consumption and rejection of replayed results.
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

## 客户端手动验收

1. 两端安装同版本的本模组与 TinkersNirvana。进入测试世界，确认创造栏中的护甲、部件、模具和宝典正常显示，没有紫黑贴图。
2. 用护甲模具加工木质部件，再用不同材料组装一件护甲；观察预览人偶、实际穿戴和物品图标。取消／关闭界面时不应扣料或产生额外成品；Shift 取出后应只扣一次。
3. 测试修复、换部件、抛光和浮雕；放入错误核心、无关原料或冲突强化时不应产生可取出的结果。把护甲用坏后应保留物品，修复后恢复防护。
4. 安装旅行配件后按 G。测试腰带交换、药水腰带与背包的存取、关闭重开、脱装和重新登录；物品应保留且不能把带库存的护甲套进自身。
5. 分别测试缩放、夜视、灵魂视野、隐身与缓降的开关。脱装、损坏或关闭功能后，本模组施加的状态应清理，普通药水效果应保留。确认附近实体的灵魂标记、护目镜／披风／背包模型位置与声效。
6. 测试弹跳、潜浮、高步和冰霜行者；确认移动、落地、冰面与不稳定黑曜石消退表现。多人同时操作同一工作台时不应复制物品。

These are the remaining manual acceptance checks, not claims of automated client verification.
