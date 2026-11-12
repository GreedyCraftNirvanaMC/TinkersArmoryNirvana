# 附属模组开发文档

[项目说明](../../README.zh-CN.md) · [English reference](../en/API.md) · [KubeJS 魔改](KUBEJS.md)

## 开发依赖与注册时机

使用 Java 21、Minecraft 1.21.1、NeoForge 21.1.265 或以上的 21.1 版本。开发环境同时引用 TinkersNirvana 与本模组 JAR：

```groovy
dependencies {
    implementation files('libs/TinkersNirvana-1.21.1-1.0.2.jar')
    implementation files('libs/TinkersArmoryNirvana-1.21.1-1.0.4.jar')
}
```

在附属的 `neoforge.mods.toml` 中分别声明 `tconstruct` 和 `conarm` 必需依赖，`ordering="AFTER"`、`side="BOTH"`，最低版本分别为 `1.21.1-1.0.2` 与 `1.21.1-1.0.4`。完整 TOML 示例见英文参考对应章节。

普通定义注册放进 `FMLCommonSetupEvent.enqueueWork`，在两个依赖完成注册之后执行。KubeJS 的修改事件在之后的插件 `afterInit` 阶段触发，因此整合包可以调整 Java 附属注册的内容。不要从异步线程修改全局定义表。纯 Java 附属不必依赖 KubeJS。

公开 API 位于 `com.gctn.tinkersarmorynirvana.api`。不要依赖旧版 `c4.conarm` 类名；这里提供的是新版源码 API，不保留 Forge 1.12 的二进制 ABI。

## 新增材料与词条

护甲材料依附于一个真实存在的 TinkersNirvana 材料，复用它的名称、颜色、原料匹配、可加工性与可浇筑性。护甲定义不会覆盖该材料的工具属性。

```java
import com.gctn.tconstruct.api.MaterialDefinition;
import com.gctn.tinkersarmorynirvana.api.*;

ArmoryRegistry.registerTrait(new ArmorTrait("exampleaddon:resolve")
        .maxLevel(2)
        .on("modifyStats", ctx -> ctx.stats.toughness += 1.5F * ctx.level));

new MaterialDefinition("exampleaddon:amethyst")
        .color(0xA77CE3).craftable(true)
        .head(600, 6, 5, 2).handle(1.05F, 50).extra(75)
        .item("minecraft:amethyst_shard", 1)
        .install();

new ArmorMaterialDefinition("exampleaddon:amethyst")
        .core(16, 15).plates(1.1F, 3, 2).trim(7)
        .trait("core", "exampleaddon:resolve", 1)
        .trait("plates", "exampleaddon:resolve", 1)
        .trait("trim", "exampleaddon:resolve", 1)
        .install();
```

材料先注册到 TinkersNirvana，再安装护甲属性。新增内容使用自己的命名空间。内置材料可使用 `iron` 或 `tconstruct:iron`；物品内保存依赖中的规范 ID。

| 接口 | 参数与含义 |
| --- | --- |
| `core(durability, defense)` | 核心基础耐久倍率、整套防御基础值 |
| `plates(multiplier, durability, toughness)` | 护甲板耐久倍率、附加耐久倍率、单件韧性 |
| `trim(durability)` | 夹板附加耐久倍率 |
| `trait(part, id, level)` | 给 `core`、`plates` 或 `trim` 添加词条贡献 |
| `clearTraits(part)` | 清除指定部件的词条贡献 |
| `install()` | 校验并安装或替换护甲材料定义 |

原版耐久因子依次为靴子 13、护腿 15、胸甲 16、头盔 11。核心与夹板分别乘该因子，先加和，再乘护甲板倍率，最后加入护甲板附加耐久。防御按上述部位分配为 0.14、0.30、0.40、0.16。`modifyStats` 阶段的 `defense` 仍是分配前的整套基础值；`ArmorData.stats(stack)` 返回分配后的单件值，供原生属性使用；`ArmorData.displayStats(stack)` 保留分配前的数值，供原版风格的界面和提示显示。两者共用无副作用的计算过程，不要把显示值直接写进原生属性。其余未特别说明的属性为单件值。

修改现有定义时，用 `new ArmorMaterialDefinition(ArmoryRegistry.material("iron"))` 构造副本，修改后 `install()`。缺少基础材料、非法倍率、非有限数值、未知词条或越界等级都会报错。`removeMaterial(id)` 可禁用护甲材料；已有装备保留材料 ID，在定义缺失时失去防护，重新提供定义后可以恢复。

