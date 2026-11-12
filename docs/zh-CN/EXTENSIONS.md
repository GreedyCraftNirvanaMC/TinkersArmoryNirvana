# 附属数据与强化槽扩展接口

[Java API](API.md) · [English](../en/EXTENSIONS.md)

要求 TinkersArmoryNirvana **1.21.1-1.0.4+**、TinkersNirvana **1.21.1-1.0.2+**。附属的 `conarm` 依赖最低版本使用 `versionRange="[1.21.1-1.0.4,)"`。接口提供物品持久数据、被动额外强化槽和全局属性计算事件；经验来源、等级曲线、等级上限、奖励、命令、声音与提示均由附属实现。

## 保存数据与强化槽贡献

来源必须使用完整 ID，例如 `exampleaddon:progression`。接口要求已初始化的模块护甲，可先用 `ArmorData.initialized(stack)` 判断。写操作应在服务端游戏线程对实际所属物品执行。

| 方法 | 行为 |
| --- | --- |
| `read(armor, id)` | 返回独立 `CompoundTag` 副本；没有数据时返回空标签 |
| `modifierSlots(armor, id)` | 该来源贡献的额外强化槽，缺省为零 |
| `totalModifierSlots(armor)` | 所有已保存非负贡献的总和 |
| `write(armor, id, data)` | 替换当前来源的数据，保留其加槽贡献 |
| `setModifierSlots(armor, id, slots)` | 设置当前来源的绝对贡献，保留其数据 |
| `update(armor, id, data, slots)` | 一次更新数据和加槽贡献 |
| `remove(armor, id)` | 仅删除当前来源的数据及加槽贡献 |

贡献是绝对值，不要每 tick 累加。两个附属分别贡献 2 和 1，就在通常的三基础槽上增加三槽，再计入其他词条／强化；重复设置 2 仍只贡献 2。贡献必须非负，最终总属性遵守已有的 8192 强化槽校验上限。附属若需不同基础槽设计，可在全局属性事件中给已算出的槽数增加“目标基础槽 − 3”，避免覆盖其他贡献。

每次修改都先在独立护甲副本中重算并验证，成功后才提交组件；失败不会留下半次更新。成功后立即更新原生属性与最大耐久，保留其他来源、自定义名称、库存和当前损伤；最大耐久改变时仍按正常规则限制损伤值。输入数据和读取结果均不会与物品共享可变 NBT 引用。

```java
import com.gctn.tinkersarmorynirvana.api.ArmorExtensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

// 附属自行决定等级和奖励后，在服务端对实际所属护甲调用。
public static void saveProgress(ItemStack armor, int level, long xp, int bonusSlots) {
    CompoundTag data = ArmorExtensions.read(armor, "exampleaddon:progression");
    data.putInt("Level", level);
    data.putLong("Experience", xp);
    ArmorExtensions.update(armor, "exampleaddon:progression", data, bonusSlots);
}
```

数据位于 `conarm:armor -> Extensions -> <来源ID>`，下设 `Data` 与 `ModifierSlots`。使用既有原生组件保存／网络编码，修复和替换部件会保留。数据和贡献会持续保存在物品上，直到对应来源更新或移除，附属未安装时也不自动撤回。移除加槽贡献不会删除已安装强化；超出预算的装备只是没有剩余槽位。是否撤回奖励、补偿或限制操作由附属决定。分解护甲会随原装备一起失去附属数据。

## 使用原生事件处理游戏触发

按受伤升级的附属可订阅 NeoForge `LivingDamageEvent.Post`，自行选择 `getOriginalDamage()`、`getNewDamage()` 和 `getSource()` 的含义与适用条件。是否接收绕过护甲的伤害、损坏装备、哪些部位和穿戴者，由附属决定。确认服务端及 `ArmorData.initialized` 后，再修改实际穿戴的 ItemStack。依附于材料词条的设计也可继续使用原有 `afterDamage` 回调。现有 `experience` 词条钩子调整的是 Minecraft 掉落经验，并非护甲等级事件。

等级提示可以由附属订阅 `ItemTooltipEvent`，读取已同步的扩展数据，使用自己的翻译键。本体不显示等级，也不发送升级消息或播放升级声音。`ArmorData.level(stack, modifierId)` 仍表示指定强化的等级，不是整件护甲的经验等级。

## 全局护甲属性事件

在 **NeoForge 游戏事件总线**注册，两物理端应保持相同计算规则：

```java
import com.gctn.tinkersarmorynirvana.api.ArmorExtensions;
import com.gctn.tinkersarmorynirvana.api.ArmorStatsEvent;
import net.neoforged.neoforge.common.NeoForge;

NeoForge.EVENT_BUS.addListener(ArmorStatsEvent.class, event -> {
    var data = ArmorExtensions.read(event.getArmor(), "exampleaddon:attributes");
    event.getStats().toughness += data.getFloat("BonusToughness");
});
```

事件不可取消，适用于所有材料有效的模块护甲，不要求安装特定词条。`getArmor()` 每次返回独立快照，修改快照不会改变原装备。`getStats()` 是当前可修改计算结果：已应用材料、词条、强化、附属槽位贡献及抛光，防御仍是分配前的原版显示值，之后才做原生部位分配及最终校验。`ArmorData.stats` 和 `displayStats` 都触发此事件。

属性回调必须确定且没有副作用。物品提示、预览、服务端重算和接口验证都会反复调用，没有穿戴者或世界上下文。禁止在其中调用 `ArmorExtensions` 写操作、`ArmorData.rebuild/stats`、增长经验、保存状态、发包或修改世界。只读取快照并修改属性结果；状态更新提前放到游戏事件中执行。

## 内置升级移除后的行为

`ArmorLeveling` 与全部 `[leveling]` 配置已移除。通常基础强化槽为三，再计入词条／强化和附属贡献。旧根字段 `Level`、`Experience` 在写入或重算时删除，不迁移、不补偿；附属命名空间的 `Data` 内可以正常使用同名字段。无需改动 TinkersNirvana 源码。
