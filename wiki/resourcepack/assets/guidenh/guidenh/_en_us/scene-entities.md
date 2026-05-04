---
navigation:
  title: Entity Scenes
  parent: index.md
---

# Entity Scenes

`<Entity>` tag tests inside `<GameScene>`.

## Mobs

Sheep (baby, colored) and zombie (baby):

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>

## Player — Custom Rotations

`headRotation`, `rightArmRotation`, `leftArmRotation`, `rightLegRotation`, `leftLegRotation`, `capeRotation` (degrees X Y Z each):

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    baby={true}
    name="Circulation_"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>

## Player — Name & Cape Toggles

`showName` and `showCape` control visibility:

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>

## Player — Elytra via NBT `data`

Slot `102b` is the chestplate slot; setting it to an elytra item renders the elytra on the player:

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="Circulation_"
    data='{Inventory:[{Slot:102b,id:"etfuturum:elytra",Count:1b}]}'
  />
  <Entity
    id="player"
    x="2"
    y="1"
    baby={true}
    name="Circulation_"
    data='{Inventory:[{Slot:102b,id:"etfuturum:elytra",Count:1b}]}'
  />
</GameScene>
