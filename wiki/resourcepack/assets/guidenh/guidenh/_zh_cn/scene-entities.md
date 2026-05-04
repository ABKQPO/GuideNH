---
navigation:
  title: 实体场景
  parent: index.md
---

# 实体场景

`<GameScene>` 内的 `<Entity>` 标签测试。

## 生物

小羊（baby，染色）与僵尸（baby）：

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>

## 玩家 — 自定义旋转

`headRotation`、`rightArmRotation`、`leftArmRotation`、`rightLegRotation`、`leftLegRotation`、`capeRotation`（X Y Z 角度）：

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

## 玩家 — 名字与披风开关

`showName` 和 `showCape` 控制是否显示：

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>

## 玩家 — 通过 NBT `data` 穿鞘翅

插槽 `102b` 是胸甲槽，填入鞘翅物品后即可渲染：

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
