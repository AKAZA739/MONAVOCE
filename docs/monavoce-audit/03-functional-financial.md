<div dir="rtl" lang="ar">

# 03 — تدقيق الوظائف والمحرك المالي وسلامة البيانات (Functional & Financial Engine Audit)

يفحص هذا التقرير دقة العمليات الحسابية المالية، وسلامة مخطط قاعدة البيانات (**Room SQLite**)، وآليات التصدير والاستيراد، وحالات الحافة المحاسبية في التطبيق.

---

## 1. فحص هيكل قاعدة البيانات والعلاقات (Database Schema & Entity Integrity)

تتكون قاعدة البيانات [ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt) من جدولين رئيسيين:

1. **جدول الفئات (`categories` — [CategoryEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/CategoryEntity.kt)):**
   - المفتاح الأساسي: `id: String` (مثل `"food"`, `"transport"`, `"other"`, أو `"custom_<UUID>"`).
   - الحقول: `nameAr`, `nameEn`, `iconName`, `colorHex: Long`, `keywordsCsv: String`, `isCustom: Boolean`, `sortOrder: Int`.
   - يتم بذر (Seeding) 8 فئات افتراضية ثنائية اللغة غنية بالكلمات المفتاحية العربية والإنجليزية عبر [CategoryEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/CategoryEntity.kt) داخل `onCreate` و`seedDefaultsIfEmpty()`.

2. **جدول المصاريف (`expenses` — [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt)):**
   - المفتاح الأساسي: `id: Long` (`autoGenerate = true`).
   - المفتاح الأجنبي: `categoryId: String` مرتبط بـ `CategoryEntity.id` مع فهرسين على `categoryId` و`timestampMillis`.
   - الحقول المالية والزمنية: `amount: Double`, `currencyCode: String = "YER"`, `note: String`, `rawVoiceText: String?`, `timestampMillis: Long`, `createdAtMillis: Long`.

---

## 2. الأخطاء والثغرات المحاسبية والمعمارية المكتشفة في المحرك المالي

### 1. `[MONA-FIN-001]` (High / Release Blocker) — خلل جمع العملات المتعددة دون تصفية أو تجميع حسب `currencyCode`
- **المواقع الدقيقة:**
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
- **الدليل والسبب الجذري:**
  - عندما يسجل المستخدم مصروفًا (سواء بالصوت أو يدويًا)، يتم حفظ العملة المختارة حاليًا في الإعدادات داخل الحقل `expense.currencyCode` ([MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt)).
  - إذا سجل المستخدم مصروفًا بقيمة `5,000 YER` (ريال يمني)، ثم غيّر العملة في شاشة الإعدادات ([SettingsScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/SettingsScreen.kt)) إلى `USD` (دولار أمريكي) وسجل مصروفًا بقيمة `20 USD`، فإن `MainViewModel` يجمع الرقمين معًا مباشرة (`5000.0 + 20.0 = 5020.0`) ويعرض الإجمالي في لوحة التحكم والإحصائيات والودجت على أنه **`5,020 $` (دولار أمريكي)**!
  - كذلك في بطاقة المصروف الفردية ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt))، يتم عرض رمز العملة العام `state.preferences.currency.symbol(isArabic)` بدلاً من رمز عملة السجل نفسه `item.expense.currencyCode`!
- **الأثر المحاسبي:** تشويه كامل للإجماليات المالية ونسب الفئات والرسوم البيانية بمجرد تغيير العملة أو استيراد سجلات بعملات متعددة.
- **الإصلاح المقترح:**
  1. تصفية الإجماليات والرسوم البيانية في `MainViewModel` و`ExpenseDao` حسب العملة النشطة `it.expense.currencyCode == prefs.currency.code` (أو توفير خيار تحويل/توحيد العملة عند تغييرها في الإعدادات).
  2. عرض رمز العملة الحقيقي المخزن في كل سجل `CurrencyOption.fromCode(item.expense.currencyCode).symbol(isArabic)` في قائمة المعاملات.

---

