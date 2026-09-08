# 🛡️ mAntiESP

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Minecraft-1.16.5%20--%201.21.x-568840?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.16.5 - 1.21.x" />
  <img src="https://img.shields.io/badge/PacketEvents-2.13.0-007ACC?style=for-the-badge" alt="PacketEvents" />
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-black?style=for-the-badge" alt="Paper / Purpur" />
</p>

A blazing-fast, packet-level Anti-ESP solution for modern Minecraft servers. Built on top of **PacketEvents** and custom 3D voxel DDA raytracing with bit-compressed chunk occlusion caching, **mAntiESP** prevents cheat clients (Entity ESP, Tracers, Box ESP) from rendering concealed players and entities behind walls without inducing server lag or client-side visual artifacts.

---

## **⚡ Fast 3D DDA Raytracing**: Powered by a zero-heap voxel traversal algorithm (Amanatides & Woo), minimizing JVM garbage collection overhead during intensive raycasting checks.
- **🎥 Third-Person Camera (F5) Support**: Accurately simulates vanilla Minecraft camera raytracing for both behind and front (selfie) views. Traces camera rays against occluding blocks and clips camera distance to prevent cheating while ensuring targets in legitimate third-person view remain visible.
- **💾 Bit-Compressed Chunk Occlusion Cache**: Stores block transparency states in compact bitsets (~1 bit per block), avoiding expensive Bukkit/NMS chunk and block state lookups. Cache updates dynamically on block changes and chunk loading.
- **🔍 Case-Insensitive Pattern & Keyword Block Matching**: Easily declare transparent materials by group keywords (`DOOR`, `FENCE`, `BUTTON`, `LEAVES`, `GLASS`, `SIGN`) or wildcards (`*GLASS*`, `OAK_*`) without listing dozens of individual material variations.
- **🎯 Dynamic Multi-Point Silhouette Sampling**:
  - Casts rays toward multiple anatomical points (feet, mid-torso, head).
  - Computes dynamic horizontal tangent offsets perpendicular to the observer's viewing angle to spot players peeking around corners.
  - Supports configurable 3-axis hitbox expansion (`X`, `Y`, `Z`).
- **🏃 Movement Prediction & Extrapolation**: Tracks player velocity and inter-tick displacement, extrapolating future positions to eliminate peek delay and visual pop-in when players sprint around corners.
- **📦 Pre-Spawn Packet Interception**: Inspects `SPAWN_ENTITY` and `SPAWN_PLAYER` packets before they reach the network pipeline. Concealed entities behind blocks are dropped instantly, preventing cheats from catching players even for a single tick.
- **🏷️ Vanilla Nametag & Armor Stripping**:
  - Respects vanilla nametag visibility: standing players whose nametags are naturally visible through walls are not despawned.
  - When concealed behind walls with `ignore-nametag: false`, the entity remains alive to show the nametag, but all armor, weapons, and equipment packets are sanitized and stripped (`ItemStack.EMPTY`), preventing cheat clients from seeing equipment or inventory.
  - Full equipment and metadata are seamlessly restored the moment the player emerges into direct line of sight.
  - Automatically completely hides sneaking (crouched), invisible, or spectator players behind obstacles.
- **🌫️ Environmental Occlusion**:
  - Hides targets when the observer is impaired by Blindness fog (`> 5 blocks`).
  - Hides targets when either player is immersed in Lava (`> 5 blocks`).
  - Glowing entities bypass occlusion checks or follow custom rules.
- **⚙️ Hot Reloading**: Update settings on the fly with `/mantiesp reload` with full safe entity restoration and cache rebuild.

---

## 🛠️ How It Works

```
                     [ Observer Player ]
                              |
              +---------------+---------------+
              |                               |
     (1st Person Eye Point)          (F5 Camera Raycasts)
              |                               |
       [ Direct Line ]               [ Behind & Front View ]
              |                               |
     (3D DDA FastRaytracer)          (Camera Block Clipping)
              |                               |
   [ Bitwise Chunk Occlusion ]      [ Predicted Tangents ]
              |                               |
              +---------------+---------------+
                              |
                     Line of Sight Clear?
                    /                    \
                [YES]                    [NO]
                  |                        |
         Show Entity Packet       Send Destroy Entity Packet
```

