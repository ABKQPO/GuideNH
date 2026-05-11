# 结构导出

## 中文

`/exportStructure` 用于把 StructureLib 预览或 GuideNH 已加载的 GameScene 导出为 PNG 截图。指令只在客户端注册。`structureLib` 子指令只有在检测到 StructureLib 已加载时才能使用。

导出使用正交渲染。输出分辨率会随着结构或场景大小变化，但配置的方块比例保持稳定。默认比例为每个方块 `128` 像素。

### 指令格式

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

`structureLib` 的 `controller` 使用 `modid:name` 或 `modid:name:meta`。省略时会尝试发现所有 StructureLib 控制器。

`gameScene` 不使用位置参数，会导出当前已加载 guide 中编译出的全部 GameScene。

### 通用参数

| 参数 | 说明 |
| --- | --- |
| `--out <dir>` | 输出目录。默认是 `screenshots/structurelib/<timestamp>/` 或 `screenshots/gameScene/<timestamp>/`。 |
| `--pixelsPerBlock <int>` | 每个世界方块对应的像素密度。默认是 `128`。 |
| `--scale <float>` | 乘到 `pixelsPerBlock` 上的缩放倍率。 |
| `--layers <expr\|each\|all>` | 控制渲染哪些 Y 层。默认是 `all`。 |
| `--view <preset>` | 相机预设。StructureLib 默认是 `isometric-south-east`。GameScene 默认尊重场景自己的相机。 |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | 精细相机覆盖。GameScene 模式中，只要显式传入视角或旋转参数，就会从场景相机切换为自动适配的导出相机。 |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | 相机覆盖的兼容别名。 |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG 背景。默认是 `transparent`。 |
| `--maxPixels <long>` | 单张图片允许的最大像素数。默认是 `655360000`。使用 `-1` 表示无限制。 |
| `--batchSize <int>` | 每完成多少个结果就刷新一次 `manifest.json`。默认是 `16`。 |
| `--force` | 允许生成超过 256 张截图。 |
| `--dry-run` | 只生成计划和 manifest，不写 PNG。 |
| `--config <file>` / `@file.json` | 读取 JSON 配置。 |

### StructureLib 参数

| 参数 | 说明 |
| --- | --- |
| `--tier <expr>` | 主 tier 值。 |
| `--channel <name=expr>` | 指定 StructureLib channel 的值。可以重复使用。 |
| `--facing <list>` | 批量导出朝向。 |
| `--rotation <list>` | 批量导出旋转。 |
| `--flip <list>` | 批量导出翻转。 |
| `--orientation <facing:rotation:flip,...>` | 显式指定朝向组合。 |
| `--gt-active-controller` | 仅 GregTech。尽可能把控制器渲染为机器运行中的纹理。 |
| `--gt-place-hatches` | 仅 GregTech。启用正常 GT Hatch channel 逻辑来放置只允许 Hatch 的预览位置；省略时默认仍优先 fallback casing。 |

### GameScene 参数

| 参数 | 说明 |
| --- | --- |
| `--show-annotations` | 渲染场景注解，包括 in-world 注解和 overlay 注解。默认 `false`。 |
| `--show-grid` | 渲染场景地面网格。默认 `false`。 |

GameScene 模式默认尊重每个场景自己配置的相机。只有使用 `--view`、`--yaw`、`--pitch`、`--roll`、`--rotateX`、`--rotateY` 或 `--rotateZ` 时，才会切换为自动适配导出视角。

### 数字过滤

`--tier`、`--channel` 和 `--layers` 都使用同一种数字过滤语法。

```text
0
0-12
0-12,!5
!0,1
```

`0` 匹配单个值，`0-12` 匹配闭区间，`!` 用于排除，逗号用于组合多个 token。

### 层

`--layers all` 会把完整结构或场景导出为一张图。

`--layers 0-12,!5` 会导出一张图，只显示匹配的层。

`--layers each` 会按照实际 Y 层逐层导出。

按层过滤渲染时会强制渲染暴露方块面，避免相邻层隐藏后出现缺面。

### StructureLib Tier 和 Channel

当省略 `--tier` 和 `--channel` 时，导出器会分析控制器，并按可用的统一 tier 每个导出一张图。统一 tier 会同时应用到主 tier 和每个发现的 StructureLib channel。自动统一 tier 导出每个控制器/朝向最多 100 张。

当提供 `--tier` 但省略 `--channel` 时，请求的 tier 会同时驱动每个发现的 channel，并按 channel 自身范围裁剪。

当显式提供一个或多个 `--channel` 时，会使用这些 channel 值。多个 tier 和 channel 会按笛卡尔积组合。

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech 选项

默认情况下，GregTech 集成会把可选 Hatch 位置保留为正常 fallback casing。强制 Hatch 元素，例如 Muffler 位置，仍会渲染为指定 Hatch。

当你希望按 GT 正常 StructureLib Hatch channel 逻辑放置截图用 Hatch 时，使用 `--gt-place-hatches`。例如声明为 `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` 的元素，可以放置所需 Hatch 预览并更新纹理，而不是只显示 fallback casing。

当控制器需要显示机器运行中的纹理时，使用 `--gt-active-controller`。导出器仍会同步预览状态，但不会把机器检查失败当作截图失败。

### 朝向

批量语法：

```text
--facing north,south --rotation normal,clockwise --flip none
```

显式语法：

```text
--orientation north:normal:none,south:clockwise:none
```

两种形式可以一起使用。当 StructureLib alignment limits 拒绝某个组合时，该组合会被跳过。没有指定朝向时使用控制器默认值。

### 视角

支持的预设包括：

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

可以进一步微调：

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON 配置

StructureLib 示例：

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "tier": "1-4",
  "channels": {
    "coil": "1-4",
    "casing": "1,2"
  },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

GameScene 示例：

```json
{
  "out": "screenshots/gameScene/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "layers": "all",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "showAnnotations": false,
  "showGrid": false,
  "force": false,
  "dryRun": false
}
```

运行配置：

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

相对配置路径会从当前工作目录和 `config/guidenh/structure_exports/` 查找。

### 输出和 Manifest

每个导出目录包含 PNG 文件和 `manifest.json`。

StructureLib 图片名以控制器 `ItemStack` 的显示名开头，后缀包含 tier、channel、layers、orientation 和 view。

GameScene 图片名包含 guide ID、page ID、scene 序号、layers、相机模式，以及可选的 annotation/grid 后缀。

文件名会按 Windows 规则安全化。`manifest.json` 会记录输出路径、图片尺寸、选择的变体、warnings 和 errors。

### 性能

默认情况下，导出计划超过 256 张截图会被拒绝，除非使用 `--force`。

默认单张图片不能超过 `655360000` 像素。这个默认值按 200x100x200 级别机器和投影方块 `128` 像素预算估算。超限时可以降低 `--pixelsPerBlock` 或 `--scale`，使用 `--layers` 缩小范围，显式提高 `--maxPixels`，或者使用 `--maxPixels -1` 关闭像素上限。

当图片超过 GPU 纹理大小时，会使用分块 framebuffer 渲染。
