[English](Guide-Page-Format)

# 指南页面格式

GuideNH 的运行时页面使用 Markdown 文件，当前解析支持：

- 标准 Markdown 块级与行内语法
- YAML frontmatter
- GFM 表格
- 删除线
- `==text==` 行内高亮
- GuideNH 行内下划线扩展：`++text++`（直下划线）、`^^text^^`（波浪下划线）、`::text::`（着重号 / 点状下划线）
- `{/* ... */}` 形式的 MDX 注释
- MDX 风格的自定义标签

## 已支持的 Markdown

GuideNH 页面当前支持示例指南里常用的这些 Markdown 能力：

- 标题
- 段落
- 行内强调、粗体、删除线、行内代码
- 行内高亮（`==text==`）
- 行内下划线（`++text++`）、波浪下划线（`^^text^^`）和着重号（`::text::`）
- 链接与图片
- 直接写出的 URL、`www.` 域名和邮箱自动链接
- 引用式链接与引用式图片
- 无序列表与有序列表
- 引用块
- 分隔线
- 围栏代码块
- 缩进代码块
- GFM 表格
- 纯小写 HTML 片段，例如 `<kbd>`、`<sub>`
- 页面正文中的 MDX 注释

可参考 `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md` 查看实际运行时示例。

## 高亮

使用 `==text==` 可以生成行内高亮文字。需要自定义颜色时，可以使用 `<mark color="#8A6A00">text</mark>`。默认高亮色是偏暗的金黄色，用于在白色文字下保持可读性。

## 代码块

运行时代码块当前支持：

- 显式指定围栏语言，例如 `java`、`lua`、`scala`、`csv`、`mermaid`
- 围栏语言省略时自动推断语言
- 在代码块顶部显示语言标签
- 在游戏内右上角提供一键复制按钮
- 对识别出的语言做轻量运行时语法高亮

示例：

````md
```lua
local value = 42
print(value)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

缩进代码块同样支持：

````md
    print("indented code")
````

当围栏代码块识别为 `mermaid`，并且内容是当前支持的 `mindmap` 语法时，GuideNH 会把它渲染成可交互的运行时思维导图，而不是普通代码块。

当围栏代码块显式标记为 `csv` 时，GuideNH 会把它渲染成运行时表格，而不是普通代码块。如果不写围栏语言，即使内容看起来像 CSV，也仍然保留为代码块，只把语言识别结果用于标签和高亮。

显式 CSV 表格也可以提供列宽 hint：

````md
```csv widths=120,80
name,value
iron,42
gold,17
```
````

围栏 `meta` 也支持 `header=false` 和带引号的宽度列表：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

普通段落文本里也支持 GitHub 风格的直接自动链接：

````md
访问 https://example.com/docs、www.example.org 或 guide@example.com
````

## Mermaid 思维导图

GuideNH 当前的 Mermaid 运行时支持聚焦在 `mindmap`：

- 围栏 ```` ```mermaid ```` 代码块
- 内容以 `mindmap` 开头时的自动识别
- 显式 `<Mermaid>...</Mermaid>` 标签
- 显式 `<Mermaid src="./diagram.mmd" />` 资源导入
- Mermaid 节点文本中的富文本行内 Markdown
- 作为 `<Mermaid>` 子元素的 `<NodeContent id="...">...</NodeContent>`，可为指定节点放入任意运行时块内容
- 游戏内整张图拖拽平移
- Mermaid 源文本里的 `layout: tidy-tree`
- 常见 mindmap 节点形状，例如方形、圆角、圆形、bang、cloud、hexagon
- `::icon(...)` 与 `:::class` 元数据解析

示例：

