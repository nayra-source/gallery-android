# Full Audit and Implementation Plan

## Executive Summary
This document provides a comprehensive audit and prioritized execution plan for the Gallery system across its three modules:
1. `gallery-backend/` (Ktor Kotlin backend)
2. `gallery-android/` (Jetpack Compose Android app)
3. `gallery-dashboard/` (React + Vite + Tailwind admin dashboard)

All three modules currently compile and pass basic tests. However, a deep code inspection identified critical security flaws, API contract mismatches, input validation gaps, duplicate network calls, and minor lifecycle/hygiene issues.

---

## Prioritized Issue List

### 1. Critical Priority (Crashes, Security, Broken Contracts)

#### C1. Backend: Unhandled Exceptions in WebSocket Incoming Frame Loop
- **Location:** `gallery-backend/src/main/kotlin/com/gallery/sockets/DeviceWebSocket.kt`
- **What's wrong:** `UUID.fromString(incoming.cmdId)` and database transaction errors inside the incoming frame loop are unhandled. An invalid UUID string or database exception breaks out of the loop and forcibly closes the device's WebSocket connection.
- **Proposed fix:** Wrap frame handling and database updates in a `runCatching` or `try-catch` block inside the loop to log/report errors without terminating the active WebSocket session.
- **Estimated Effort:** 15 mins
- **Risk:** Low

#### C2. Backend: Unhandled `IllegalArgumentException` on Invalid UUID Route Parameters
- **Location:** `gallery-backend/src/main/kotlin/com/gallery/routes/DeviceRoutes.kt`, `TransferRoutes.kt`
- **What's wrong:** Calling `UUID.fromString(id)` directly on raw route parameters throws `IllegalArgumentException` when an invalid UUID is supplied, resulting in unhandled 500 Internal Server Errors instead of clean 400 Bad Request responses.
- **Proposed fix:** Add a `String.toUuidOrNull()` helper and return `HttpStatusCode.BadRequest` ("invalid_uuid") if parsing fails.
- **Estimated Effort:** 20 mins
- **Risk:** Low

#### C3. Backend Security: Path Traversal Vulnerability in Multipart File Upload Staging
- **Location:** `gallery-backend/src/main/kotlin/com/gallery/routes/TransferRoutes.kt`
- **What's wrong:** `fileName = "$id-${part.originalFileName ?: "media"}"` uses raw `originalFileName`. Malicious filenames containing directory traversal sequences (e.g. `../../`) could write files outside `stagingDir`.
- **Proposed fix:** Sanitize file names using `File(part.originalFileName ?: "media").name` and verify destination path resides within `stagingDir`.
- **Estimated Effort:** 15 mins
- **Risk:** Low

#### C4. Cross-Cutting API Contract Mismatch: `ApiErrorResponse` Field Name
- **Locations:**
  - `gallery-android/app/src/main/java/com/aryan/gallery/data/remote/models/PairingModels.kt`
  - `gallery-dashboard/src/services/api.ts`
- **What's wrong:** Backend returns JSON error payload `{"code": "...", "message": "..."}`. Android defined `data class ApiErrorResponse(val error: String, val message: String?)`, causing `apiError?.error` to always evaluate to `null`. Dashboard checked `errorJson.error` instead of `errorJson.code`.
- **Proposed fix:** Update Android's `ApiErrorResponse` to expect `val code: String` and update Dashboard `api.ts` to check `errorJson.code`.
- **Estimated Effort:** 15 mins
- **Risk:** Low

#### C5. Android: Incorrect Default Base URL for Development Flavor
- **Location:** `gallery-android/app/build.gradle.kts`
- **What's wrong:** `dev` product flavor sets `BASE_URL` to `"http://10.84.187.249:8080"`, a hardcoded specific LAN IP that fails on standard Android emulators.
- **Proposed fix:** Change `dev` flavor `BASE_URL` to `"http://10.0.2.2:8080"` and `WS_URL` to `"ws://10.0.2.2:8080/ws/device"`.
- **Estimated Effort:** 5 mins
- **Risk:** Low

---

### 2. High Priority (Reliability, Logic Flaws, State Management)

#### H1. Dashboard: Duplicate `LIST_MEDIA` Command Dispatching
- **Location:** `gallery-dashboard/src/components/MediaGridScreen.tsx`
- **What's wrong:** `fetchMediaList` explicitly calls `api.sendCommand(device.id, 'LIST_MEDIA', ...)` and then immediately calls `api.executeCommandAndPoll(...)`, which internally issues a second `api.sendCommand(...)`. This triggers duplicate commands to the Android device on every page load/refresh.
- **Proposed fix:** Remove the redundant explicit `api.sendCommand` call prior to `executeCommandAndPoll`.
- **Estimated Effort:** 10 mins
- **Risk:** Low

