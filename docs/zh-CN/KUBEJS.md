# KubeJS 魔改文档

[项目说明](../../README.zh-CN.md) · [Java API 与完整钩子表](API.md) · [English](../en/KUBEJS.md)

适用于 Minecraft 1.21.1、NeoForge 与 KubeJS `2101.7.2-build.381`。KubeJS 是可选依赖，不装也能使用本模组。完整可运行示例位于 [`examples/kubejs`](../../examples/kubejs)，需要启用示例时才将该目录复制进游戏目录。示例会新增紫水晶材料、词条和强化，并修改木材料属性。

## 文件位置与生效方式

| 目录 | 用途 | 生效方式 |
| --- | --- | --- |
| `kubejs/startup_scripts/` | 注册或修改材料、词条、强化行为 | 完整重启客户端与服务端 |
| `kubejs/server_scripts/` | 修改原生配方和标签 | `/reload` 或重启 |
| `kubejs/client_scripts/` | 客户端翻译 | 按 KubeJS 支持方式重载客户端脚本或重启 |

注册顺序为：物品／注册表 → Java common setup → TinkersNirvana 插件定义及修改事件 → `ArmoryEvents.modifyTraits` → `modifyModifiers` → `modifyMaterials`。护甲事件通过 KubeJS 的正式插件 `afterInit` 执行，`conarm` 在依赖之后加载。因此可以先在 `StartupEvents.registry('tconstruct:material', ...)` 创建匠魂材料，之后再添加护甲属性。

客户端与服务端必须使用相同的启动定义。只有服务端有配方脚本是可以的，但材料、词条和强化行为需要两端一致，才能正确显示属性、名称和材质。

## 创建一套护甲材料

```js
// kubejs/startup_scripts/my_armor.js
StartupEvents.registry('tconstruct:material', event => {
  event.create('example:amethyst')
    .color(0xA77CE3)
    .craftable(true)
    .head(600, 6, 5, 2)
    .handle(1.05, 50)
    .extra(75)
    .item('minecraft:amethyst_shard', 1)
})

ArmoryEvents.modifyTraits(event => {
  event.create('example:resolve', trait => {
    trait.maxLevel(2).on('modifyStats', ctx => {
      ctx.stats.toughness += 1.5 * ctx.level
    })
  })
})

ArmoryEvents.modifyMaterials(event => {
  event.create('example:amethyst', material => {
    material.core(16, 15)
      .plates(1.1, 3, 2)
      .trim(7)
      .trait('core', 'example:resolve', 1)
      .trait('plates', 'example:resolve', 1)
      .trait('trim', 'example:resolve', 1)
  })
})
```

第一段创建真正的匠魂材料，提供颜色、工具属性、原料来源与可加工性；第三段只增加护甲属性，不会凭空创建原料或流体。内置材料允许 `iron` 或 `tconstruct:iron`，自定义材料使用自己的完整命名空间。

| 方法 | 参数 |
| --- | --- |
| `core(durability, defense)` | 核心耐久倍率、整套基础防御 |
| `plates(multiplier, durability, toughness)` | 护甲板耐久倍率、附加耐久、单件韧性 |
| `trim(durability)` | 夹板附加耐久 |
| `trait(part, id, level)` | 给核心 `core`、护甲板 `plates`、夹板 `trim` 添加词条 |
| `clearTraits(part)` | 清除该部件现有词条 |

具体耐久与部位分配规则见 [API 文档](API.md)。不存在的基础材料、未知词条、非法倍率、非有限数值与越界词条等级会明确报错。

## 修改与禁用内容

