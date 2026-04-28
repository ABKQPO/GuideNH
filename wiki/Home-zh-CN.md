[English](Home-en-US)

# GuideNH

GuideNH 是一个面向 GTNH 时代 Minecraft 模组的游戏内指南框架。本 wiki 说明项目的组织方式、运行时指南格式的工作原理，以及如何在不把仅限游戏内的语法直接混入 GitHub Wiki 的前提下编写页面、资源、配方、场景和注解。

## 从这里开始

- [安装](Installation-zh-CN)
- [快速开始](Getting-Started-zh-CN)
- [实时预览](Live-Preview-zh-CN)
- [指南页面格式](Guide-Page-Format-zh-CN)
- [导航](Navigation-zh-CN)
- [搜索](Search-zh-CN)
- [图片与资源](Images-And-Assets-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
- [GameScene](GameScene-zh-CN)
- [注解](Annotations-zh-CN)
- [配方](Recipes-zh-CN)
- [本地化](Localization-zh-CN)
- [示例](Examples-zh-CN)
- [常见问题](FAQ-zh-CN)

## 仓库结构

| 路径 | 用途 |
| --- | --- |
| `wiki/` | 面向人的 GitHub Wiki 文档页面 |
| `wiki/resourcepack/` | 模组在构建时使用的运行时指南源目录 |
| `wiki/resourcepack/assets/guidenh/guidenh/` | 内置示例指南页面与资源 |
| `build/resources/main/assets/` | 开发运行时由 Gradle 复制后的运行时指南资源输出目录 |

## 两层 Markdown

GuideNH 有意将文档编写分成两层：

- `wiki/*.md` 中的 GitHub Wiki Markdown 用于仓库文档，应保持为普通 GitHub Wiki Markdown。
- `wiki/resourcepack/...` 中的运行时 Markdown 用于游戏内渲染器，可以使用 YAML frontmatter 以及 GuideNH 专用的 MDX 标签，例如 `<GameScene>`、`<RecipeFor>` 和 `<Tooltip>`。

本 wiki 会解释运行时语法，但不会在代码围栏之外直接使用这些运行时标签。

## 快速编写清单

1. 将运行时指南文件放在 `wiki/resourcepack/assets/<modid>/guidenh/` 下。
2. 添加 `_en_us/`、`_zh_cn/` 之类的语言目录。
3. 将 Markdown 页面放入这些语言目录中。
4. 如果希望页面出现在侧边导航中，请在 frontmatter 中声明导航元数据。
5. 页面私有资源使用相对路径，指南根资源使用 `/...` 形式的根路径。
6. 编写 3D 场景时，可根据需要组合 `<GameScene>`、`<ImportStructure>`、`<ImportStructureLib>`、`<RemoveBlocks>` 和 `<BlockAnnotationTemplate>`。

## 快速迭代

对于仓库内置的示例指南，推荐使用 [实时预览](Live-Preview-zh-CN) 中说明的工作流。它会以开发源目录启动客户端，并在启动后直接打开指南。

## 场景编写亮点

- `<ImportStructure>` 用于把外部 SNBT/NBT 结构导入场景。
- `<ImportStructureLib>` 用于按控制器 id 导入 StructureLib 多方块，并支持 GTNH 风格的 `modid:block[:meta]`。
- `<RemoveBlocks>` 用于在导入后移除已放置的辅助方块，同时不影响周围状态。
- `<BlockAnnotationTemplate>` 会把同一组子注解复制到场景中所有已存在的匹配方块上。
- 交互式场景可以在底层场景数据支持的情况下自动提供层级滑块、StructureLib 频道滑块、舱口高亮按钮以及丰富的悬浮提示。

## 运行时示例资源

当前内置示例指南位于：

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/rendering.md`
- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/structure.md`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

在阅读本 wiki 时，这些文件是查看真实运行示例的最佳入口。现在两份 `index.md` 都包含了 `ImportStructureLib`、`RemoveBlocks` 和 `BlockAnnotationTemplate` 的混合场景示例。
