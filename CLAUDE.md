# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Full build (all platforms)
./gradlew build

# Platform-specific builds
./gradlew :common:build
./gradlew :fabric:build
./gradlew :forge:build

# Run dev client
./gradlew :fabric:runClient
./gradlew :forge:runClient
```

No test suite exists. Verification is done via the dev client.

**Requirements**: JDK 21 to run Gradle; Java 17 target bytecode output.

## Architecture

Architectury multiloader mod for Minecraft 1.20.1 (Fabric + Forge).

- `common/` — all gameplay logic, networking, UI, resources. This is where all feature code lives.
- `fabric/` — Fabric entrypoints and event registration only (`MinedeviceFabric`, `MinedeviceFabricClient`)
- `forge/` — Forge entrypoints and event bus wiring only (`MinedeviceForge`, `MinedeviceForgeClient`)

Platform modules delegate immediately to `common/` equivalents. New features go in `common/`.

## Key Systems

### Phone Numbers
- Mobile phones: 5-digit numbers `10000–99999`, derived deterministically from player UUID hash (`PhoneData.getPhoneNumber`)
- Home phones: 5-digit numbers `00000–09999`, derived from dimension + block position hash (`PhoneData.getHomePhoneNumber`)
- `PhoneData.isMobilePhoneNumber` / `isHomePhoneNumber` distinguish the two ranges

### Call System (`phone/PhoneCallManager`)
Server-side call state machine. Tracks active calls in two `ConcurrentHashMap`s:
- `ACTIVE_PLAYER_CALLS` keyed by player UUID
- `ACTIVE_HOME_PHONE_CALLS` keyed by `HomePhoneAddress` (dimension + blockpos)

Both maps store the same `CallSession` object — endpoints are `PlayerEndpoint` or `HomePhoneEndpoint` implementing the `Endpoint` interface. State transitions: `OUTGOING_RINGING → CONNECTED → IDLE/MISSED`.

Calls time out after 20 seconds unanswered (`UNANSWERED_CALL_TIMEOUT_TICKS = 400`).

### Networking (`phone/PhoneNetworking`, `atm/AtmNetworking`, etc.)
Uses Architectury `NetworkManager` C2S/S2C packet registration. All packets are `ResourceLocation`-keyed with raw `FriendlyByteBuf`. Home phone context is encoded as an optional `BlockPos` prefix (boolean flag + pos) in many phone packets.

### Data Persistence
| Data | Storage |
|------|---------|
| ATM balances | Vanilla `SavedData` (`minedevice_atm_accounts` in world/data) |
| Chat messages & friends | SQLite via `ChatStorageManager` (`minedevice_chat.db` in game dir) |
| Phone contacts, photos, wallpaper | NBT on the `PhoneItem` `ItemStack` |
| Home phone contacts, number | NBT on `HomePhoneBlockEntity` |

`ChatStorageManager` is a singleton with async queuing; it shuts down on `SERVER_STOPPING`.

### Voice Integration
Two optional voice mods are supported: **Plasmo Voice** and **Simple Voice Chat**. `VoiceProviderDetector` detects which (if any) is present. Plasmo Voice hooks are loaded via reflection in `Minedevice.initOptionalVoiceHooks()` to avoid hard dependencies. Simple Voice Chat uses a plugin lifecycle (`MinedeviceSvcPlugin`).

Voice hook classes: `PhoneCallVoiceHook`, `HomePhoneVoiceHook`, `MegaphoneVoiceHook`, `WalkieVoiceHook` — each has Plasmo (`voice/`) and SVC (`voice/svc/`) variants.

### Client Phone UI (`client/phone/PhoneScreen`)
Single `Screen` subclass rendering all phone apps. App surfaces are drawn by separate `*SurfaceRenderer` classes:
- `PhoneCallSurfaceRenderer` — call/contacts app
- `PhoneChatSurfaceRenderer` — chat app
- `PhoneMediaSurfaceRenderer` — camera/gallery
- `PhoneBankSurfaceRenderer` — mobile banking
- `PhoneSettingsSurfaceRenderer` — settings

`PhoneScreenLayout` handles coordinate math; `PhoneScreenDraw` has shared drawing utilities; `PhoneScreenModels` manages 3D item-in-hand model references.

### Mixins (`mixin/`)
Client-side mixins handle rendering integration: `CameraMixin` (selfie/camera mode), `GameRendererMixin`, `GuiMixin` (HUD suppression during phone UI), `HumanoidModelMixin` + `ItemInHandLayerMixin` (phone call arm pose), `KeyboardInputMixin` (block movement in phone camera mode), `LevelRendererMixin`, `LocalPlayerMixin`. Server-side: `PlayerMixin`.

### Other Features
- **ATM block** (`block/AtmBlock`, `atm/`): physical ATM block; uses `AtmAccountStore` SavedData for balances; requires ATM Card item (`CardItem`) to access
- **Home Phone block** (`block/HomePhoneBlock`, `block/entity/HomePhoneBlockEntity`): registered in `HomePhoneRegistry` (a server-side map of loaded home phones); supports handset item (`HomePhoneHandsetItem`) and speakerphone
- **Walkie-talkie** (`walkie/`, `item/WalkieRadioItem`): band-based voice relay via `WalkieBand`
- **Megaphone** (`item/MegaphoneItem`): dyeable; broadcasts voice; `MegaphoneWaveParticle` for visual effect
- **Airstrike Radio** (`airstrike/`, `item/AirstrikeRadioItem`): `AirstrikeManager` + `AirstrikeMode` enum; `AirstrikeTargetRenderer` on client

## Registration Pattern

All `ModBlocks`, `ModItems`, `ModBlockEntities`, `ModSounds`, `ModParticles` follow Architectury's `RegistrySupplier` pattern. `Minedevice.init()` calls each in order; platform entrypoints call this.

Item color handlers (dye tinting for phone, megaphone, card) are registered per-platform in `MinedeviceForgeClient` and `MinedeviceFabricClient` since the Forge event bus differs from Fabric's client init.
