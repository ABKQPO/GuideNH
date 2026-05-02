[English](Tags-Reference)

# 标签参考

本页列出由 `DefaultExtensions` 注册的内置运行时标签。

## 使用规则

- 标签可以出现在块级上下文或行内上下文，具体取决于对应编译器。
- 页面内容支持使用 `{/* ... */}` 形式的 MDX 注释，运行时解析器会忽略这些注释。
- 无效标签或无效属性不会静默失败，而是以内联指南错误的形式显示。
- 配方和 3D 场景这类大型功能标签会在独立页面中说明：
  - [配方](Recipes-zh-CN)
  - [GameScene](GameScene-zh-CN)
  - [注解](Annotations-zh-CN)

## 行内与流式标签

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<a>` | 内部/外部链接，以及可选锚点名 | `href`, `title`, `name` |
| `<br>` | 换行 | `clear="none\|left\|right\|all"` |
| `<kbd>` | 键位风格行内强调 | 无 |
| `<sub>` | 较小的下标风格行内文本 | 无 |
| `<sup>` | 较小的上标风格行内文本 | 无 |
| `<Color>` | 彩色行内文本 | `id` 或 `color` |
| `<Tooltip>` | 带 Markdown/标签子内容的富悬浮提示 | `label` |
| `<PlayerName>` | 插入当前玩家用户名 | 无 |
| `<KeyBind>` | 插入按键绑定显示名 | `id` |
| `<ItemImage>` | 行内物品图标 | `id` 或 `ore`，`scale`，`noTooltip`，`yOffset` |
| `<ItemLink>` | 物品 tooltip + 可选导航链接 | `id` 或 `ore` |
| `<CommandLink>` | 可点击的聊天命令链接 | `command`, `title`, `close` |
| `<QuestLink>` | BetterQuesting 任务链接，按任务状态自动调整样式（兼容标签，仅当 BetterQuesting 已加载时注册） | `id`, `text` |

## 块级标签

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<div>` | 透传块包装器 | 无 |
| `<details>` | 可折叠运行时块 | `open` |
| `<FileTree>` | 目录树式大纲（带连接线） | `indent`、`gap` |
| `<Row>` | 横向 flex 布局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | 纵向 flex 布局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | 运行时 Markdown 脚注使用的限宽脚注容器 | `width` |
| `<ItemGrid>` | 紧凑物品图标网格 | 子元素必须是 `<ItemIcon id="..."/>` 或 `<ItemIcon ore="..."/>` |
| `<BlockImage>` | 方块的物品形态图标 | `id` 或 `ore`，`scale` |
| `<FloatingImage>` | 浮动图片块 | `src`, `align`, `title`, `width`, `height` |
| `<SubPages>` | 导航子页面列表 | `id`, `alphabetical` |
| `<CategoryIndex>` | 分类页面列表 | `category` |
| `<Structure>` | 2.5D 等轴方块布局预览 | `width`, `height` |
| `<Mermaid>` | 运行时 Mermaid 图导入/内联 | `src` |
| `<CsvTable>` | 运行时 CSV 文件导入表格 | `src`, `header`, `widths` |
| `<ColumnChart>` | 簇状柱形图 | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | 横向条形图 | 同 `<ColumnChart>` |
| `<LineChart>` | 折线图（类别或数值 X 轴） | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | 饼图 | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY 散点图 | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | 配方渲染器 | 详见 [配方](Recipes-zh-CN) |
| `<GameScene>`, `<Scene>` | 3D 指南场景 | 详见 [GameScene](GameScene-zh-CN) |
| `<QuestCard>` | 块级 BetterQuesting 任务摘要卡片（兼容标签，仅当 BetterQuesting 已加载时注册） | `id`, `show_desc` |

## 标签细节

### `<a>`