#### H2. Backend: Incomplete Transfer Cleanup Expiration Logic
- **Location:** `gallery-backend/src/main/kotlin/com/gallery/routes/TransferRoutes.kt`
- **What's wrong:** `TransferCleanup.purgeExpired()` only checked transfers with `status eq "ready"`. Transfers abandoned in `"requested"` or `"uploading"` status past `expiresAt` were never marked expired.
- **Proposed fix:** Update query to match transfers where `status in ("requested", "uploading", "ready")` and `expiresAt less now`.
- **Estimated Effort:** 15 mins
- **Risk:** Low

#### H3. Android: Recoverable Security Exception Safety in MediaStore Operations
- **Location:** `gallery-android/app/src/main/java/com/aryan/gallery/ui/remote/PendingRemoteConfirmDialog.kt`
- **What's wrong:** Executing `context.contentResolver.update` for MediaStore renames without catching `RecoverableSecurityException` / `SecurityException` causes unhandled failures when renaming items created outside the app.
- **Proposed fix:** Safely wrap MediaStore rename update calls with exception handling and log/inform appropriately.
- **Estimated Effort:** 15 mins
- **Risk:** Low

---

### 3. Medium Priority (Robustness, Git & Env Hygiene)

#### M1. Dashboard: Missing `.gitignore` and `.env.example`
- **Location:** `gallery-dashboard/`
- **What's wrong:** `gallery-dashboard` lacks `.gitignore` and `.env.example` files.
- **Proposed fix:** Add `.gitignore` ignoring `node_modules`, `dist`, `.env`, `.env.local` and `.env.example`.
- **Estimated Effort:** 5 mins
- **Risk:** Low

#### M2. Backend: Production Warning for Fallback Secrets
- **Location:** `gallery-backend/src/main/kotlin/com/gallery/auth/JwtConfig.kt`
- **What's wrong:** Hardcoded fallback JWT secret key is used silently if `JWT_SECRET` is unset.
- **Proposed fix:** Log a prominent warning if `JWT_SECRET` environment variable is not present.
- **Estimated Effort:** 5 mins
- **Risk:** Low

---

### 4. Low Priority / Polish

#### L1. Verification & Automated Test Pass
- Re-run unit tests across all three modules (`./gradlew test` for backend, `app:testDevDebugUnitTest` for Android, `npm run build` for Dashboard).
- Generate final `AUDIT_REPORT.md` at repository root summarizing all fixes and instructions.

---

## Order of Execution & Dependencies

1. **Step 1: Backend Security & Contract Fixes** (C1, C2, C3, H2, M2)
   - Fix WebSocket frame error handling, UUID parameter validation, upload path sanitization, transfer cleanup query, and JWT secret warning.
   - Run backend test suite (`./gradlew test`).

2. **Step 2: Cross-Cutting & Android Fixes** (C4, C5, H3)
   - Fix `ApiErrorResponse` data class model and `build.gradle.kts` dev flavor `BASE_URL`.
   - Improve `PendingRemoteConfirmDialog.kt` error safety.
   - Run Android test suite (`./gradlew app:testDevDebugUnitTest`).

3. **Step 3: Dashboard Fixes** (C4, H1, M1)
   - Fix duplicate command dispatching in `MediaGridScreen.tsx`.
   - Update error parsing in `api.ts`.
   - Add `.gitignore` and `.env.example` to `gallery-dashboard`.
   - Run dashboard build (`npm run build`).

4. **Step 4: Comprehensive Verification & Reporting**
   - Re-run all tests across all 3 modules.
   - Write full `AUDIT_REPORT.md`.

---

## Deliberately Not Changing (and Reasons)

1. **Exposed Database Schema (`MediaMetadata` / `TrashItems` tables):**
   - The backend uses on-demand WebSocket commands to fetch real-time media metadata directly from connected Android devices rather than caching everything in PostgreSQL. The tables in `Tables.kt` are retained for future offline sync features.
2. **Third-Party Libraries / Major Architectural Refactors:**
   - The current architecture (Ktor + Exposed on backend, Hilt + Compose + Paging3 on Android, React + Vite + Tailwind on Dashboard) is sound, clean, and modern. No heavy new frameworks will be introduced.

---

## Assumptions & Confirmation Flags
- **Assumption 1:** Standard Android emulator local connection uses `10.0.2.2:8080`.
- **Assumption 2:** All three modules run locally during development (`localhost:8080` / `127.0.0.1:8080`).
