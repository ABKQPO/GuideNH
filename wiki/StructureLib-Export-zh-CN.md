# StructureLib 导出

`/exportStructureLib` 用于把 StructureLib 加载出的多方块预览导出为 PNG 截图。该指令只会在客户端注册，并且只有检测到 StructureLib 已加载时才可用。

导出器使用正交相机。图片分辨率会随结构尺寸变化，但每个方块的像素比例保持稳定。默认比例是每个方块 `256` 像素。

## 指令形式

```text
/exportStructureLib [controller] [options...]
/exportStructureLib @file.json
/exportStructureLib --config file.json
```

`controller` 使用 `modid:name` 或 `modid:name:meta`。`meta` 可省略，省略时使用 `0`。不填写 controller 时，导出器会尝试发现所有可解析为 StructureLib constructable 的控制器。

## 参数

| 参数 | 说明                                                |
| --- |---------------------------------------------------|
| `--out <dir>` | 输出目录。默认是 `screenshots/structurelib/<timestamp>/`。 |
| `--pixelsPerBlock <int>` | 每个世界方块对应的像素密度。默认是 `256`。                          |
| `--scale <float>` | 乘到 `pixelsPerBlock` 上的缩放倍率。                       |
| `--tier <expr>` | master tier 数值过滤表达式。                              |
| `--channel <name=expr>` | 指定 StructureLib channel 的数值，可重复使用多个 channel。      |
| `--layers <expr\|each\|all>` | 控制渲染哪些 Y 层。默认是 `all`。                             |
| `--facing <list>` | 批量导出时使用的 facing 列表。                               |
| `--rotation <list>` | 批量导出时使用的 rotation 列表。                             |
| `--flip <list>` | 批量导出时使用的 flip 列表。                                 |
| `--orientation <facing:rotation:flip,...>` | 精确指定一组或多组朝向组合。                                    |
| `--view <preset>` | 相机预设。默认是 `isometric-south-east`。                  |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | 细化相机角度。                                           |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | 与 GameScene 命名兼容的相机角度别名。                          |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG 背景。默认是 `transparent`。                         |
| `--maxPixels <long>` | 单张图片允许的最大像素数。默认是 `655360000`，按 200x100x200 级别机器和投影方块 `128` 像素估算。使用 `-1` 表示无限制。                      |
| `--batchSize <int>` | 每完成多少个结果刷新一次 `manifest.json`。默认是 `16`。            |
| `--gt-active-controller` | 仅 GregTech。尽可能把控制器渲染为机器运行中的纹理。                  |
| `--gt-place-hatches` | 仅 GregTech。启用 GT 正常 Hatch channel 放置逻辑；未指定时，可选 Hatch 位置默认仍优先使用 fallback 方块。 |
| `--force` | 允许生成超过 256 张截图。                                   |
| `--dry-run` | 只生成计划和 `manifest.json`，不写 PNG。                    |
| `--config <file>` / `@file.json` | 加载 JSON 配置文件。                                     |

## 数字过滤

`--tier`、`--channel` 和 `--layers` 都使用同一种数字过滤语法。

```text
0
0-12
0-12,!5
!0,1
```

`0` 匹配单个数值。`0-12` 匹配闭区间。`!` 表示排除。`,` 表示组合多个 token。

## 层导出

`--layers all` 会把完整结构导出为一张图。

`--layers 0-12,!5` 会导出一张图，只显示匹配的层。

`--layers each` 会按照生成结构中的实际 Y 层，每层导出一张图。

当启用层过滤时，渲染器会强制渲染暴露出来的方块面，避免隐藏相邻层后出现缺面。

## Tier 和 Channel

如果没有指定 `--tier` 和 `--channel`，导出器会检查控制器，并按可用的统一 tier 逐个导出截图。同一个 tier 值会同时应用到 master tier 和所有发现的 StructureLib channel。自动统一 tier 导出对每个控制器/朝向最多生成 100 张截图。