其行为类似 HTML 风格锚点标签：

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` 可以是相对路径、根路径、显式 `modid:path`，或 HTTP/HTTPS 链接
- `title` 会作为 tooltip 使用
- `name` 会插入一个页面锚点目标

### `<br>`

GuideNH 也支持带浮动清理能力的 MDX 风格换行标签：

````md
Text before.<br clear="all" />Text after.
````

可接受的 `clear` 值：

- `none`
- `left`
- `right`
- `all`

### `<kbd>`、`<sub>` 与 `<sup>`

GuideNH 运行时支持一小组常见的小写文档标签：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

用于创建可折叠的运行时内容块：

````md
<details open>
<summary>More</summary>

Hidden-by-default body text
</details>
````

### `<FileTree>`

渲染目录树式大纲，并依据每行前缀符号绘制真实的连接线。前缀同时支持 Unicode 框线（`│ ├ └ ─`）与 ASCII 形式（`| +-- \-- ` / 4 个空格），可任意混用。每行的文本部分支持常规行内 Markdown（链接、**加粗**、`代码` 等）。等价语法是 ` ```tree ` / ` ```filetree ` 围栏代码块。

````md
<FileTree indent="14" gap="0">
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
</FileTree>
````

可在每行开头加入可选图标指令：

- `{:icon=文本}` — 简短的文本图标
- `{:iconPng=path/to/file.png}` — PNG 资源（按当前页面解析路径）
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft 物品图标。可选 `meta` 为损词值（`*` 表示通配）；可选末尾 `:{snbt}` 能附加 SNBT 到物品。

````md
```filetree
world
|-- {:iconItem=minecraft:grass} 草地生物群系
|   \-- {:icon=树} 橡木森林
\-- {:iconPng=test1.png} 示例资源
```
````

属性：

- `indent` — 每层缩进像素数（默认 `14`）
- `gap` — 行间额外像素数（默认 `0`）

### `<Color>`

可使用符号颜色 id，或显式十六进制颜色值：

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>
````

规则：

- `id` 和 `color` 在实际使用中应二选一
- `color` 支持 `#RRGGBB`、`#AARRGGBB` 或 `transparent`

### `<Tooltip>`

创建带下划线的文本，并在悬停时显示富内容 tooltip。

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

若省略 `label`，默认触发文字为 `tooltip`。

### `<PlayerName>`

插入当前 Minecraft 会话用户名：

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

通过 id 查找按键绑定，并渲染玩家当前实际绑定的按键名称。

可接受的 id 形式：

- 绑定本身的描述 id，例如 `key.jump` 或 `key.guidenh.open_guide`
- 兼容旧写法的 `category.description` 形式，例如 `key.categories.movement.key.jump`

示例：

````md
Press <KeyBind id="key.jump" /> to jump.
````

### MDX 注释

GuideNH 会忽略页面内容里的 MDX 注释：

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

### `<ItemImage>`

显示行内物品图标。

| 属性 | 含义 |
| --- | --- |
| `ore` | 矿辞名；默认取第一个匹配结果 |
| `id` | 当未提供 `ore` 时使用的物品引用 |
| `scale` | float，默认 `1` |
| `noTooltip` | 传入真值字符串或空属性时禁用 tooltip |
| `yOffset` | scale 为 `1` 时的像素偏移覆盖值 |

说明：

- 同时提供 `ore` 和 `id` 时，优先使用 `ore`
- 若安装了 GregTech，选中的矿辞结果会先经过 `GTOreDictUnificator.setStack(...)` 统一化

示例：

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
````

### `<ItemLink>`

使用物品显示名创建文本链接，并附带物品 tooltip。若 `item_ids` 把该物品映射到了某一页面，点击后还会导航过去。也可以用 `ore` 通过矿辞的第一个匹配结果来决定显示的物品。

````md
<ItemLink id="minecraft:compass" />
<ItemLink ore="stickWood" />
````

### `<CommandLink>`

点击后发送聊天命令。

| 属性 | 含义 |
| --- | --- |
| `command` | 必填，且必须以 `/` 开头 |
| `title` | 可选 tooltip 标题 |
| `close` | 布尔属性；当前会被解析，但不会实际关闭指南 |