````md
```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [首页](./index.md)"]
    runtime["运行时块"]
    export["站点导出"]

<NodeContent id="runtime">
运行时节点里可以混排普通文本、链接和块内容。

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png)
</NodeContent>
</Mermaid>
````

当前运行时尚未支持的 Mermaid 图类型，会继续按带有 Mermaid 标签的普通代码块显示。

## CSV 表格导入

GuideNH 也支持通过显式标签在运行时导入 CSV 文件：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路径会像普通运行时资源链接和场景 `src` 导入一样，相对当前页面解析。

导入的 CSV 表格同样可以提供列宽 hint：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

也可以直接在 Markdown 里通过显式 `csv` 围栏写内联表格：

````md
```csv
name,value
iron,42
gold,17
```
````

## Markdown 表格列宽 Hint

普通 GFM Markdown 表格也可以在表格后面紧跟一行运行时属性，提供列宽 hint：

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

这样表格本体依然保持标准 Markdown，只是在 GuideNH 运行时应用列的首选宽度。

## 任务列表、提示块与脚注

GuideNH 运行时还支持几种常见的 GFM 风格行为：

- 使用 `- [ ]` 和 `- [x]` 的任务列表
- GitHub 风格提示引用块，例如 `[!NOTE]`、`[!TIP]`、`[!IMPORTANT]`、`[!WARNING]`、`[!CAUTION]`
- 脚注引用与定义

示例：

````md
- [x] 已完成
- [ ] 待处理

> [!NOTE]
> 这里是提示内容

脚注引用[^one]

[^one]: 这里是脚注内容
````

脚注引用会在正文中渲染成 tooltip 风格标记，并在页面下方追加一个紧凑的脚注列表。

## 列表宽度自定义

标准 Markdown 列表本身没有宽度控制，但 GuideNH 运行时容器可以约束列表宽度：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

这是当前运行时里自定义列表行宽的推荐写法。

## 引用式链接与图片

GuideNH 支持 CommonMark 引用定义：

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## 原生 HTML 片段

纯小写 HTML 标签会按字面量内联或块级 HTML 内容处理，而不会当成 GuideNH 运行时标签：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
````

## MDX 注释

GuideNH 支持 MDX 注释写法，并会在真正编译 Markdown 前忽略它们：

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH 会读取第一个 YAML frontmatter 块，并解析这些已知键：

| 键 | 类型 | 含义 |
| --- | --- | --- |
| `navigation` | map | 将页面加入导航树 |
| `categories` | 字符串列表 | 将页面加入分类索引 |
| `item_ids` | 物品引用列表 | 让页面可被 `<ItemLink>` 发现 |
| `ore_ids` | 矿辞名列表 | 让页面可被矿辞物品（如 `ingotIron`、`oreCopper`）索引 |
| `quest_ids` | BetterQuesting 任务 id 列表 | 让页面可被 `<QuestLink>` / `<QuestCard>` 以及 BQ 任务 GUI 中的打开指南快捷键发现。支持标准 UUID 字符串和 BetterQuesting 的紧凑 Base64 形式。仅在 BetterQuesting 加载时生效。参见 [模组兼容](Mod-Compatibility-zh-CN) |
| `author` | string | 单一作者名称。显示在底部栏中。 |
| `authors` | 字符串列表或 `{name: ...}` 映射列表 | 多位作者名称。最多显示两位，多余的用 `...` 替代。与 `author` 同时存在时以 `authors` 为准。 |
| `date` | 字符串或 YYYY-MM-DD 日期 | 内容创建日期。显示在底部栏中。 |
| `updated` | 字符串或 YYYY-MM-DD 日期 | 最后更新日期。显示在底部栏中。 |
| `zoom` | 正浮点数 | 单页内容缩放倍数（如 `1.5` 表示 150%）。与 ModConfig 中全局 `contentZoom` 设置相乘。默认 `1.0`。 |
| 其他任意键 | 任意 YAML 值 | 保存在 `additionalProperties` 中，供扩展或工具使用 |

### `navigation`

| 字段 | 必需 | 类型 | 说明 |
| --- | --- | --- | --- |
| `title` | 是 | string | 导航显示名称，也可作为搜索标题后备值 |
| `parent` | 否 | page id | 父页面 id；省略时为顶级节点 |
| `position` | 否 | integer | 同级排序顺序，默认 `0`，数值越大越靠前 |
| `priority` | 否 | integer | 同路径页面覆盖时的加载优先级；默认 `0`，数值更高者胜出，相同优先级时后处理的资源包条目覆盖先处理的 |
| `icon` | 否 | item id | 导航和搜索中显示的物品图标。支持 `modid:name`、`modid:name:meta`（冒号分隔的损伤值/子类型）、`<modid:name:meta>`（严格形式；meta `32767` 匹配所有子类型）以及 `modid:name meta`（空格分隔，过滤表达式风格）。 |
| `icons` | 否 | item id 列表 | 循环轮播的物品图标列表（每秒切换一次）。每项语法同 `icon`。存在时优先于 `icon` 使用。 |
| `icon_texture` | 否 | asset path | 纹理图标路径，按普通资源路径解析 |
| `icon_textures` | 否 | asset path 列表 | 循环轮播的纹理图标列表（每秒切换一次）。存在时优先于 `icon_texture` 使用。 |
| `icon_components` | 否 | map | 以 YAML map 形式写入物品 NBT 数据（1.7.10 等价实现）。例如 `display.Name` 可自定义图标物品名称。仅作用于 `icon` 字段的单个物品，`icons` 列表不支持该字段。 |

### Frontmatter 示例

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book
  # 使用 meta/损伤値选择特定子类型：
  # icon: minecraft:wool:1       （橙色羊毛，冒号写法）
  # icon: <minecraft:wool:1>     （严格尖括号写法）
  # icon: minecraft:wool 1       （空格写法，过滤表达式风格）

  # 循环图标列表——每秒切换一次：
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4
  #   - minecraft:wool:14

  # 为图标物品附加 NBT（仅单 icon: 有效，icons: 不适用）：
  # icon_components:
  #   display:
  #     Name: "我的自定义书"

  icon_texture: test1.png
  # 循环纹理列表：
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: 示例作者
date: 2024-01-15
updated: 2024-06-01
```