### 2. `[MONA-FIN-002]` (High / Release Blocker) — استعادة النسخة الاحتياطية تحذف جميع البيانات خارج معاملة ذرية (`@Transaction`) ودون تحقق مسبق
- **المواقع الدقيقة:**
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
- **الدليل والسبب الجذري:**
  1. الدالة `replaceAllFromBackup` ليست مغلفة داخل `database.withTransaction { ... }`. إذا حدث خطأ أثناء `expenseDao.insertAll(expenses)` (أو أُغلق التطبيق بعد تنفيذ `expenseDao.deleteAll()`)، يفقد المستخدم جميع مصاريفه السابقة إلى الأبد ولا يتم التراجع (Rollback) عن الحذف!
  2. في `BackupManager.restoreJsonBackup` (السطر 133): `amount = obj.getDouble("amount")` لا يتحقق من أن المبلغ أكبر من الصفر (`amount > 0 && amount.isFinite()`)، ولا يتحقق من أن `categoryId` الخاص بكل مصروف مستورد موجود فعلاً في جدول `categories` قبل الإدراج. وبسبب وجود قيد المفتاح الأجنبي `ForeignKey` في `ExpenseEntity` (الأسطر 12–18)، إذا احتوى ملف النسخة الاحتياطية على مصروف يشير إلى `categoryId` غير موجود، فإن `expenseDao.insertAll(expenses)` سيرمي استثناء `SQLiteConstraintException (FOREIGN KEY constraint failed)` **بعد** أن يكون `expenseDao.deleteAll()` قد مسح جميع بيانات المستخدم!
- **الأثر المحاسبي:** خطر فقدان كلي للبيانات المالية عند استعادة ملف JSON يحتوي على فئة محذوفة أو سجل غير مكتمل.
- **الإصلاح المقترح:**
  1. تغليف عملية الاستعادة بالكامل داخل `database.withTransaction { ... }`.
  2. التحقق في `BackupManager` من أن جميع `categoryId` المشار إليها في `expenses` موجودة إما في قائمة `categories` المستوردة أو الفئات الافتراضية، وإلا يتم إعادة توجيهها تلقائيًا إلى `DefaultCategories.OTHER` مع رفض المبالغ غير الصالحة (`<= 0` أو `NaN`/`Infinite`).

---

### 3. `[MONA-FIN-003]` (Medium) — استخدام `Double` (فاصلة عائمة ثنائية IEEE-754) لتمثيل المبالغ المالية
- **الموقع الدقيق:** [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt) (`val amount: Double`).
- **التحليل:**
  - دالة التنسيق `DateTimeUtils.formatAmount` ([DateTimeUtils.kt](../../app/src/main/java/com/myexpenses/app/domain/DateTimeUtils.kt)) تستخدم `DecimalFormat("#,##0.##")` مما يخفي أخطاء التقريب البسيطة عند العرض (مثل `0.1 + 0.2 = 0.30000000000000004` التي تُعرض `0.3`).
  - ومع ذلك، عند تراكم آلاف المعاملات الكسرية بالعملات ذات السنتات/الهللات/الفلسات (`USD`, `EUR`, `SAR`, `AED`, `KWD`) أو عند تصدير المبلغ الخام في JSON (`put("amount", item.expense.amount)` في [BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt))، يمكن أن تظهر كسور عشرية غير دقيقة مثل `1404.8500000000001`.
- **الإصلاح المقترح:** تقريب المبالغ دائمًا إلى منزلتين عشريتين (أو 3 منازل للدينار الكويتي) عبر `BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()` قبل الحفظ والجمع والتصدير، أو الانتقال مستقبلاً إلى تخزين الوحدات الصغرى (`amountMinorUnits: Long`).

---

### 4. `[MONA-FIN-004]` (Medium) — عدم تطابق تعريف `onDelete = ForeignKey.SET_DEFAULT` مع غياب `@ColumnInfo(defaultValue = "other")`
- **الموقع الدقيق:** [ExpenseEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/ExpenseEntity.kt).
- **التحليل:** تم تحديد `onDelete = ForeignKey.SET_DEFAULT` في ترويسة `@Entity`، ولكن العمود `val categoryId: String = DefaultCategories.OTHER` لا يحمل التعليق التوضيحي `@ColumnInfo(defaultValue = "other")`. في Room، القيمة الافتراضية في لغة Kotlin لا تُترجم تلقائيًا إلى `DEFAULT 'other'` في جملة SQLite `CREATE TABLE`؛ لذا فإن حذف فئة مباشرة في SQL بدون استدعاء `expenseDao.reassignCategory(category.id, DefaultCategories.OTHER)` (كما يفعل `ExpenseRepository.deleteCategory` حاليًا في السطر 40) سيؤدي إلى خطأ `NOT NULL constraint failed`.
- **الإصلاح المقترح:** إضافة `@ColumnInfo(defaultValue = "other")` مع ترقية مخطط Room أو تغيير السياسة إلى `ForeignKey.RESTRICT` مع الإبقاء على إعادة التعيين الصريحة داخل معاملة `@Transaction`.

