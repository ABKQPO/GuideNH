---
navigation:
  title: 导航与索引
  parent: index.md
  position: 195
  recommend: 5
categories:
  - markdown
---

# 导航与内容索引

GuideNH 使用 YAML frontmatter 声明导航结构——无需 `index.md` 硬编码，无需 manifest 文件。每个页面通过自身的 frontmatter 决定在侧边栏中的位置。

当游戏内侧边栏展开多层页面时，只要某个祖先节点的可见子孙仍在列表中，它就会固定在顶部。多层祖先可以同时逐层堆叠，便于在深层树结构中滚动定位。

## 快速示例

```yaml
---
navigation:
  title: 高级 IO 总线
  parent: aae_intro/aae_intro-index.md
  icon: advanced_ae:advanced_io_bus_part
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
---
```

## 导航字段

### `navigation.title`

侧边栏中的显示标题。页面必须设置此字段才能出现在导航中。

### `navigation.parent`

父页面的页面 ID。相对路径（如 `getting-started.md`）会基于页面自身的命名空间解析。不设置则表示该页面为顶级根节点。

```yaml
# 根节点（无 parent）
navigation:
  title: 快速开始

# 另一个页面的子节点
navigation:
  title: 高级 IO 总线
  parent: aae_intro/aae_intro-index.md

# 显式命名空间，根路径
navigation:
  title: GregTech 集成
  parent: gregtech:/index.md
```

Markdown 页面链接也使用同一套规则。`guide.md`、`./guide.md` 和 `/guide.md` 都留在当前页面命名空间内；
`gregtech:guide.md` 和 `gregtech:/guide.md` 会显式打开 `gregtech` 命名空间，也就是在当前指南文件夹为
`guidenh` 时加载 `gregtech:guidenh` 数据驱动指南里的页面。

### `navigation.position`

同级页面中的整数排序值。数字越大越靠前。相同 position 时按 `title` 字母序排列。

```yaml
navigation:
  title: Markdown 基础
  parent: index.md
  position: 10
```

### `navigation.recommend`

用于首页推荐面板的可选整数。只有写了这个字段的页面才会出现在推荐列表中。
`0` 是有效值，数值越大越靠前，数值相同时按标题字母序排序。

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

### `navigation.priority`

用于控制同一路径页面在多个模组/资源包中同时存在时的加载优先级。未写时为 `0`；数值更高者胜出。
如果两个候选页面优先级相同，则后处理的资源包条目覆盖先处理的，保持 Minecraft 常规资源包顺序。

```yaml
navigation:
  title: 整合包覆盖页面
  parent: index.md
  priority: 100
```

### `navigation.icon`

显示在侧边栏页面标题旁的一个物品图标。

```yaml
navigation:
  title: 配方
  icon: minecraft:crafting_table
```

### `navigation.icon_texture`

一个 PNG 纹理路径（相对于指南 assets 目录），用作导航图标。

```yaml
navigation:
  title: 首页
  icon_texture: my_icon.png
```

### `navigation.icons`

一组在侧边栏中循环显示的物品 ID 列表。图标会周期性切换。

```yaml
navigation:
  title: 颜色演示
  icons:
    - minecraft:wool:1
    - minecraft:wool:4
    - minecraft:wool:14
```

### `navigation.icon_textures`

一组循环显示的纹理路径列表。

```yaml
navigation:
  title: 幻灯片
  icon_textures:
    - frame_1.png
    - frame_2.png
```

## 内容索引字段

### `categories`

MediaWiki 风格分类列表。每一项既可以写成 `分类名`，也可以写成 `分类名|排序名`。

```yaml
categories:
  - markdown
  - charts|Charts Overview
```

在父页面中使用 `<Category name="markdown" rows="3" />` 即可自动生成该分类下所有页面的列表。
每个分类还会自动生成一个隐藏但可搜索的页面，例如 `Category:markdown`。
GuideNH 还提供隐藏的特殊页面，例如 `Special:SpecialPages`、`Special:AllPages` 与 `Special:Categories`。
可分别通过 `<Special name="SpecialPages" rows="3" />`、`<Special name="AllPages" rows="3" />` 和 `<Special name="Categories" rows="3" />` 直接嵌入。
所有 `Special:*` 页面都不会进入普通指南搜索，而 `Category:*` 页面仍然可被搜索到。

### `item_ids`

将物品关联到此页面，用于 G 键查找和右键导航。格式：`namespace:name` 或 `namespace:name:meta`。可追加 `#anchor` 跳转到页面内的特定标题。

```yaml
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - minecraft:diamond#Usage
```

### `required_mods`

仅当所有列出的模组都已加载时，此页面才会出现在导航和索引中。

```yaml
navigation:
  title: AE2 集成
required_mods:
  - appliedenergistics2
```

## 可选元数据字段

| 字段 | 描述 |
|------|------|
| `title` | 页面标题覆盖（显示在指南界面左上角） |
| `author` | 作者署名，显示在页面页脚 |
| `date` | 创建日期，显示在页面页脚 |
| `updated` | 最后修改日期，显示在页面页脚 |

## 完整示例

```yaml
---
title: 高级 IO 总线
navigation:
  title: 高级 IO 总线
  parent: getting-started.md
  position: 10
  icon: advanced_ae:advanced_io_bus_part
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
  - advanced_ae:advanced_io_bus_part:1
required_mods:
  - advanced_ae
author: pedroksl
date: 2025-01-01
---

# 高级 IO 总线

高级 IO 总线提供更快的物品传输...
```
