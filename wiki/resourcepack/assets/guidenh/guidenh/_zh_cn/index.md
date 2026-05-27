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

## Markdown

<Category name="markdown" rows="3" />

## 场景

重点场景文档：[场景粒子](./scene-particles.md)

<Category name="scenes" rows="3" />

## 组件

<Category name="widgets" rows="3" />

## 其他

<Category name="other" rows="3" />

## 隐藏的 MediaWiki 页面

可以直接体验这些自动生成的 MediaWiki 风格页面：

- [Special:SpecialPages](Special:SpecialPages)
- [Special:AllPages](Special:AllPages)
- [Special:Categories](Special:Categories)
- [Category:markdown](Category:markdown)

## 行内元素

<Recipe id="missingrecipe" fallbackText="该物品无法合成。" />

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
