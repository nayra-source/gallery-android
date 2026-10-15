# Android Project Audit & Architectural Refactoring Report

## 1. Executive Summary
A comprehensive security, architecture, quality, runtime, and technical debt audit was conducted across the `gallery-android` repository alongside its integrated modules (`gallery-backend` and `gallery-dashboard`).

Overall, the project demonstrates a clean Jetpack Compose Clean Architecture with Hilt dependency injection, Room metadata tracking, and OkHttp/Retrofit network streaming for local MediaStore and remote sync functionality. Before this audit and refactoring, the app contained runtime bugs (e.g., date calculation corruption in media viewer, ContentResolver disk I/O on the UI thread), hardcoded configurations (e.g., magic numbers for timeouts, backoff intervals, thumbnail sizes, notification IDs, retention thresholds), raw inline strings in Composables, and hardcoded development IPs in build scripts.

All identified vulnerabilities, logic flaws, hardcoded magic values, inline strings, and build configurations have been completely eliminated. The Android application builds cleanly (`app:assembleDevDebug`), and 100% of unit tests pass (`35/35 passed`).

---

## 2. Technical Debt & Refactoring Matrix

| Refactoring Area | Before (Hardcoded / Technical Debt) | After (Centralized Architecture) |
|---|---|---|
| **Environment URLs & IPs** | Private IPs hardcoded in `build.gradle.kts` (`http://10.84.187.249`) | Dynamic `local.properties` / ENV variables in `build.gradle.kts` with `local.properties.example` template & clean fallback |
| **URL Normalization** | Ad-hoc string manipulation e.g., `baseUrl.removePrefix(...)` in WebSocket client | Centralized `UrlNormalizer` class handling schemes, port ranges, hostnames, and WebSocket URI generation |
| **Network & WS Parameters** | Hardcoded timeouts, `1000L`/`60000L` backoff, `30s` ping interval, `8192` buffer size | Injected `@Singleton` `NetworkConfig` & `WebSocketConfig` models provided via Hilt |
| **Media & UI Dimensions** | Hardcoded thumbnail sizes (`Size(300, 300)`), paging limit (`50`, `60`), prefetch (`20`) | Injected `@Singleton` `MediaConfig` and `Dimens.kt` Material 3 spacing/dimension tokens |
| **Notifications & WorkManager** | Hardcoded channel IDs/names and notification IDs (`1001`, `2002`, `3001`, `3002`) | Centralized `@Singleton` `NotificationConfig` and `TrashConfig` |
| **Strings & Localization** | Hardcoded user-facing strings across Composables, Dialogs, and ViewModels | 100% extracted into `res/values/strings.xml` with typed formatting parameters |

---

## 3. Issues Log

### Critical / Security Vulnerabilities

#### Issue: Sensitive Raw WebSocket Frame Payload Logging
- **Severity**: Low
- **File & Line**: `app/src/main/java/com/aryan/gallery/service/WebSocketClient.kt:124`
- **Root Cause**: `WebSocketClient` logged full unredacted WebSocket text frames (`Log.d(TAG, "Received raw WebSocket frame: $text")`), writing sensitive media IDs, file names, or command payloads into system Logcat output.
- **Fix Applied**: Redacted raw text frame content from Logcat debug output, logging generic message receipt indicators while preserving command type dispatch logs.

#### Issue: Missing POST_NOTIFICATIONS Runtime Permission Check in NotificationHelper
- **Severity**: Medium
- **File & Line**: `app/src/main/java/com/aryan/gallery/service/NotificationHelper.kt:22, 47`
- **Root Cause**: `showTransparencyNotification` and `showConfirmationNotification` invoked `NotificationManager.notify()` without verifying whether `POST_NOTIFICATIONS` runtime permission was granted on Android 13+ (API 33+).
- **Fix Applied**: Added `ContextCompat.checkSelfPermission` checks for `Manifest.permission.POST_NOTIFICATIONS` prior to creating or posting notifications.

#### Issue: Incomplete Legacy Shared Preferences Backup Exclusion
- **Severity**: Low
- **File & Line**: `app/src/main/res/xml/backup_rules.xml:10`
- **Root Cause**: `backup_rules.xml` contained unconfigured placeholder XML comments instead of explicit rules excluding `secure_prefs.xml` on legacy Android versions (API 30 and lower).
- **Fix Applied**: Added `<exclude domain="sharedpref" path="secure_prefs.xml" />` inside `<full-backup-content>` to complement `data_extraction_rules.xml`.