## 定义表、等级与强化

`ArmoryRegistry` 提供 `registerTrait`、`replaceTrait`、`removeTrait`、`registerModifier`、`removeModifier`，以及 `traits()`、`modifiers()`、`materials()` 只读映射。查单项用 `trait(id)`、`modifier(id)`、`material(id)`。未限定命名空间的词条／强化 ID 默认属于 `conarm`；附属应始终使用完整 ID。重复注册会报错。直接修改已有定义后调用 `changed()` 更新定义版本。

`ArmorTrait.on(hook, Consumer<ArmorContext>)` 追加回调，`clear(hook)` 清除该钩子的已有回调。等级策略通过 `stacking` 指定：

| 策略 | 行为 |
| --- | --- |
| `max` | 默认，取最高贡献等级 |
| `unique` | 不同初始等级各加一次；2、1、1 合并为 3 |
| `sum` | 所有贡献相加，包括重复贡献 |

最终等级受 `maxLevel` 限制。内置磁力和平凡采用 `unique`，普通非等级词条不会因为核心、板、夹板重复而重复执行。

```java
ArmoryRegistry.registerModifier(new ArmorModifier("exampleaddon:amethyst_guard")
        .maxLevel(3).pointsPerLevel(8).slotsPerLevel(1)
        .equipment("head", "chest", "legs", "feet")
        .on("incomingDamage", ctx -> ctx.amount *= .96F));
```

每进入一个新的 `pointsPerLevel` 分组即获得一个等级。`slotsOnce(true)` 表示只有首次安装占槽；`slotsPerLevel(0)` 表示不占槽。`conflicts(...)` 指定互斥词条或强化。`accessory(true)` 启用每件护甲只能安装一个旅行配件的限制。强化材料使用原生 `conarm:armor_modifier` 配方，定义行为和配方材料相互独立。

## 回调上下文与约束

`ArmorContext` 的主要字段：`armor` 为当前物品，`wearer` 为可空的穿戴者／持有者，`id`、`level` 为当前定义与等级，`state` 为当前定义的状态副本，`source` 为可空伤害源，`amount` 为可修改数值，`stats` 为仅在属性计算中存在的属性对象。

| 钩子 | 时机与用途 |
| --- | --- |
| `modifyStats` | 计算属性；必须确定且无副作用，`wearer/source` 为 null |
| `inventoryTick` | 服务端物品栏更新，未穿戴或已损坏时也可触发 |
| `armorTick` | 服务端玩家穿着完整有效的护甲时触发 |
| `incomingDamage` | 原生护甲减伤前，写 `amount` 调整伤害 |
| `armorEffectiveness` | 从 1 开始，修改该护甲对原生护甲减伤的贡献效率 |
| `beforeDamage` | 护甲与魔法减伤后、吸收与生命扣除前 |
| `afterDamage` | 实际造成正伤害之后，用于反击等副作用 |
| `outgoingDamage` | 穿戴者为攻击者时，修改输出伤害 |
| `durabilityDamage` | 正耐久损失应用前，可修改 `amount` |
| `repair` | 修复数值，可在预览中触发，`wearer` 可能为空 |
| `fall` | `amount` 为坠落距离 |
| `knockback` | `amount` 为击退强度，可能在两逻辑端触发 |
| `jump` | 实体跳跃，`amount` 为内置潜浮强化加成 |
| `milk` | 饮奶完成后 |
| `experience` | 方块或生物掉落经验，可修改 `amount` |
| `equip` / `unequip` | 服务端穿脱装备通知，装备可能已损坏 |

修改 `state` 后必须调用 `ctx.save()`。它只把当前定义的状态合并回最新组件，保留其他回调已保存的数据。不要在回调间共享同一可变 NBT 引用。

`modifyStats` 和修复预览回调可能反复执行，禁止在其中保存状态、扣物品、使用随机数、发送数据包或修改世界。世界修改应放在真实服务端战斗／tick 钩子里。处理 `wearer/source/stats` 前按照上表判断是否为空。运行时回调可能在启动脚本执行完很久之后被调用，不要长期持有玩家、容器或世界引用。