1. **Chunk Caching**: Whenever a chunk loads, solid blocks are mapped into 1-bit flags. Air, glass, bars, and other configured transparent materials are treated as non-blocking.
2. **Periodic Check Task**: Every `ticks-period` ticks, the server scans nearby entities within `max-distance + F5 distance`.
3. **Early Exits**:
   - Entities closer than `min-distance` are always visible to prevent close-range pop-in.
   - Entities farther than `max-distance` are immediately hidden.
   - Bypass permissions and spectator modes are evaluated first.
4. **Raycasting**: Multiple rays are checked from first-person eye and F5 camera positions against the chunk cache. If all points are occluded, movement prediction checks if either player's velocity will reveal them in upcoming frames.ity will reveal them in upcoming frames.

---

## ⚙️ Configuration (`config.yml`)

```yaml
# Interval (in server ticks) between visibility checks
# 1 tick = 50ms. Lower values increase responsiveness, higher values reduce CPU usage
ticks-period: 3

# Whether to conceal only players or include other entities
only-player: true

# Active only when "only-player: false"
# Specify 'all' or list individual EntityType names
entities-list:
  - "player"
  - "villager"

# Third-person perspective (F5) camera support
# Simulates player camera behind and in front of the player
f5:
  enabled: true
  # Camera distance in blocks (vanilla Minecraft is 4.0)
  distance: 4.0
  # Collision offset from solid blocks (in blocks)
  collision-offset: 0.1
  # Check front (selfie) camera view
  front-view: true

# Distance thresholds (in blocks)
min-distance: 3.0   # Raytracing begins beyond this radius (closer targets stay visible)
max-distance: 64.0  # Targets beyond this radius are hidden automatically

# Worlds where mAntiESP is disabled (e.g. spawn lobbies, minigame hubs)
disabled-worlds:
  - "example_world"

# Block materials considered transparent to raycasts
# Supports exact names (ICE, BARRIER), keywords (DOOR, FENCE, LEAVES, BUTTON, GLASS), or wildcards (*GLASS*)
# Case-insensitive (e.g. door, Door, DOOR)
transparent-blocks:
  - "GLASS"
  - "PANE"
  - "LEAVES"
  - "FENCE"
  - "GATE"
  - "BUTTON"
  - "PRESSURE_PLATE"
  - "DOOR"
  - "TRAPDOOR"
  - "BARS"
  - "CHAIN"
  - "ROD"
  - "ICE"
  - "BARRIER"
  - "STRUCTURE_VOID"
  - "LIGHT"
  - "SCAFFOLDING"
  - "COBWEB"
  - "SLIME_BLOCK"
  - "HONEY_BLOCK"
  - "TORCH"
  - "LANTERN"
  - "CAMPFIRE"
  - "CANDLE"
  - "SIGN"
  - "BANNER"
  - "GRASS"
  - "FERN"
  - "FLOWER"
  - "SAPLING"
  - "VINE"
  - "MUSHROOM"
  - "FUNGUS"
  - "ROOTS"
  - "SPROUTS"
  - "DRIPLEAF"
  - "SEAGRASS"
  - "KELP"
  - "WHEAT"
  - "CARROTS"
  - "POTATOES"
  - "BEETROOTS"
  - "NETHER_WART"
  - "SWEET_BERRY_BUSH"
  - "SUGAR_CANE"
  - "BAMBOO"
  - "CACTUS"
  - "LILY_PAD"
  - "RAIL"
  - "REDSTONE_WIRE"
  - "REPEATER"
  - "COMPARATOR"
  - "LEVER"
  - "TRIPWIRE"
  - "DAYLIGHT_DETECTOR"
  - "BELL"
  - "ENCHANTING_TABLE"
  - "BREWING_STAND"
  - "CAULDRON"
  - "LECTERN"
  - "GRINDSTONE"
  - "STONECUTTER"
  - "AMETHYST"
  - "DRIPSTONE"
  - "POT"
  - "HEAD"
  - "SKULL"
  - "BED"
  - "CARPET"

# Fine-grained hiding options
hide:
  # When true: hides players behind walls regardless of nametag visibility
  # When false: players with visible nametags are not hidden (vanilla behavior)
  ignore-nametag: false

  # Allow spectators to bypass hiding
  ignore-spectator: true

  # Do not hide glowing entities (Spectral Arrow, Glowing effect)
  ignore-glowing: false

  # Hide entities when observer has Blindness
  blindness: true
  blindness-distance: 5.0

  # Hide armor & equipment when either party is submerged in lava
  in-lava: true
  lava-distance: 5.0

# Hitbox expansion along X, Y, Z axes (in blocks)
# Helps detect players slightly peeking out of corners or tiny gaps
hitbox-expansion:
  x: 0.25
  y: 0.2
  z: 0.25

# Movement extrapolation & prediction
# Anticipates player trajectories to prevent pop-in when sprinting around walls
movement-prediction:
  enabled: true
  # Multiplier applied to movement velocity (in ticks)
  multiplier: 1.5
```

