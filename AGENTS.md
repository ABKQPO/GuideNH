# AGENTS.md — GuideNH

GuideNH 是 [GuideME](https://github.com/AppliedEnergistics/guideme) 向 **Minecraft 1.7.10 / Forge 10.13.4.1614 (GTNH)** 的回移，提供 Markdown 驱动的游戏内手册系统。本文件给 AI 代理列出**仓库专属**约定，避免在 1.7.10 + GTNH RFG 工具链下走错路。

## 必读链接

- 总览与功能列表：[README.md](README.md)
- 用户向 Wiki（标签、场景、Recipe、本地化等）：[wiki/Home.md](wiki/Home.md) · [wiki/GameScene.md](wiki/GameScene.md) · [wiki/Tags-Reference.md](wiki/Tags-Reference.md)
- Markdown 运行时支持矩阵：[docs/markdown-runtime-support-matrix.md](docs/markdown-runtime-support-matrix.md)
- 本目录其他规则：[.github/instructions/](.github/instructions)

## 构建 / 校验（按此顺序）

```powershell
gradlew --no-daemon spotlessApply
gradlew --no-daemon spotlessCheck compileJava -x test --console=plain
```

- 任何 Java 改动**必须先 `spotlessApply`**，否则 CI 立即失败。
- 完整构建：`gradlew --no-daemon build -x test`（冷缓存约 20–35 s）。
- 测试：`gradlew --no-daemon test`。运行客户端：`gradlew runClient`。

## 文件编码（每次新建文件都要注意）

- **严禁 UTF-8 BOM**：`spotlessJavaCheck` 会报 `非法字符: '\ufeff'`。
- PowerShell `Set-Content -Encoding UTF8` 会写 BOM，禁用。改用编辑工具或 `[System.IO.File]::WriteAllBytes`。

## 第三方 Mod 兼容（compat/）布局

所有第三方集成位于 [src/main/java/com/hfstudio/guidenh/compat/](src/main/java/com/hfstudio/guidenh/compat) 下的 `{mod}/` 子包，11 个 Mod 常量集中在 `compat/Mods.java`。规则：

- **双重守卫**：所有外部入口先 `Mods.X.isModLoaded()` 短路，必要时再叠加 `@cpw.mods.fml.common.Optional.Method(modid="X")`。
- 新加 Mod 集成 → 在 `compat/{mod}/{Mod}Helpers.java` 集中类型导入；已有 `Guide*Support` 类保持为 thin facade，不要把它删了。
- 优先使用 Mixin Accessor（`mixins/early/{forge,fml,minecraft}/Accessor*.java`）替代反射；只有当目标是 Scala / 合成接口 / AutoValue 时才保留反射，并在文件头注释原因。
- 详细决策表见 [.github/instructions/compat-structure.instructions.md](.github/instructions/compat-structure.instructions.md)（如不存在表示尚未抽取，可直接读取既有 helper 推断模式）。

## Shadow / 依赖陷阱

- `org.joml.*` **不得**被 `shadowJar` relocate —— GTNHLib 0.9.50 已经把 JOML 1.10.8 透明打入运行时。直接 `import org.joml.Matrix4f;` 即可。
- 已 relocate：`snakeyaml / methvin / lucene / commons / jna`（前缀 `com.hfstudio.guidenh.shadow.`）。新增依赖前查阅 [build.gradle.kts](build.gradle.kts) 的 `shadowJar { relocate ... }` 段。

## 场景渲染（GameScene / 场景编辑器）

涉及 `<GameScene>`、`<ImportStructureLib>`、场景预览、`GuidebookLevelRenderer` 的改动**先读** [.github/instructions/gamescene-rendering.instructions.md](.github/instructions/gamescene-rendering.instructions.md)。这条管线对 ForgeMultipart / GregTech / CarpentersBlocks 各有专门 hook，绕过它们会立刻让特定方块"消失但仍有碰撞箱"。

## 1.7.10 兼容禁区

- `EntityPlayer` ≠ `Player`，`String` ≠ `Component`，没有 `BuiltInRegistries`、没有 `Codec.unboundedMap` 之外的现代 ExtraCodecs、没有 `SoundInstance`、没有 `MouseButtonInfo`。回移代码时遇到这些类型直接换 1.7.10 等价物（参考 [src/main/java/com/hfstudio/guidenh/guide/document/](src/main/java/com/hfstudio/guidenh/guide/document) 现状）。
- 注释禁用 `// =====` / `// -----` banner 风格（spotless 会通过，但代码风格统一要求）。

## License

LGPL-3.0（与 GuideME 上游一致）。新文件无需逐文件加版权头，spotless 不强制。