```js
ArmoryEvents.modifyMaterials(event => {
  event.modify('wood', material => {
    material.core(6, 8)
      .clearTraits('trim')
      .trait('trim', 'example:resolve', 1)
  })
  // 禁用纸护甲材料；已有装备的材料 ID 会保留，但失去防护。
  // event.remove('paper')
})

ArmoryEvents.modifyTraits(event => {
  event.modify('conarm:ecological', trait => {
    // clear 清除原回调，on 追加新回调；仅写 on 会叠加到原有行为之后。
    trait.clear('inventoryTick').on('inventoryTick', ctx => {
      if (ctx.wearer.tickCount % 200 === 0) {
        const ArmorData = Java.loadClass('com.gctn.tinkersarmorynirvana.armor.ArmorData')
        ArmorData.repair(ctx.armor, ctx.wearer, 1)
      }
    })
  })
  // event.remove('conarm:magnetic')
})
```

`create(id, callback)` 新增定义，重复 ID 报错；`modify` 修改已有定义，未知 ID 报错；三类事件均支持 `remove(id)`。材料编辑在副本上执行，安装前校验。移除词条或强化后，其已保存 ID 和状态保留，但行为停止；重新提供相同 ID 可恢复解析。更改启动脚本后务必完整重启。

## 创建与调整强化

```js
ArmoryEvents.modifyModifiers(event => {
  event.create('example:amethyst_guard', modifier => {
    modifier.maxLevel(3)
      .pointsPerLevel(8)
      .slotsPerLevel(1)
      .equipment('head', 'chest', 'legs', 'feet')
      .on('incomingDamage', ctx => { ctx.amount *= 0.96 })
  })

  event.modify('conarm:speedy', modifier => {
    modifier.maxLevel(4)
  })
})
```

| 设置 | 行为 |
| --- | --- |
| `maxLevel(n)` | 强化等级上限 |
| `pointsPerLevel(n)` | 每级对应的材料点数；进入新分组的第一个点即开始该等级 |
| `slotsPerLevel(n)` | 每级消耗槽位，可设为 0 |
| `slotsOnce(true)` | 仅首次安装占槽，后续等级不再占槽 |
| `equipment(...names)` | 可安装部位，使用 `head/chest/legs/feet` |
| `conflicts(...ids)` | 与指定词条或强化互斥 |
| `accessory(true)` | 作为旅行配件，每件护甲最多一个 |
| `on(hook, callback)` | 追加行为 |
| `clear(hook)` | 清除该钩子的全部已有行为 |

词条另有 `stacking('max'/'unique'/'sum')`，最终受 `maxLevel` 限制。`max` 取最高贡献；`unique` 将不同初始等级各加一次；`sum` 叠加所有贡献。建议始终写完整 ID；省略命名空间的词条／强化 ID 默认属于 `conarm`。

## 使用回调与保存状态

