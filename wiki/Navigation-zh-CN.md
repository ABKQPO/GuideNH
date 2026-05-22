[English](Navigation)

# 导航

GuideNH 会根据页面 frontmatter 构建导航树。

在游戏内侧边栏中，已经展开且其子树仍然可见的祖先页面会固定在顶部。多层祖先可以逐层堆叠固定，只有当对应可见子树整体滚出后，该 sticky 行才会一起被顶走，行为类似 VSCode 的文件树。

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
| `parent` | 可选，父页面 id；解析规则与指南页面链接相同 |
| `position` | 可选，同级排序提示 |
| `recommend` | 可选，首页推荐优先级；缺省时不会出现在推荐面板中 |
| `priority` | 可选，同路径页面覆盖时的加载优先级；默认 `0` |
| `icon` | 可选，物品图标 |
| `icon_texture` | 可选，从指南资源中解析的纹理图标 |
| `icon_components` | 会被解析，但当前内置渲染尚未使用 |
| `required_mod` | 可选，单个模组 id；该模组未加载时页面不可见 |
| `required_mods` | 可选，模组 id 列表；列出的全部模组都加载时页面才可见 |

### `navigation.position`

`navigation.position` 是用于控制同级页面顺序的可选整数。

- 未填写时默认按 `0` 处理。
- 数值越大越靠前。
- 数值相同时，按标题字母序排序。

## 首页推荐

### `navigation.recommend`

`navigation.recommend` 是用于首页推荐面板的可选整数。

- 只有写了这个字段的页面才会出现在推荐面板中。
- `0` 是有效值。
- 数值越大越靠前。
- 数值相同时，按标题字母序排序。
- 推荐面板按 `GuidePage` 工作，因此每一项都会直接跳转到对应页面。

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## 模组需求

使用 `required_mod` 或 `required_mods` 可以让页面依赖一个或多个模组的加载状态。
当需求未满足时，页面会从导航树和所有页面索引（物品、分类等）中排除，
因此无法通过导航或搜索找到该页面。

```yaml
navigation:
  title: Applied Energistics 集成
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: 多模组功能
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

两个键可以同时使用；只有列出的所有模组都已加载，页面才会显示。

## 加载优先级

当多个已加载资源包提供同一条 guide 页面路径时，GuideNH 会先读取页面 frontmatter，
然后选择 `navigation.priority` 最高的候选页面。

```yaml
navigation:
  title: 整合包覆盖页面
  parent: index.md
  priority: 100
```

规则：

- 未写 `priority` 时按 `0` 处理
- 取值为 Java int，最大 `2147483647`
- 数值更高者胜出
- 优先级相同时，后处理的资源包条目覆盖先处理的，保持 Minecraft 资源包覆盖顺序
- priority 只在同一页面路径、同一语言/回退层级的候选之间生效

这适合模组自带基础指南页面、整合包又希望稳定覆盖它的场景，不必只依赖资源包排序。

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

`navigation.parent` 使用与 Markdown 页面链接相同的命名空间规则：

- `parent: index.md` 和 `parent: ./index.md` 会在当前页面命名空间内解析。
- `parent: /index.md` 会从当前页面命名空间根路径解析。
- `parent: gregtech:index.md` 或 `parent: gregtech:/index.md` 会显式指向另一个命名空间。

数据驱动指南按命名空间隔离。`assets/guidenh/guidenh/_zh_cn/...` 下的页面属于
`guidenh:guidenh`；`assets/gregtech/guidenh/_zh_cn/...` 下的页面属于 `gregtech:guidenh`。
相对 parent 和相对链接都不会回退到其他模组的同名页面。

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
  - minecraft:iron_ore#crafting
```

这些映射会被 `<ItemLink>` 使用。

可在条目末尾加 `#anchor` 后缀，点击链接时会自动滚动到指定标题。
锚点由标题文本转换而来：全部小写、空格替换为连字符
（例如 `## Crafting Recipe` → `#crafting-recipe`）。

查找顺序如下：

1. 精确物品 + 精确 meta
2. 如果存在，则回退到通配 meta

## 标题锚点链接

GuideNH 在 Markdown 链接和 `<a>` 标签中支持标题锚点跳转。
锚点由标题文本全部小写并将空格替换为连字符后生成。

**同页面锚点：**

```md
[跳转到安装章节](#installation)
[跳转到合成配方](#crafting-recipe)
```

**跨页面锚点：**

```md
[查看入门教程](./getting-started.md#installation)
[其他页面](other-guide.md#usage)
```

**绝对路径锚点**（使用指南命名空间，可避免子目录中相对路径歧义）：

```md
[绝对路径](guidenh:other-guide.md#usage)
[任意命名空间](mymods:crafting/iron.md#smelting)
```

`namespace:path` 格式直接匹配 ID 为 `namespace:path` 的页面。
效果与相对路径相同，但无需使用 `../` 导航。
目标页面必须与链接来源处于同一份指南中。

**命名内联锚点**也可通过 MDX 的 `<a name="...">` 放置：

```md
<a name="custom-anchor" />

...内容...

[跳转到这里](#custom-anchor)
```

跳转带锚点的链接时，指南会自动滚动到目标标题或命名锚点处。

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
