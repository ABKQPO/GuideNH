---
navigation:
  title: 图表
  parent: index.md
---

# 图表

GuideNH 内置五种交互式图表标签：`<ColumnChart>`（竖柱）、`<BarChart>`（横条）、`<LineChart>`（折线）、`<PieChart>`（饼图）、`<ScatterChart>`（散点图）。

通用属性：
- `title`：图表标题（字符串）。
- `categories`：逗号分隔的类目标签（字符串），对应横轴（ColumnChart/LineChart/BarChart）或图例（PieChart）。
- `xAxisLabel` / `yAxisLabel`：轴标签，不填则使用 `xAxisUnit` / `yAxisUnit`。
- `xAxisUnit` / `yAxisUnit`：轴单位，追加在自动生成标签后。
- `width` / `height`：画布尺寸（像素），默认为图表类型的合理默认值。

## ColumnChart

<ColumnChart
  title="季度产量"
  categories="Q1,Q2,Q3,Q4"
  width="400"
  height="240"
>
  <Series name="铁" color="#888888" data="120,200,150,80" />
  <Series name="金" color="#FFAA00" data="30,50,90,70" />
</ColumnChart>

## BarChart

<BarChart
  title="模组下载量 (万)"
  categories="GTNH,IC2,Thermal,Mekanism"
  width="400"
  height="240"
>
  <Series name="2024" color="#4488ff" data="350,180,210,90" />
</BarChart>

## LineChart

**类目 X 轴：**

<LineChart
  title="温度"
  categories="周一,周二,周三,周四,周五"
  yAxisUnit="℃"
  width="400"
  height="240"
>
  <Series name="室外" color="#ff6644" data="18,22,25,20,17" />
  <Series name="室内" color="#44aaff" data="22,23,23,22,22" />
</LineChart>

**数值 X 轴**（不设 `categories`，`<Point>` 给出 `x y`）：

<LineChart
  title="信号衰减"
  xAxisLabel="距离 (m)"
  yAxisLabel="强度 (dB)"
  width="400"
  height="240"
>
  <Series name="空气" color="#44ff88">
    <Point x="0"  y="0"   />
    <Point x="10" y="-2"  />
    <Point x="20" y="-6"  />
    <Point x="40" y="-14" />
    <Point x="80" y="-26" />
  </Series>
  <Series name="混凝土" color="#cc8844">
    <Point x="0"  y="0"   />
    <Point x="10" y="-8"  />
    <Point x="20" y="-20" />
    <Point x="40" y="-40" />
  </Series>
</LineChart>

## PieChart

<PieChart title="资源占比" width="340" height="240">
  <Slice name="铁"   color="#888888" value="40" />
  <Slice name="铜"   color="#CC6633" value="25" />
  <Slice name="金"   color="#FFAA00" value="15" />
  <Slice name="钻石" color="#55DDFF" value="10" />
  <Slice name="其它" color="#AAAAAA" value="10" />
</PieChart>

## ScatterChart

<ScatterChart
  title="身高-体重"
  xAxisLabel="身高 (cm)"
  yAxisLabel="体重 (kg)"
  width="400"
  height="260"
>
  <Series name="样本 A" color="#ff6644">
    <Point x="160" y="55" />
    <Point x="170" y="65" />
    <Point x="175" y="70" />
    <Point x="180" y="75" />
    <Point x="185" y="82" />
  </Series>
  <Series name="样本 B" color="#44aaff">
    <Point x="155" y="50" />
    <Point x="162" y="58" />
    <Point x="168" y="63" />
    <Point x="178" y="72" />
  </Series>
</ScatterChart>

## 组合：ColumnChart + LineSeries + PieInset

`<LineSeries>` 叠加到 ColumnChart 上，`<PieInset>` 在右上角嵌入一个小饼图：

<ColumnChart
  title="月度产量组合"
  categories="1月,2月,3月,4月,5月,6月"
  width="480"
  height="280"
>
  <Series name="铁" color="#888888" data="80,120,200,150,180,220" />
  <Series name="金" color="#FFAA00" data="20,30,50,40,60,80" />
  <LineSeries name="合计" color="#FF4466" data="100,150,250,190,240,300" />
  <PieInset title="合计占比" width="120" height="120">
    <Slice name="铁" color="#888888" value="950" />
    <Slice name="金" color="#FFAA00" value="280" />
  </PieInset>
</ColumnChart>
