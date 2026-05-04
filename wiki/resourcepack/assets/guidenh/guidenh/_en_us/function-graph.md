---
navigation:
  title: Function Graphs
  parent: markdown.md
---

# Function Graphs

Desmos-style interactive function graphs. Three variants: `funcgraph` fenced block, `<FunctionGraph>` MDX container, and `<Function>` shorthand for a single curve.

All variants share the same panel: configurable size, X/Y range, optional grid/axes, automatic quadrant expansion, and per-curve domain limits. Hold the mouse on a curve and drag to scrub a labelled point along it; the tooltip stays anchored above the point and flips below when there is no room.

Any curve given a `label` is also listed in a legend rendered just below the panel. Each entry is a small colour swatch followed by the label; entries flow left-to-right and wrap onto a new row whenever the next entry would not fit.

## Fenced Block

The first line of a `funcgraph` fence sets panel-wide attributes (`width`, `height`, `xRange=a..b` / `xMin` / `xMax` / `xStep`, `yRange` / `yMin` / `yMax` / `yStep`, `quadrants=1,2,3,4` or `all`, `title`, `background`, `border`, `axisColor`, `gridColor`, `showGrid`, `showAxes`). Each subsequent non-blank line is either an expression with optional pipe-delimited attributes, an explicit point `:x,y`, or a plot-anchored point `@plot=N atX=v` / `@plot=N atY=v`. Comments start with `#`.

```funcgraph
width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all
sin(x)        | color=#ff5566 label="sin"
cos(x)        | color=#3399ff label="cos"
x/2           | color=#88cc77 domain=-pi..pi
:0,0
@plot=0 atX=1.5708
```

## `<FunctionGraph>` Container

The container accepts the same panel attributes as the fence header. Children are `<Plot expr="..." />` (or `<Function expr="..." />`) for curves and `<Point ... />` for marked points. Plot attributes: `expr`, `inverse={true}` to treat the expression as `x = f(y)`, `domain="a..b"` or comma-separated clauses like `x>=0, x<=pi`, `color`, `label`. Point attributes: explicit `x` + `y`, or `plot="N"` plus either `atX="v"` or `atY="v"` to anchor on a curve.

```mdx
<FunctionGraph width="360" height="220" xRange="-6..6" yRange="-3..3" quadrants="all">
  <Plot expr="sin(x)" color="#ff5566" label="sin x"/>
  <Plot expr="x^2 / 4" color="#3399ff" domain="-4..4" label="x² / 4"/>
  <Plot expr="|x| - 1" color="#88cc77" label="|x| - 1"/>
  <Point x="0" y="0"/>
  <Point plot="0" atX="1.5708"/>
</FunctionGraph>
```

<FunctionGraph width="360" height="220" xRange="-6..6" yRange="-3..3" quadrants="all">
  <Plot expr="sin(x)" color="#ff5566" label="sin x"/>
  <Plot expr="x^2 / 4" color="#3399ff" domain="-4..4" label="x² / 4"/>
  <Plot expr="|x| - 1" color="#88cc77" label="|x| - 1"/>
  <Point x="0" y="0"/>
  <Point plot="0" atX="1.5708"/>
</FunctionGraph>

## `<Function>` Shorthand

When you only need a single curve, `<Function>` skips the wrapper:

```mdx
<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff"/>
```

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff"/>

## Expression Syntax

* Operators: `+ - * / %` plus `^` (right-associative) and unary minus.
* Postfix `!` is factorial, extended to real numbers via the gamma function (negative integers return NaN).
* `|expr|` is absolute value; `√`/`sqrt` and `∛`/`cbrt` are roots.
* Implicit multiplication: `2x`, `2pi`, `(x+1)(x-1)`.
* Built-in functions: `sin cos tan asin acos atan sinh cosh tanh exp ln log log2 log10 sqrt cbrt abs sign floor ceil round`, plus two-arg `atan2 min max pow hypot mod`.
* Constants: `pi`, `tau`, `e`, `phi`.
* Domain clauses (`domain="..."`): `min..max` shorthand for x bounds; clauses like `x>=0`, `x<5` separated by commas; constants accepted on either side.
* Set `inverse={true}` (MDX) or `inverse=true` (fence attrs) to plot `x = f(y)` — the same expression is evaluated against `y` and the curve is rotated.

## Default Quadrants

If you omit `xRange` / `yRange` and `quadrants`, the panel starts in quadrant 1 (`x>=0`, `y>=0`). When sampling reveals negative `y` values and the y-bounds were not set explicitly, the panel automatically expands to include quadrants 3 and 4 so the curves stay visible.