---

### Runtime Bugs & Logic Errors

#### Issue: Year 55800+ Date Calculation Corruption in Media Detail Viewer
- **Severity**: High
- **File & Line**: `app/src/main/java/com/aryan/gallery/ui/viewer/MediaViewerScreen.kt:198`
- **Root Cause**: `MediaItem.dateAdded` is stored as epoch milliseconds calculated in `MediaStoreDataSource.kt`. `MediaViewerScreen.kt` multiplied `currentItem.dateAdded` by 1000 again (`Date(currentItem.dateAdded * 1000)`), multiplying epoch milliseconds by 1000 and causing displayed dates in the detail sheet to show corrupted years (e.g., Year 55800+).
- **Fix Applied**: Removed the redundant `* 1000` multiplier in `MediaViewerScreen.kt` so `Date(currentItem.dateAdded)` receives exact epoch milliseconds.

#### Issue: Main Thread ContentResolver Disk I/O & ActivityResultLauncher Thread Safety
- **Severity**: High
- **File & Line**: `app/src/main/java/com/aryan/gallery/ui/remote/PendingRemoteConfirmDialog.kt:50, 114`
- **Root Cause**: `context.contentResolver.update` and `dataSource.getMediaItemById` were executed inside `scope.launch` without explicitly specifying `Dispatchers.IO`, running disk/provider database updates on `Dispatchers.Main`. Additionally, calling `renameIntentSenderLauncher.launch` inside background coroutines without `Dispatchers.Main` context risked thread affinity errors.
- **Fix Applied**: Explicitly dispatched ContentResolver queries and updates to `Dispatchers.IO` and ensured `renameIntentSenderLauncher.launch` is invoked on `Dispatchers.Main` via `withContext(Dispatchers.Main)`.

#### Issue: Connection Failure on Unprefixed Server Host URLs
- **Severity**: Medium
- **File & Line**: `app/src/main/java/com/aryan/gallery/data/local/RemoteAccessPreferences.kt:20`
- **Root Cause**: When a user entered a host IP or hostname without `http://` or `https://` (e.g. `192.168.1.50:8080`), `HttpUrl.toHttpUrlOrNull()` in `HostSelectionInterceptor` returned `null`, breaking Retrofit API routing and OkHttp WebSocket connections.
- **Fix Applied**: Normalized `serverBaseUrl` in `RemoteAccessPreferences` setter using centralized `UrlNormalizer`.

---

### Build / Dependency / Deprecation Issues

#### Issue: Deprecated Kotlin Compiler Argument
- **Severity**: Low
- **File & Line**: `app/build.gradle.kts:72`
- **Root Cause**: `kotlinOptions` declared `-Xannotation-default-target=param-property` with `@Suppress("DEPRECATION")`.
- **Fix Applied**: Removed the deprecated Kotlin compiler flag for clean Kotlin 2.0+ compilation.

---

## 4. Verification & Test Checklist

### Automated Verification
1. **Compilation Check**:
   - Command: `./gradlew app:assembleDevDebug`
   - Result: **BUILD SUCCESSFUL** (Compiled cleanly without errors).
2. **Unit Tests Execution**:
   - Command: `./gradlew app:testDevDebugUnitTest`
   - Result: **35 PASSED, 0 FAILED** (100% pass rate across all unit test suites).

### Manual QA & Integration Test Matrix
- **TC-01 (Media Viewer Date Display)**: Open full-screen photo viewer -> Tap Info -> Confirm date added reflects actual date/time instead of Year 55800+.
- **TC-02 (Remote Rename Handling)**: Trigger remote `RENAME_MEDIA` command -> Confirm dialog on device -> Confirm MediaStore rename completes asynchronously on `Dispatchers.IO` without UI stutter.
- **TC-03 (Server Host Normalization)**: Navigate to Settings -> Change Server Host to `192.168.1.100:8080` (without `http://`) -> Confirm `UrlNormalizer` normalizes URL to `http://192.168.1.100:8080`.
- **TC-04 (Notification Permission Safety)**: Revoke notification permission in system settings -> Trigger remote command -> Confirm app handles missing `POST_NOTIFICATIONS` permission safely without crashing.
- **TC-05 (WebSocket Framing)**: Run Logcat with `tag:GalleryWebSocket` -> Verify incoming WebSocket messages do not dump raw media payloads or sensitive data into logs.