对于 BetterQuesting 集成，`quest_ids` 支持两种格式：

- 标准 UUID 字符串，例如 `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting 的紧凑 quest id，例如 `AAAAAAAAAAAAAAAAAAAMug==`

不要在同一页面的 `quest_ids` 中同时填写同一个任务的两种写法；它们会归一化成同一个内部 UUID，并被视为重复项。

当 `author`、`authors`、`date` 或 `updated` 中任意一项存在时，GuideNH 会在
指南界面底部显示一个与顶部工具栏风格一致的底部栏，靠右对齐显示形如：
*内容来自 我的模组，作者 示例作者，日期 2024-01-15，更新日期 2024-06-01* 的信息。

多作者示例：
```yaml
authors:
  - 爱丽丝
  - 鲍勃
  - 查理   # 只显示爱丽丝和鲍勃，后跟 ...
```
或使用结构化写法：
```yaml
authors:
  - name: 爱丽丝
  - name: 鲍勃
```

### `zoom`

`zoom` 字段可以单独放大或缩小某一页的内容，而不影响其他页面。其值为正浮点数，作为倍数使用：

| 示例值 | 效果 |
| --- | --- |
| `1.0`（默认） | 正常大小 |
| `1.5` | 150%，内容放大 50% |
| `0.75` | 75%，内容缩小 25% |

单页 `zoom` 与 ModConfig → GuideNH → UI 中的全局 **contentZoom** 设置相乘生效。
这使模组包可以设置合理的基准缩放，同时允许个别页面根据内容宽窄自行微调。

示例：将本页设置为基准缩放的 150%：

```yaml
zoom: 1.5
navigation:
  title: 我的密集页面
```

缩放会以调整后的宽度重新计算页面布局，因此无论缩放级别如何，文字换行和所有块布局都能保持正确。

## 链接解析规则

GuideNH 按以下规则解析 id 和路径：

### 页面链接

| 输入 | 含义 |
| --- | --- |
| `subpage.md` | 相对当前页面，并使用当前页面命名空间 |
| `./subpage.md` | 相对当前页面，并使用当前页面命名空间 |
| `/guide.md` | 相对当前页面命名空间根路径，等价于 `currentmod:guide.md` |
| `gregtech:guide.md` | 显式命名空间；当前指南路径为 `guidenh` 时会打开 `gregtech:guidenh` |
| `gregtech:/guide.md` | 显式命名空间加根路径，会规范化为 `gregtech:guide.md` |
| `subpage.md#anchor` | 页面加锚点片段 |
| `guidenh:other.md#anchor` | 显式 `modid:path#anchor` |
| `https://example.com` | 外部 HTTP/HTTPS 链接 |

页面链接按命名空间隔离。例如在 `assets/guidenh/guidenh/_zh_cn/index.md` 中写
`[Guide](guide.md)` 会解析为 `guidenh:guide.md`；同样的文本放在
`assets/gregtech/guidenh/_zh_cn/index.md` 中会解析为 `gregtech:guide.md`。如果当前命名空间下找不到目标页面，
GuideNH 会报告坏链，而不会回退到其他模组的同名页面。

显式 `modid:path` 链接可以跨到另一个模组的数据驱动指南。目标 guide id 会由目标页面命名空间和当前指南路径推导，
所以从 `guidenh:guidenh` 链接到 `gregtech:guide.md` 时，会打开 `gregtech:guidenh` 中的
`gregtech:guide.md` 页面。

锚点片段会使指南滚动到对应标题（标题文本全部小写、空格替换为连字符，例如 `#crafting-recipe` 对应 `## Crafting Recipe`），或滚动到 `<a name="...">` 命名锚点处。

### 资源链接

资源使用与页面链接相同的解析规则。例如：

- `test1.png` 相对当前页面文件解析
- `/assets/example_structure.snbt` 解析到指南资源根目录
- `guidenh:textures/gui/example.png` 作为显式资源定位符解析

## 物品引用语法

若干标签支持扩展物品引用语法：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

规则如下：

- 省略 `meta` 时默认使用 `0`
- `*`、`32767` 或大写标记（例如 `ANY`）会被视为通配 meta
- SNBT 从第一个 `{` 开始，并会被解析为物品 NBT

示例：

```text
minecraft:diamond
minecraft:wool:14
minecraft:wool:*
minecraft:written_book:0:{title:TestBook,author:GuideNH}
```

## 错误处理

如果页面解析失败，GuideNH 会生成错误页，而不是让整个指南崩溃。无效标签、id 和属性会以内联指南错误文本的形式显示出来。

## 相关页面

- [导航](Navigation-zh-CN)
- [图片与资源](Images-And-Assets-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
