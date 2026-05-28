[English](Tags-Reference)

# 标签参考

本页列出由 `DefaultExtensions` 注册的内置运行时标签。

## 使用规则

- 标签可以出现在块级上下文或行内上下文，具体取决于对应编译器。
- 页面内容支持使用 `{/* ... */}` 形式的 MDX 注释，运行时解析器会忽略这些注释。
- 无效标签或无效属性不会静默失败，而是以内联指南错误的形式显示。
- 配方和 3D 场景这类大型功能标签会在独立页面中说明：
  - [配方](Recipes-zh-CN)
  - [游戏场景](GameScene-zh-CN)
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
| `<SoundLink>` | 可点击的富文本音效触发器 | `sound` 或 `src`，`volume`，`pitch`，`cooldown` |
| `<mark>` | 行内高亮文本；等价于 `==text==`，并可自定义颜色 | `color` |
| `<PlayerName>` | 插入当前玩家用户名 | 无 |
| `<KeyBind>` | 插入按键绑定显示名 | `id` 或 `action` |
| `<ItemImage>` | 行内物品图标 | `id` 或 `ore`，`scale`，`noTooltip`，`showTooltip`，`showIcon`，`label`，`format`，`yOffset`，`labelYOffset` |
| `<ItemLink>` | 物品 tooltip + 可选导航链接 | `id` 或 `ore`，`linksTo`，`showTooltip`，`noTooltip`，`showIcon` |
| `<CommandLink>` | 可点击的聊天命令链接 | `command`, `title`, `close` |
| `<Latex>` | LaTeX 数学公式；在流式上下文中行内渲染，在块级上下文中居中显示为独立公式块 | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting 任务链接，按任务状态自动调整样式（兼容标签，仅当 BetterQuesting 已加载时注册） | `id`, `text`, `show_tooltip` |

行内 Markdown 也支持音效动作链接：

