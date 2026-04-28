[English](Navigation)

# 导航

GuideNH 会根据页面 frontmatter 构建导航树。

## 导航 Frontmatter

`navigation` map 控制一个页面是否会出现在指南树中。

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### 字段说明

| 字段 | 说明 |
| --- | --- |
| `title` | 必填，显示标题 |
| `parent` | 可选，父页面 id |
| `position` | 可选，同级排序提示 |
| `icon` | 可选，物品图标 |
| `icon_texture` | 可选，从指南资源中解析的纹理图标 |
| `icon_components` | 会被解析，但当前内置渲染尚未使用 |

## 图标来源

GuideNH 会按以下顺序选择导航/搜索图标：

1. 若 `icon_texture` 能成功加载，则优先使用它
2. 若 `icon` 对应的物品存在，则使用物品图标
3. 两者都不可用时，不显示图标

纹理图标来自运行时资源，因此像 `test1.png` 这样的页面私有相对文件也能正常工作。

## 父节点与根节点

- 省略 `parent` 会创建一个根节点。
- 设置 `parent: index.md` 或任意其他页面 id 会创建子节点。
- 父页面必须存在于同一份指南导航树中。

## 分类页面

页面可以通过 frontmatter 加入一个或多个命名分类：

```yaml
categories:
  - basics
  - machines
```

这些分类可通过内置 `<CategoryIndex>` 标签查询。

## 物品索引页面

页面可使用 `item_ids` 注册“物品到页面”的映射：

```yaml
item_ids:
  - minecraft:compass
  - minecraft:wool:*
```

这些映射会被 `<ItemLink>` 使用。

查找顺序如下：

1. 精确物品 + 精确 meta
2. 如果存在，则回退到通配 meta

## `<SubPages>`

`<SubPages>` 会渲染导航子页面链接列表。

### 属性

| 属性 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `id` | page id 或空字符串 | 当前页面 | 列出其子页面的页面 id |
| `alphabetical` | boolean expression | `false` | 按标题字母排序，而不是按导航顺序排序 |

### 示例

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

特殊情况：`id=""` 会列出所有根导航节点。

## `<CategoryIndex>`

`<CategoryIndex>` 会渲染某个命名分类下的全部页面链接。

````md
<CategoryIndex category="machines" />
````

如果分类不存在，GuideNH 会显示内联错误。

## 搜索结果标题

搜索标题按以下顺序确定：

1. `navigation.title`
2. 第一个一级标题（`# Heading`）
3. 原始页面 id

## 相关页面

- [指南页面格式](Guide-Page-Format-zh-CN)
- [搜索](Search-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
