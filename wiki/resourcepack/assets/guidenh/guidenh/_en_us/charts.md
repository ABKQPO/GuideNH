---
navigation:
  title: Charts
  parent: markdown.md
---

# Charts

GuideNH ships with five interactive chart tags: `<ColumnChart>` clustered columns, `<BarChart>` horizontal bars, `<LineChart>` line, `<PieChart>` pie, and `<ScatterChart>` XY scatter. All charts show value + percentage tooltips on hover, and highlight the hovered element (column/bar grows, line points pop along the normal, pie slices pop outward, scatter points enlarge).

## Common Attributes

* `title`
* `width` / `height` (defaults: 320 x 200)
* `background` / `border` colors
* `titleColor` / `labelColor`
* `legend` position: `none` / `top` / `bottom` / `left` / `right` (default `top`)
* `labelPosition`: `none` / `inside` / `outside` / `above` / `below` / `center`

Color formats: `#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`.

## ColumnChart

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>
```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>

Extra attributes: `categories` (X-axis labels, comma separated), `barWidthRatio` (default 0.7), `xAxisLabel` / `yAxisLabel` / `yAxisMin` / `yAxisMax` / `yAxisStep` / `yAxisUnit` / `yAxisTickFormat`, `showXGrid={true}` / `showYGrid={true}`.

## BarChart

```mdx
<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>
```

<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>

Same attributes as ColumnChart but categories are on the Y-axis.

## LineChart

Categorical X:

```mdx
<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>
```

<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>

Numeric X:

```mdx
<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>
```

<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>

Extra: `numericX={true}` enables a numeric X axis; `showPoints={false}` hides point markers.

## PieChart

```mdx
<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice label="Iron" value="45" color="#4E79A7"/>
  <Slice label="Copper" value="25" color="#F28E2B"/>
  <Slice label="Gold" value="15" color="#E15759"/>
  <Slice label="Diamond" value="10"/>
  <Slice label="Other" value="5"/>
</PieChart>
```

<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice label="Iron" value="45" color="#4E79A7"/>
  <Slice label="Copper" value="25" color="#F28E2B"/>
  <Slice label="Gold" value="15" color="#E15759"/>
  <Slice label="Diamond" value="10"/>
  <Slice label="Other" value="5"/>
</PieChart>

Extra: `startAngle` (default -90, i.e. 12 o'clock); `clockwise={false}` to reverse direction.

## ScatterChart

```mdx
<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>
```

<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>

## Combo: ColumnChart + LineSeries + PieInset

`<ColumnChart>` and `<BarChart>` accept extra `<LineSeries>` (line overlay sharing the value axis) and `<PieInset>` (small corner pie) children, letting one chart combine several styles.

The pie inset's `position` attribute accepts `topRight` / `topLeft` / `bottomRight` / `bottomLeft` (corner overlay) or `right` (the chart auto-extends its width and the pie occupies a dedicated outside column).

Line overlays share hover/tooltip behavior with the underlying columns/bars: hovering a line point thickens its adjacent segments, enlarges the point, and shows a tooltip with the series name and value.

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="Total share">
    <Slice label="Iron" value="225" color="#a0a0a0"/>
    <Slice label="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="Total share">
    <Slice label="Iron" value="225" color="#a0a0a0"/>
    <Slice label="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