---

### 5. `[MONA-FIN-005]` (Medium) — غياب تصدير مخطط Room (`exportSchema = false`) وخطط الترحيل (`Migrations`)
- **الموقع الدقيق:** [ExpenseDatabase.kt](../../app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt).
- **التحليل:** ضبط `exportSchema = false` يمنع Room من حفظ ملفات JSON التاريخية للمخطط (`schemas/com.monavoce.app.data.local.ExpenseDatabase/1.json`)، مما يجعل كتابة واختبار `Migration(1, 2)` في الإصدارات القادمة عرضة للأخطاء.

---

### 6. `[MONA-FIN-006]` (Low) — تثبيت بداية الأسبوع على يوم السبت وعدم تحديث تاريخ `today` تلقائيًا عند تجاوز منتصف الليل
- **الموقع الدقيق:** [DateTimeUtils.kt](../../app/src/main/java/com/myexpenses/app/domain/DateTimeUtils.kt) و [MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt).
- **التحليل:**
  - `startOfCurrentWeekMillis()` يثبت بداية الأسبوع على `DayOfWeek.SATURDAY` (مناسب لليمن وبعض الدول العربية، لكنه غير مناسب للمستخدمين الدوليين في أوروبا وأمريكا حيث يبدأ الأسبوع الإثنين أو الأحد).
  - `val today = LocalDate.now()` داخل `combine(...)` في `MainViewModel` لا يُعاد حسابه إذا بقي التطبيق مفتوحًا بعد الساعة 12:00 منتصف الليل دون حدوث تغيير في التدفقات الخمسة المراقبة.

---

## 3. تقييم الوظائف المنفذة فعليًا (Verified Functional Capabilities)

| الوظيفة | حالة التنفيذ الفعلي | التقييم والدليل |
|---|---|---|
| **إضافة مصروف يدويًا أو صوتيًا** | مكتملة 100% | تدعم المبلغ، الفئة، التاريخ المخصص، الملاحظة، والنص الصوتي الأصلي ([ExpenseRepository.kt](../../app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt)). |
| **تعديل مصروف قائم (`Edit Expense`)** | مكتملة 100% | نافذة `AddEditExpenseDialog` في [DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt) تسمح بتعديل المبلغ والفئة والملاحظة. |
| **حذف مصروف مع التراجع (`Undo`) ونافذة تأكيد** | مكتملة 100% | حذف فردي مع تأكيد + `SnackbarResult.ActionPerformed` لاستعادة المصروف المحذوف، بالإضافة إلى أمر صوتي `"احذف آخر مصروف"`. |
| **إدارة الفئات المخصصة (`Categories CRUD`)** | مكتملة 100% | إضافة/تعديل/حذف فئات مخصصة بأسماء عربية وإنجليزية، واختيار من 18 أيقونة، و12 لونًا، وكلمات مفتاحية مخصصة للتصنيف الصوتي ([CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt)). |
| **البحث والتصفية الزمنية والرسوم البيانية** | مكتملة 100% | تصفية حسب (اليوم، الأسبوع، الشهر، السنة، الكل)، تصفح التواريخ للأمام والخلف، بحث فوري، ومخطط دائري `DonutChart` + أعمدة أسبوعية ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)). |
| **تصدير CSV وJSON ومشاركتها عبر `FileProvider`** | مكتملة 100% | تصدير CSV بترميز UTF-8 مع BOM (`\uFEFF`) لدعم العربية في Excel، وتصدير/استعادة JSON كامل ([BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt)). |
| **الودجت (`AppWidget`)** | مكتملة 100% | يعرض إجمالي اليوم وعدد العمليات مع زر مايكروفون يفتح `QuickVoiceActivity` مباشرة ([ExpenseWidgetProvider.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/ExpenseWidgetProvider.kt)). |

</div>