````md
&[启动机器](sound:guidenh:machine.start)
&[播放文件音效](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## 块级标签

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<div>` | 透传块包装器 | 无 |
| `<details>` | 可折叠运行时块 | `open`、`width`、`height`、`wrap`、`align` |
| `<FileTree>` | 目录树式大纲（带连接线） | `indent`、`gap` |
| `<Row>` | 横向 flex 布局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | 纵向 flex 布局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | 运行时 Markdown 脚注使用的限宽脚注容器 | `width` |
| `<ItemGrid>` | 紧凑物品图标网格 | 子元素必须是 `<ItemIcon id="..."/>` 或 `<ItemIcon ore="..."/>` |
| `<BlockImage>` | 非交互式的 3D 单方块预览 | `id` 或 `ore`，`scale`，`float`，`perspective`，`nbt` |
| `<FloatingImage>` | 浮动图片块 | `src`, `align`, `title`, `width`, `height` |
| `<SubPages>` | 导航子页面列表 | `id`, `alphabetical` |
| `<Category>` | 分类页面列表 | `name`, `rows` |
| `<Special>` | 内置 MediaWiki 特殊页面列表 | `name`, `rows` |
| `<Structure>` | 2.5D 等轴方块布局预览 | `width`, `height` |
| `<Mermaid>` | 运行时 Mermaid 图导入/内联 | `src` |
| `<CsvTable>` | 运行时 CSV 文件导入表格 | `src`, `header`, `widths` |
| `<ColumnChart>` | 簇状柱形图 | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | 横向条形图 | 同 `<ColumnChart>` |
| `<LineChart>` | 折线图（类别或数值 X 轴） | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*`, `cornerLegend` |
| `<PieChart>` | 饼图 | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY 散点图 | `xAxis*`, `yAxis*`, `legend`, `labelPosition`, `cornerLegend` |
| `<FunctionGraph>` | Desmos 风格的多曲线函数图 | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes`, `cornerLegend` |
| `<Function>` | 单曲线简写，等价于只含一个 `<Plot>` 的 `<FunctionGraph>` | `expr`，及全部 `<FunctionGraph>` 面板属性 |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | 配方渲染器 | 详见 [配方](Recipes-zh-CN) |
| `<GameScene>`, `<Scene>` | 3D 指南游戏场景 | 详见 [游戏场景](GameScene-zh-CN) |
| `<QuestCard>` | 块级 BetterQuesting 任务摘要卡片（兼容标签，仅当 BetterQuesting 已加载时注册） | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` 是 `<GameScene>` 的子标签。它支持 `controller`、`piece`、`facing`、
`rotation`、`flip`、`channel` 属性，也支持用于设置 StructureLib 默认值的子标签：
`<Tier>`、`<Channel>`、`<Facing>`、`<Rotation>`、`<Flip>`、`<Orientation>`、
`<GregTechActiveController>`、`<GregTechPlaceHatches>`。

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

用于创建可折叠的运行时内容块。`<summary>` 行支持常规行内 Markdown/标签内容，正文则可以放普通文本与任意块级标签，例如 `<BlockImage>`、`<FloatingImage>`、`<GameScene>`、表格、图表或布局容器。

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>更多 <ItemImage id="minecraft:diamond" /></summary>

默认折叠的正文文本，里面可以放[普通页面链接](./index.md)。

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

属性：

- `open` — 存在时默认展开
- `width` — 外层首选宽度，单位像素
- `height` — 正文视口首选高度，单位像素；超出部分会在游戏内和站点导出中变为可滚动区域
- `wrap` — 支持常见块嵌入模式，例如 `square`、`tight`、`through`
- `align` — `left`、`center` 或 `right`；与浮动型 `wrap` 搭配时会让整个 details 块浮动

### `<FileTree>`

渲染目录树式大纲，并依据每行前缀符号绘制真实的连接线。前缀同时支持 Unicode 框线（`│ ├ └ ─`）与 ASCII 形式（`| +-- \-- ` / 4 个空格），可任意混用。每行的文本部分支持常规行内 Markdown（链接、**加粗**、`代码` 等），这些链接在游戏内和内置站点导出中都可以点击。等价语法是 ` ```tree ` / ` ```filetree ` 围栏代码块。

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

### `<SoundLink>` 与音效动作链接

`<SoundLink>` 会渲染一段可点击的富文本内容，点击后播放音效。它不会进行页面导航，
并且本次点击会使用自定义音效替代指南默认点击音效。

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **启动机器**
</SoundLink>

&[启动机器](sound:guidenh:machine.start)
&[使用音效文件](sound-src:guidenh:sounds/machine/start.ogg)
````

音效属性：

- `sound` 是音效事件 id，例如 `modid:event.name`
- `src` 指向 `.ogg` 文件；`modid:sounds/machine/start.ogg` 会转换为 `modid:machine.start`
- `volume` 默认 `1.0`
- `pitch` 默认 `1.0`
- `cooldown` 是重复播放的间隔毫秒数，默认 `250`
- `radius` 和 `minVolume` 用于场景中的屏幕空间衰减

### `<PlayerName>`

插入当前 Minecraft 会话用户名：

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

通过 id 或 action 查找按键绑定，并渲染玩家当前实际绑定的按键名称。

可接受的 id 形式：

- 绑定本身的描述 id，例如 `key.jump` 或 `key.guidenh.open_guide`
- 兼容旧写法的 `category.description` 形式，例如 `key.categories.movement.key.jump`

示例：

````md
Press <KeyBind id="key.jump" /> to jump.
攻击键：<KeyBind action="key.attack" />。
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

GuideNH 也会忽略显式 `<Comment>` 标签：

````md
可见文字。<Comment>这里不会渲染。</Comment>仍然可见。
````

### `<ItemImage>`

显示行内物品图标。

| 属性 | 含义 |
| --- | --- |
| `ore` | 矿辞名；默认取第一个匹配结果 |
| `id` | 当未提供 `ore` 时使用的物品引用 |
| `scale` | float，默认 `1` |
| `noTooltip` | 传入真值字符串或空属性时禁用 tooltip（旧写法，推荐改用 `showTooltip`） |
| `showTooltip` | boolean，默认 `true`；`false` 时禁用鼠标悬停 tooltip |
| `showIcon` | boolean，默认 `true`；`false` 时不渲染图标图形 |
| `label` | `left` 或 `right`——在图标左侧或右侧以文字显示物品名称；省略时不显示文字 |
| `format` | 标签文字的格式模式；支持 Markdown 风格的包裹标记（`**粗体**`、`*斜体*`、`~~删除线~~`、`__下划线__`、`^^波浪__`、`::点状::`），可包含 `%s` 占位符代替物品名；默认（不写本属性）以斜体渲染物品名 |
| `yOffset` | scale 为 `1` 时**图标**的像素偏移覆盖值；不影响标签文字 |
| `labelYOffset` | scale 为 `1` 时**标签文字**的像素偏移覆盖值；不影响图标 |

说明：

- 同时提供 `ore` 和 `id` 时，优先使用 `ore`
- 若安装了 GregTech，选中的矿辞结果会先经过 `GTOreDictUnificator.setStack(...)` 统一化
- `label` 需要至少一个可见元素（图标或文字），若同时设置 `showIcon="false"` 且不写 `label`，则渲染为空
- `format` 仅在设置了 `label` 时生效；若 `format` 中没有 `%s`，则以格式字面量作为标签文字

示例：

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
````

### `<ItemLink>`

使用物品显示名创建文本链接，并附带物品 tooltip。若 `item_ids` 把该物品映射到了某一页面，点击后还会导航过去。也可以用 `ore` 通过矿辞的第一个匹配结果来决定显示的物品。

| 属性 | 默认值 | 含义 |
| --- | --- | --- |
| `id` | — | 物品注册 ID，如 `minecraft:compass` 或 `minecraft:wool:1` |
| `ore` | — | 矿辞名称，使用第一个匹配的物品堆叠 |
| `linksTo` | *（自动）* | 覆盖跳转目标，接受带可选 `#anchor` 的页面 ID，如 `./crafting.md#usage` 或 `#usage`；省略时由 `item_ids` / `ore_ids` 索引自动解析 |
| `showTooltip` | `true` | 设为 `false` 时悬停不显示 tooltip；`noTooltip` 是旧版兼容别名 |
| `showIcon` | *（无）* | `left` 或 `right`（或任意真值 → 右侧）— 在链接文字的左侧或右侧显示物品图标；省略则仅显示文字 |

示例：

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
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

渲染一个非交互式的 3D 单方块场景。这个预览没有场景背景、没有场景按钮、没有 layer 控件，
也不支持注解功能，但鼠标悬停时仍然会显示选中线框和 tooltip。若使用 `ore`，该矿辞必须
解析到一个方块物品。

| 属性 | 含义 |
| --- | --- |
| `id` | 方块 id；支持常规 `modid:block[:meta][:{snbt}]` 写法 |
| `ore` | 矿辞查询；使用第一个匹配到的方块物品 |
| `scale` | 相机缩放倍率，默认 `1` |
| `float` | 旧版流式浮动支持：`left` 或 `right` |
| `perspective` | `isometric-north-east`（默认）、`isometric-north-west` 或 `up` |
| `nbt` | 可选的 TileEntity SNBT；会合并到 `id` 内联 SNBT 之上 |

说明：

- 为兼容旧内容，`id` 里的内联 SNBT 仍然有效，但更推荐写成独立的 `nbt="..."` 属性
- 当同时提供内联 SNBT 和 `nbt` 时，`nbt` 会最后合并，因此同名键会以 `nbt` 为准
- GuideNH 当前的 1.7.10 运行时不支持这里的现代方块状态属性语法，因此不会支持
  GuideME 里的 `p:<state>` 写法

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" perspective="isometric-north-west" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<FloatingImage>`

完整行为请参见 [图片与资源](Images-And-Assets-zh-CN)。

### `<SubPages>`、`<Category>` 与 `<Special>`

完整导航行为请参见 [导航](Navigation-zh-CN)。

### `<Structure>`

当你在静态结构预览和完整 3D 游戏场景之间做选择时，可结合 [示例](Examples-zh-CN) 和 [游戏场景](GameScene-zh-CN) 参考。

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

### `<Latex>`

使用 jlatexmath 渲染 LaTeX 数学公式。在行内（段落或文本流中）使用时，渲染为自动缩放的字形，并将所在行的行高扩展以适应公式高度。独立成段（块级上下文）使用时，居中渲染为展示式公式块。

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `formula` | 字符串 | *（必填）* | LaTeX 源字符串 |
| `color` | `#RRGGBB` 或 `#AARRGGBB` | `#FFFFFF` | 字形填充颜色 |
| `scale` | float | `1.0` | 在自动行高缩放的基础上额外叠加的显示大小倍率 |
| `sourceScale` | float | `100.0` | jlatexmath 内部渲染分辨率；数值越高，在较大尺寸下渲染越清晰 |
| `tooltip` | 字符串 | *无* | 鼠标悬停时显示的普通文本 tooltip |
| `showTooltip` | boolean | `false` | 鼠标悬停时以 tooltip 展示原始 LaTeX 源文本 |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | 仅限行内公式。行内垂直对齐方式：`baseline`（默认）使公式数学基线与文字基线对齐；`top` 使公式顶部与行顶对齐；`center` 将公式垂直居中于文字行高；`bottom` 使公式底部与文字底部对齐 |
| `offsetX` | int | `0` | 对齐后额外施加的水平像素偏移（正值为向右） |
| `offsetY` | int | `0` | 对齐后额外施加的垂直像素偏移（正值为向下） |

示例：

````md
行内：<Latex formula="E=mc^2" />

分数（自动扩展行高）：<Latex formula="\frac{a+b}{c-d}" />

金色：<Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

放大：<Latex formula="\pi" scale="1.5" />

悬停显示源码：<Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

自定义普通 tooltip：<Latex formula="E=mc^2" tooltip="能量等于质量乘以光速平方。" />

富文本 tooltip：
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **吉布斯自由能**

  - <Latex formula="\Delta H" />：焓变
  - <Latex formula="T\Delta S" />：熵项
</Latex>

底部对齐（公式底部与文字底部对齐）：<Latex formula="\frac{a}{b}" valign="bottom" />

显式基线对齐（与默认效果相同）：<Latex formula="E=mc^2" valign="baseline" />

顶部对齐并向上微调：<Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$公式$$` 简写语法

你可以在 Markdown 文本中直接使用 `$$公式$$` 语法，无需使用 `<Latex>` 标签。
所有渲染参数均使用默认值（白色、比例 1.0、无悬停提示、基线对齐）。

- **行内模式**：嵌入段落内的 `$$公式$$` 使用行内渲染。
- **展示模式**：整个段落内容仅为 `$$公式$$`（首尾可有空白）时，渲染为居中的展示式公式块。

````md
行内简写：$$E=mc^2$$ 和 $$a^2+b^2=c^2$$

行内分数：$$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

注意事项：

- 公式高度以当前行文字高度为基准自动校准。简单公式与文字等高；含分数、求和、积分等高度较大的公式会自动扩展所在行的行高。
- `valign` 仅对行内公式生效。块级（展示式）公式始终水平居中；如需垂直方向调整，请使用 `offsetY`。
- `color` 默认为白色（`#FFFFFF`）。如需半透明填充，使用 `#AARRGGBB` 格式。
- `sourceScale` 仅影响渲染清晰度，不改变显示大小。低于 `16` 的值会被截断为 `16`。
- tooltip 优先级为：标签体富文本 Markdown、`tooltip="..."`、最后才是 `showTooltip={true}` 的原始源码回退。
- 标签体 tooltip 会按普通指南 Markdown 编译，因此可以包含粗体、列表、链接、物品标签和嵌套 `<Latex>` 公式。
- `$$公式$$` 简写语法始终使用默认参数。如需自定义颜色、比例、对齐或悬停提示，请使用 `<Latex>` 标签。

### 场景运行时标签

这些标签仅能在游戏场景（`<GameScene>` / `<Scene>`）内部工作：

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<ImportStructure>` | 导入外部 SNBT/NBT 结构资源 | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | 通过控制器 id 导入 StructureLib 多方块 | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | 移除已放置且匹配指定方块匹配器的方块 | `id` |
| `<BlockAnnotationTemplate>` | 把同一组子注解扩展到场景中所有匹配方块上 | `id` |

关于场景导入/移除行为，请参见 [游戏场景](GameScene-zh-CN)；关于注解模板规则，请参见 [注解](Annotations-zh-CN)。


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
| `cornerLegend` | 图内角落图例位置：`none`/`topRight`/`topLeft`/`bottomRight`/`bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | 图内图例框最大尺寸 | `120` / `64` |
| `cornerLegendBackground` | 图内图例背景色 | `#AA111922` |

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

`<LineChart>` 与 `<ScatterChart>` 可以用 `cornerLegend="topRight"` 等位置值在绘图区内部显示紧凑图例。图例条目使用已有系列名和系列颜色。

### `<PieChart>`

附加属性：`startAngle`（起始角度，默认 -90 即 12 点钟方向）、`clockwise={false}` 反向。悬停时被悬停扇区沿其角平分线方向外移 4px。

### `<ScatterChart>`

仅绘制数据点；`<Series>` 必须使用 `points` 属性。X 轴始终为数值轴。

## 函数图

`<FunctionGraph>` 与单曲线简写 `<Function>` 渲染交互式 Desmos 风格面板。同一面板也可通过 ` ```funcgraph ` 围栏代码块写出，详细示例见运行时 [Markdown 示例](resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md)。

面板属性（容器、简写、围栏首行通用）：

- `width` / `height`（默认 `320` × `220`）
- `title`、`background`、`border`、`axisColor`、`gridColor`
- `showGrid` / `showAxes`（默认 `true`）
- `xRange="a..b"`（或 `xMin` / `xMax` 分写），`xStep` 控制刻度间距；Y 轴同理
- `quadrants="1,2,3,4"` 或 `quadrants="all"` 强制可见象限；不写则默认仅第一象限，并在采样发现 `y < 0` 时自动追加第三、第四象限
- `cornerLegend`、`cornerLegendWidth`、`cornerLegendHeight`、`cornerLegendBackground` 可以把带 `label` 的曲线显示为绘图区内部的紧凑图例

曲线子节点（`<Plot>` / `<Function>`）：

- `expr="..."`：表达式。支持 `+ - * / % ^`、后缀阶乘 `!`（gamma 推广至实数）、`|x|` 绝对值、`√`/`sqrt`、`∛`/`cbrt`、隐式乘法以及常量 `pi`、`tau`、`e`、`phi`。内建调用覆盖常规 trig/log/exp/rounding 函数，并提供双参数 `atan2`、`min`、`max`、`pow`、`hypot`、`mod`。
- `inverse={true}` 将表达式解释为 `x = f(y)` 并旋转曲线。
- `domain="a..b"`（x 上下界简写）或逗号分隔的比较子句，如 `x>=0, x<5`。
- `color`、`label`。设置了非空 `label` 的曲线会自动出现在面板下方的图例里：一个小色块加曲线名，按从左到右排列，宽度不够时自动换到下一行。
- `pointEveryX="step"` 按固定 x 间隔在该曲线上生成点。
- `pointEveryY="step"` 在曲线与固定 y 间隔的交点处生成点，内部使用有上限的扫描和二分求解。
- `autoPointLabel="none|x|y|xy"` 控制自动点标签，默认 `none`。
- `autoPointColor="#..."` 覆盖自动点颜色；省略时继承曲线颜色。

标记点（`<Point>`）：

- 显式坐标：`x="..."` + `y="..."`。
- 锚定到曲线：`plot="N"` 配合 `atX="v"` 或 `atY="v"`（运行时在该曲线 x 域上二分求解）。
- 可选 `color`、`label`。

交互：鼠标悬停曲线会高亮该曲线；按住可沿曲线滑动一个标记点。提示框第一行显示表达式，第二行显示 `(x, y)`；默认锚定在该点正上方，顶部空间不足时自动翻到下方。

## BetterQuesting 兼容标签

`<QuestLink>` 与 `<QuestCard>` 仅在 BetterQuesting 模组已加载时才会被注册。详细说明位于 [模组兼容](Mod-Compatibility-zh-CN) 页面，下面给出常用用法摘要。

### `<QuestLink>`

指向 BetterQuesting 任务的行内链接。点击时默认打开 BetterQuesting 任务 GUI；若该任务 id 出现在某个指南页的 `quest_ids` 前言中，则改为跳转到该页面。

| 属性 | 含义 |
| --- | --- |
| `id` | 必填，BetterQuesting 任务 id；支持标准 UUID 字符串和紧凑 Base64 id |
| `text` | 可选，覆盖显示文本 |
| `show_tooltip` | 可选布尔值，默认 `true`；设为 `false` 可关闭任务描述 tooltip。也接受别名 `showTooltip` |

按玩家进度在编译时决定外观：

- 已可见 / 已完成的任务渲染为可点击链接（已完成的会被染为绿色并追加 `✓`）
- 已锁定但未隐藏的任务仍会渲染为可点击任务链接，因此仍可打开 BetterQuesting 任务界面或对应的索引指南页
- 隐藏 / 机密的任务渲染为更深的斜体占位符，使用 `guidenh.compat.bq.hidden`
- 未知任务 id 渲染为红色占位符，使用 `guidenh.compat.bq.missing`

示例：

````md
下一步请参考 <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />。
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="第二阶段任务" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
然后再看 <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="紧凑 quest id 示例" />。
````

### `<QuestCard>`

块级任务摘要卡片。标题使用与 `<QuestLink>` 相同的状态相关样式；当任务对当前玩家可见时，会附带任务描述作为正文段落。

| 属性 | 含义 |
| --- | --- |
| `id` | 必填，BetterQuesting 任务 id；支持标准 UUID 字符串和紧凑 Base64 id |
| `show_desc` | 可选布尔，默认 `true`；设为 `false` 可隐藏描述正文 |
| `show_tooltip` | 可选布尔值，默认 `true`；设为 `false` 可关闭可点击标题上的任务描述 tooltip。也接受别名 `showTooltip` |

卡片边框颜色随任务状态变化：已完成绿色、锁定 / 隐藏灰色、缺失红色、可见时使用标准链接色。标题在可见、已完成以及锁定但未隐藏时都会保持可点击。

示例：

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
