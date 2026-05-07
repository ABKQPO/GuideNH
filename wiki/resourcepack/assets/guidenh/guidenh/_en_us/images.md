---
navigation:
  title: Images
  parent: index.md
  icon: minecraft:wool:1
  # Cycling icons example — uncomment to enable:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4
  #   - minecraft:wool:14
  # icon_components example (applies NBT to the icon: item):
  # icon_components:
  #   display:
  #     Name: "Colored Wool Demo"
---

# Images

`<FloatingImage>`, `<ItemImage>`, and `<BlockImage>` rendering tests.

## FloatingImage

Relative path (`test1.png` in the same directory):

![Test Image](test1.png)

Inline image mixed with text: here ![inline](test1.png) is an inline image.

`<FloatingImage>` accepts `width` / `height` (pixels): giving one keeps the aspect ratio; giving both **stretches** the image (ratio not preserved); giving neither falls back to the default natural / 4 + availableWidth clamp.

Fixed 64×64 (single dim, keeps ratio):

<FloatingImage src="test1.png" align="left" width="64" title="width=64" />

Forced 200×80 stretch (ratio not preserved):

<FloatingImage src="test1.png" align="right" width="200" height="80" title="stretch 200x80" />

Fixed height 40 (width derived):

<FloatingImage src="test1.png" align="left" height="40" title="height=40" />

## ImageAnnotation

`<ImageAnnotation>` children attach hover tooltips (and optional colored borders) to rectangular
regions of a `<FloatingImage>`. Coordinates (`x`, `y`, `w`, `h`) are in **image pixels**; the
region is automatically scaled when the image is resized or stretched. Omitting all four covers the
entire image.

Whole-image annotation (hover anywhere over the image to see the tooltip):

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation>
    This tooltip appears when you hover over **any part** of the image.
  </ImageAnnotation>
</FloatingImage>

Region annotation with a visible red border (x=10, y=10, w=60, h=40):

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    Hovering the **red-bordered region** shows this tooltip.
  </ImageAnnotation>
</FloatingImage>

Multiple annotations on one image — each region shows a different tooltip:

<FloatingImage src="test1.png" align="left" width="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>

Stretched image (200×80) with an annotation that follows the stretch:

<FloatingImage src="test1.png" align="right" width="200" height="80">
  <ImageAnnotation x="0" y="0" w="128" h="128" border borderColor="#FFFFFF44" borderThickness="2">
    Left portion of the stretched image.
  </ImageAnnotation>
</FloatingImage>

## ItemImage Scale

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

### Inline Icon vs. Text Baseline

Inline `<ItemImage>` icons are nudged upward by ~4 pixels (scaled by `scale`) so their visual center lines up with the surrounding text baseline. The label text receives a separate, smaller default nudge (-2 px). Both can be overridden independently.

- Default offset (-4px icon, -3px label): this line mixes <ItemImage id="minecraft:diamond" label="right" /> diamond, <ItemImage id="minecraft:apple" label="right" /> apple and <ItemImage id="minecraft:iron_ingot" label="right" /> iron ingot.
- Disabled icon offset (`yOffset="0"`, label unchanged): <ItemImage id="minecraft:diamond" yOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" label="right" /> apple.
- Disabled label offset (`labelYOffset="0"`, icon unchanged): <ItemImage id="minecraft:diamond" labelYOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" labelYOffset="0" label="right" /> apple.
- Both offsets zeroed (`yOffset="0" labelYOffset="0"`): <ItemImage id="minecraft:diamond" yOffset="0" labelYOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" labelYOffset="0" label="right" /> apple.

> Values are pixels at `scale=1` and are multiplied by the current scale at render time.

## ItemImage Label

Label to the right (default italic name):

<ItemImage id="minecraft:diamond" label="right" />

Label to the left:

<ItemImage id="minecraft:iron_ingot" label="left" />

Bold format with `%s` placeholder:

<ItemImage id="minecraft:gold_ingot" label="right" format="**%s**" />

Strikethrough format:

<ItemImage id="minecraft:rotten_flesh" label="right" format="~~%s~~" />

Underline (using `__`):

<ItemImage id="minecraft:emerald" label="right" format="__%s__" />

Wavy underline:

<ItemImage id="minecraft:blaze_rod" label="right" format="^^%s^^" />

Dotted underline:

<ItemImage id="minecraft:ender_pearl" label="right" format="::Custom Label::" />

Icon hidden, label only:

<ItemImage id="minecraft:diamond" showIcon="false" label="right" />

Icon shown, no tooltip:

<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />

## BlockImage Scale

<Row>
  <BlockImage id="minecraft:stone" scale="1" />
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" scale="3" />
  <BlockImage id="minecraft:stone" scale="4" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

## BlockImage Row Samples

<Row>
  <BlockImage id="minecraft:log" scale="4" />
  <BlockImage id="minecraft:log2" scale="4" />
  <BlockImage id="minecraft:planks" scale="4" />
  <BlockImage id="minecraft:cobblestone" scale="4" />
  <BlockImage id="minecraft:stonebrick" scale="4" />
  <BlockImage id="minecraft:mossy_cobblestone" scale="4" />
</Row>

<ItemImage id="minecraft:compass" />

## ItemLink

Basic link (text only, tooltip enabled):

<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />

Icon to the left of the link text:

<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />

Icon to the right, tooltip suppressed:

<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />

Ore-dictionary lookup:

<ItemLink ore="stickWood" />
