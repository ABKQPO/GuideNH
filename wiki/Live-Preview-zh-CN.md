[English](Live-Preview)

# 实时预览

GuideNH 支持面向开发的实时预览流程。启用后，GuideNH 可以直接从本地目录读取指南页面和资源，并在游戏运行时重载改动，不需要完整重启。

## 适用场景

当你正在编辑 `wiki/resourcepack/` 或整合包资源包里的运行时指南内容，并希望在游戏内快速看到结果时，可以启用实时预览。

它特别适合：

- Markdown 内容修改
- 导航结构调整
- 页面私有图片
- 指南根目录资源，例如导入结构
- 被 `<ImportPonder>` 引用的 Ponder JSON 文件

## 支持的系统属性

| 属性 | 含义 |
| --- | --- |
| `guideme.<guide_namespace>.<guide_path>.sources` | 单个 guide 使用的开发源根目录 |
| `guideme.<guide_namespace>.<guide_path>.sourcesNamespace` | 从该目录加载文件时使用的可选命名空间覆盖 |
| `guideme.resourcePacks.sources` | 按标准资源包根目录加载并监控的开发资源包目录，可以重复指定 |
| `guideme.resourcePack.sources` | `guideme.resourcePacks.sources` 的别名，可以重复指定 |
| `guideme.showOnStartup` | 可选，启动后在标题界面自动打开的指南或页面 |
| `guideme.validateAtStartup` | 可选，启动后执行一次校验的 guide id 列表，使用逗号分隔 |

## 本仓库的 GuideNH 示例

对于本仓库内置示例指南：

- guide id: `guidenh:guidenh`
- 开发源根目录：`wiki/resourcepack/assets/guidenh/guidenh`

对应的系统属性是：

```text
guideme.guidenh.guidenh.sources=<absolute-path-to-repo>/wiki/resourcepack/assets/guidenh/guidenh
```

GuideNH 也支持更宽的开发源根目录：

| 源根目录 | 页面 id 映射 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh` | content-root 模式；文件通过 `sourcesNamespace` 映射，默认是 `guidenh` |
| `wiki/resourcepack/assets` | assets-root 模式；`assets/<modid>/guidenh/...` 映射为 `<modid>:...` |
| `wiki/resourcepack` | resourcepack-root 模式；`assets/<modid>/guidenh/...` 映射为 `<modid>:...` |

数据驱动指南仍然会合并为同一个 guide。多个命名空间下存在同名 Markdown 时，会保留命名空间作为页面 id 的一部分，因此
`assets/gregtech/guidenh/_zh_cn/index.md` 和 `assets/appliedenergistics2/guidenh/_zh_cn/index.md`
会分别成为 `gregtech:index.md` 和 `appliedenergistics2:index.md`，不会互相覆盖。

## 多资源包实时预览

当你希望一个或多个目录像标准资源包一样被加载，并监控其下所有 `assets/<modid>/guidenh/...` 指南内容时，使用
`guideme.resourcePacks.sources`。

```text
-Dguideme.resourcePacks.sources=E:/packs/base
-Dguideme.resourcePacks.sources=E:/packs/override
```

这个属性可以重复指定。GuideNH 会按 JVM 参数出现的顺序读取它们，因为 Java 的系统属性本身会把重复的 `-D` key 折叠成最后一个值。

冲突规则如下：

- 不同 modid 通过命名空间隔离，例如 `gregtech:index.md` 和 `appliedenergistics2:index.md` 是两张不同页面
- 同一个 modid、同一种语言、同一个 Markdown 路径重复出现时，先出现的 `guideme.resourcePacks.sources` 目录获胜
- 如果同一页面也存在于普通已加载资源包里，开发资源包目录优先

修改这些开发资源包根目录下的 Markdown、Ponder JSON、图片或其他文件时，会触发客户端资源重载，合并指南和场景内容会一起刷新。

## `showOnStartup` 支持的格式

`guideme.showOnStartup` 支持以下形式：

- `guidenh:guidenh`
- `guidenh:guidenh!index.md`
- `guidenh:guidenh!index.md#anchor`

`!` 后面的相对页面 id 会自动按指南命名空间解析，因此 `index.md` 会变成 `guidenh:index.md`。

## 性能说明

GuideNH 不会每帧轮询指南文件。

- 只有当至少一个 guide 启用了 development sources 时，才会注册单 guide 的开发源 watcher
- 只有当至少存在一个 `guideme.resourcePacks.sources` 根目录时，才会注册多资源包实时预览 watcher
- watcher 处理会被节流为每 `20` 个客户端 tick 执行一次
- 非 Markdown 指南资源也会触发页面重新解析，因此 Ponder JSON 等外部文件也可以实时更新
- 启动时的指南打开和校验只会在标题界面出现后执行一次

这样既能保持实时预览足够灵敏，也不会引入不必要的高频客户端开销。

## 推荐工作流

1. 通过 `runGuide` 或带有对应 `-D` 参数的客户端启动游戏。
2. 编辑 `wiki/resourcepack/assets/...` 或配置的开发资源包根目录下的运行时指南文件。
3. 等待 watcher 检测到变更并触发资源重载。
4. 如果修改了导航结构或启动目标，可以重新打开或重新访问对应页面。

## 哪些情况仍然需要重启

实时预览适合内容迭代，但以下情况通常仍需要重启或完整资源重载：

- 修改的是代码而不是指南内容
- 修改了构建逻辑
- 新增了配置的开发源目录之外的资源

## 相关页面

- [安装](Installation-zh-CN)
- [快速开始](Getting-Started-zh-CN)
- [游戏场景](GameScene-zh-CN)
- [常见问题](FAQ-zh-CN)
