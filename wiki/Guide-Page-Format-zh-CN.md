[English](Guide-Page-Format)

# 指南页面格式

GuideNH 的运行时页面是 Markdown 文件，支持以下内容：

- 标准 Markdown 块级和行内语法
- YAML frontmatter
- GFM 表格
- 删除线
- 使用 `{/* ... */}` 的 MDX 注释
- MDX 风格的自定义标签

## 支持的 Markdown

GuideNH 页面支持示例指南中常用的 Markdown 能力：

- 标题
- 段落
- 行内强调、粗体、删除线和代码
- 链接与图片
- 无序列表和有序列表
- 引用块
- 分隔线
- 围栏代码块
- GFM 表格
- 页面正文中的 MDX 注释

可参考 `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` 查看实际运行示例。

## MDX 注释

GuideNH 支持 MDX 注释写法，并会在真正编译 Markdown 之前将其忽略：

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH 会读取第一个 YAML frontmatter 区块，并解析以下已知字段：

| 键 | 类型 | 含义 |
| --- | --- | --- |
| `navigation` | map | 将页面加入导航树 |
| `categories` | 字符串列表 | 将页面加入分类索引 |
| `item_ids` | 物品引用列表 | 让页面能通过 `<ItemLink>` 被发现 |
| 其他任意键 | 任意 YAML 值 | 保存在 `additionalProperties` 中，供扩展或工具使用 |

### `navigation`

| 字段 | 必需 | 类型 | 说明 |
| --- | --- | --- | --- |
| `title` | 是 | string | 导航显示名称，同时也是搜索标题的后备来源 |
| `parent` | 否 | page id | 父页面 id；省略时为顶级节点 |
| `position` | 否 | integer | 同级排序顺序，默认 `0` |
| `icon` | 否 | item id | 导航/搜索中显示的物品图标 |
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
```

## 链接解析规则

GuideNH 按以下规则解析 id 和路径：

### 页面链接

| 输入 | 含义 |
| --- | --- |
| `subpage.md` | 相对于当前页面 |
| `./subpage.md` | 相对于当前页面 |
| `/assets/example.png` | 相对于当前指南命名空间根路径 |
| `guidenh:index.md` | 显式 `modid:path` 资源定位符 |
| `subpage.md#anchor` | 页面加锚点片段 |
| `https://example.com` | 外部 HTTP/HTTPS 链接 |

### 资源链接

资源使用与页面链接相同的解析规则。例如：

- `test1.png` 相对于当前页面文件解析
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
- `*`、`32767` 或大写标记（如 `ANY`）会被视为通配 meta
- SNBT 以第一个 `{` 开始，并会被解析为物品 NBT

示例：

```text
minecraft:diamond
minecraft:wool:14
minecraft:wool:*
minecraft:written_book:0:{title:TestBook,author:GuideNH}
```

## 错误处理

如果页面解析失败，GuideNH 会生成错误页，而不是让整个指南崩溃。无效标签、id 和属性会以内联的指南错误文本形式显示出来。

## 相关页面

- [导航](Navigation-zh-CN)
- [图片与资源](Images-And-Assets-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
