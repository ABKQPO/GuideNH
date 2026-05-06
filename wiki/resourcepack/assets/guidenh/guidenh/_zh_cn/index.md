---
item_ids:
  - guidenh:guide
navigation:
  title: 首页
  icon_texture: test1.png
---

# 起始页

## 导航

| 分类 | 页面 |
|---|---|
| Markdown | [Markdown 基础](./markdown.md) · [图表](./charts.md) · [函数图](./function-graph.md) |
| 3D 场景 | [方块场景](./scene-blocks.md) · [实体场景](./scene-entities.md) · [镜头与视口](./scene-camera.md) |
| 注解 | [注解](./scene-annotations.md) · [导入结构](./scene-import.md) |
| 组件 | [配方](./recipes.md) · [图片](./images.md) · [Tooltip](./tooltips.md) |
| 其他 | [渲染演示](./rendering.md) · [调试](./debug.md) |

## 行内元素

缺失配方（回退文字）：<Recipe id="minecraft:missingrecipe" fallbackText="该物品无法合成。" />

玩家名：<PlayerName />

按键绑定：<KeyBind action="key.attack" />

注释（不渲染）：<Comment>这里是注释，不会显示在页面上。</Comment>

颜色：<Color color="#FF5555">红色文字</Color> / <Color color="#55FF55">绿色文字</Color> / <Color color="#5555FF">蓝色文字</Color>

命令链接：<CommandLink command="/say 你好，世界！">点击发送命令</CommandLink>

方块图（行内）：<BlockImage id="minecraft:stone" /> 石头 · <BlockImage id="minecraft:iron_block" /> 铁块 · <BlockImage id="minecraft:gold_block" /> 金块

物品链接：<ItemLink id="minecraft:diamond" /> · <ItemLink id="minecraft:emerald" /> · <ItemLink id="minecraft:nether_star" />
