# 02 — Comprehensive Security & Privacy Audit (OWASP MASVS / MASTG & Privacy Audit)

This report is based on the **OWASP Mobile Application Security Verification Standard (MASVS v2.1)**, the **Mobile Application Security Testing Guide (MASTG)**, and **MITRE CWE** classifications, combining source code inspection with analysis of the release manifest ([AndroidManifest.xml](../../app/src/main/AndroidManifest.xml)).

---

## 1. MONAVOCE Security Threat Model

As an offline voice-powered personal expense tracker:
1. **Protected Assets:**
   - User financial records (amounts, categories, timestamps, notes, raw voice transcript `rawVoiceText`) stored in the Room SQLite database (`my_expenses_db`).
   - Real-time user voice audio captured via the microphone (`android.permission.RECORD_AUDIO`).
   - Exported and imported backup files (`JSON` and `CSV`).
   - Neural speech recognition model files (`encoder.int8.onnx`, `decoder.int8.onnx`, `joiner.int8.onnx`, `tokens.txt`).
2. **Potential Threat Actors:**
   - Malicious third-party apps installed on the same device attempting to abuse exported activities/services (`android:exported="true"`) or read shared storage.
   - Maliciously crafted import files (`Backup JSON` or `Model ZIP`) designed to exploit decompression (Zip Bomb / Zip Slip) or trigger memory exhaustion (`OOM`).
   - Opening exported `CSV` files in spreadsheet software (Excel / Google Sheets / LibreOffice Calc) and associated CSV Formula Injection attacks.
   - Physical access or `adb logcat` inspection of system logs that may leak spoken financial details.

---

## 2. Manifest & Exported Component Attack Surface

Both the source manifest ([app/src/main/AndroidManifest.xml](../../app/src/main/AndroidManifest.xml)) and the merged release manifest were inspected:

### A. Permissions Audit
- `android.permission.RECORD_AUDIO` (line 5 in manifest): Required for core voice expense logging; requested as a runtime permission before starting a voice session.
- **Complete Absence of Internet Permission (`MONA-SEC-007` - Positive Verification):** There is no `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, `READ_EXTERNAL_STORAGE`, or `WRITE_EXTERNAL_STORAGE`. This fundamentally eliminates network exfiltration risks (Zero Exfiltration Attack Surface).
- **Cloud Backup Disabled:** `android:allowBackup="false"` (line 10 in source manifest, line 40 in merged manifest), preventing database extraction via `adb backup` or unencrypted automatic cloud uploads.

### B. Exported Component Analysis (`android:exported="true"`)

| Component | Type | Protection / Required Permission | Security Assessment |
|---|---|---|---|
| `.MainActivity` | Activity | `MAIN` / `LAUNCHER` + `ACTION_QUICK_VOICE` | Safe as the primary app entry point. |
| `.QuickVoiceActivity` | Activity | **No permission (`exported="true"`)** | **Medium Risk (`MONA-SEC-003`)**: Accepts `ACTION_ASSIST` and `com.myexpenses.app.ACTION_QUICK_VOICE` from any external app and starts recording automatically. |
| `.service.ExpenseVoiceInteractionService` | Service | `android.permission.BIND_VOICE_INTERACTION` | **Safe**: Protected by a system permission (`signature|privileged`) held only by Android OS. |
| `.service.ExpenseVoiceInteractionSessionService` | Service | `android.permission.BIND_VOICE_INTERACTION` | **Safe**: Protected by a `signature` system permission. |
| `.service.ExpenseStubRecognitionService` | Service | `android.permission.BIND_RECOGNITION_SERVICE` | **Security-safe** (immediately rejects sessions with `ERROR_CLIENT`), though it requires review for Google Play policies (`MONA-PLAY-003`). |
| `.service.QuickVoiceTileService` | Service | `android.permission.BIND_QUICK_SETTINGS_TILE` | **Safe**: Protected by the system permission for Quick Settings tiles. |
| `.widget.ExpenseWidgetProvider` | Receiver | `android.appwidget.action.APPWIDGET_UPDATE` | **Low Risk**: Listens only for widget updates and reads today's total from the local database. |

---

## 3. Confirmed Security Findings

### 1. `[MONA-SEC-001]` (High) — CSV Formula Injection in Exported CSV Files (CWE-1236)
- **Exact Location:** [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt), `csvEscape` function at lines [149-156](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt):
  ```kotlin
  private fun csvEscape(value: String): String {
      val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
      if (!needsQuotes) return value
      return "\"${value.replace("\"", "\"\"")}\""
  }
  ```
- **Evidence & Root Cause:** The function only escapes commas, quotes, and newlines, without checking whether the value (`note`, `rawVoiceText`, or custom category names `categoryNameAr`/`categoryNameEn`) starts with spreadsheet formula trigger characters: `=`, `+`, `-`, `@`, `\t`, `\r`.
- **Security Impact:** If a user enters a note or imports a JSON file containing text such as `=cmd|' /C calc'!A0` or `-500+HYPERLINK(...)`, exports a CSV report, and opens it in Microsoft Excel or Google Sheets, the spreadsheet application may execute the formula automatically.
- **Recommended Fix:** Update `csvEscape` to prepend a single quote `'` (inside quotes) before any text field starting with `=`, `+`, `-`, `@`, `\t`, or `\r`.

