[English](GameScene)

# GameScene

`<GameScene>` 是 GuideNH 的 3D 预览标签，`<Scene>` 是具有相同行为的别名。

## 场景属性

| 属性 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `width` | integer | `256` | 视口宽度（像素） |
| `height` | integer | `192` | 视口高度（像素） |
| `zoom` | float | `1.0` | 相机缩放倍率 |
| `perspective` | string | `isometric-north-east` | 相机预设 |
| `rotateX` | float | auto | 显式 X 旋转覆盖值 |
| `rotateY` | float | auto | 显式 Y 旋转覆盖值 |
| `rotateZ` | float | auto | 显式 Z 旋转覆盖值 |
| `offsetX` | float | auto | 屏幕空间水平平移 |
| `offsetY` | float | auto | 屏幕空间垂直平移 |
| `centerX` | float | auto | 显式世界旋转中心 X |
| `centerY` | float | auto | 显式世界旋转中心 Y |
| `centerZ` | float | auto | 显式世界旋转中心 Z |
| `interactive` | boolean expression | `true` | 是否启用鼠标交互 |

## 视角预设

可接受的 `perspective` 值：

- `isometric-north-east`
- `isometric-north-west`
- `up`

未知值会回退到 `isometric-north-east`。

## 示例

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## 场景子元素

GuideNH 当前注册了以下场景子标签：

- `<Block>`
- `<ImportStructure>`
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<RemoveBlocks>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- 各类注解标签，例如 `<BoxAnnotation>` 和 `<LineAnnotation>`

## `<Block>`

在预览世界中放置一个方块。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是，除非提供了 `ore` | 方块 id |
| `ore` | 否 | 矿辞名；第一个匹配结果必须能解析成方块物品 |
| `x` | 否 | 世界坐标 X，整数，默认 `0` |
| `y` | 否 | 世界坐标 Y，整数，默认 `0` |
| `z` | 否 | 世界坐标 Z，整数，默认 `0` |
| `meta` | 否 | 方块 metadata，整数 |
| `facing` | 否 | `down`、`up`、`north`、`south`、`west`、`east` |
| `nbt` | 否 | SNBT TileEntity compound |

说明：

- 同时提供 `ore` 和 `id` 时，优先使用 `ore`；若安装了 GregTech，选中的结果还会先经过 `GTOreDictUnificator.setStack(...)` 统一化
- 若省略 `meta`，且 `ore` 解析出的物品携带具体且非通配符的 damage，则会先使用该值，再回退到 `facing`
- 若省略 `meta`，部分方块会根据 `facing` 推导合理默认值
- 若 `nbt` 能成功创建 TileEntity，预览中会使用该实体

示例：

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt="{id:\"Chest\",Items:[{Slot:0b,id:\"minecraft:diamond\",Count:1b,Damage:0s}]}" />
````

## `<ImportStructure>`

把外部结构文件加载到场景中。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `src` | 是 | 结构资源路径 |
| `x` | 否 | 平移 X，整数 |
| `y` | 否 | 平移 Y，整数 |
| `z` | 否 | 平移 Z，整数 |

支持的格式：

- SNBT 文本
- gzip 压缩的二进制 NBT
- 未压缩的二进制 NBT

必须包含的结构键：

- `palette`
- `blocks`

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
````

## `<ImportStructureLib>`

通过控制器 id 导入 StructureLib 多方块预览。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `controller` | 是 | 控制器方块 id，格式为 `modid:block[:meta]` |
| `piece` | 否 | StructureLib piece 名称覆盖值 |
| `facing` | 否 | 传给导入器的朝向覆盖值 |
| `rotation` | 否 | 传给导入器的旋转覆盖值 |
| `flip` | 否 | 传给导入器的镜像覆盖值 |
| `channel` | 否 | 支持频道结构的频道整数覆盖值 |

说明：

- 导入结构会从场景 `0 0 0` 开始；控制器不会被强制放在 `0 0 0`
- 若有足够元数据，该标签会启用 StructureLib 专用 tooltip、舱口高亮和频道滑块 UI
- 控制器匹配支持 GTNH 风格 `modid:block:meta`

示例：

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
````

## `<IsometricCamera>`

显式指定等轴相机的 yaw/pitch/roll。

若省略该标签，场景会继续使用 `<GameScene>` 的 `perspective` 预设。默认的
`isometric-north-east` 预设等价于：

````md
<IsometricCamera yaw="225" pitch="30" />
````

| 属性 | 含义 |
| --- | --- |
| `yaw` | float |
| `pitch` | float |
| `roll` | float |

示例：

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

移除所有已放置且匹配目标方块 id 的方块。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 要移除的方块 id，使用 `modid:block[:meta]` 格式 |

