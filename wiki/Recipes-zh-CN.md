[English](Recipes)

# 配方

GuideNH 可以直接在指南页面中渲染合成配方和基于 NEI 的配方。

## 支持的标签

- `<Recipe>`
- `<RecipeFor>`
- `<RecipesFor>`

三者使用同一套编译器和属性集。

## 标签语义

| 标签 | 行为 |
| --- | --- |
| `<Recipe>` | 为目标物品渲染单个配方 |
| `<RecipeFor>` | 与单配方行为相同，但命名更易读 |
| `<RecipesFor>` | 渲染多个匹配配方 |

如果存在多个配方，而你使用单配方形式，GuideNH 默认只会渲染其中一个，除非额外过滤将结果缩小。

## 通用属性

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `id` | 是 | 目标物品引用 |
| `fallbackText` | 否 | 没有可用配方时显示的文本 |
| `handlerName` | 否 | 对 handler 名称做大小写不敏感的子串过滤 |
| `handlerId` | 否 | 对 overlay/handler id 做大小写不敏感的精确过滤 |
| `handlerOrder` | 否 | 过滤后 handler 的 0 基索引 |
| `input` | 否 | 输入物品过滤表达式 |
| `output` | 否 | 输出物品过滤表达式 |
| `limit` | 否 | 正整数，限制最多渲染的配方数 |

## 物品 id 语法

`id`、`input` 和 `output` 都使用 GuideNH 扩展物品引用格式：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

通配 meta 写法：

- `*`
- `32767`
- 大写标记，例如 `ANY`

## 过滤表达式语法

`input` 和 `output` 支持：

- `,` 表示 OR
- `&` 表示 AND
- `!` 表示 NOT

示例：

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

## 渲染顺序

GuideNH 会按以下顺序尝试配方：

1. NEI 原生 handler 渲染
2. NEI 槽位数据回退渲染
3. 内置原版合成回退渲染

如果都没有匹配：

- 若设置了 `fallbackText`，则显示它
- 否则显示内联编写错误

## 示例

### 单个配方

````md
<RecipeFor id="minecraft:crafting_table" />
````

### 多个配方

````md
<RecipesFor id="minecraft:torch" />
````

### Handler 过滤

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### 输入/输出过滤

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### 回退文本

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## 最佳实践

- 面向可选模组整合时，优先提供 `fallbackText`
- 若已知确切的 NEI handler，优先使用 `handlerId`
- 当标签可能展开出大量配方时，使用 `limit`
- 复杂过滤逻辑最好在标签旁边用注释解释，便于维护

## 实时运行示例

可参考 `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md`，其中包含大量配方示例，包括 handler 过滤、通配 meta 和带 NBT 的物品 id。