`ArmorStats` 可修改字段包括：`durability`、`defense`、`toughness`、`modifiers`、`speed`、`knockbackResistance`、`health`、`swimSpeed`、`attack`、`attackSpeed`、`reach`、`stepHeight`。`baseDurability/baseDefense/baseToughness` 是词条应用前的只读基础值。`speed/attack/attackSpeed` 为乘算增量比例，其他属性增量使用原生单位；`stepHeight` 为增加的高度。最终数值必须有限，耐久为正，槽位非负。

## 物品构建、保存与损坏

`ArmorData.create(ModularArmorItem, core, plates, trim)` 构建护甲，`ArmorPartItem.create(material)` 构建部件。内置物品供应器位于 `ArmoryContent.ARMORS/PARTS`。内置组装仅支持四个人形护甲槽。

`ArmorData.read(stack)` 返回 `conarm:armor` 持久组件的副本，保存版本、`Core/Plates/Trim`、`Modifiers`（ID→点数）、`State`（ID→状态）、`Disabled`、可选抛光／刻印（`EmbossedTraits`、`EmbossedMaterial`）、修复次数和命名空间 `Extensions` 数据。修改后通过 `ArmorData.write` 写回。不得直接修改共享组件或别人的预览物品。

`ArmorStorage` 单独管理 `conarm:storage`。其 `Inventories` 下分别保存 `travel_sack`、`potion_belt`、`travel_belt`，编号槽位使用 Minecraft 原生带注册表的 ItemStack 序列化。将库存分开可以避免频繁属性计算复制整个背包。两个组件都通过原生编码器保存与同步。

通过 `ArmorData.rebuild/damage/repair/level/points/freeSlots/traits/broken` 操作装备，不要重复实现伤害逻辑。损坏装备停止提供属性与主动能力，但保留物品和库存；修复后恢复。支持铁砧命名，关闭普通附魔、双装备铁砧修复和原版双物品修复配方，以维持工作台强化流程并避免吞掉带库存的护甲。

自 1.21.1-1.0.9 起，`ArmorData.traitMaterial(stack, id)` 返回词条首次来源的规范材料 ID，依次检查核心、护甲板、夹板，最后检查刻印；未知来源返回空字符串。`traitColor(stack, id)` 返回该材料当前的 24 位 RGB 色，来源未知时为中性灰 `0xaaaaaa`。词条 ID 可用短名或完整命名空间。两个方法不改变词条等级和叠加规则。新刻印保存 `EmbossedMaterial`，旧物品缺少该记录时不猜测来源。护甲／部件提示和工作台词条栏使用来源色；手册保留原版深色下划线正文及材料色悬停描述。

## 附属数据与全局属性计算

自 1.21.1-1.0.4 起，`ArmorExtensions.read/write/update/remove` 管理完整命名空间下的独立数据，`modifierSlots/setModifierSlots/totalModifierSlots` 提供被动强化槽贡献。写操作先在副本上重算并校验，成功后提交组件，保留其他附属数据并立即更新属性。基础强化槽为三，再计入词条／强化和附属贡献。

`ArmorStatsEvent` 在 `NeoForge.EVENT_BUS` 上触发，时机为内置计算之后、防御按部位分配及最终校验之前。装备对象是独立快照，只有可修改的 `ArmorStats` 影响结果。伤害／经验触发使用 NeoForge 原生游戏事件；本体没有经验升级引擎和等级提示。旧根字段 `Level`、`Experience` 会在写入或重算时删除，附属命名空间内部的同名字段不受影响。完整契约和示例见[扩展指南](EXTENSIONS.md)。

## 新工作台操作

`ArmoryWorkshop.registerPlanner(id, BiFunction<Container, Level, Plan>)` 在内置规划器前执行。仅在启动期间注册；不匹配时返回 null。输入固定为六槽（0—5），规划器不得修改输入或世界。

构建 `Plan(result, consumed, remainders, snapshot)` 时，保存全部输入的快照。构造器会复制数据并检查扣料数量；`valid(container)` 比较完整快照；`commit(container)` 再次校验并扣料一次，返回剩余物。菜单负责将返还物放入背包或掉落溢出物。结果预览不存进方块实体，因此关闭界面或破坏方块不会额外掉落预览产物。不要在预览阶段提交或自行再次扣料。

`ArmoryWorkshop.applyModifier` 返回独立结果，拒绝时返回 `ItemStack.EMPTY`。原生匠魂工具装配台使用相同规划器适配已有护甲和护甲模具；附属规划器可扩展这两条入口支持的物品处理。

