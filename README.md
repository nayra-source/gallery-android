# Gallery Android Application

`gallery-android` is a native, modern Android application engineered in Kotlin. It functions both as a rich local gallery viewer and as an active remote sync node within the Gallery ecosystem. It integrates with Android's `MediaStore` system framework, maintains a secure, persistent WebSocket connection to `gallery-backend`, responds to remote admin commands, and stages media transfers.

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture & Directory Structure](#2-architecture--directory-structure)
3. [How the Application Works](#3-how-the-application-works)
4. [Prerequisites & Build Setup](#4-prerequisites--build-setup)
5. [Emulator Testing Guide](#5-emulator-testing-guide)
6. [Physical Android Phone Testing Guide](#6-physical-android-phone-testing-guide)
7. [Test Matrix & Acceptance Criteria](#7-test-matrix--acceptance-criteria)
8. [Troubleshooting](#8-troubleshooting)
9. [Security, Privacy & Data Handling](#9-security-privacy--data-handling)
10. [Development & Debugging](#10-development--debugging)
11. [Limitations & Future Roadmap](#11-limitations--future-roadmap)

---

## 1. Project Overview

### Purpose
The application provides users with an intuitive Jetpack Compose gallery interface while running a foreground sync service (`GalleryConnectionService`) that enables authorized remote administrators (`gallery-dashboard`) to inspect photo/video metadata, generate thumbnails, request full media downloads, rename items, and manage a local Room trash system.

### SDK & Version Targets
* **Compile SDK**: `37` (Android 15/16 ready)
* **Target SDK**: `35` (Android 15)
* **Min SDK**: `33` (Android 13) — Ensures full compatibility with modern granular media permissions.
* **Java / Kotlin Target**: Java 17, Kotlin `2.0.20+`

### Technology Stack
* **UI**: Jetpack Compose, Material Design 3, Compose Navigation
* **Dependency Injection**: Hilt (`dagger.hilt.android`)
* **Asynchronous Execution**: Kotlin Coroutines & Flow
* **Local Storage & Database**: Room (`AppDatabase`), AndroidX Security Crypto (`EncryptedSharedPreferences`)
* **Networking**: Retrofit 2, OkHttp 4 (WebSocket & HTTP Logging Interceptor), Kotlinx Serialization
* **Image & Video Thumbnail Decoding**: Coil Compose & Coil Video (`io.coil-kt:coil-video`)
* **Video Playback**: AndroidX Media3 ExoPlayer (`androidx.media3:media3-exoplayer`)
* **Background Tasks**: AndroidX WorkManager (`PeriodicWorkRequestBuilder`)

---

## 2. Architecture & Directory Structure

The codebase adheres to Android Clean Architecture with strict layer separation:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                               Presentation                              │
│         (Jetpack Compose Screens, ViewModels, Compose Navigation)       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           ▼                                                   ▼
┌──────────────────────────┐                        ┌─────────────────────┐
│    Local Media & Trash   │                        │   Foreground Sync   │
│ (MediaStore, Room, Work) │                        │ (ConnectionService) │
└──────────┬───────────────┘                        └──────────┬──────────┘
           │                                                   │
           │                                                   ▼
           │                                        ┌─────────────────────┐
           │                                        │   WebSocketClient   │
           │                                        └──────────┬──────────┘
           │                                                   │
           ▼                                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          CommandDispatcher                              │
│     (ListMedia, GetThumbnail, GetFullMedia, DeleteMedia, RenameMedia)   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Important Package Hierarchy

```
app/src/main/java/com/aryan/gallery/
├── GalleryApplication.kt               # Application entrypoint & WorkManager / Service initialization
├── MainActivity.kt                     # Single-activity shell holding Compose GalleryNavigation
├── data/
│   ├── local/
│   │   ├── RemoteAccessPreferences.kt  # User preferences for remote access toggles
│   │   └── TokenStore.kt               # EncryptedSharedPreferences for device JWT persistence
│   ├── media/
│   │   ├── Album.kt & MediaItem.kt     # Local domain data models
│   │   ├── MediaPagingSource.kt        # Jetpack Paging 3 source for MediaStore queries
│   │   ├── MediaRepository.kt          # Abstraction layer over MediaStore
│   │   └── MediaStoreDataSource.kt     # ContentResolver engine querying MediaStore
│   ├── remote/
│   │   ├── AuthEventBus.kt             # Event bus signaling 401 unauthorized / token revocation
│   │   ├── PairingApi.kt & Repo.kt     # Retrofit client for /api/pairing/exchange
│   │   └── TransferApi.kt              # Retrofit client for staging uploads (/api/transfers/:id/upload)
│   └── trash/
│       ├── AppDatabase.kt              # Room database definition
│       ├── AutoPurgeWorker.kt          # Daily WorkManager job purging trash items > 30 days
│       ├── TrashDao.kt & Entity.kt     # Room DAO and entity models
│       └── TrashRepository.kt          # Handles local trash, restore, and delete operations
├── di/
│   ├── DatabaseModule.kt               # Hilt provider for Room AppDatabase & TrashDao
│   └── NetworkModule.kt                # Hilt providers for OkHttp, Retrofit, and WebSockets
├── service/
│   ├── BootReceiver.kt                 # BroadcastReceiver restarting service on device boot
│   ├── CommandDispatcher.kt            # Routes incoming WebSocket commands to handlers
│   ├── GalleryConnectionService.kt     # Foreground service maintaining persistent WebSocket
│   ├── NotificationHelper.kt           # Builds persistent notification channel & controls
│   ├── WebSocketClient.kt              # OkHttp WebSocket engine with backoff & NetworkCallback
│   └── handlers/
│       ├── DeleteMediaHandler.kt       # Handles DELETE_MEDIA remote commands
│       ├── GetFullMediaHandler.kt      # Handles GET_FULL_MEDIA remote upload commands
│       ├── GetThumbnailHandler.kt      # Handles GET_THUMBNAIL remote base64 thumbnail commands
│       ├── ListMediaHandler.kt         # Handles LIST_MEDIA remote indexing commands
│       └── RenameMediaHandler.kt       # Handles RENAME_MEDIA remote title edit commands
└── ui/
    ├── album/                          # Album grid & album detail screens
    ├── gallery/                        # Main media grid, selection state, and ViewModel
    ├── navigation/                     # NavHost & bottom navigation bar setup
    ├── pairing/                        # 6-digit code entry UI & pairing flow
    ├── permissions/                    # Granular Android 13/14+ permission screens & dialogs
    ├── settings/                       # Remote access toggles, connection status, & server settings
    ├── trash/                          # Local trash bin management UI
    └── viewer/                         # Full-screen image/video player powered by ExoPlayer
```

---

## 3. How the Application Works

### A. Application Startup & Lifecycle
1. **`GalleryApplication.onCreate()`**: Initializes Hilt graph, schedules `AutoPurgeWorker` with WorkManager (runs every 24 hours), and checks `TokenStore.isPaired()`.
2. **Foreground Service Launch**: If paired and remote access is enabled in `RemoteAccessPreferences`, launches `GalleryConnectionService` in the foreground with notification type `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.

### B. MediaStore Discovery & Granular Permissions
* **Permissions Framework (`PermissionUtils`)**:
  * **Android 13 (SDK 33)**: Requests `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`.
  * **Android 14+ (SDK 34+)**: Supports `READ_MEDIA_VISUAL_USER_SELECTED`. If the user selects "Select photos and videos" (Partial Access), the app detects partial permission state and displays a banner allowing the user to modify their selection.
* **Query Engine (`MediaStoreDataSource`)**: Uses `ContentResolver` to query `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` sorted by `DATE_TAKEN DESC`.

### C. Authentication & Pairing Flow
1. User obtains a 6-digit code from `gallery-dashboard`.
2. User enters the code in `PairingScreen` (or scans a QR code).
3. `PairingRepository.exchangeCode()` posts to `POST /api/pairing/exchange`.
4. On success, the backend returns a long-lived `deviceToken` and `deviceId`.
5. `TokenStore` securely saves the token in `EncryptedSharedPreferences`.
6. `GalleryConnectionService` is immediately started.

### D. WebSocket Connection & Remote Command Execution
1. **`WebSocketClient.connect()`**: Connects OkHttp WebSocket to `${BuildConfig.WS_URL}?token=$token`.
2. **Exponential Backoff**: If disconnected, retries reconnection starting at 1s, doubling up to 60s.
3. **Network Sensitivity**: Listens to `ConnectivityManager.NetworkCallback`. When internet becomes available, immediately triggers reconnect.
4. **Command Processing**:
   * Incoming frame arrives -> parsed as `DeviceCommandEnvelope`.
   * Handled by `CommandDispatcher`:
     * `LIST_MEDIA`: Queries `MediaStoreDataSource` and returns JSON list of items.
     * `GET_THUMBNAIL`: Loads image/video thumbnail, scales down, encodes to Base64, and returns to backend.
     * `GET_FULL_MEDIA`: Uploads full binary file to `/api/transfers/{id}/upload` via `TransferApi`.
     * `DELETE_MEDIA`: Moves item to local Room trash database (`TrashRepository`).
     * `RENAME_MEDIA`: Updates `DISPLAY_NAME` in `MediaStore`.
   * Formats result as `DeviceCommandResultEnvelope` and sends back over WebSocket frame.
5. **Token Revocation Handling**: If backend returns `401` or `invalid_token`/`revoked` close frame, `WebSocketClient` triggers `AuthEventBus.emitUnauthorized()`, clears `TokenStore`, and stops the service.

### E. Trash & Auto-Purge System
When an item is deleted (locally or remotely via `DELETE_MEDIA`), it is added to the Room database (`TrashEntity`) with `deletedAt` timestamp. `AutoPurgeWorker` executes once daily via WorkManager, deleting any trashed items older than 30 days.

---

## 4. Prerequisites & Build Setup

### System Prerequisites
* **Android Studio**: Android Studio Ladybug (2024.2.1+) or newer.
* **JDK**: JDK 17 (configured under **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK**).
* **Android SDK**: SDK 37 installed via SDK Manager.

### Environment Product Flavors
Configured in `app/build.gradle.kts`:

| Flavor | Target Server URL | Usage |
|---|---|---|
| **`dev`** | `http://10.84.187.249:8080` (or host IP) | Local development on emulator or LAN physical phone |
| **`staging`** | `https://staging-api.your-domain.com` | Staging QA environment |
| **`prod`** | `https://api.your-domain.com` | Production release |

### Building the Project
Select build variant in Android Studio (**Build Variants** panel) e.g., `devDebug`:
```bash
./gradlew assembleDevDebug
```

---

## 5. Emulator Testing Guide

Follow this step-by-step procedure to test the Android application on an Android Studio Emulator:

### Step 1: Launch Backend
Ensure `gallery-backend` and PostgreSQL are running:
```bash
cd gallery-backend
./gradlew run
```

### Step 2: Create Emulator
1. Open **Tools -> Device Manager** in Android Studio.
2. Click **Create Device** -> Select **Pixel 8 Pro** -> Next.
3. Select System Image **API 34 (Android 14)** or **API 35 (Android 15)** -> Finish.
4. Launch the emulator.

### Step 3: Configure Network Alias
* On standard Android emulators, `10.0.2.2` maps directly to your host computer's `localhost`.
* Update `app/build.gradle.kts` `dev` flavor `BASE_URL` to `"http://10.0.2.2:8080"` and `WS_URL` to `"ws://10.0.2.2:8080/ws/device"`.

### Step 4: Add Sample Media
Drag and drop `.jpg` images and `.mp4` videos directly onto the emulator window to populate the emulator's `MediaStore`.

### Step 5: Install & Launch
1. Select variant `devDebug` in Android Studio.
2. Click **Run 'app'** (`Shift + F10`).
3. App launches and presents `PermissionsScreen`.

### Step 6: Test Permissions
* Click **Grant Permissions**. Select **Allow All**.
* Verify local gallery populates with the sample photos/videos.

### Step 7: Test Pairing & Sync
1. Open `gallery-dashboard` (`http://localhost:5173`).
2. Click **Pair New Device**. Note the 6-digit code (e.g. `920135`).
3. In Android app, navigate to **Settings -> Pair Device**.
4. Enter `920135` and tap **Pair Device**.
5. Observe notification banner: `Gallery Sync Active`.
6. Dashboard will now reflect device status as **Online**.

---

## 6. Physical Android Phone Testing Guide

Follow this procedure to test on a physical Android phone over Wi-Fi:

### Step 1: Enable Developer Options & USB Debugging
1. On phone, go to **Settings -> About Phone**.
2. Tap **Build Number** 7 times until "You are now a developer!" appears.
3. Go to **Settings -> System -> Developer Options**.
4. Enable **USB Debugging** (and optionally **Wireless Debugging**).

### Step 2: Determine Computer LAN IP Address
Find your development computer's local Wi-Fi IP address:
* **macOS / Linux**:
  ```bash
  ipconfig getifaddr en0 || ip a
  ```
  _Example output: `192.168.1.150`_
* **Windows**:
  ```cmd
  ipconfig
  ```

> [!CAUTION]
> Do NOT use `localhost` or `127.0.0.1` when testing on a physical phone! On a phone, `127.0.0.1` refers to the phone itself, causing connection refused errors.

### Step 3: Configure Android App Base URL
Update `app/build.gradle.kts` `dev` flavor with your computer's LAN IP:
```kotlin
create("dev") {
    dimension = "environment"
    buildConfigField("String", "BASE_URL", "\"http://192.168.1.150:8080\"")
    buildConfigField("String", "WS_URL", "\"ws://192.168.1.150:8080/ws/device\"")
}
```

### Step 4: Verify Firewall & Network
1. Ensure phone and computer are connected to the **same Wi-Fi network**.
2. Verify computer firewall permits incoming connections on port `8080`.
3. Open Chrome on the phone and visit `http://192.168.1.150:8080/health`.
4. Confirm response: `{"status":"ok"}`.

### Step 5: Install & Test
1. Connect phone via USB cable to computer.
2. In Android Studio, select your physical phone in the device dropdown.
3. Click **Run 'app'**.
4. Complete pairing flow using the LAN-accessible dashboard.

---

## 7. Test Matrix & Acceptance Criteria

| Test ID | Feature | Test Environment | Preconditions | Execution Steps | Expected Result | Status |
|---|---|---|---|---|---|---|
| **TC-01** | Initial Permission Request | Emulator / Physical | Fresh app install | Launch app | App displays permission rationale screen | Not Run |
| **TC-02** | Full Permission Granted | Emulator / Physical | Permission prompt shown | Tap "Allow All" | Local photos/videos indexed and displayed in Compose grid | Not Run |
| **TC-03** | Partial Media Access | Physical (Android 14+) | Android 14+ device | Select "Select photos/videos" | App displays "Partial Access" banner; only selected items show | Not Run |
| **TC-04** | Device Pairing | Emulator / Physical | Backend running | Enter 6-digit code from dashboard | Device pairs; token saved in EncryptedSharedPreferences | Not Run |
| **TC-05** | Persistent Sync Service | Emulator / Physical | App paired | Check notification shade | Foreground notification "Gallery Sync Active" visible | Not Run |
| **TC-06** | Remote Indexing (`LIST_MEDIA`) | Emulator / Physical | Device online | Trigger view gallery in dashboard | Dashboard populates with phone's remote photos/videos | Not Run |
| **TC-07** | Remote File Transfer (`GET_FULL_MEDIA`) | Emulator / Physical | Device online | Click "Download" in dashboard | App streams binary payload to `/api/transfers/:id/upload` | Not Run |
| **TC-08** | Remote Deletion (`DELETE_MEDIA`) | Emulator / Physical | Device online | Click "Delete" in dashboard | Item moves to Room trash DB; auto-purges after 30 days | Not Run |
| **TC-09** | Device Revocation | Emulator / Physical | Device online | Click "Revoke" in dashboard | Phone receives 401; clears tokens; service stops | Not Run |
| **TC-10** | Wi-Fi Reconnection | Physical Device | App paired | Toggle phone Wi-Fi off and on | `WebSocketClient` automatically reconnects upon network restore | Not Run |

---

## 8. Troubleshooting

### 1. Connection Refused (`java.net.ConnectException`)
* **Cause**: App is pointing to `localhost` or an unreachable IP address.
* **Solution**: For emulator, use `10.0.2.2:8080`. For physical phone, use computer's LAN IP (`192.168.1.x:8080`) and confirm host firewall allows port 8080.

### 2. WebSocket Disconnects Immediately
* **Cause**: Invalid or revoked device JWT token.
* **Solution**: Check Logcat for `Auth failure detected on WebSocket`. Unpair device in app settings and perform fresh pairing.

### 3. Media Items Not Appearing
* **Cause**: `READ_MEDIA_IMAGES` or `READ_MEDIA_VIDEO` permissions denied.
* **Solution**: Go to phone **Settings -> Apps -> Gallery -> Permissions -> Photos and Videos -> Allow**.

---

## 9. Security, Privacy & Data Handling

* **Token Storage**: Credentials and device tokens are stored in `EncryptedSharedPreferences` backed by Android KeyStore.
* **Cleartext Traffic**: Cleartext HTTP (`http://`) is permitted only for local development (`10.0.2.2` / LAN IPs). Production builds enforce HTTPS (`https://`) and WSS (`wss://`).
* **Header Sanitization**: Tokens and sensitive headers are excluded from release log output.

---

## 10. Development & Debugging

### Useful Logcat Filter Tags
Filter Logcat in Android Studio using:
```
package:mine tag:GalleryWebSocket | tag:GalleryService | tag:MediaStoreDS
```

### Running Unit Tests
Execute local JVM tests:
```bash
./gradlew test
```

---

## 11. Limitations & Future Roadmap

* **Direct P2P Transfer**: Current file transfers relay through server staging. Future iterations may support direct WebRTC / Local Wi-Fi Direct transfers.
* **Background Auto-Sync**: Background indexing executes on demand via WebSocket triggers.
