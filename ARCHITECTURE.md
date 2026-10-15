# Gallery System Architecture (v2.0)

## 1. System Topology Diagram

```
+-----------------------------------------------------------------------------------+
| 1. gallery-dashboard (React / Vite Admin Control Panel)                           |
|    - Device Fleet Management, Live Status Indicators, Remote Media Operations    |
+-----------------------------------------------------------------------------------+
                                         |
                                         | HTTP REST API & WebSockets
                                         v
+-----------------------------------------------------------------------------------+
| 2. gallery-backend (Ktor 2.3 + Exposed ORM + PostgreSQL 18)                       |
|    - Device Registration, Command Queue, Auth JWT, WebSocket Broker              |
+-----------------------------------------------------------------------------------+
                                         |
                                         | WebSocket Persistent Sync Channel
                                         v
+-----------------------------------------------------------------------------------+
| 3. gallery-android (Kotlin + Jetpack Compose + Hilt DI + Room v2)                |
|    - MediaStore ContentObserver, Paging 3, Submarine Self-Healing Service         |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core v2 Subsystems

### A. Submarine Self-Healing Background Sync Engine
- **Layer 1 (Keep-Alive Pulse)**: 15-second background ticker in `GalleryConnectionService.kt` checks WebSocket status and auto-reconnects.
- **Layer 2 (WorkManager Safety Net)**: `ConnectionKeepAliveWorker.kt` (15 min periodic worker) recovers service execution if terminated by Android OS under low memory or Doze mode.
- **Layer 3 (Network restoration trigger)**: `NetworkCallback` instantly reconnects upon network restoration (Wi-Fi <-> Mobile Data).
- **Layer 4 (Boot & Package Replaced)**: `BootReceiver.kt` handles `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.

### B. Universal Remote Media Modification Engine
- **MediaStore ContentObserver**: `MediaStoreObserver.kt` listens to `MediaStore.Images` and `MediaStore.Video` mutations and notifies `MediaPagingSource` to invalidate Paging 3 caches instantly.
- **App-Level Trashing**: `DeleteMediaHandler.kt` records deleted media in Room `TrashEntity` and excludes trashed IDs in `MediaStoreDataSource.kt`.
- **Display Name Override Entity**: `RenameMediaHandler.kt` records custom display names in `MediaOverrideEntity` and substitutes overridden names in `MediaStoreDataSource.kt`.
