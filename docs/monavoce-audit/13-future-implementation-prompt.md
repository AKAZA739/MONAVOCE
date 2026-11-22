# 13 — موجه التنفيذ الهندسي المستقبلي (Future Implementation Prompt for MONAVOCE v2.0.0)

> [!IMPORTANT]
> **تنبيه خاص بوضع الفحص (RULE ZERO Notice):**
> هذا الملف هو **مخرج توثيقي فقط (Documentation Deliverable Only)**. لم يتم تنفيذ أي أمر من الأوامر الواردة أدناه خلال جلسة التدقيق الحالية التزامًا بالقاعدة الصفرية. يمكنك نسخ الموجه الكامل أدناه ولصقه في جلسة تطوير جديدة عندما تقرر بدء التنفيذ الفعلي للتحول إلى **MONAVOCE**.

---

## نص الموجه الجاهز للنسخ والتنفيذ (Copy-Paste Implementation Prompt)

```markdown
# MISSION: IMPLEMENT MONAVOCE v2.0.0 — PRODUCTION REMEDIATION & FULL BRAND TRANSFORMATION

You are a Principal Android Engineer & UI/UX Architect tasked with executing the approved production remediation roadmap and brand transformation of the Android project in the repository root from `"My Expenses | مصاريفي"` (`com.myexpenses.app`) into the global commercial release of:

**MONAVOCE** (Arabic: **موناڤوتشي**)
Concept: *Moneta* (Money) + *Voce* (Voice) — 100% On-Device Voice-Powered Personal Finance Vault.

Before making any changes, read the audit reports in `docs/monavoce-audit/`, specifically:
- `09-findings-register.md`
- `10-remediation-roadmap.md`
- `06-brand-identity.md`

Execute the implementation in the following strict phases and verify every step with unit tests and a clean release build.

---

## PHASE 1 — P0 FINANCIAL ENGINE & DATA INTEGRITY FIXES (`MONA-FIN-001`, `MONA-FIN-002`, `MONA-FIN-003`, `MONA-FIN-004`, `MONA-FIN-005`)

1. **Atomic Backup Restore & Pre-Validation (`MONA-FIN-002`):**
   - In `ExpenseRepository.kt`, inject `private val database: ExpenseDatabase` and wrap `replaceAllFromBackup(categories, expenses)` AND `deleteCategory(category)` inside `database.withTransaction { ... }`.
   - In `BackupManager.kt` (`restoreJsonBackup`), validate every parsed expense before calling `replaceAllFromBackup`:
     - Skip or reject records where `amount <= 0.0 || !amount.isFinite()`.
     - Build the set of valid category IDs (`DefaultCategories.ALL.map { it.id }.toSet() + parsedCategories.map { it.id }.toSet()`). If any imported expense references an unknown `categoryId`, remap its `categoryId` to `DefaultCategories.OTHER` so foreign-key constraints never fail mid-restore.
2. **Multi-Currency Consistency & Exact Monetary Rounding (`MONA-FIN-001`, `MONA-FIN-003`):**
   - In `CurrencyFormatter.kt`, add `fun roundMoney(amount: Double): Double = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()` and apply it before saving/updating expenses in `ExpenseRepository.kt` and when exporting JSON/CSV.
   - In `MainViewModel.kt`, filter dashboard/statistics aggregations (`todayTotal`, `weekTotal`, `monthTotal`, `filteredTotal`, `categoryBreakdown`, `dailyTotals`) by the active currency (`it.expense.currencyCode.equals(preferences.currency.code, ignoreCase = true)`) OR provide a one-tap currency migration helper when changing currency in Settings so mixed currencies are never blindly summed together.
   - In `DashboardScreen.kt` (`ExpenseListItemCard`), display each transaction's actual currency symbol using `CurrencyOption.fromCode(item.expense.currencyCode).symbol(isArabic)`.
   - In `ExpenseDao.kt` & `ExpenseWidgetProvider.kt`, filter widget today totals by the active currency code stored in preferences.
3. **Room Schema & ForeignKey Default (`MONA-FIN-004`, `MONA-FIN-005`):**
   - In `ExpenseEntity.kt`, add `@ColumnInfo(defaultValue = DefaultCategories.OTHER)` to `val categoryId: String = DefaultCategories.OTHER`.
   - In `ExpenseDatabase.kt` and `app/build.gradle.kts`, enable Room schema export (`exportSchema = true`, `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`).

---

## PHASE 2 — P0/P1 SECURITY & PRIVACY HARDENING (`MONA-SEC-001`, `MONA-SEC-002`, `MONA-SEC-003`, `MONA-SEC-004`, `MONA-SEC-005`, `MONA-SEC-006`)

1. **CSV Formula Injection Protection (`MONA-SEC-001`):**
   - In `BackupManager.kt` (`csvEscape`), if a trimmed cell starts with `'='`, `'+'`, `'-'`, `'@'`, `'\t'`, or `'\r'` (and is not a pure negative number in the numeric `Amount` column), prefix the text value with a single quote `'` and wrap in double quotes so Excel/Google Sheets never execute formulas from voice notes or category names.
2. **Bounded Zip & JSON Streams + Internal Storage Model Dir (`MONA-SEC-002`, `MONA-SEC-005`):**
   - In `SherpaOfflineArabicEngine.kt` (`importModelFromZipUri`), enforce `MAX_SINGLE_ENTRY_BYTES = 160L * 1024 * 1024` and `MAX_TOTAL_UNZIP_BYTES = 220L * 1024 * 1024` inside the read loop; abort and clean up `.tmp` files if exceeded.
   - Change `getModelDir(context)` to prefer internal protected storage `File(context.filesDir, "offline_asr_ar")` for newly imported models while still checking `context.getExternalFilesDir("offline_asr_ar")` as a read fallback if files already exist there.
   - In `BackupManager.kt` (`restoreJsonBackup`), enforce a maximum input size of `15 * 1024 * 1024` bytes before reading the JSON string into memory.