这在导入结构后非常有用，适合为了展示清晰度而隐藏某些方块。

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<BlockAnnotationTemplate>`

将一个或多个子注解扩展到当前场景中所有已经存在的匹配方块上。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 方块匹配器，格式为 `modid:block[:meta]` |

规则：

- 应将其放在待匹配方块或导入结构之后
- 匹配发生在解析时，针对的是当前场景状态
- 子注解使用相对于每个匹配方块的局部坐标

示例：

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

向预览场景中加入实体。

这些属性遵循类似 summon 的实体放置与 SNBT 数据语义。

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 实体类型 id；支持旧式名称如 `Sheep`、现代原版 id 如 `minecraft:sheep`，也支持 `modid.entityName` 或 `modid:entityName` 形式的模组实体 id |
| `x` | 否 | 实体中心点 X，float，默认 `0.5` |
| `y` | 否 | 实体底部 Y，float，默认 `0` |
| `z` | 否 | 实体中心点 Z，float，默认 `0.5` |
| `rotationY` | 否 | yaw，角度，默认 `-45` |
| `rotationX` | 否 | pitch，角度，默认 `0` |
| `data` | 否 | summon 风格 SNBT，会在生成前并入实体 NBT |
| `name` | 否 | 当 `id` 是 `player`、`fakeplayer`、`minecraft:player` 或 `minecraft:fakeplayer` 时使用的预览玩家名 |
| `uuid` | 否 | 使用上述玩家 id 时的预览玩家 UUID |
| `showName` | 否 | 控制预览玩家头顶名牌的布尔表达式；对玩家预览 id 默认 `true` |
| `showCape` | 否 | 控制预览玩家披风显示的布尔表达式；对玩家预览 id 默认 `true` |
| `headRotation` | 否 | 预览玩家头部旋转，格式为 `x y z` 角度 |
| `leftArmRotation` | 否 | 预览玩家左臂旋转，格式为 `x y z` 角度 |
| `rightArmRotation` | 否 | 预览玩家右臂旋转，格式为 `x y z` 角度 |
| `leftLegRotation` | 否 | 预览玩家左腿旋转，格式为 `x y z` 角度 |
| `rightLegRotation` | 否 | 预览玩家右腿旋转，格式为 `x y z` 角度 |
| `capeRotation` | 否 | 预览玩家披风旋转，格式为 `x y z` 角度；默认静止站立角度 `6 0 0` |

说明：

- 实体包围盒会参与场景自动居中和可见层筛选
- 当预览世界尚未准备好时，实体创建会优雅回退，并在首次渲染时绑定
- 玩家预览 id 会创建客户端侧 fake remote player，以复用普通玩家渲染器和皮肤管线
- 若玩家预览既未提供 `name` 也未提供 `uuid`，GuideNH 会回退到 `Steve` 和原版默认皮肤
- 只提供 `name` 时，GuideNH 会先尝试解析真实在线 profile 以加载皮肤和披风；失败时回退到稳定离线 UUID
- 只提供 `uuid` 时，GuideNH 会生成占位显示名，并继续尝试从 profile 解析皮肤
- `showName={false}` 会隐藏头顶名称，但仍走普通玩家渲染路径
- `showCape={false}` 会隐藏披风，同时仍保留普通玩家渲染路径和 Forge hook
- 玩家姿态属性使用三个以空格分隔的浮点数，对应模型 `X Y Z` 角度
- 若省略头部/四肢旋转，沿用原版 idle 姿态；若省略 `capeRotation`，回退到站立静止披风角度 `6 0 0`
- 玩家预览在解析时需要活动的客户端世界，因为 Minecraft 的玩家实体构造器无法脱离 world 创建
- 悬停实体时会显示其本地化显示名；若提供了自定义名，则显示自定义名

示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

预览玩家姿态示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

预览玩家名称与披风示例：

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## 相机中心行为

若未显式提供 `centerX/Y/Z`，GuideNH 会根据已放置方块的包围盒自动居中场景。若设置了任意一个显式中心坐标，则自动居中会被禁用，未提供的其余坐标默认 `0`。

## 交互说明

当 `interactive={true}` 时，场景支持旋转、平移、缩放、重置、注解开关，以及指南界面暴露出的其他交互控件。

- 跨越多个 Y 层的场景会在底部上方显示可见层滑块
- 若 StructureLib 元数据提供相关信息，场景底部还可能出现舱口高亮切换按钮和频道滑块
- 注解悬停优先级高于方块悬停；当没有悬停注解热点时，方块 tooltip 会正常显示
- StructureLib 悬停会把方块名称放在 tooltip 第一行，把结构专用文本放在第二行开始；按住 `Shift` 时还会展开替换候选项

## 相关页面

- [注解](Annotations-zh-CN)
- [配方](Recipes-zh-CN)
- [示例](Examples-zh-CN)
