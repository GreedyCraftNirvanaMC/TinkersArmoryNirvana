# 配置说明

[项目说明](../../README.zh-CN.md) · [English](../en/CONFIGURATION.md) · [KubeJS 魔改](KUBEJS.md)

NeoForge 会生成 `config/conarm-server.toml`，客户端还会生成 `config/conarm-client.toml`。持久修改建议先关闭游戏或服务端。KubeJS 启动定义是另一层独立的内容配置。

## 服务端配置

| 配置项 | 默认值 | 含义 |
| --- | --- | --- |
| `spawnWithBook` | `true` | 首次进入该世界时发放盔甲手册 |
| `bounceDurability` | `true` | 弹跳靴成功弹跳时消耗一点耐久 |

本体没有护甲升级配置。经验获取、等级、奖励和对应配置由附属通过[扩展接口](EXTENSIONS.md)提供，旧 `[leveling]` 配置不再读取。

首次发书使用玩家持久标记，因此重连和复活不会重复领取；背包满时在玩家旁边掉落手册。

## 客户端配置

`compactView=false`：显示工作台侧边的护甲预览人偶；改为 true 后隐藏，使用紧凑界面。

配件按键是普通 Minecraft 按键绑定：默认 **G 头盔、H 胸甲、J 护腿**，直接操作对应部位已安装的配件，可在“控制”菜单分别修改，不写在 TOML 中。护目镜与披风开关分别保存在对应护甲上；库存和主动配件都要求护甲已穿戴且未损坏。

## 依赖侧配置

原料标签、是否允许直接加工可浇筑材料、黏土铸模与冶炼炉规则由 TinkersNirvana 管理，本模组直接复用，避免维护两套冲突设置。护甲材料数值、词条与强化行为使用 Java／KubeJS API 修改，普通合成及护甲强化配方使用数据包或 `ServerEvents.recipes` 修改。
