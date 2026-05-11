[English](Annotations)

# 注解

GuideNH 的场景注解是放在游戏场景（`<GameScene>` / `<Scene>`）内部的子标签。它们在世界空间中渲染，并且可以包含子 Markdown/标签内容，后者会成为富 tooltip。

## 通用规则

- 注解只能在场景内部使用
- 子内容会成为 tooltip 正文
- 注解可通过场景 UI 开关隐藏
- `alwaysOnTop` 会在对应注解类型支持时，让注解绘制在场景几何体之上

## 支持的注解标签

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH 还支持 `<BlockAnnotationTemplate>`，它会把自己的子注解应用到当前场景里所有已经存在且匹配的方块上。

## `<BlockAnnotation>`

高亮一个 `1x1x1` 方块体积。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 向量 |
| `color` | 否 | `#RRGGBB`、`#AARRGGBB` 或 `transparent` |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |

示例：

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Highlights the controller block.
</BlockAnnotation>
````

## `<BoxAnnotation>`

高亮任意轴对齐包围盒。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `min` | 是 | `x y z` 最小向量 |
| `max` | 是 | `x y z` 最大向量 |
| `color` | 否 | 注解颜色 |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |

GuideNH 会在每个轴上自动交换反向提供的 min/max 坐标。

示例：

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Half-height area highlight.
</BoxAnnotation>
````

## `<LineAnnotation>`

在世界空间中绘制一条线段。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `from` | 是 | `x y z` 起点向量 |
| `to` | 是 | `x y z` 终点向量 |
| `color` | 否 | 注解颜色 |
| `thickness` | 否 | 线宽，float |
| `alwaysOnTop` | 否 | boolean expression |

示例：

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signal path.
</LineAnnotation>
````

## `<DiamondAnnotation>`

在世界坐标位置放置一个面向屏幕的菱形标记。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 是 | `x y z` 标记位置 |
| `color` | 否 | 着色；省略时默认亮绿色 |

示例：

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

绘制场景中的气泡文本标签。它可以跟随一个世界坐标锚点，也可以固定在场景中心附近的屏幕坐标上。与其他注解不同，它的子内容会作为气泡正文，而不是悬停 tooltip。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `pos` | 否 | `x y z` 世界坐标锚点 |
| `x`, `y`, `z` | 否 | 未提供 `pos` 时可用的独立坐标分量 |
| `text` | 否 | 气泡文本；省略时使用子 Markdown 内容 |
| `color` | 否 | 气泡边框颜色，默认浅灰色 |
| `backgroundAlpha` | 否 | 背景透明度，范围 `0` 到 `255`，默认 `204` |
| `maxWidth` | 否 | 像素换行宽度；`0` 表示单行 |
| `independent` | 否 | `true` 时固定在屏幕空间 |
| `yOffset` | 否 | `independent={true}` 时相对场景中心的垂直像素偏移 |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | 否 | 可选伴生高亮框范围 |
| `highlightColor` | 否 | 可选高亮框颜色 |

世界锚定的气泡会绘制一条连接线指向锚点。独立气泡会水平居中，并使用 `yOffset` 控制垂直位置。导入思索时间线的 `text` 注解时也会使用同一个运行时注解。

示例：

````md
<TextAnnotation pos="1.5 2 1.5" color="#FF44AAFF" maxWidth={120} backgroundAlpha={180}>
  在这里放入**优先级**物品。
</TextAnnotation>
````

固定屏幕坐标示例：

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  独立状态文本
</TextAnnotation>
````

## 富 Tooltip 内容

注解的子内容会按普通 GuideNH 内容编译，因此 tooltip 内可以包含：

- Markdown 段落和标题
- 物品/方块图片
- 配方
- 嵌套的非交互场景

示例：

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

当你希望把同一种注解盖到所有匹配方块上时使用它。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 方块匹配器，格式为 `modid:block[:meta]` |

规则：

- 模板只会看到它被解析时场景里已经存在的方块
- 应放在 `<Block>`、`<ImportStructure>` 或 `<ImportStructureLib>` 之后
- 子注解使用相对于每个匹配方块的局部坐标

示例：

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Template-generated tooltip
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## 相关页面

- [游戏场景](GameScene-zh-CN)
- [示例](Examples-zh-CN)