示例：

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` 与 `<Column>`

用于块内容的 flex 风格容器。

| 属性 | 含义 |
| --- | --- |
| `gap` | 子元素间距，整数，默认 `5` |
| `alignItems` | `start`、`center`、`end` |
| `fullWidth` | boolean expression，默认 `false` |
| `width` | 整数首选宽度，适合约束列表等块内容的行宽 |

示例：

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

如果想约束普通 Markdown 列表的宽度，可以这样包一层：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

### `<FootnoteList>`

GuideNH 会在运行时 Markdown 脚注展开后内部使用这个块标签。必要时也可以手写：

````md
<FootnoteList width="220">
1. 第一条脚注
2. 第二条脚注
</FootnoteList>
````

### `<ItemGrid>`

渲染紧凑的物品图标网格。其子元素必须是原始 `<ItemIcon>` 元素，由网格编译器直接解析。每个子元素都可以使用 `id` 或 `ore`。

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

渲染方块的物品形态。若使用 `ore`，该矿辞必须解析到一个方块物品。

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" />
````

### `<FloatingImage>`

完整行为请参见 [图片与资源](Images-And-Assets-zh-CN)。

### `<SubPages>` 与 `<CategoryIndex>`

完整导航行为请参见 [导航](Navigation-zh-CN)。

### `<Structure>`

当你在静态结构预览和完整 3D 场景之间做选择时，可结合 [示例](Examples-zh-CN) 和 [GameScene](GameScene-zh-CN) 参考。

### `<Mermaid>`

用于运行时 Mermaid 图内容。当前运行时支持重点是 `mindmap`，可以直接写子内容，也可以通过 `src` 从同目录资源导入：

````md
<Mermaid src="./markdown-mindmap.mmd" />
````

### `<CsvTable>`

用于在运行时把 CSV 文件解析成表格：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路径会像场景导入和普通资源链接一样，相对当前页面解析。

可选属性：

- `header`
  默认是 `true`；设置 `header={false}` 时，首行不会加粗
- `widths`
  逗号分隔的整数列宽 hint，例如 `widths="120,80"`

示例：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
````

对应的围栏 CSV 运行时写法也支持相同语义的 `meta`：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

### 场景运行时标签

这些标签仅能在 `<GameScene>` / `<Scene>` 内部工作：

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<ImportStructure>` | 导入外部 SNBT/NBT 结构资源 | `src`, `x`, `y`, `z` |
| `<ImportStructureLib>` | 通过控制器 id 导入 StructureLib 多方块 | `controller`, `piece`, `facing`, `rotation`, `flip`, `channel` |
| `<RemoveBlocks>` | 移除已放置且匹配指定方块匹配器的方块 | `id` |
| `<BlockAnnotationTemplate>` | 把同一组子注解扩展到场景中所有匹配方块上 | `id` |

关于场景导入/移除行为，请参见 [GameScene](GameScene-zh-CN)；关于注解模板规则，请参见 [注解](Annotations-zh-CN)。


## 数据图表

`<ColumnChart>`、`<BarChart>`、`<LineChart>`、`<PieChart>`、`<ScatterChart>` 均为交互式图表块。所有图表共享下列通用属性：

| 属性 | 说明 | 默认 |
| --- | --- | --- |
| `title` | 标题 | 无 |
| `width` / `height` | 显式尺寸 | 320 / 200 |
| `background` / `border` | 背景与边框颜色（支持 `#RGB`、`#RRGGBB`、`#AARRGGBB`、`0x...`） | 深灰 |
| `titleColor` / `labelColor` | 标题与数据标签颜色 | 浅灰 |
| `legend` | 图例位置：`none`/`top`/`bottom`/`left`/`right` | `top` |
| `labelPosition` | 数据标签位置：`none`/`inside`/`outside`/`above`/`below`/`center` | `none` |

笛卡尔系图表（柱形/条形/折线/散点）支持坐标轴属性 `xAxisLabel`、`xAxisMin`、`xAxisMax`、`xAxisStep`、`xAxisUnit`、`xAxisTickFormat`，以及对应的 `yAxis*`，外加 `showXGrid={true}` 与 `showYGrid={true}` 控制网格线。

子元素：

