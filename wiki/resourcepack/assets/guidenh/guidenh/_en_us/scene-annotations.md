---
navigation:
  title: Annotations
  parent: index.md
---

# Annotations

All annotation kinds live in world space and can be toggled with the scene's *Show/Hide annotations* button.

## DiamondAnnotation

Place a **diamond marker** at any world coordinate. The diamond always faces the screen; hovering shows a semi-transparent white overlay while its compiled child content is rendered as a rich tooltip.

Activated beacon — 3×3 diamond block base, beacon on top; marker tooltip contains a nested 3D preview:

<GameScene width="256" height="192" zoom={4} interactive={true}>
  <Block id="minecraft:diamond_block" x="-1" z="-1" />
  <Block id="minecraft:diamond_block"         z="-1" />
  <Block id="minecraft:diamond_block" x="1"  z="-1" />
  <Block id="minecraft:diamond_block" x="-1" />
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:diamond_block" x="1" />
  <Block id="minecraft:diamond_block" x="-1" z="1" />
  <Block id="minecraft:diamond_block"         z="1" />
  <Block id="minecraft:diamond_block" x="1"  z="1" />
  <Block id="minecraft:beacon" y="1" />
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <Color color="#FFD24C">**Effect**</Color>: grants nearby players continuous buffs — speed /
    jump boost / resistance / strength / regeneration.

    Activation: a 3×3 / 5×5 / 7×7 / 9×9 pyramid of **diamond / iron / gold / emerald / netherite**
    blocks beneath the beacon.

    <GameScene width="160" height="128" zoom={5} interactive={false}>
      <Block id="minecraft:diamond_block" x="-1" />
      <Block id="minecraft:diamond_block" />
      <Block id="minecraft:diamond_block" x="1" />
      <Block id="minecraft:beacon" y="1" />
    </GameScene>

    <Color color="#AAFFAA">Tip</Color>: more pyramid tiers = more effect options; the beam color
    is determined by stained glass placed in the beam path.
  </DiamondAnnotation>
</GameScene>

## Box / Block / Line Annotations

- `BoxAnnotation` accepts `min="x y z"` / `max="x y z"` (floats) for an arbitrary AABB.
- `BlockAnnotation` accepts a single `pos="x y z"` (integers); shorthand for a 1×1×1 box.
- `LineAnnotation` accepts `from="x y z"` / `to="x y z"` (floats) for a line segment.

All three support `color="#AARRGGBB" or "#RRGGBB"`, `thickness` in pixel units (default `1`), and `alwaysOnTop` to draw above other geometry. Children are used as a rich-text hover tooltip.

<GameScene width="384" height="224" zoom={4} interactive={true}>
  <Block id="minecraft:iron_block" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:iron_block" x="2" />
  <Block id="minecraft:gold_block" z="2" />
  <Block id="minecraft:gold_block" x="2" z="2" />

  <BoxAnnotation color="#ee3333" min="0 1 0" max="1 1.6 0.6" thickness="0.04">
    **Box annotation** wraps half a block, thickness `0.04`. The tooltip is rich content:

    <Row>
      <ItemImage id="minecraft:iron_ingot" scale="2" />
      Iron ingot — smelt iron ore in a furnace.
    </Row>
    <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
  </BoxAnnotation>

  <BlockAnnotation color="#33ddee" pos="2 0 2" alwaysOnTop={true}>
    **Block annotation**: `alwaysOnTop` punches through depth. Recipe inside the tooltip:

    <RecipeFor id="minecraft:gold_block" />
  </BlockAnnotation>

  <LineAnnotation color="#ffd24c" from="0.5 1.2 0.5" to="2.5 1.2 2.5" thickness="0.08">
    **Line annotation**: a slightly thicker line (`thickness=0.08`). Tooltips can embed a 3D preview:

    <GameScene width="160" height="96" zoom={5} perspective="isometric_north_east" interactive={false}>
      <Block id="minecraft:iron_block" />
      <Block id="minecraft:gold_block" x="1" />
      <DiamondAnnotation pos="0.5 1.2 0.5" color="#ffd24c">Endpoint A</DiamondAnnotation>
      <DiamondAnnotation pos="1.5 1.2 0.5" color="#ee3333">Endpoint B</DiamondAnnotation>
    </GameScene>
  </LineAnnotation>
</GameScene>