常见钩子为 `modifyStats`、`inventoryTick`、`armorTick`、`incomingDamage`、`armorEffectiveness`、`beforeDamage`、`afterDamage`、`outgoingDamage`、`durabilityDamage`、`repair`、`fall`、`knockback`、`jump`、`milk`、`experience`、`equip`、`unequip`。完整时机表见 [API 文档](API.md#回调上下文与约束)。不支持的名字会报错。

`ctx.amount` 是伤害、修复、坠落、击退或经验等钩子的可修改值；`ctx.stats` 仅在属性计算时存在；`ctx.wearer/source` 在属性计算和修复预览时可能为空。`ctx.state` 是当前定义的独立状态副本，修改后调用 `ctx.save()`。

```js
ArmoryEvents.modifyTraits(event => {
  event.create('example:timer', trait => {
    trait.on('armorTick', ctx => {
      if (ctx.wearer.tickCount % 20 !== 0) return
      ctx.state.putInt('SecondsWorn', ctx.state.getInt('SecondsWorn') + 1)
      ctx.save()
    })
  })
})
```

属性和预览回调会重复执行，必须保持纯计算：不能随机、扣料、保存状态、发送数据包或修改世界。真实服务端战斗／tick 回调才适合世界操作。不要在启动时长期保存某个玩家或容器引用供后续回调使用。

## 原生强化配方与删除配方

```js
// kubejs/server_scripts/armor_recipes.js
ServerEvents.recipes(event => {
  event.remove({ id: 'conarm:modifiers/diamond' })

  event.custom({
    type: 'conarm:armor_modifier',
    modifier: 'example:amethyst_guard',
    points: 2,
    ingredients: [
      { ingredient: { item: 'minecraft:amethyst_shard' }, count: 1 },
      { ingredient: { tag: 'c:ingots/iron' }, count: 2 }
    ]
  }).id('example:armory/amethyst_guard')
})
```

完全相同的对象也可以保存为数据包 `data/example/recipe/armory/amethyst_guard.json`。

| 字段 | 说明 |
| --- | --- |
| `type` | 固定为 `conarm:armor_modifier` |
| `modifier` | 已注册的强化 ID |
| `points` | 本次增加的点数，默认 1，范围 1—1000000 |
| `ingredients` | 1—6 个需求条目 |
| `ingredient` | 原生 Ingredient，支持物品、标签、备选项与 NeoForge 组件条件 |
| `count` | 该需求的物品数量，默认 1，范围 1—384 |

护甲占首格，因此锻造台最多同时放五堆强化原料；装配台只开放前三个投入格。需求可以重叠，也可以拆成多堆，匹配器会统一分配；放入完全无关的额外物品会拒绝操作。只有取出结果才扣料，容器返还物和副产物会交还玩家。产物由输入护甲计算，不使用固定 `result` 字段。

配方仍须满足剩余槽位、装备部位、点数上限、互斥与旅行配件数量限制。一次配方增加的点数超过上限时会拒绝，不会吞掉多余原料。定义行为与投入配方独立，单纯创建强化不会自动创建材料配方。

内置强化配方位于 `src/main/resources/data/conarm/recipe/modifiers/`，ID 形如 `conarm:modifiers/diamond`。包括钻石、绿宝石、加固、急速、潜浮、各类抗性、手套、水下呼吸、冰霜行者、高步、发光、寄生、修补、灵魂绑定与旅行配件等。普通物品和工作台配方使用原生合成配方类型，也可用常规 KubeJS 删除、替换。

## 浇筑绑定

```js
ArmoryEvents.modifyMaterials(event => {
  // 仅在已经创建 example:alloy 且基础材料已标记为可浇筑时使用。
  event.castingFluid('example:alloy', 'tconstruct:cobalt')
})
```

此方法关联已存在的流体，不创建材料或流体。流体使用注册 ID，不是流体方块或桶的 ID，例如 `tconstruct:cobalt` 是源流体，而 `tconstruct:molten_cobalt` 是流体方块。基础材料的输入、颜色与可浇筑标记通过 TinkersNirvana 的材料 API 设置。绑定在服务端启动前完成，改动需要重启。

## 中文翻译与手册

```js
// kubejs/client_scripts/armor_lang.js
ClientEvents.lang('zh_cn', event => {
  event.add('trait.example.resolve.name', '坚毅')
  event.add('trait.example.resolve.desc', '提升护甲韧性。')
  event.add('modifier.example.amethyst_guard.name', '紫晶守护')
  event.add('modifier.example.amethyst_guard.desc', '减轻受到的伤害。')
})
```

翻译键分别为 `trait.<namespace>.<path>.name/.desc` 与 `modifier.<namespace>.<path>.name/.desc`。材料名称与颜色由基础匠魂材料提供。宝典读取当前定义，新内容会自动进入相应页面。

## 排错

查看本次启动的 `logs/kubejs/startup.log`、`logs/kubejs/server.log` 和 `logs/latest.log`。脚本语法无报错不代表注册回调已生效；应检查创造物品栏、宝典、新建护甲的实际数值及回调效果。调试接口时可运行 [服务端示例验证](../VERIFICATION.md)。配方 `/reload` 不会重新注册材料和回调。不要在不同客户端、服务端安装不同版本的启动定义。

需要附属式运行数据和加槽奖励时，可用 `Java.loadClass('com.gctn.tinkersarmorynirvana.api.ArmorExtensions')` 调用扩展接口。写操作应在服务端、纯属性计算／预览回调之外执行，数据参数使用原生 `CompoundTag`；接口本身不实现经验和等级。详见[扩展指南](EXTENSIONS.md)。
