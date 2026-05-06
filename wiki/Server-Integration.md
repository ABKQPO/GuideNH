[中文](Server-Integration-zh-CN)

# Server Integration

This page describes how the GuideNH client and server interact. Understanding this helps diagnose permission issues with structure placement, clarify which features differ between singleplayer and multiplayer, and correctly deploy GuideNH in custom modpacks.

## Core Design: Guide Content Is Client-Only

**Guide page content is never sent over the network.** The server does not need to have GuideNH installed for players to read guides. All pages, images, and scene data are loaded from the client's own resource packs (the mod jar or external resource packs).

Loading sequence:

```
F3+T / game start
  └── GuideReloadListener.onResourceManagerReload()
        ├── Scans all resource packs for .md files under assets/<modid>/guidenh/_<lang>/
        ├── Calls PageCompiler.parse() for each page
        ├── Writes results to GuideRegistry (in-memory map)
        └── Triggers GuideSearch.indexAll() (incremental Lucene index, 5 ms/tick background)
```

## Network Messages

GuideNH registers **3** Forge SimpleNetworkWrapper messages on the `guidenh` channel:

| ID | Message Class | Direction | Purpose |
|----|--------------|-----------|---------|
| 0 | `GuideNhServerHelloMessage` | Server → Client | Handshake on player login — tells the client structure commands are available |
| 1 | `GuideNhStructureRequestMessage` | Client → Server | Send/cache structure data and request server-side placement |
| 2 | `GuideNhClientBridgeMessage` | Server → Client | Trigger the client-side structure import UI |

## Login Handshake (Hello)

When a player enters the world the server immediately sends an empty `GuideNhServerHelloMessage` to that player:

```
Player logs in
  └── GuideNhNetworkEvents.onPlayerLoggedIn()
        ├── GuideStructureServerSessionStore.reset(playerId)   // clear previous session
        └── GuideNhNetwork.channel().sendTo(new GuideNhServerHelloMessage(), player)
              └── client GuideNhServerHelloHandler.onMessage()
                    ├── serverStructureCommandsAvailable = true   // unlock structure commands
                    └── clientStructureSyncNeeded = true          // trigger one sync pass
```

After the client receives the Hello, the "Place in World" button in the guide UI becomes enabled. **If the server does not have GuideNH installed, this message is never sent and the button stays disabled.**

On logout the server clears all cached data for that player:

```
Player logs out
  └── GuideNhNetworkEvents.onPlayerLoggedOut()
        └── GuideStructureServerSessionStore.clear(playerId)
```

## Structure Placement (Client → Server)

The `<GameScene>` block in a guide page allows exporting and placing the scene's structure into the real world. Full flow:

```
User clicks "Place Structure"
  └── client sends GuideNhStructureRequestMessage (ACTION_IMPORT_AND_PLACE)
        ├── structureText = SNBT text of scene blocks
        └── x, y, z = target coordinates

Server GuideNhStructureRequestHandler.onMessage()
  ├── Permission check: player.canCommandSenderUseCommand(3, "guidenh")  // requires OP level 3
  ├── Passes: GuideStructureServerSessionStore.remember(playerId, "client-import", snbt)
  │         └── GuideStructurePlacementService.parse(snbt) → GuideStructureData
  └── GuideStructurePlacementService.place(world, data, x, y, z)
        └── iterates SNBT palette + blocks table, calls world.setBlock() for each
              └── sends success/failure chat message to player
```

### Batch Placement

`ACTION_CACHE` and `ACTION_PLACE_ALL` allow caching multiple structures and placing them all at once along the X axis:

```
client (per structure) → ACTION_CACHE → server caches in GuideStructureMemoryStore (per player)
client → ACTION_PLACE_ALL + (x, y, z) → server iterates cache, places each structure offset +X
```

The server maintains a separate `GuideStructureMemoryStore` per player (stored in `ConcurrentHashMap<UUID, GuideStructureMemoryStore>`), which is automatically cleared on logout.

## Server Bridge Command (Server → Client)

`/guidenh` is a **server-side command** that lets server scripts or admins trigger the client-side import UI.

| Subcommand | Purpose |
|-----------|---------|
| `/guidenh importstructure <x> <y> <z>` | Server sends `GuideNhClientBridgeMessage` to the executing player; client shows the structure import dialog pre-filled with the given coordinates |
| `/guidenh placeallstructures <x> <y> <z>` | Server takes all structures from that player's session cache and places them at the given coordinates |

Flow:

```
Admin runs /guidenh importstructure ~ ~ ~
  └── GuideNhBridgeCommand → permission check (OP level 3)
        └── GuideNhNetwork.channel().sendTo(GuideNhClientBridgeMessage.importStructure(x,y,z), player)
              └── client GuideNhClientBridgeHandler
                    └── Minecraft.func_152344_a(task)  // schedule on main thread
                          └── GuideNhClientBridgeController.beginImportStructure(x, y, z)
```

## Permission Requirements

| Action | Required Permission Level |
|--------|--------------------------|
| Place structure (single) | OP level 3 |
| Place all structures | OP level 3 |
| `/guidenh importstructure` | OP level 3 |
| `/guidenh placeallstructures` | OP level 3 |
| View guides / search | None |

Singleplayer has OP by default. Multiplayer servers require a server admin to grant permissions.

## Singleplayer vs. Multiplayer

| Feature | Singleplayer (integrated server) | Client-only (no server GuideNH) |
|---------|----------------------------------|----------------------------------|
| View guide pages | ✅ Works | ✅ Works |
| Search | ✅ Works | ✅ Works |
| 3D scene preview | ✅ Works | ✅ Works |
| Place structure in world | ✅ Available (local server) | ❌ Button disabled (no Hello received) |
| `/guidenh` command | ✅ Available | ❌ Not registered |

## Related Pages

- [Installation](Installation)
- [GameScene](GameScene)
- [Getting Started](Getting-Started)
