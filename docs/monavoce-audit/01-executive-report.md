# 01 — Executive Audit Report (MONAVOCE Executive Audit Report)

- **Product name at the time of the audit:** `My Expenses` (`com.myexpenses.app` — Version `v1.3.0`, `versionCode = 5`)
- **Target brand approved for global launch:** **MONAVOCE** (*Moneta* [Money/Financial Value] + *Voce* [Voice])
- **Audit mode:** **RULE ZERO — STRICT AUDIT-ONLY MODE** (read-only forensic and engineering inspection without modifying source code, configuration, or data)
- **Audit date:** 2026-11-22
- **Approved physical test device:** Samsung Galaxy A54 5G (`SM-A546E` — Android 16 / API 36 — One UI 8.5 — `arm64-v8a`)

---

## 1. Current State Executive Summary

**My Expenses** (the candidate for rebranding into the global brand **MONAVOCE**) is a native Android application built entirely with **Kotlin 2.1.0** and **Jetpack Compose (Material 3)** using a local **Room SQLite v2.6.1** database, equipped with a 100% offline neural Arabic speech recognition engine (**Sherpa-ONNX v1.10.45 + NVIDIA NeMo FastConformer Arabic Conformer CTC int8**).

The engineering, security, and functional inspection of the codebase (26 source and test files) and real-device testing on the **Samsung Galaxy A54 5G** demonstrated that the app has an **exceptional and rare technical core in the personal finance category**:
1. **Forensically Verified Absolute Privacy (Zero-Network Architecture):** The merged release manifest ([AndroidManifest.xml](../../app/src/main/AndroidManifest.xml)) is completely free of `android.permission.INTERNET`, tracking libraries, and ads, with automatic cloud backup disabled (`android:allowBackup="false"`).
2. **Instant Offline Arabic Speech Recognition (On-Device Neural ASR):** Neural decode latency on the physical phone CPU ranges between **`179ms` and `252ms`** in Flight Mode, paired with a natural-language parser ([VoiceInputParser.kt](../../app/src/main/java/com/myexpenses/app/domain/parser/VoiceInputParser.kt)) supporting Arabic numerals, dialects (Yemeni, Gulf, Levantine, Egyptian, Modern Standard Arabic), fractions, and multiple expenses in a single sentence—passing **16/16 automated unit tests** at 100%.
3. **Deep Hardware & OS Quick-Launch Integration:** Supports instant launch via a long press of the side power button through `VoiceInteractionService` ([ExpenseAssistantServices.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/ExpenseAssistantServices.kt)), alongside a Quick Settings Tile, a home screen AppWidget, and a launcher App Shortcut.

However, the deep audit uncovered **5 high-severity release blockers (P0)** and **12 medium/other priority findings (P1/P2)** that prevent an immediate **Google Play Store** launch under the **MONAVOCE** brand until resolved:
- **Critical Multi-Currency Accounting Bug (`MONA-FIN-001` - High):** SQL sum queries (`SUM(amount)`) and `MainViewModel` sum amounts across all records without filtering or grouping by `currencyCode`, causing mixed currencies to be added together (e.g., `5000 YER + 10 USD = 5010 USD`) when the default currency is changed in Settings.
- **Data Loss Risk During Backup Restore (`MONA-FIN-002` - High):** `replaceAllFromBackup` in [ExpenseRepository.kt](../../app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt) deletes all expenses (`expenseDao.deleteAll()`) before inserting new rows **outside** an atomic database transaction (`@Transaction` / `withTransaction`), and without pre-validating all records in [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt).
- **CSV Formula Injection Vulnerability (`MONA-SEC-001` - High / CWE-1236):** `csvEscape` in [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt) does not neutralize values starting with formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) when opened in Excel or Google Sheets.
- **Package Identity Conflict & Google Play Packaging Requirements (`MONA-PLAY-001` & `MONA-PLAY-002` - High):** The app currently uses package ID `com.myexpenses.app` and name `My Expenses` (owned by another well-known Play Store app). Additionally, the offline Arabic voice model (`130 MB` / `101.2 MiB ZIP`) must be packaged via **Play Asset Delivery (PAD `install-time` asset pack)** inside the `.aab` bundle so the app works offline immediately after Play Store installation without Internet permission or manual external ZIP downloads.

---

## 2. Project Architecture Discovery