---

### 2. `[MONA-SEC-002]` (Medium) — Unbounded Decompression & Memory Exhaustion on Model ZIP & JSON Backup Import (CWE-409)
- **Exact Location 1 (ZIP Import):** [SherpaOfflineArabicEngine.kt](../../app/src/main/java/com/myexpenses/app/voice/SherpaOfflineArabicEngine.kt) (`importModelFromZipUri`).
  - *Positive Note:* The code protects against **Zip Slip (Path Traversal)** by taking only the bare filename `val baseName = File(entry.name).name` and validating it against the allowlist `expectedFiles = setOf(ENCODER_NAME, DECODER_NAME, JOINER_NAME, TOKENS_NAME)` (lines 88–94).
  - *Vulnerability:* The copy loop `while (zis.read(buffer).also { read = it } > 0)` (lines 103–105) enforces no maximum byte limit per entry (e.g., `MAX_ENTRY_BYTES = 200L * 1024 * 1024`). If a user selects a malicious ZIP (Zip Bomb) containing an entry named `encoder.int8.onnx` compressed to 50 MB that inflates to 20 GB, it will exhaust device storage (`Disk Exhaustion DoS`).
- **Exact Location 2 (JSON Import):** [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt) (`restoreJsonBackup`):
  ```kotlin
  val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
      input.bufferedReader().readText()
  } ?: return@withContext Result.failure(...)
  ```
  - *Vulnerability:* Reading the entire file into memory via `readText()` without checking file size first or enforcing a read cap (e.g., `10 MB`) exposes the app to an immediate `OutOfMemoryError` crash if a huge file is selected by mistake.

---

### 3. `[MONA-SEC-003]` (Medium) — Exported `QuickVoiceActivity` Auto-Activates Microphone Without Caller Restrictions (MASVS-PLATFORM-1)
- **Exact Location:** [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml) and [QuickVoiceActivity.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/QuickVoiceActivity.kt).
- **Evidence & Root Cause:** `QuickVoiceActivity` is exported (`android:exported="true"`) so the OS and Samsung shortcuts (`RegiStar` / `Side Button`) can launch it via `com.myexpenses.app.ACTION_QUICK_VOICE` and `android.intent.action.ASSIST`. Upon opening, line 77 immediately starts audio recording without requiring an extra tap:
  ```kotlin
  LaunchedEffect(Unit) {
      startOrRequestMic()
  }
  ```
- **Security Impact:** Any other installed app on the device (even one without microphone permission) can fire `Intent("com.myexpenses.app.ACTION_QUICK_VOICE")` to bring up `QuickVoiceActivity`, which automatically opens the mic and logs whatever the user says as a new expense in the database.
- **Recommended Fix:** Validate the incoming `Intent`, ensure the screen is interactive, or separate the shortcut action from the public action and avoid auto-executing destructive commands (like deletion) on untrusted external launches without confirmation.

---

### 4. `[MONA-SEC-004]` (Medium) — Sensitive Voice Transcripts & Financial Text Logged to `Logcat` in Release Builds (CWE-532 / MASVS-STORAGE-2)
- **Exact Locations:**
  - [OfflineSpeechManager.kt](../../app/src/main/java/com/myexpenses/app/voice/OfflineSpeechManager.kt):
    `Log.i(TAG, "onResults(localeTag=$localeTag, count=${matches.size}, best='$best')")`
  - [OfflineSpeechManager.kt](../../app/src/main/java/com/myexpenses/app/voice/OfflineSpeechManager.kt):
    `Log.i(TAG, "Sherpa-ONNX offline Arabic result: '$text'")`
  - [SherpaOfflineArabicEngine.kt](../../app/src/main/java/com/myexpenses/app/voice/SherpaOfflineArabicEngine.kt):
    `Log.i(TAG, "Sherpa-ONNX offline decode done in ${decodeMs}ms (audio=${seconds}s): '$rawText'")`
  - [proguard-rules.pro](../../app/proguard-rules.pro): Missing `-assumenosideeffects class android.util.Log` rules to strip `Log.d` and `Log.i` calls in `release` builds.