## 浇筑与外观扩展

`ArmoryCasting.bind(materialId, fluidId)` 在启动阶段关联已注册流体，并不创建新流体。基础匠魂材料还必须标记为可浇筑。护甲部件的熔炼、金属铸模和黏土铸模规则在服务端启动时安装，遵守 TinkersNirvana 的黏土铸模配置。

`ArmorVisuals.register(modifierId, textureId, color, geometry)` 必须在模组构造阶段、护甲材质注册前调用，并在两物理端一致注册。纹理不写 `textures/` 和 `.png`，例如 `exampleaddon:models/modifiers/rune`。几何类型为 `armor/belt/sack/goggles/cloak`，应匹配原有 UV 布局。仅客户端类必须隔离在客户端订阅器中。内置物品图标按原贴图层组合，资源重载时重建模型缓存。

翻译键为 `trait.<namespace>.<path>.name/.desc`、`modifier.<namespace>.<path>.name/.desc` 及常规物品翻译键。宝典读取当前定义，因此附属注册的内容也能进入手册。

手册读取原版 `assets/conarm/book/index.json` 和各章节数组。页面位于 `book/<语言>/<章节>/<条目>.json`，缺失时回退到英文；标签和风味文字位于对应的 `language.lang`，外观通过 `appearance.json` 与本地化覆盖调整。保留 `text`、`imageText2`、`armor`、`armormaterial`、`armormodifier`、`armormodifier_polished` 模板。材料页按匠魂材料顺序读取当前定义，附属强化追加到相应原版章节，配方图读取当前 `conarm:armor_modifier` 配方。固定模板的文字区域有边界，资源包作者应按原布局控制长度；重载后重新打开书本。阅读位置保存在书本 `CUSTOM_DATA` 的 `conarm:BookPage` 中。源码对应关系见 [UI 移植说明](../UI_PORT.md)。

物品模型优先使用 1.21 的 `textures/item/`、`textures/block/` 路径；采用其它目录时，必须在 `assets/minecraft/atlases/blocks.json` 显式声明 sprite 来源。仅有 PNG 文件并不保证模型能够显示，目标图集也必须收集它。穿戴护甲纹理与 GUI 纹理直接加载，不需要加入方块图集。

## 原生部件加工与界面契约

七种既有护甲部件通过依赖的 `AntiqueParts.registerExternal` 接入原生模具目录、选中模具名称、部件加工台与部件箱，未创建第二套物品。材料谓词在加工时读取当前定义，KubeJS 修改后继续生效。自己的附属部件可使用同一接口，提供完整命名空间 ID／属性标识、材料消耗、已有物品供应器、纯材料谓词、独立 ItemStack 工厂和本地化名称。两端在物品注册后统一注册；渲染、浇筑、铸模和组装规则仍由物品所属附属负责。完整示例见依赖的 [EXTERNAL_PARTS.md](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/blob/codex/armory-parts-api/docs/EXTERNAL_PARTS.md)。

`ArmoryWorkshop.plan(input, level, allowModifiers)` 的布尔参数限制内置强化配方、抛光与刻印，普通装配台传 false；原双参数重载保持允许这些操作。附属规划器仍优先执行，需自行定义其操作的额外进度规则。`rename(input, name)` 可以生成不含其他投入物的纯命名计划。

`ArmoryMenu` 的菜单槽编号为：产物 0、六个输入 1–6、玩家库存 7–42；容器输入本身仍按 0–5 索引。标签 0 是修复，1–4 分别是头盔、胸甲、护腿、靴子，由服务端校验的原生菜单按钮切换。`ArmoryStationLayout` 提供坐标和匹配部件类型。分解预览不是真实输入物品，原护甲在首个部件被取走时才消耗；不要绕过方块实体直接提交预览或修改所有权。

`ArmoryMaterialItems`（自 1.21.1-1.0.3 提供）在构造时快照当前材料原料。`representative(material)` 返回独立的单个物品，优先取材料显式指定的代表物品，否则与依赖宝典一样依次优先选择锭、一份材料价值的原料。材料目录和详情侧栏使用同一选择结果。附属／KubeJS 可在依赖的材料定义中调用 `.representativeItem("命名空间:物品")` 明确指定代表物品。标签或原料重载后需新建目录，不应把快照做成永久全局缓存。
