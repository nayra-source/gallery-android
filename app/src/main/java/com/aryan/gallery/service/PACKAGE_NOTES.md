# service

Foreground service holding the WebSocket connection + remote command
dispatcher (`GalleryConnectionService`, `WebSocketClient`, `BootReceiver`).

## OEM Battery Optimization & Process Lifetime Limitation
Aggressive OEM task managers (common on Xiaomi/MIUI, Huawei, OnePlus/OxygenOS, and Samsung in deep sleeping apps mode) can and will kill background foreground services regardless of `START_STICKY`, `FOREGROUND_SERVICE_DATA_SYNC`, or battery optimization exemptions (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

There is no reliable, standard cross-OEM API to fully override vendor-specific task killers from within an app. We handle process lifetime through standard Android mechanisms (`START_STICKY`, `BOOT_COMPLETED`, and app launch restarts) and document this limitation explicitly rather than attempting brittle or unsafe workarounds.
