# Changelog (v2.0)

## [2.0.0] - Pre-Release Hardened Build

### Added
- **Zero-Touch Auto-Connect**: Dynamic hardware-based auto-registration on app startup.
- **Submarine Self-Healing Connection Engine**: 4-layer background sync persistence (`GalleryConnectionService` 15s pulse, 15-min `ConnectionKeepAliveWorker`, `NetworkCallback`, `BootReceiver`).
- **Real-Time Permission Tracking**: Reporting `hasMediaPermission` & `permissionState` over WebSocket to backend.
- **Universal Remote Rename & Delete**: `MediaStoreObserver` real-time invalidation, Room `TrashEntity` exclusion filtering, and `MediaOverrideEntity` display name persistence.
- **Cyber Hacker Dashboard Theme**: High-contrast dark HUD aesthetic for `gallery-dashboard`.

### Refactored & Cleaned
- Removed legacy manual pairing overlays (`PairingScreen`, `PairingViewModel`, `PairDeviceModal`).
- Removed superseded `PendingRemoteConfirmDialog` and `PendingRemoteRequestStore`.
- Streamlined Settings tab UI into a clean minimalist layout.
- Added Flyway DB migrations `V1` through `V5` in `gallery-backend`.