3. **Strip Sensitive Speech Transcripts from Release Logs (`MONA-SEC-004`):**
   - In `OfflineSpeechManager.kt` (lines 480, 628) and `SherpaOfflineArabicEngine.kt` (line 283), never log raw spoken text in production builds (log only char length and `decodeMs`).
   - In `app/proguard-rules.pro`, add `-assumenosideeffects class android.util.Log { public static int v(...); public static int d(...); public static int i(...); }`.
4. **QuickVoiceActivity Intent Hardening (`MONA-SEC-003`):**
   - In `QuickVoiceActivity.kt`, require user confirmation before executing a destructive `DeleteLastExpense` voice command when launched from the lock screen or external intent.

---

## PHASE 3 — P1 VOICE NLP PARSER & PERFORMANCE OPTIMIZATIONS (`MONA-VOX-001`, `MONA-VOX-002`, `MONA-VOX-003`, `MONA-PERF-001`, `MONA-ARCH-001`)

1. **Quantity + Price Clause Merging (`MONA-VOX-001`):**
   - In `VoiceInputParser.kt` (`splitIntoExpenseClauses` / `parseSingleExpenseClause`), handle patterns where a small quantity number precedes an item noun followed by a price preposition (`"اشتريت 2 حليب ب 1000 ريال"`, `"3 شاورما بسعر 4500"`). Keep it as a single expense with `amount = 1000.0` and `note = "2 حليب"`.
2. **Category-Only Update Command (`MONA-VOX-002`):**
   - Allow `VoiceCommandResult.UpdateLastExpense.newAmount` to be nullable (`Double?`). When the user says `"عدل آخر مصروف إلى المواصلات"` without a new amount, retain the last expense's existing amount and update its category/note.
3. **Lazy ONNX Warm-Up & Background Thread Filtering (`MONA-VOX-003`, `MONA-PERF-001`, `MONA-ARCH-001`):**
   - Remove `container.sherpaArabicEngine.warmUpAsync()` from `MyExpensesApp.onCreate()`. Call `warmUpAsync()` inside `MainActivity.onCreate()` and `QuickVoiceActivity.onCreate()` instead so background widget updates do not allocate 220 MB of native ONNX memory.
   - In `MainViewModel.kt`, add `.flowOn(Dispatchers.Default)` to `dashboardState` before `.stateIn(...)`.
   - In `ExpenseWidgetProvider.kt`, use `val pendingResult = goAsync()` inside `onUpdate` and call `pendingResult.finish()` in `finally`.

---

## PHASE 4 — MONAVOCE BRAND IDENTITY, UI/UX, ACCESSIBILITY & PLAY ASSET DELIVERY (`MONA-PLAY-001`, `MONA-PLAY-002`, `MONA-UX-001`, `MONA-UX-002`, `MONA-UX-003`)

1. **Rebrand to MONAVOCE (`MONA-PLAY-001`):**
   - Apply the **MONAVOCE (موناڤوتشي)** brand identity across `strings.xml`, `Theme.kt`, `Color.kt`, launcher icons (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`), widget labels, and app headers as specified in `06-brand-identity.md`:
     - Display Name (EN): `MONAVOCE`
     - Display Name (AR): `MONAVOCE | موناڤوتشي`
     - Tagline (EN): `Speak Your Spend. 100% On-Device.`
     - Tagline (AR): `صوتك المالي، بخصوصية مطلقة دون إنترنت.`
     - Update package ID / authorities to `com.monavoce.app` (or keep a productFlavor if preserving side-by-side install on the test phone is desired).
2. **Theme Harmony & Accessibility (`MONA-UX-001`, `MONA-UX-002`):**
   - Apply the `Obsidian Vault` (`#07080D` / `#11131C` / `#8E8FFA` / `#00E5FF`) dark palette and `Alabaster Prestige` (`#F6F7FB` / `#FFFFFF` / `#4F46E5`) light palette.
   - Ensure `VoiceOverlayPanel.kt` adapts cleanly in both Dark and Light themes.
   - Add bilingual `contentDescription` semantics to `CosmicVoiceOrb`, date navigation steppers, category edit/delete buttons, and ensure interactive touch targets are at least `48.dp`.
3. **Play Asset Delivery Option (`MONA-PLAY-002`):**
   - Configure an optional `:arabic_asr_pack` (`com.android.asset-pack`, `install-time`) or bundle hook so `bundleRelease` produces a Play-Store-ready `.aab` with out-of-the-box offline Arabic recognition while keeping `android.permission.INTERNET` completely absent.

---

## PHASE 5 — VERIFICATION & ACCEPTANCE TESTING

1. Add unit tests in `VoiceInputParserTest.kt` and a new `BackupAndCurrencyTest.kt` covering:
   - `"اشتريت 2 حليب ب 1000 ريال"` -> single expense (`1000.0`, `"food"`).
   - `"عدل آخر مصروف إلى المواصلات"` -> category-only update.
   - CSV formula injection escaping (`"=cmd|..."` -> `"'=cmd|..."`).
   - Multi-currency isolation (`YER` vs `USD`).
   - Monetary rounding (`0.1 + 0.2 == 0.3`).
2. Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleRelease` and ensure 100% green build and tests.
```
