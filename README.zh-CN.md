# 匠魂护甲：涅槃

将 **AntiqueArmory / Construct's Armory** 移植至 **Minecraft 1.21.1 + NeoForge 21.1.265+**，依赖 **TinkersNirvana 1.21.1-1.0.2+**。沿用 `conarm` 模组 ID 与内容命名空间，采用 **LGPL-3.0-or-later**。

[English](README.md) · [附属开发文档](docs/zh-CN/API.md) · [KubeJS 魔改文档](docs/zh-CN/KUBEJS.md) · [配置说明](docs/zh-CN/CONFIGURATION.md) · [材料表](docs/MATERIALS.md) · [验证与手动验收](docs/VERIFICATION.md) · [许可与署名](NOTICE.md)

包含四件模块化护甲、七种材质部件、原版三十种护甲材料数值、护甲词条与强化、组装、修复、部件替换、抛光、浮雕、装甲装配台和锻造台、冶炼炉浇筑，以及旅行腰带、药水腰带、背包、护目镜和披风。护甲损坏后保留物品，可以修复。配件库存保存在所属护甲的数据组件中。穿戴模型、配件模型与贴图沿用原项目资源并适配新版渲染接口。

手册沿用原版的介绍、护甲、护甲材料、强化、配件五章。打开封面后，左侧是深色封底，右侧是图标目录。材料页保留侧边物品列、两列属性、词条提示与风味文字；强化图读取当前数据包／KubeJS 配方。箭头、滚轮、左右方向键或 A/D 翻页，Home 返回目录，Backspace 返回链接前的位置。阅读位置保存在书本物品上。资源或配方重载后，关闭再打开手册。

## 安装

使用 Java 21、Minecraft 1.21.1 与 NeoForge 21.1.265 或更新的 21.1 版本。客户端和服务端均需安装 TinkersNirvana 与本模组的运行 JAR；不要把 `-sources.jar` 放进 `mods`。1.12 的 Forge、TinkersAntique、Mantle JAR 不能在本版本加载。

KubeJS 可选，已针对 `2101.7.2-build.381` 验证。使用时还需安装 KubeJS 声明的依赖，并保持客户端与服务端的启动脚本一致。不装 KubeJS 也可以正常加载本模组。

按照本次移植范围，JEI 暂缓：提供的 TinkersNirvana 尚无 JEI 集成实现。旧版 CraftTweaker／ContentTweaker 能力通过 Java API 和 KubeJS 接口承接。原先可选的 Tool Leveling 功能提供原生配置实现，默认关闭，不强制新增依赖。

## 基本使用

1. 用工作台和四个匠魂空白模具合成装甲装配台。在匠魂原生**模具台**选择护甲部件模具，再到原生**部件加工台**投入可加工原料制作部件。模具保留。材料消耗：头盔核心 4、胸甲核心 6、护腿核心 5、靴子核心 4、护甲板 3、夹板 1、抛光磨具 2 份。
2. 在装甲装配台左侧选择头盔、胸甲、护腿或靴子标签，下方中间放对应核心、左侧放护甲板、右侧放夹板。三者允许不同材料。左栏预览穿戴外观，右侧两栏显示属性和特性；取出成品时才消耗部件。
3. 金属等可浇筑材料使用共用的匠魂冶炼炉；在部件上浇注金或铝黄铜获得金属铸模，也支持由依赖配置控制的一次性黏土铸模。
4. 选择“修复&强化”标签，使用原版环形六槽。中间放护甲，其余槽放核心材料或同材质抛光磨具修复；放入新部件可替换对应材料。文本框用于命名产物。3×3 合成栏也支持抛光磨具修复。
5. 普通装配台负责组装、修复、换部件和命名；**装甲锻造台**用于强化、抛光和刻印。抛光使用护甲、抛光磨具和沙子；刻印使用护甲、目标部件、绿／蓝／熔岩史莱姆结晶和金块，每件护甲只允许一次。
6. 恢复原版按键：**G 头盔、H 胸甲、J 护腿**，直接切换已安装的护目镜／披风、打开背包／药水腰带或交换旅行腰带快捷栏。可在控制菜单分别修改。要求护甲已穿戴且未损坏。

每件护甲只能安装一个旅行配件。三种手套仅用于胸甲且互斥；高步、发光和冰霜行者仅用于靴子；水下呼吸强化仅用于头盔。不同抗性类型互斥，同类型抗性只有首次安装占用强化槽。

依赖的 `deconstructTools` 开启时，在修复标签输入为空的情况下，将完整、无损坏的护甲放到右侧产物槽，预览三个部件。取回护甲可以取消；取走任一部件才完成分解，原强化不保留。配件库存需事先取空。尚未取部件便拆掉工作台时，只掉落原护甲一次。

原有 `conarm:pattern` 物品继续兼容；右键可按当前部件和数量转换成原生模具。新模具通过原生模具台制作。材料表、部件箱与冶炼系统共用 TinkersNirvana 的实现。

## 构建

先构建包含 `AntiqueParts.registerExternal` 接口的 TinkersNirvana **1.21.1-1.0.2 或更新版本**，接口已提交 [PR #10](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/pull/10)，尚未合并到 Stable，源码位于 [codex/armory-parts-api 分支](https://github.com/GreedyCraftNirvanaMC/TinkersNirvana/tree/codex/armory-parts-api)。再通过 `tinkersNirvanaJar` 指定依赖 JAR。默认查找相邻项目下的 `build/libs/TinkersNirvana-1.21.1-1.0.2.jar`。

```powershell
$env:JAVA_HOME = 'C:\你的\JDK21\目录'
.\gradlew.bat build '-PtinkersNirvanaJar=C:/实际路径/TinkersNirvana-1.21.1-1.0.2.jar'
```

产物位于 `build/libs/`，包含运行 JAR 与源码 JAR。`-PwithKubeJS` 可以在开发运行配置中加载 KubeJS。仓库不提交依赖二进制或游戏测试世界。完整开发约定与魔改示例见上方文档；原版界面类与新版实现的对应关系见 [UI 源码映射](docs/UI_PORT.md)。

此移植不提供 1.12 附属的二进制兼容，也不自动转换 1.12 世界和物品 NBT。护甲防护通过 1.21 原生护甲、属性和伤害事件实现；旧版 `ISpecialArmor` 与脚本需要迁移。
