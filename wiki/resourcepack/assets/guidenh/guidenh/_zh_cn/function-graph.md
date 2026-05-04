---
navigation:
  title: 函数图
  parent: index.md
---

# 函数图

GuideNH 提供 Desmos 风格的交互函数图，支持平移、缩放、点击查看函数值。

## 围栏代码块

在 Markdown 中用 ` ```funcgraph ` 作为语言标识符，每行一个表达式：

```funcgraph
y = x^2
y = x^2 - 1
```

## `<FunctionGraph>` 容器

MDX 组件写法，支持更细粒度的属性控制：

<FunctionGraph xMin="-5" xMax="5" yMin="-2" yMax="5">
  <Function expr="y = x^2" color="#4488ff" label="y = x²" />
  <Function expr="y = |x| - 1" color="#ff6644" label="y = |x| − 1" />
  <Function expr="y = sqrt(x)" color="#44bb88" label="y = √x" />
</FunctionGraph>

## `<Function>` 简写

`<Function>` 自带独立 `<FunctionGraph>` 包装器，可当作简洁的单函数展示：

<Function expr="y = sin(x)" />

## 表达式语法参考

| 语法            | 说明                                        |
|---------------|-------------------------------------------|
| `x^n`         | 幂运算（`^` 优先级高于乘）                         |
| `sqrt(x)`     | 平方根，等价于 `x^0.5`                          |
| `cbrt(x)`     | 立方根，等价于 `x^(1/3)`                        |
| `abs(x)`      | 绝对值，等价于 `\|x\|`                          |
| `\|x\|`       | 绝对值花括号写法（管道符对）                           |
| `sin/cos/tan` | 三角函数（弧度制）                                 |
| `asin/acos`   | 反三角函数                                     |
| `atan(y, x)`  | 双参数反正切（同 `atan2`）                        |
| `ln(x)`       | 自然对数                                      |
| `log(b, x)`   | 以 `b` 为底的对数                               |
| `floor/ceil`  | 向下/向上取整                                   |
| `round(x)`    | 四舍五入                                      |
| `mod(x, m)`   | 取模                                        |
| `min/max`     | 最小/最大值，支持多参数                              |
| `pi` / `e`    | 内置常量 π ≈ 3.14159 / e ≈ 2.71828          |

运算符优先级（从高到低）：`^` → 一元 `-` → `* /` → `+ -`。

## 默认象限行为

不设 `xMin/xMax/yMin/yMax` 时，图表自动扩展到显示所有函数曲线的最小包围盒，并各留 10% 边距；用户可用鼠标拖拽平移、滚轮缩放。

<FunctionGraph>
  <Function expr="y = x" color="#4488ff" label="y = x" />
  <Function expr="y = -x + 4" color="#ff6644" label="y = −x + 4" />
</FunctionGraph>