如果指定了 `--tier`，但没有指定 `--channel`，每个指定 tier 也会用同一个值控制所有发现的 channel，并按各 channel 支持的范围自动夹取。

如果显式指定了一个或多个 `--channel`，导出器会使用这些明确的 channel 值。多个显式 tier 与多个显式 channel 会按笛卡尔积组合。

```text
/exportStructureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

该显式示例会导出 tier、`coil` 和 `casing` 的所有组合。

## GregTech 专属选项

默认情况下，GregTech 集成会把可选 Hatch 位置保留为声明里的 fallback 方块。强制 Hatch 元素，例如 Muffler 位置，仍会渲染为对应 Hatch。

当需要预览 GT 正常 StructureLib Hatch channel 逻辑时，使用 `--gt-place-hatches`。例如 `atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` 这样的元素可以放置所需 Hatch 并更新纹理，而不是只显示 fallback casing。

当控制器需要显示机器运行中的纹理时，使用 `--gt-active-controller`。导出器仍会同步预览状态，但不会把机器检查失败当作截图失败。

## 朝向

批量语法：

```text
--facing north,south --rotation normal,clockwise --flip none
```

精确语法：

```text
--orientation north:normal:none,south:clockwise:none
```

两种语法可以同时使用。导出器会尊重 StructureLib 的 `IAlignmentLimits`；如果某个控制器不允许指定组合，该组合会被跳过。未指定朝向时，会保留控制器自身默认朝向，即使默认朝向不是 north/normal/none。

## 视角

支持的预设：

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

可以继续细化预设：

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

`rotateX`、`rotateY`、`rotateZ` 分别对应 `pitch`、`yaw`、`roll`。

## JSON 配置

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 256,
  "scale": 1.0,
  "tier": "1-4",
  "channels": {
    "coil": "1-4",
    "casing": "1,2"
  },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "rotateX": 30,
  "rotateY": 315,
  "rotateZ": 0,
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

运行：

```text
/exportStructureLib @my_export.json
```

相对配置路径会从当前工作目录和 `config/guidenh/structurelib_exports/` 中查找。

## 输出和 Manifest

每个导出目录包含 PNG 文件和 `manifest.json`。

图片名以控制器 `ItemStack` 的显示名开头。变体后缀包含 tier、channel、layers、orientation 和 view。文件名会按 Windows 规则安全化。

`manifest.json` 会记录 controller ID、meta、显示名、输出路径、图片尺寸、tier、channel、layers、`explicitLayer`、orientation、view、warnings 和 errors。

## 性能和限制

默认情况下，如果计划生成超过 256 张截图，指令会拒绝执行；使用 `--force` 可以放开限制。

默认情况下，单张图片不能超过 `655360000` 像素。这个默认值按 200x100x200 级别机器和投影方块 `128` 像素预算估算，避免大结构长时间导出时耗尽客户端内存。超限时该截图会失败并写入 `manifest.json`。可以降低 `--pixelsPerBlock` 或 `--scale`，使用 `--layers` 缩小范围，显式提高 `--maxPixels`，或者使用 `--maxPixels -1` 关闭像素上限。

一次指令仍然可以导出所有发现到的结构。导出器会按控制器逐个规划和导出，不会先把所有任务一次性保存在内存里。每张图片写入 PNG 后会立即释放像素缓冲，并按 `--batchSize` 分段刷新 `manifest.json`。每个控制器完成后还会清理 StructureLib 分析缓存，便于 8GB 客户端长时间批量导出。

当图片尺寸超过 GPU 最大纹理尺寸时，导出器会使用分块 framebuffer 渲染，再合并为一张 PNG。

全量导出会扫描已注册方块并尝试解析 StructureLib constructable，因此比指定单个 controller 更慢。建议批量导出前先用指定 controller 和 `--dry-run` 验证参数组合。
