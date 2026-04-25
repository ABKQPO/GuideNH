---
navigation:
  title: 调试测试页
---

# 调试测试页

本页用于验证常见组件的渲染：链接、表格、锁定预览、多参数视角等。返回 [首页](./index.md) 。

## 链接悬浮下划线 + 点击

- 内部跳转：[Markdown 语法](./markdown.md)
- 内部跳转：[渲染测试](./rendering.md)
- 外部链接：[Minecraft Wiki](https://minecraft.wiki/)

## 表格

| 名称 | ID | 说明 |
|------|----|------|
| 工作台 | `minecraft:crafting_table` | 标准 3×3 合成 |
| 铁镐 | `minecraft:iron_pickaxe` | 可挖掘铁矿 |
| 木门 | `minecraft:wooden_door` | 基础门 |

## 锁定视角（不可旋转）

<GameScene zoom={3} interactive={false}>
    <Block id="minecraft:crafting_table" />
    <Block id="minecraft:stone" x="1" />
    <Block id="minecraft:log" z="1" />
</GameScene>

## 不同初始旋转 + 缩放

缩放 2、俯视角度较大：

<GameScene zoom={2} rotateX={60} rotateY={0}>
    <Block id="minecraft:grass" />
    <Block id="minecraft:grass" x="1" />
    <Block id="minecraft:grass" z="1" />
    <Block id="minecraft:grass" x="1" z="1" />
    <Block id="minecraft:log" y="1" />
</GameScene>

缩放 5、几乎正侧视：

<GameScene zoom={5} rotateX={10} rotateY={45}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:stone" y="1" />
    <Block id="minecraft:stone" y="2" />
</GameScene>

## 配方渲染（空格 + 箭头）

<RecipeFor id="minecraft:crafting_table" />

<RecipeFor id="minecraft:iron_pickaxe" />
