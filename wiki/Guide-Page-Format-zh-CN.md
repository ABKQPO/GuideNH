[English](Guide-Page-Format)

# 指南页面格式

GuideNH 的运行时页面使用 Markdown 文件，当前解析支持：

- 标准 Markdown 块级与行内语法
- YAML frontmatter
- GFM 表格
- 删除线
- `{/* ... */}` 形式的 MDX 注释
- MDX 风格的自定义标签

## 已支持的 Markdown

GuideNH 页面当前支持示例指南里常用的这些 Markdown 能力：

- 标题
- 段落
- 行内强调、粗体、删除线、行内代码
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
| 其他任意键 | 任意 YAML 值 | 保存在 `additionalProperties` 中，供扩展或工具使用 |

### `navigation`

| 字段 | 必需 | 类型 | 说明 |
| --- | --- | --- | --- |
| `title` | 是 | string | 导航显示名称，也可作为搜索标题后备值 |
| `parent` | 否 | page id | 父页面 id；省略时为顶级节点 |
| `position` | 否 | integer | 同级排序顺序，默认 `0` |
| `icon` | 否 | item id | 导航和搜索中显示的物品图标 |
| `icon_texture` | 否 | asset path | 纹理图标路径，按普通资源路径解析 |
| `icon_components` | 否 | map | frontmatter 会解析，但当前运行时渲染尚未使用 |

### Frontmatter 示例

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  parent: index.md
  position: 10
  icon: minecraft:book
  icon_texture: test1.png
categories:
  - basics
  - examples
ore_ids:
  - ingotIron
  - oreCopper
```

## 链接解析规则

GuideNH 按以下规则解析 id 和路径：

### 页面链接

| 输入 | 含义 |
| --- | --- |
| `subpage.md` | 相对当前页面 |
| `./subpage.md` | 相对当前页面 |
| `/assets/example.png` | 相对当前指南命名空间根路径 |
| `guidenh:index.md` | 显式 `modid:path` 资源定位符 |
| `subpage.md#anchor` | 页面加锚点片段 |
| `https://example.com` | 外部 HTTP/HTTPS 链接 |

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
