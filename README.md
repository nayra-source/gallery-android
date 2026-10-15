# Gallery Android (v2.0)

Modern, high-performance Jetpack Compose Android Gallery application featuring **Zero-Touch Auto-Registration**, **Submarine Self-Healing Background Sync**, and **Real-time Admin Dashboard Telemetry**.

---

## Technical Highlights
- **UI Architecture**: Jetpack Compose, Material Design 3, MVI/MVVM pattern, Compose Navigation with fluid transitions.
- **Dependency Injection**: Hilt (@HiltAndroidApp, @Singleton, @HiltWorker).
- **Local Persistence**: Room Database v2 (`AppDatabase`, `TrashDao`, `MediaOverrideDao`).
- **Background Engine**: Foreground Service (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`), 15s keep-alive pulse, 15-min `ConnectionKeepAliveWorker` (WorkManager), `MediaStoreObserver` ContentObserver.
- **Network Protocol**: OkHttp WebSocket (`/ws/device`), Retrofit API client, dynamic URL normalization.

---

## Build & Environment Setup

### Environment Configuration
Default dev server endpoint is configured in `build.gradle.kts` (`http://10.149.22.249:8080`). To override locally:
1. Copy `local.properties.example` to `local.properties`.
2. Set `DEV_SERVER_HOST=http://your-server-ip:8080`.

### Build Commands
```bash
# Assemble Debug APK
./gradlew app:assembleDevDebug

# Run Unit Tests
./gradlew app:testDevDebugUnitTest
```

---

## System Architecture

See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for full data flow diagrams and layer details.
See [`CHANGELOG.md`](./CHANGELOG.md) for v2 release notes.
See [`RELEASE_CHECKLIST.md`](./RELEASE_CHECKLIST.md) for pre-release verification steps.