* `<Series name="..." color="#..." data="10,20,30"/>` — 用于按类别取值的图表（柱形/条形/类别 X 折线）。
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` — 用于数值 X（折线 `numericX={true}`、散点）。
* `<Slice label="..." value="..." color="#..."/>` — 仅 `<PieChart>` 使用。

未指定 `color` 时按内置 16 色调色板循环分配。

`<Series>` 与 `<Slice>` 还接受以下可选图标/tooltip 属性：

* `icon="modid:item"`（可带 `@meta` 或互动 NBT JSON，同 `<ItemImage>` 的 `id`）— 使用 `ItemStack` 作为图例色块与悬停图标；悬停时会显示原版物品 tooltip 并在末尾追加图表说明。
* `iconImage="images/foo.png"` — 使用 PNG 资源作为图例色块（被 `icon` 覆盖）。
* `tooltip="..."` — 额外追加到 tooltip 末尾的文本（多行用 `\n`）。

示例：

```mdx
<PieChart title="产出占比">
  <Slice label="铁錠" value="40" icon="minecraft:iron_ingot" tooltip="来自冶炼烉" />
  <Slice label="金錠" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

附加属性：`categories`（X 轴/Y 轴类别，逗号分隔）、`barWidthRatio`（默认 0.7）。`<BarChart>` 类别在 Y 轴、数值在 X 轴。

#### 组合扩展

`<ColumnChart>` 和 `<BarChart>` 额外支持两类子元素，便于在同一坐标系内叠加多种图形：

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — 在柱簇上方再叠加一条折线。每个折点位于对应类别簇中心，与宿主图共享数值轴；可同时声明多条 `<LineSeries>`。
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — 在绘图区四角之一（`topRight`/`topLeft`/`bottomRight`/`bottomLeft`）绘制小型饼图，内部 `<Slice>` 子元素与 `<PieChart>` 一致。

```mdx
<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4">
  <Series name="铁"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="金"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="合计" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="合计占比">
    <Slice label="铁" value="225" color="#a0a0a0"/>
    <Slice label="金" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

附加属性：`numericX={true}`（启用数值 X 轴，子元素改用 `points`）、`showPoints={false}`（隐藏点）。悬停某个数据点时该点沿曲线法向外推 2px、半径 +2 并加黑边，相邻线段加粗。

### `<PieChart>`

附加属性：`startAngle`（起始角度，默认 -90 即 12 点钟方向）、`clockwise={false}` 反向。悬停时被悬停扇区沿其角平分线方向外移 4px。

### `<ScatterChart>`

仅绘制数据点；`<Series>` 必须使用 `points` 属性。X 轴始终为数值轴。

## BetterQuesting 兼容标签

`<QuestLink>` 与 `<QuestCard>` 仅在 BetterQuesting 模组已加载时才会被注册。详细说明位于 [模组兼容](Mod-Compatibility-zh-CN) 页面，下面给出常用用法摘要。

### `<QuestLink>`

指向 BetterQuesting 任务的行内链接。点击时默认打开 BetterQuesting 任务 GUI；若该任务的 UUID 出现在某个指南页的 `quest_ids` 前言中，则改为跳转到该页面。

| 属性 | 含义 |
| --- | --- |
| `id` | 必填，任务 UUID |
| `text` | 可选，覆盖显示文本 |

按玩家进度在编译时决定外观：

- 已可见 / 已完成的任务渲染为可点击链接（已完成的会被染为绿色并追加 `✓`）
- 已锁定的任务渲染为不可点击的斜体灰色占位符，使用本地化键 `guidenh.compat.bq.locked`
- 隐藏 / 机密的任务渲染为更深的斜体占位符，使用 `guidenh.compat.bq.hidden`
- 未知 UUID 渲染为红色占位符，使用 `guidenh.compat.bq.missing`

示例：

````md
下一步请参考 <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />。
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="第二阶段任务" />
````

### `<QuestCard>`

块级任务摘要卡片。标题使用与 `<QuestLink>` 相同的状态相关样式；当任务对当前玩家可见时，会附带任务描述作为正文段落。

| 属性 | 含义 |
| --- | --- |
| `id` | 必填，任务 UUID |
| `show_desc` | 可选布尔，默认 `true`；设为 `false` 可隐藏描述正文 |

卡片边框颜色随任务状态变化：已完成绿色、锁定 / 隐藏灰色、缺失红色、可见时使用标准链接色。

示例：

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
````
