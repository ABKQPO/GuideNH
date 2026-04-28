[English](Examples)

# 示例

GuideNH 已经在 `wiki/resourcepack/` 中内置了一份运行时示例指南。本页将重要示例文件与它们所展示的功能对应起来。

## 核心示例页面

| 运行时文件 | 展示内容 |
| --- | --- |
| `.../_en_us/index.md` | frontmatter、item ids、配方、物品/方块图片、命令链接、tooltip、场景、注解、`ImportStructureLib`、`RemoveBlocks`、`BlockAnnotationTemplate` |
| `.../_en_us/markdown.md` | 普通 Markdown 功能和表格 |
| `.../_en_us/rendering.md` | 块级渲染和布局行为 |
| `.../_en_us/structure.md` | `<Structure>` 用法与坐标格式 |
| `.../_en_us/japanese.md` | 导航子页面示例 |
| `.../_en_us/subpage.md` | 导航 / 链接示例 |

## 资源示例

| 运行时文件 | 用途 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | 页面私有图片示例 |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | 供 `<ImportStructure>` 和 `<RemoveBlocks>` 使用的共享根结构资源 |

## 示例片段

### Frontmatter + Navigation

```yaml
item_ids:
  - guidenh:guide
navigation:
  title: Root
  icon_texture: test1.png
```

### 相对图片

````md
![Test Image](test1.png)
````

### 根路径结构资源

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib 场景导入

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
</GameScene>
````

### 导入结构后的清理

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### 场景注解 Tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### 配方过滤

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## 什么时候该用哪个示例

- 若你在验证解析器基础能力，先看 `markdown.md`
- 若你要一起测试混合运行时特性，包括 StructureLib tooltip、舱口高亮和清理逻辑，用 `index.md`
- 若你只需要静态方块布局预览，用 `structure.md`
- 若你需要可复用的导入结构资源，用 `example_structure.snbt`

## 推荐学习顺序

1. [快速开始](Getting-Started-zh-CN)
2. [指南页面格式](Guide-Page-Format-zh-CN)
3. [标签参考](Tags-Reference-zh-CN)
4. [GameScene](GameScene-zh-CN)
5. [注解](Annotations-zh-CN)
6. [配方](Recipes-zh-CN)