| Technical Dimension | Verified Value in Codebase | Reference Evidence |
|---|---|---|
| **Language & UI Framework** | Kotlin `2.1.0` + Jetpack Compose BOM `2025.01.01` (`Material 3`) | [build.gradle.kts](../../build.gradle.kts), [app/build.gradle.kts](../../app/build.gradle.kts) |
| **Android Gradle Plugin (AGP)** | `8.8.0` (Gradle Wrapper `8.11.1`) | [build.gradle.kts](../../build.gradle.kts), [gradle-wrapper.properties](../../gradle/wrapper/gradle-wrapper.properties) |
| **Android SDK Range** | `minSdk = 26` (Android 8.0) / `targetSdk = 35` (Android 15) / `compileSdk = 35` | [app/build.gradle.kts](../../app/build.gradle.kts) |
| **Architecture** | Single-Module MVVM + Repository + Manual DI (`AppContainer` inside `MyExpensesApp`) | [MyExpensesApp.kt](../../app/src/main/java/com/myexpenses/app/MyExpensesApp.kt) |
| **Local Database** | Room SQLite `2.6.1` (`my_expenses_db`, `version = 1`, `exportSchema = false`) | [ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt) |
| **Preferences Storage** | `DataStore Preferences 1.1.2` + synchronized `SharedPreferences` mirror for fast access | [UserPreferences.kt](../../app/src/main/java/com/myexpenses/app/data/preferences/UserPreferences.kt) |
| **Offline Voice Engine (Arabic)** | `sherpa-onnx-1.10.45.aar` (JNI `arm64-v8a`, `armeabi-v7a`, `x86_64`) + NeMo FastConformer CTC | [SherpaOfflineArabicEngine.kt](../../app/src/main/java/com/myexpenses/app/voice/SherpaOfflineArabicEngine.kt) |
| **Fallback Voice Engine (English/OS)** | Android `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE = true` with `checkRecognitionSupport` check) | [OfflineSpeechManager.kt](../../app/src/main/java/com/myexpenses/app/voice/OfflineSpeechManager.kt) |
| **Code Shrinking & Obfuscation (Release)** | `isMinifyEnabled = true`, `isShrinkResources = true` (R8 enabled and passing) | [app/build.gradle.kts](../../app/build.gradle.kts) |
| **Release Artifact Sizes** | Release APK: `40,507,911` bytes (`38.63 MiB`) / Model bundle: `106,106,155` bytes (`101.19 MiB`) | `dist/MyExpenses-v1.3.0-release.apk` |

---

## 3. Production Readiness Scorecard Summary

| Dimension | Score (/100) | Summary Assessment |
|---|---|---|
| 1. Architecture & Code Quality | **82 / 100** | Clean layer separation; needs SQL-level filtering instead of in-memory loops and cleanup of unused DAO methods. |
| 2. Security | **78 / 100** | Excellent Zero-Internet architecture; requires fixing CSV Injection, ZIP decompression bounds, and removing raw transcript Logcat output. |
| 3. Privacy & Offline Compliance | **96 / 100** | Zero network permissions, zero third-party tracking SDKs, 100% on-device speech processing. |
| 4. Financial Accuracy & Data Integrity | **68 / 100** | Single-currency math is accurate; however, multi-currency summation (`MONA-FIN-001`) and non-transactional restore (`MONA-FIN-002`) require immediate fixes. |
| 5. Arabic Voice Recognition Quality | **94 / 100** | NeMo FastConformer model with dialect dictionary in `VoiceInputParser` delivers outstanding offline speed and accuracy (`~210ms`). |
| 6. English Voice Recognition Quality | **84 / 100** | Works via the OS offline engine with English number-word parsing; depends on `en-US` pack availability on the device. |
| 7. Hardware / Quick-Launch Reliability | **92 / 100** | Strong integration with the side power button (`VoiceInteractionService`), Quick Settings tile, and lock screen launch. |
| 8. UI/UX Quality | **83 / 100** | Obsidian OLED + Cosmic Voice Orb design is very attractive; held back by ignoring Light Mode in the voice overlay and missing onboarding. |
| 9. Accessibility & Localization | **72 / 100** | Instant RTL/LTR switching, but includes buttons missing `contentDescription`, touch targets under `48dp`, and hardcoded strings instead of `strings.xml`. |
| 10. Performance & Stability | **86 / 100** | Fast launch and instant speech recognition; needs to stop preloading the `130 MB` model on background widget updates (`MONA-VOX-003`). |
| 11. Testing Coverage | **74 / 100** | 16 comprehensive unit tests for the voice parser (`100% PASS`); lacks DAO/Repository tests and `androidTest` UI tests. |
| 12. Brand Cohesion (**MONAVOCE** Readiness) | **60 / 100** | Dark UI aligns well with the brand aesthetic, but naming, package ID, launcher icon, and splash screen still say `My Expenses`. |
| 13. Google Play Store Readiness | **64 / 100** | Requires migrating `applicationId` to `com.monavoce.app`, bundling the model via Play Asset Delivery (`.aab`), and setting up a privacy policy URL. |
| **Overall Weighted Readiness Score** | **79.5 / 100** | **CONDITIONAL GO (Ready for local/direct use; conditional on closing P0 items before global Google Play release)** |

---

## 4. Final Release Verdict

> [!IMPORTANT]
> **Final Release Decision:** **`CONDITIONAL GO`**
> - **For direct APK distribution and personal/local use:** The app is **ready, stable, and functional (`GO`)** on `arm64-v8a` devices such as the Samsung Galaxy A54 5G with the offline voice model bundle.
> - **For global commercial launch on Google Play under the MONAVOCE brand:** **Conditional (`CONDITIONAL GO`)** upon completing the P0 release blockers `MONA-FIN-001`, `MONA-FIN-002`, `MONA-SEC-001`, `MONA-PLAY-001`, and `MONA-PLAY-002`.