---

## 💻 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/mantiesp reload` |  `mantiesp.admin` | Performs a clean reload cycle: restores all entities, reloads `config.yml`, reinitializes listeners, and rebuilds occlusion caches |

### Additional Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `mantiesp.bypass` | `op` | Players with this permission are never hidden and can see all entities through walls |

---

## 🏗️ Project Architecture

The codebase follows clean package separation and strict object-oriented design principles:

```
dev.murk.antiesp
├── MAntiESP.java                     # Main plugin lifecycle (enable/disable/reload)
├── cache
│   ├── ChunkCacheManager.java        # Multi-world occlusion bitset storage & cache invalidation
│   ├── ChunkOcclusion.java           # 3D bitset chunk section representation (~1 bit per block)
│   └── MaterialClassifier.java       # Fast O(1) block opacity and transparency lookup
├── command
│   └── AntiESPCommand.java           # Administrative command handling & tab completion
├── config
│   └── Config.java                   # Typed configuration model & parsing
├── listener
│   ├── BlockEventListener.java       # Real-time cache updates on block break/place/burn
│   ├── ChunkEventListener.java       # Asynchronous chunk snapshot processing & cache eviction
│   └── VisibilityListener.java       # Entity tracking, periodic check task & player lifecycle
├── packet
│   ├── PacketCancelListener.java     # Netty packet pipeline interception (PacketEvents)
│   └── PacketSender.java             # Packet construction (spawn, teleport, equipment, metadata)
├── raytrace
│   └── FastRaytracer.java            # Zero-allocation 3D DDA (Digital Differential Analyzer) raytracer
└── visibility
    ├── VisibilityManager.java        # Entity state machine (Visible / Stripped / Occluded)
    └── VisibilityService.java        # Line-of-sight orchestration, hitboxes & velocity prediction
```

---

## 💡 Author Note & AI Collaboration

> *"Using modern tools responsibly is part of being an effective, pragmatic engineer."*

This project was conceived, architected, and primarily developed by the repository author. An AI coding assistant (LLM) was utilized as an interactive engineering copilot for:
- Pair programming and iterative brainstorming.
- Researching protocol nuances in modern Minecraft / PacketEvents versions.
- Debugging edge cases (e.g., Netty thread-safety constraints, slab/stair raycast occlusion).
- Code review, packaging restructuring, and documentation polishing.

All core design decisions, business logic, algorithmic choices, test suites, and final implementations were directed, reviewed, and verified by the developer.

---

## 🔨 Building from Source

### Prerequisites
- **JDK 17** or higher
- **Maven 3.8+**

### Compile & Package
```bash
git clone https://github.com/<your-username>/mAntiESP.git
cd mAntiESP
mvn clean package
```

The compiled and shaded jar will be generated inside `target/mAntiESP-1.0.jar`.

---

## 📋 Requirements

- **Paper**, **Purpur**, or compatible fork (1.16.5 – 1.21.x)
- **PacketEvents 2.13.0+** installed as a plugin or dependency

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
