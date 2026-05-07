---
navigation:
  title: 图片
  parent: index.md
  icon: minecraft:wool:1
  # 循环图标示例——取消注释即可启用：
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4
  #   - minecraft:wool:14
  # icon_components 示例（为 icon: 物品附加 NBT）：
  # icon_components:
  #   display:
  #     Name: "彩色羊毛演示"
---

# 图片

`<FloatingImage>`、`<ItemImage>` 和 `<BlockImage>` 渲染测试。

## FloatingImage

相对路径引用当前目录下 `test1.png`：

![测试图片](test1.png)

段落内嵌图：这是一张图 ![inline](test1.png) 嵌在文字里。

`<FloatingImage>` 支持 `width` / `height`（像素）属性：只给一维则按纹理原比例换算另一维，两维都给则**拉伸**（不保比例），都不给则沿用默认尺寸。

固定 64×64（等比缩到 64）：

<FloatingImage src="test1.png" align="left" width="64" title="width=64" />

同一张图强制 200×80 拉伸（不保比例）：

<FloatingImage src="test1.png" align="right" width="200" height="80" title="stretch 200x80" />

固定高度 40（宽度按比例换算）：

<FloatingImage src="test1.png" align="left" height="40" title="height=40" />

## ImageAnnotation

`<ImageAnnotation>` 是 `<FloatingImage>` 的子标签，用于为图片的矩形区域添加悬停 tooltip 和可选的彩色边框。坐标（`x`、`y`、`w`、`h`）以**图片像素**为单位；当图片被缩放或拉伸时，注解区域会随之自动缩放。省略全部四个坐标时，注解覆盖整张图片。

整图注解（鼠标悬停在图片任意位置均显示 tooltip）：

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation>
    悬停在图片**任意位置**都会显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>

区域注解，显示红色边框（x=10, y=10, w=60, h=40）：

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    悬停在**红色边框区域**内显示此 tooltip。
  </ImageAnnotation>
</FloatingImage>

同一张图上的多个注解——每个区域显示不同 tooltip：

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    左半部分
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    右半部分
  </ImageAnnotation>
</FloatingImage>

拉伸图（200×80）上的注解会随拉伸自动适配：

<FloatingImage src="test1.png" align="right" width="200" height="80">
  <ImageAnnotation x="0" y="0" w="128" h="128" border borderColor="#FFFFFF44" borderThickness="2">
    拉伸图左侧区域。
  </ImageAnnotation>
</FloatingImage>

## ItemImage 缩放

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

### 内联图标与文字的纵向对齐

内联的 `<ItemImage>` 默认会向上偏移约 2 像素（随 `scale` 等比例缩放），让图标视觉中心与文字基线对齐。

- 默认偏移（-2px）：这行里有 <ItemImage id="minecraft:diamond" /> 钻石 <ItemImage id="minecraft:apple" /> 苹果和 <ItemImage id="minecraft:iron_ingot" /> 铁锭。
- 禁用偏移（`yOffset="0"`）：这行里有 <ItemImage id="minecraft:diamond" yOffset="0" /> 钻石 <ItemImage id="minecraft:apple" yOffset="0" /> 苹果和 <ItemImage id="minecraft:iron_ingot" yOffset="0" /> 铁锭。
- 加大偏移（`yOffset="-4"`）：这行里有 <ItemImage id="minecraft:diamond" yOffset="-4" /> 钻石 <ItemImage id="minecraft:apple" yOffset="-4" /> 苹果和 <ItemImage id="minecraft:iron_ingot" yOffset="-4" /> 铁锭。

> 偏移量以 scale=1 下的像素数给出，实际渲染时会乘以当前 `scale`。

## BlockImage 缩放

<Row>
  <BlockImage id="minecraft:stone" scale="1" />
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" scale="3" />
  <BlockImage id="minecraft:stone" scale="4" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

## BlockImage 行列示例

<Row>
  <BlockImage id="minecraft:log" scale="4" />
  <BlockImage id="minecraft:log2" scale="4" />
  <BlockImage id="minecraft:planks" scale="4" />
  <BlockImage id="minecraft:cobblestone" scale="4" />
  <BlockImage id="minecraft:stonebrick" scale="4" />
  <BlockImage id="minecraft:mossy_cobblestone" scale="4" />
</Row>

<ItemImage id="minecraft:compass" />
