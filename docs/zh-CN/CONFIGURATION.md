# 配置说明

[项目说明](../../README.zh-CN.md) · [English](../en/CONFIGURATION.md) · [KubeJS 魔改](KUBEJS.md)

NeoForge 会生成 `config/conarm-server.toml`，客户端还会生成 `config/conarm-client.toml`。持久修改建议先关闭游戏或服务端；修改升级规则后完整重启，使已有物品缓存与两端规则一致。KubeJS 启动定义是另一层独立的内容配置。

## 服务端配置

| 配置项 | 默认值 | 含义 |
| --- | --- | --- |
| `spawnWithBook` | `true` | 首次进入该世界时发放盔甲手册 |
| `bounceDurability` | `true` | 弹跳靴成功弹跳时消耗一点耐久 |
| `leveling.enabled` | `false` | 启用原生护甲升级，承接旧版可选升级联动 |
| `leveling.startingModifiers` | `3` | 开启升级后的初始基础强化槽，范围 0—1024 |
| `leveling.maximumLevels` | `0` | 等级限制；0 表示使用实现上限 1000 |
| `leveling.baseXp` | `100` | 第一次升级所需经验 |
| `leveling.levelMultiplier` | `2.0` | 后续等级需求增长倍率，至少为 1 |
| `leveling.xpGainCap` | `100` | 一次实际正伤害事件最多获得的护甲经验 |
| `leveling.damageToXp` | `0.25` | 原始伤害转换成护甲经验的比例，正伤害至少获得 1 点 |

开启后，有效护甲在受到实际正伤害时获得经验，绕过护甲的伤害除外。初始等级为 0，每级增加一个强化槽。进度保存在各件护甲上，修复和换部件不会清除。经验需求限制在整数范围，实现支持最高 1000 级，即使没有设置更小的配置上限也是如此。本功能无需额外安装 Tool Leveling 模组。

关闭升级后停止经验增长，重新计算属性时不再计入升级奖励槽，但保留已保存进度，也不会删除已安装强化。降低槽位预算可能使旧装备暂时没有剩余槽位，需要恢复预算或调整部件。

首次发书使用玩家持久标记，因此重连和复活不会重复领取；背包满时在玩家旁边掉落手册。

## 客户端配置

`compactView=false`：显示工作台侧边的护甲预览人偶；改为 true 后隐藏，使用紧凑界面。

配件控制按键是普通 Minecraft 按键绑定，默认 **G**，在“控制”菜单中修改，不写在 TOML 中。护目镜与披风开关分别保存在对应护甲上；库存和主动配件都要求护甲已穿戴且未损坏。

## 依赖侧配置

原料标签、是否允许直接加工可浇筑材料、黏土铸模与冶炼炉规则由 TinkersNirvana 管理，本模组直接复用，避免维护两套冲突设置。护甲材料数值、词条与强化行为使用 Java／KubeJS API 修改，普通合成及护甲强化配方使用数据包或 `ServerEvents.recipes` 修改。
