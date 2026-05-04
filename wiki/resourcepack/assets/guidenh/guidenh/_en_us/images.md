---
navigation:
  title: Images
  parent: index.md
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

## ItemImage Scale

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

### Inline Icon vs. Text Baseline

Inline `<ItemImage>` icons are nudged upward by ~2 pixels (scaled by `scale`) so their visual center lines up with the surrounding text baseline.

- Default offset (-2px): this line mixes <ItemImage id="minecraft:diamond" /> diamond, <ItemImage id="minecraft:apple" /> apple and <ItemImage id="minecraft:iron_ingot" /> iron ingot.
- Disabled (`yOffset="0"`): <ItemImage id="minecraft:diamond" yOffset="0" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" /> apple and <ItemImage id="minecraft:iron_ingot" yOffset="0" /> iron ingot.
- Larger offset (`yOffset="-4"`): <ItemImage id="minecraft:diamond" yOffset="-4" /> diamond, <ItemImage id="minecraft:apple" yOffset="-4" /> apple and <ItemImage id="minecraft:iron_ingot" yOffset="-4" /> iron ingot.

> Values are pixels at `scale=1` and are multiplied by the current scale at render time.

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
