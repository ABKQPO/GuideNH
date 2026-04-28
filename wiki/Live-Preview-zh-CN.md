[English](Live-Preview)

# 实时预览

GuideNH 支持一种面向开发模式的实时预览工作流。启用后，GuideNH 会直接从本地目录读取页面和资源，并在游戏运行中热更新改动，而无需整次重启。

## 它适合做什么

当你正在编辑 `wiki/resourcepack/` 下的运行时指南内容，并希望获得更紧凑的编写反馈回路时，就应使用实时预览。

它特别适合：

- Markdown 内容修改
- 导航结构调整
- 页面私有图片
- 诸如导入结构之类的指南根资源

## 支持的系统属性

| 属性 | 含义 |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | 作为开发源根目录使用的本地文件夹 |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | 从该目录加载文件时使用的可选命名空间覆盖 |
| `guideme.showOnStartup` | 可选，启动后在标题界面自动打开的指南或页面 |
| `guideme.validateAtStartup` | 可选，启动后执行一次校验的 guide id 列表，使用逗号分隔 |

## 本仓库中的 GuideNH 示例

对于本仓库内置示例指南：

- guide id: `guidenh:guidenh`
- 开发源根目录: `wiki/resourcepack/assets/guidenh/guidenh`

对应的系统属性是：

```text
guideme.guidenh.guidenh.sources=<absolute-path-to-repo>/wiki/resourcepack/assets/guidenh/guidenh
```

## `showOnStartup` 支持的格式

`guideme.showOnStartup` 支持以下形式：

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

`!` 之后的相对页面 id 会自动按指南命名空间解析，因此 `index.md` 会变成 `guidenh:index.md`。

## 本仓库内置的 Gradle 运行任务

本仓库现在提供了专门的实时预览任务：

- `runGuide`
- `runGuide17`
- `runGuide21`
- `runGuide25`

它们会继承普通客户端运行配置，并额外注入：

- `guideme.guidenh.guidenh.sources`
- `guideme.showOnStartup=guidenh:guidenh!index.md`

典型用法：

```text
.\gradlew.bat runGuide
```

## 性能说明

GuideNH 不会每帧都轮询指南文件。

- 只有当至少一份指南真正启用了 development sources 时，才会注册开发态 watcher
- watcher 处理被节流为每 `20` 个客户端 tick 执行一次
- 启动时的指南打开和校验只会在标题界面出现后执行一次

这样既能保持实时预览足够灵敏，也不会引入不必要的高频客户端开销。

## 推荐工作流

1. 通过 `runGuide` 启动客户端。
2. 编辑 `wiki/resourcepack/assets/...` 下的运行时指南文件。
3. 等待节流后的 watcher 检测到变更。
4. 如果你修改了导航结构或启动目标，可重新打开或重新访问对应页面。

## 哪些情况下仍然需要重启

实时预览很适合内容迭代，但以下情况通常仍需要重启或完整资源重载：

- 你改的是代码而不是指南内容
- 你改了构建逻辑
- 你新增了开发源树之外的资源

## 相关页面

- [安装](Installation-zh-CN)
- [快速开始](Getting-Started-zh-CN)
- [GameScene](GameScene-zh-CN)
- [常见问题](FAQ-zh-CN)
