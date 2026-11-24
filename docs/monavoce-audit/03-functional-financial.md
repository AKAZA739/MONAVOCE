# 03 — Functional & Financial Engine Audit

This report examines financial calculation accuracy, **Room SQLite** database schema integrity, export/import mechanisms, and accounting edge cases across the application.

---

## 1. Database Schema & Entity Integrity Inspection

The database ([ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt)) consists of two primary tables:

1. **Categories Table (`categories` — [CategoryEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/CategoryEntity.kt)):**
   - Primary key: `id: String` (e.g., `"food"`, `"transport"`, `"other"`, or `"custom_<UUID>"`).
   - Columns: `nameAr`, `nameEn`, `iconName`, `colorHex: Long`, `keywordsCsv: String`, `isCustom: Boolean`, `sortOrder: Int`.
   - Seeds 8 bilingual default categories rich with Arabic and English keywords via [CategoryEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/CategoryEntity.kt) in `onCreate` and `seedDefaultsIfEmpty()`.

2. **Expenses Table (`expenses` — [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt)):**
   - Primary key: `id: Long` (`autoGenerate = true`).
   - Foreign key: `categoryId: String` linked to `CategoryEntity.id` with two indices on `categoryId` and `timestampMillis`.
   - Financial and temporal fields: `amount: Double`, `currencyCode: String = "YER"`, `note: String`, `rawVoiceText: String?`, `timestampMillis: Long`, `createdAtMillis: Long`.

---

## 2. Discovered Financial & Architectural Bugs

### 1. `[MONA-FIN-001]` (High / Release Blocker) — Multi-Currency Summation Without Filtering or Grouping by `currencyCode`
- **Exact Locations:**
  - [ExpenseDao.kt](../../app/src/main/java/com/myexpenses/app/data/local/dao/ExpenseDao.kt):
    ```sql
    SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestampMillis BETWEEN :startMillis AND :endMillis
    ```
  - [MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt):
    ```kotlin
    val todayTotal = allExpenses.filter { it.expense.timestampMillis in todayRange.first..todayRange.second }
        .sumOf { it.expense.amount }
    val weekTotal = allExpenses.filter { it.expense.timestampMillis in weekRange.first..weekRange.second }
        .sumOf { it.expense.amount }
    val monthTotal = allExpenses.filter { it.expense.timestampMillis in monthRange.first..monthRange.second }
        .sumOf { it.expense.amount }
    ```
- **Evidence & Root Cause:**
  - When a user logs an expense (by voice or manually), the currently selected currency in Settings is stored in `expense.currencyCode` ([MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt)).
  - If the user logs `5,000 YER` (Yemeni rial), then changes the currency in [SettingsScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/SettingsScreen.kt) to `USD` and logs `20 USD`, `MainViewModel` adds both numbers directly (`5000.0 + 20.0 = 5020.0`) and displays the total on the dashboard, statistics screen, and widget as **`5,020 $`**!
  - Furthermore, in individual expense cards ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)), the global preference symbol `state.preferences.currency.symbol(isArabic)` is displayed instead of the record's own `item.expense.currencyCode`!
- **Accounting Impact:** Complete distortion of financial totals, category percentages, and charts whenever the user changes currency or imports multi-currency records.
- **Recommended Fix:**
  1. Filter totals and charts in `MainViewModel` and `ExpenseDao` by the active currency `it.expense.currencyCode == prefs.currency.code` (or offer a currency conversion/migration option when changing currencies in Settings).
  2. Display the actual stored currency symbol `CurrencyOption.fromCode(item.expense.currencyCode).symbol(isArabic)` on each transaction row.

---

### 2. `[MONA-FIN-002]` (High / Release Blocker) — Backup Restore Deletes All Data Outside an Atomic `@Transaction` and Without Pre-Validation
- **Exact Locations:**
  - [ExpenseRepository.kt](../../app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt):
    ```kotlin
    suspend fun replaceAllFromBackup(
        categories: List<CategoryEntity>,
        expenses: List<ExpenseEntity>
    ) {
        if (categories.isNotEmpty()) {
            categoryDao.insertAll(categories)
        }
        expenseDao.deleteAll()
        if (expenses.isNotEmpty()) {
            expenseDao.insertAll(expenses)
        }
    }
    ```
  - [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt) (`restoreJsonBackup`).
- **Evidence & Root Cause:**
  1. `replaceAllFromBackup` is not wrapped in `database.withTransaction { ... }`. If an error occurs during `expenseDao.insertAll(expenses)` (or the app is killed right after `expenseDao.deleteAll()`), the user loses all previous expenses permanently with no rollback!
  2. In `BackupManager.restoreJsonBackup` (line 133), `amount = obj.getDouble("amount")` does not verify `amount > 0 && amount.isFinite()`, nor does it verify that every imported expense's `categoryId` actually exists in the `categories` table before insertion. Because `ExpenseEntity` enforces a `ForeignKey` constraint (lines 12–18), if a backup contains an expense referencing a missing `categoryId`, `expenseDao.insertAll(expenses)` throws `SQLiteConstraintException (FOREIGN KEY constraint failed)` **after** `expenseDao.deleteAll()` has already wiped all user data!
