---
item_ids:
  - guidenh:guide
navigation:
  title: GuideNH 示例
  position: 200
  recommend: 3
author: GuideNH
date: 2024-01-01
---

# GuideNH 示例

欢迎使用 GuideNH 演示指南。本指南展示了作者可用的渲染、布局和场景能力。参见 [导航与索引](./navigation-guide.md) 了解如何构建你自己的指南页面。

| 分类 | 页面 |
|---|---|
| Markdown | [Markdown 基础](./markdown.md) · [图表](./charts.md) · [函数图](./function-graph.md) |
| 3D 场景 | [方块场景](./scene-blocks.md) · [实体场景](./scene-entities.md) · [镜头与视口](./scene-camera.md) |
| 注解 | [注解](./scene-annotations.md) · [导入结构](./scene-import.md) · [思索动画](./ponder.md) |
| 组件 | [配方](./recipes.md) · [图片](./images.md) · [Tooltip](./tooltips.md) · [内容嵌入](./content-embed.md) |
| 其他 | [渲染演示](./rendering.md) · [调试](./debug.md) |

## 行内元素

缺失配方（回退文字）：<Recipe id="minecraft:missingrecipe" fallbackText="该物品无法合成。" />

欢迎来到 <ItemImage id="minecraft:stone" /> 的世界，<PlayerName />！

按键测试：跳跃 = <KeyBind id="key.jump" />，攻击 = <KeyBind id="key.attack" />，GuideNH 热键 = <KeyBind id="key.guidenh.open_guide" />。

注释测试：可见之前。{/* 隐藏的行内注释 */} 可见之后。

注释块测试：
{/*
整个块被解析器忽略。
*/}
多行注释之后仍然可见。

你可能 ~~需要~~ 一扇 <Color color="#ff0000">门</Color> <Color id="RED">门</Color>！

<CommandLink command="/tp @s 0 90 0" title="提示" close={true}>传送！</CommandLink>

<BlockImage id="minecraft:crafting_table" />

<ItemLink id="minecraft:stick" />

场景补充页：[场景粒子](./scene-particles.md)
