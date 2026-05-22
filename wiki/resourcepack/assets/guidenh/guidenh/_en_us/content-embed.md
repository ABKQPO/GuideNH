---
navigation:
  title: Content Embedding
  parent: index.md
  position: 135
  icon: minecraft:wool:3
categories:
  - widgets
---

# Content Embedding and Text Wrapping

All block-level tags support `wrap` and `align` attributes that mirror
Microsoft Word's "Text Wrapping" options.

## Wrap Modes

| Mode | Description |
|---|---|
| `inline` | Default — block occupies its own vertical slot |
| `square` | Float left/right; text wraps around in a rectangle |
| `tight` | Equivalent to `square` in this layout system |
| `through` | Equivalent to `square` in this layout system |
| `top-bottom` | No text beside the block; `align` controls horizontal position |
| `behind` | Block aligns like `inline` but renders behind text |
| `front` | Block aligns like `inline` but renders in front of text |

## Alignment

| Value | Meaning |
|---|---|
| `left` | Flush left (default) |
| `center` | Horizontally centred |
| `right` | Flush right |

## Inline (Default)

A block preview in default inline mode. It sits in the vertical flow.

<BlockImage id="minecraft:stone" />

Some text after the block preview.

## Top-Bottom, Centred

<BlockImage id="minecraft:planks" align="center" />

Text only appears above and below the placed-block preview, which is centred horizontally.

## Top-Bottom, Right-Aligned

<BlockImage id="minecraft:planks" align="right" />

The placed-block preview is pushed to the right edge.

## Square Float Left

<BlockImage id="minecraft:stone" wrap="square" align="left" scale={2} />

This paragraph will flow to the right of the block preview. The preview registers as
a left-side document float and subsequent paragraphs automatically shrink their
available width to avoid it, just like a CSS `float: left`. More text here to
demonstrate the wrapping effect across multiple lines of content.

## Square Float Right

<BlockImage id="minecraft:glass" wrap="square" align="right" scale={2} />

This paragraph flows to the left of the block preview. `wrap="square"` combined
with `align="right"` registers a right-side float in the layout engine. Longer
sentences will wrap across several lines on the left of the float.

## Center Alignment (No Wrapping)

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />

The placed-block preview is centred horizontally. No text flows to the sides.

## Item Images

<ItemImage id="minecraft:diamond" align="center" />

<ItemImage id="minecraft:emerald" align="right" />

## FloatingImage with wrap / align

<FloatingImage src="test1.png" wrap="square" align="left" width="64" title="Float left" />

Text to the right of a floating image using the new `wrap` + `align` syntax.
This is equivalent to the legacy `align="left"` attribute on `<FloatingImage>`.

<FloatingImage src="test1.png" wrap="square" align="right" width="64" title="Float right" />

Text to the left of a floating image using `wrap="square" align="right"`.

## Recipes

<Recipe id="minecraft:stone" wrap="square" align="left" fallbackText="(recipe unavailable)" />

Text to the right of a recipe box. The recipe floats left and subsequent
paragraph text fills the space beside it.

<Recipe id="minecraft:glass" wrap="square" align="right" fallbackText="(recipe unavailable)" />

Text to the left of a recipe box using a right-side float.

## GameScene Float Left

<GameScene wrap="square" align="left" zoom={4} showBackground={false} width="120" height="90">
  <Block id="minecraft:furnace" />
</GameScene>

This paragraph text flows to the right of the scene viewport. The
`wrap="square" align="left"` combination registers the scene as a
left-side document float, so all subsequent paragraphs narrow their
available width until the float clears. Additional text is added here
to show multi-line wrapping around the scene.

## GameScene Centred

<GameScene align="center" zoom={4} showBackground={false} width="200" height="120">
  <Block id="minecraft:crafting_table" />
</GameScene>

A scene that is horizontally centred using `align="center"` without any
float — text appears only above and below, never beside.

## GameScene Float Right

<GameScene wrap="square" align="right" zoom={4} showBackground={false} width="120" height="90">
  <Block id="minecraft:chest" />
</GameScene>

This paragraph text flows to the left of the scene. Multiple lines of
text here demonstrate that the right-float reduces the line box on
the right side for each line until the scene's registered float clears.

## Column Float Left

<Column wrap="square" align="left" gap="4" width="90">

**Crafting Table**

Used to craft items.

</Column>

A `<Column>` block carrying `wrap="square" align="left"` floats left just
like any other block-level tag. The column contains structured content
(headings, text) and text in the document flows to the right of it.

## Column Centred

<Column align="center" gap="4" width="160">

**Note**

This column is centred horizontally without floating.

</Column>

Text that appears below a centred Column block.

Text to the left of a right-floating recipe box.