- **Security Impact:** Every spoken phrase (including amounts and private purchase details) is printed in plain text to the system `logcat` ring buffer even in `Release APK` builds, allowing ADB-connected tools or system services with `READ_LOGS` to read the user's spoken financial history.
- **Recommended Fix:** Suppress sensitive transcript content in logs outside `BuildConfig.DEBUG`, and add ProGuard/R8 rules in `proguard-rules.pro` to strip `Log.d`/`Log.i`/`Log.v` in release builds.

---

### 5. `[MONA-SEC-005]` (Medium) — AI Model Stored in External App Storage & Unencrypted SQLite Database (MASVS-STORAGE-1)
- **Exact Locations:**
  1. [SherpaOfflineArabicEngine.kt](../../app/src/main/java/com/myexpenses/app/voice/SherpaOfflineArabicEngine.kt):
     ```kotlin
     fun getModelDir(context: Context): File {
         val base = context.getExternalFilesDir("offline_asr_ar") ?: File(context.filesDir, "offline_asr_ar")
     ```
     `getExternalFilesDir` points to `/sdcard/Android/data/com.myexpenses.app/files/offline_asr_ar/`. Although Android 11+ enforces Scoped Storage, this directory remains accessible and modifiable via `adb shell` or privileged file managers, unlike Linux-permission-protected internal storage `/data/data/com.monavoce.app/files/offline_asr_ar/` (`context.filesDir`). Replacing an `.onnx` file with a corrupted payload could crash native code (`libonnxruntime.so`).
  2. [ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt): `my_expenses_db` is stored as standard unencrypted SQLite (relying solely on Android's File-Based Encryption FBE). On rooted devices, the database file can be read directly.

---

### 6. `[MONA-SEC-006]` (Low) — Silent Fallback to Debug Signing Key When `keystore.properties` Is Missing in Release Builds
- **Exact Location:** [app/build.gradle.kts](../../app/build.gradle.kts):
  ```kotlin
  signingConfig = signingConfigs.findByName("release")
      ?.takeIf { keystorePropertiesFile.exists() }
      ?: signingConfigs.getByName("debug")
  ```
- **Security Impact:** In automated CI/CD pipelines, if a developer forgets to provide `keystore.properties`, the build succeeds silently and produces a `release.apk` signed with the `debug` key (invalid for Google Play) without an explicit failure.

---

## 4. OWASP MASVS v2.1 Compliance Matrix

| MASVS Domain | Status | Evidence & Details |
|---|---|---|
| **MASVS-STORAGE-1** (Secure Sensitive Data Storage) | **PASS (Partial)** | Database is inside `/data/data/` with `allowBackup="false"`; recommend moving the model directory to `context.filesDir` after adopting Play Asset Delivery. |
| **MASVS-STORAGE-2** (Prevent Sensitive Data Leakage) | **FAIL (`MONA-SEC-004`)** | Recorded speech transcripts are printed via `Log.i` in release builds; must strip via ProGuard/R8 and `DEBUG` guards. |
| **MASVS-CRYPTO-1 / 2** (Cryptography & Key Management) | **N/A / INFO** | App does not store passwords or bank cards; if biometric lock or SQLCipher is added later, use `AndroidKeyStore`. |
| **MASVS-AUTH-1** (Local Authentication) | **INFO** | No biometric lock (`BiometricPrompt`) at app launch; suggested as an optional `P2`/`P3` feature. |
| **MASVS-NETWORK-1 / 2** (Network Security) | **PASS (100%)** | App never requests `INTERNET` and opens zero network sockets (`MONA-SEC-007`). |
| **MASVS-PLATFORM-1** (Platform & IPC Security) | **WARN (`MONA-SEC-003`)** | Voice assistant services are protected by `BIND_VOICE_INTERACTION`, but `QuickVoiceActivity` needs external invocation hardening. |
| **MASVS-CODE-1 / 2** (Input Validation & Dependencies) | **FAIL (`MONA-SEC-001`, `002`)** | Requires CSV formula neutralization (`csvEscape`) and size limits on ZIP decompression and JSON import. |
| **MASVS-RESILIENCE-1** (Code Obfuscation & Release Hardening) | **PASS** | `isMinifyEnabled = true` and `isShrinkResources = true` are enabled and working with R8. |
