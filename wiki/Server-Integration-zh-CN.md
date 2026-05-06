[English](Server-Integration)

# 服务器集成

本页说明 GuideNH 的客户端与服务端如何交互。理解这部分有助于诊断结构导入失败的权限问题、判断某功能在单人游戏与多人游戏下的差异，以及在自定义整合包中正确部署 GuideNH。

## 核心设计：指南内容纯客户端

**指南页面内容不经过网络传输。** 服务端无需安装 GuideNH，指南页面、图片和场景数据均从客户端本地的资源包（mod jar 或外部资源包）加载。

加载时序如下：

```
F3+T / 游戏启动
  └── GuideReloadListener.onResourceManagerReload()
        ├── 扫描全部资源包，发现 assets/<modid>/guidenh/_<lang>/ 下的 .md 文件
        ├── 调用 PageCompiler.parse() 编译每个页面
        ├── 写入 GuideRegistry（内存 Map）
        └── 触发 GuideSearch.indexAll()（增量 Lucene 索引，后台每 tick 5ms 推进）
```

## 网络消息总览

GuideNH 共注册了 **3 种** Forge SimpleNetworkWrapper 消息，频道名为 `guidenh`：

| ID | 消息类 | 方向 | 用途 |
|----|--------|------|------|
| 0 | `GuideNhServerHelloMessage` | 服务端 → 客户端 | 玩家登录时握手，告知客户端结构命令可用 |
| 1 | `GuideNhStructureRequestMessage` | 客户端 → 服务端 | 发送/缓存结构数据并请求服务端放置 |
| 2 | `GuideNhClientBridgeMessage` | 服务端 → 客户端 | 触发客户端结构导入 UI |

## 登录握手（Hello）

玩家进入世界时，服务端会立即向该玩家发送 `GuideNhServerHelloMessage`（消息体为空）：

```
玩家登录
  └── GuideNhNetworkEvents.onPlayerLoggedIn()
        ├── GuideStructureServerSessionStore.reset(playerId)   // 清空上次会话残留
        └── GuideNhNetwork.channel().sendTo(new GuideNhServerHelloMessage(), player)
              └── 客户端 GuideNhServerHelloHandler.onMessage()
                    ├── serverStructureCommandsAvailable = true   // 结构命令解锁
                    └── clientStructureSyncNeeded = true          // 触发一次同步
```

客户端收到 Hello 后，指南界面里的"在世界中放置"按钮才会变为可用状态。**若服务端未安装 GuideNH，此消息不会发出，放置按钮将保持禁用。**

玩家退出时服务端清理该玩家的所有缓存数据：

```
玩家退出
  └── GuideNhNetworkEvents.onPlayerLoggedOut()
        └── GuideStructureServerSessionStore.clear(playerId)
```

## 结构放置流程（客户端 → 服务端）

指南中的 `<GameScene>` 区块支持将场景内的结构"导出并放置"到真实世界中。完整流程如下：

```
用户点击"放置结构"
  └── 客户端发送 GuideNhStructureRequestMessage (ACTION_IMPORT_AND_PLACE)
        ├── structureText = SNBT 文本（场景中的方块数据）
        └── x, y, z = 目标坐标

服务端 GuideNhStructureRequestHandler.onMessage()
  ├── 权限检查：player.canCommandSenderUseCommand(3, "guidenh")  // 需要 OP 3 级
  ├── 通过：GuideStructureServerSessionStore.remember(playerId, "client-import", snbt)
  │         └── GuideStructurePlacementService.parse(snbt) → GuideStructureData
  └── GuideStructurePlacementService.place(world, data, x, y, z)
        └── 按 SNBT palette + blocks 表逐方块调用 world.setBlock()
              └── 发送成功/失败提示消息给玩家
```

### 多结构批量放置

`ACTION_CACHE` / `ACTION_PLACE_ALL` 支持把多个结构先缓存，再一次性沿 X 轴排列放置：

```
客户端（每个结构）→ ACTION_CACHE → 服务端缓存到 GuideStructureMemoryStore（每玩家独立）
客户端 → ACTION_PLACE_ALL + (x, y, z) → 服务端遍历缓存，依次沿 +X 排列放置
```

服务端对每个玩家单独维护一个 `GuideStructureMemoryStore`（存在 `ConcurrentHashMap<UUID, GuideStructureMemoryStore>` 中），玩家退出时自动清空。

## 服务端桥接命令（服务端 → 客户端）

`/guidenh` 是一条**服务端命令**，允许服务端脚本或管理员触发客户端的导入 UI。

| 子命令 | 功能 |
|--------|------|
| `/guidenh importstructure <x> <y> <z>` | 服务端向执行命令的玩家发送 `GuideNhClientBridgeMessage`，客户端弹出结构导入对话框并预填坐标 |
| `/guidenh placeallstructures <x> <y> <z>` | 服务端直接从该玩家的会话缓存中取出全部结构，批量放置到指定坐标 |

流程图：

```
管理员执行 /guidenh importstructure ~ ~ ~
  └── GuideNhBridgeCommand → 权限检查（OP 3 级）
        └── GuideNhNetwork.channel().sendTo(GuideNhClientBridgeMessage.importStructure(x, y, z), player)
              └── 客户端 GuideNhClientBridgeHandler
                    └── Minecraft.func_152344_a(task)  // 调度到主线程
                          └── GuideNhClientBridgeController.beginImportStructure(x, y, z)
```

## 权限要求

| 操作 | 所需权限等级 |
|------|------------|
| 放置结构（单个）| OP 3 级 |
| 批量放置结构 | OP 3 级 |
| `/guidenh importstructure` | OP 3 级 |
| `/guidenh placeallstructures` | OP 3 级 |
| 查看指南 / 搜索 | 无要求 |

单人游戏默认拥有 OP 权限，多人服务器需服务器管理员授权。

## 单人 vs 多人差异对照

| 功能 | 单人游戏（集成服务端）| 纯客户端（无服务端 GuideNH）|
|------|-------------------|-----------------------------|
| 查看指南页面 | ✅ 正常 | ✅ 正常 |
| 搜索 | ✅ 正常 | ✅ 正常 |
| 3D 场景预览 | ✅ 正常 | ✅ 正常 |
| 放置结构到世界 | ✅ 可用（本地服务端）| ❌ 按钮禁用（未收到 Hello）|
| `/guidenh` 命令 | ✅ 可用 | ❌ 不存在 |

## 相关页面

- [安装](Installation-zh-CN)
- [GameScene](GameScene-zh-CN)
- [快速开始](Getting-Started-zh-CN)