- **Accounting Impact:** Total data loss risk when restoring a JSON file containing a deleted category or malformed record.
- **Recommended Fix:**
  1. Wrap the entire restore operation inside `database.withTransaction { ... }`.
  2. Pre-validate in `BackupManager` that all referenced `categoryId` values exist in either the imported `categories` list or default categories, remapping unknown ones to `DefaultCategories.OTHER` and rejecting invalid amounts (`<= 0`, `NaN`, or `Infinite`).

---

### 3. `[MONA-FIN-003]` (Medium) — Using `Double` (IEEE-754 Binary Floating Point) for Monetary Amounts
- **Exact Location:** [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt) (`val amount: Double`).
- **Analysis:**
  - `DateTimeUtils.formatAmount` ([DateTimeUtils.kt](../../app/src/main/java/com/myexpenses/app/domain/DateTimeUtils.kt)) uses `DecimalFormat("#,##0.##")`, which hides minor rounding artifacts during UI display (such as `0.1 + 0.2 = 0.30000000000000004` rendering as `0.3`).
  - However, across thousands of fractional transactions in currencies with cents/halalas/fils (`USD`, `EUR`, `SAR`, `AED`, `KWD`) or when exporting raw amounts to JSON (`put("amount", item.expense.amount)` in [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt)), floating-point artifacts like `1404.8500000000001` can appear.
- **Recommended Fix:** Always round amounts to 2 decimal places (or 3 for KWD) via `BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()` before saving, summing, and exporting—or migrate in a future schema version to minor units (`amountMinorUnits: Long`).

---

### 4. `[MONA-FIN-004]` (Medium) — Mismatch Between `onDelete = ForeignKey.SET_DEFAULT` and Missing `@ColumnInfo(defaultValue = "other")`
- **Exact Location:** [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt).
- **Analysis:** `onDelete = ForeignKey.SET_DEFAULT` is declared in `@Entity`, but `val categoryId: String = DefaultCategories.OTHER` lacks `@ColumnInfo(defaultValue = "other")`. In Room, a Kotlin default parameter value is not automatically translated into `DEFAULT 'other'` in the SQLite `CREATE TABLE` statement; thus, deleting a category directly in SQL without calling `expenseDao.reassignCategory(category.id, DefaultCategories.OTHER)` (as `ExpenseRepository.deleteCategory` currently does at line 40) would fail with `NOT NULL constraint failed`.
- **Recommended Fix:** Add `@ColumnInfo(defaultValue = "other")` alongside a Room schema bump, or switch to `ForeignKey.RESTRICT` while keeping explicit reassignment inside `@Transaction`.

---

### 5. `[MONA-FIN-005]` (Medium) — Room Schema Export Disabled (`exportSchema = false`) and Missing Migration History
- **Exact Location:** [ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt).
- **Analysis:** Setting `exportSchema = false` prevents Room from generating historical schema JSON files (`schemas/com.monavoce.app.data.local.ExpenseDatabase/1.json`), making future `Migration(1, 2)` authoring and testing error-prone.

---

### 6. `[MONA-FIN-006]` (Low) — Hardcoded Saturday Week Start and No Automatic Midnight Refresh for `today`
- **Exact Location:** [DateTimeUtils.kt](../../app/src/main/java/com/myexpenses/app/domain/DateTimeUtils.kt) and [MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt).
- **Analysis:**
  - `startOfCurrentWeekMillis()` hardcodes `DayOfWeek.SATURDAY` (appropriate for Yemen and some Arab countries, but unsuitable for international users in Europe and the Americas where the week starts on Monday or Sunday).
  - `val today = LocalDate.now()` inside `combine(...)` in `MainViewModel` is not recomputed if the app remains open past 12:00 midnight without any emission from the five observed flows.

---

## 3. Verified Functional Capabilities

| Feature | Implementation Status | Evidence & Assessment |
|---|---|---|
| **Manual or voice expense entry** | 100% Complete | Supports amount, category, custom date, note, and raw voice transcript ([ExpenseRepository.kt](../../app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt)). |
| **Edit existing expense** | 100% Complete | `AddEditExpenseDialog` in [DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt) supports editing amount, category, and note. |
| **Delete expense with Undo & confirmation** | 100% Complete | Single deletion with confirmation + `SnackbarResult.ActionPerformed` undo restore, plus the voice command `"delete last expense"`. |
| **Custom categories CRUD** | 100% Complete | Add/edit/delete custom categories with Arabic and English names, 18 icons, 12 colors, and custom voice-matching keywords ([CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt)). |
| **Search, time filtering, and charts** | 100% Complete | Filter by (Day, Week, Month, Year, All), forward/back date navigation, instant search, `DonutChart`, and weekly bar chart ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)). |
| **CSV/JSON export & `FileProvider` sharing** | 100% Complete | UTF-8 CSV export with BOM (`\uFEFF`) for Arabic Excel compatibility, plus full JSON export/restore ([BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt)). |
| **Home screen AppWidget** | 100% Complete | Displays today's total and transaction count with a mic button that opens `QuickVoiceActivity` directly ([ExpenseWidgetProvider.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/ExpenseWidgetProvider.kt)). |
