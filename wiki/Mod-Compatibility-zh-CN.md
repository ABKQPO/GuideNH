# 模组兼容

GuideNH 为部分模组内置了条件性兼容支持。每项集成只在目标模组加载时启用；目标模组不存在时，相关的标签、索引与按键绑定均保持静默，指南其余功能不受影响。

## BetterQuesting

当 [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) 加载时，GuideNH 解锁三项能力：

1. 页面前言新增 `quest_ids` 键，按任务 UUID 建立索引。
2. 新增两个标签：`<QuestLink>`（行内）与 `<QuestCard>`（块级）。
3. 标准“打开指南”快捷键在 BetterQuesting 任务 GUI 中悬停某任务时也能生效：按住快捷键即可跳转到对应指南页。

### 按任务 UUID 索引页面

在希望与某个或多个任务关联的指南页 frontmatter 中添加 `quest_ids` 列表：

```yaml
---
navigation:
  title: 第二阶段 — 蒸汽时代
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - fedcba98-7654-3210-fedc-ba9876543210
---
```

值必须为纯任务 UUID 字符串（与 BetterQuesting 数据库中存储的格式一致）。格式错误或为空的条目会被跳过并在日志中警告。

一旦某个任务 UUID 被某页索引，`<QuestLink>` 与 `<QuestCard>` 的点击行为会改为跳转到该指南页，而不是直接打开 BetterQuesting 的任务 GUI。

### `<QuestLink>` 与 `<QuestCard>`

两个标签都通过 `id` 接收任务 UUID，并在编译时根据玩家进度决定外观：

| 状态 | 来源 | 渲染 |
| --- | --- | --- |
| 可见 | 任务已解锁但未完成 | 普通可点击链接 |
| 完成 | `quest.isComplete(player)` 返回 true | 可点击链接，绿色，末尾追加 `✓` |
| 锁定 | 任务存在但未解锁，可见性不为 HIDDEN/SECRET | 灰色斜体占位符，不可点击 |
| 隐藏 | 锁定且可见性为 HIDDEN 或 SECRET | 深灰斜体占位符，不泄露任务信息 |
| 缺失 | UUID 在数据库中找不到对应任务 | 红色斜体占位符 |

对可见 / 完成状态，点击目标的优先级为：

- 若任务 UUID 出现在某页的 `quest_ids` 中，则跳转该指南页
- 否则在当前屏幕之上叠加打开 BetterQuesting 的 `GuiQuest`

属性表与示例参见 [标签参考](Tags-Reference-zh-CN#questlink)。

### 隐藏任务的处理

GuideNH 永不渲染处于 `HIDDEN` 或 `SECRET` 状态且仍处于锁定的任务的标题与描述。占位文本采用翻译键，便于整合包做本地化：

| 翻译键 | 中文默认 |
| --- | --- |
| `guidenh.compat.bq.locked` | `未解锁任务` |
| `guidenh.compat.bq.hidden` | `隐藏任务` |
| `guidenh.compat.bq.missing` | `未知任务` |
| `guidenh.compat.bq.open_in_guide` | `在指南中打开` |

锁定（非隐藏）任务在 `<QuestLink>` 上仍会以悬浮提示展示描述，这与 BetterQuesting 自身在锁定任务上提供描述的行为一致；隐藏任务不会暴露任何 tooltip。

> [!NOTE]
> 任务状态在页面编译时根据本地玩家进度解析。编译结果按指南缓存；解锁或完成任务后重新打开指南会重新评估状态。

### “打开指南”快捷键集成

默认打开指南快捷键（`G`，可在 `key.guidenh.open_guide` 中重新绑定）在 BetterQuesting 加载时获得第二种触发路径。当 BetterQuesting 任务图 GUI 打开时：

1. 鼠标悬停在面板中的某个任务按钮上
2. 按住打开指南快捷键

若任意已注册指南通过 `quest_ids` 索引了该任务 UUID，GuideNH 将跳转到对应页面（如果指南尚未打开则同时开启）。若没有任何页面索引此 UUID，则快捷键不会执行任何操作 —— 不会回退到打开 BQ 任务 GUI，因为这恰好是 BQ 已经展示的内容。

该路径独立于物品 tooltip 触发流，背包内悬停物品仍按既有的物品 / 矿辞索引流程处理。

### BetterQuesting 缺席时的行为

- `<QuestLink>` 与 `<QuestCard>` 不会注册；使用了它们的页面会以标准“未知标签”错误形式呈现，直到你移除这些标签。
- `quest_ids` 仍会被解析并存入 `additionalProperties`，但不会被读取。
- 快捷键的任务悬停分支变为空操作。

这意味着面向 BetterQuesting 的指南只需编写一次，即可在没有 BetterQuesting 的环境下静默降级。
