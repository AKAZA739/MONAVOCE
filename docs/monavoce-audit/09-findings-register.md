# 09 — السجل الموحد للملاحظات والثغرات المكتشفة (Master Audit Findings Register)

يوثق هذا السجل جميع الملاحظات المكتشفة عبر مراحل التدقيق (الأمان، المحرك المالي، التعرف الصوتي، واجهة المستخدم، الأداء، المعمارية، وجاهزية متجر Google Play)، مرتبة ومصنفة بمعرفات قياسية (`MONA-*`) وأدلة رقمية دقيقة بالملف ورقم السطر.

---

## ملخص إحصائي للملاحظات

| درجة الخطورة (Severity) | العدد | المعرفات (Finding IDs) |
|---|---|---|
| **Critical (حرجة)** | **0** | لا يوجد انهيار قاتل يمنع تشغيل التطبيق أو تسريب بيانات شبكي. |
| **High (عالية / عائق إطلاق P0)** | **5** | `MONA-SEC-001`, `MONA-FIN-001`, `MONA-FIN-002`, `MONA-PLAY-001`, `MONA-PLAY-002` |
| **Medium (متوسطة / أولوية P1)** | **12** | `MONA-SEC-002`, `MONA-SEC-003`, `MONA-SEC-004`, `MONA-SEC-005`, `MONA-FIN-003`, `MONA-FIN-004`, `MONA-FIN-005`, `MONA-VOX-001`, `MONA-VOX-002`, `MONA-VOX-003`, `MONA-UX-001`, `MONA-UX-002`, `MONA-PERF-001`, `MONA-PLAY-003` *(14 عنصرًا تفصيليًا مدمجة في 12 معرفًا متوسطًا)* |
| **Low (منخفضة / تحسين P2)** | **5** | `MONA-SEC-006`, `MONA-FIN-006`, `MONA-VOX-004`, `MONA-UX-003`, `MONA-UX-004`, `MONA-ARCH-001` |
| **Informational (معلوماتية إيجابية)** | **1** | `MONA-SEC-007` |

---

## السجل التفصيلي الكامل للملاحظات (Detailed Findings Register)

### 1. `[MONA-SEC-001]` — حقن المعادلات في ملفات CSV المصدرة (CSV Formula Injection / CWE-1236)
- **الفئة:** Security (`MASVS-CODE-2` / `CWE-1236`)
- **الخطورة:** **High (P0)**
- **الملف والأسطر المتأثرة:** [BackupManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L149-L156) (`csvEscape`) والأسطر [44-59](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L44-L59) (`exportCsv`).
- **الدليل التقني:** الدالة `csvEscape(value: String)` تفحص فقط وجود `,` أو `"` أو `\n` أو `\r`، ولا تعالج النصوص التي تبدأ بـ `=`, `+`, `-`, `@`, `\t`, `\r`.
- **السبب الجذري:** غياب تطهير محارف التحكم الخاصة ببرامج جداول البيانات (Excel / Google Sheets / Calc) قبل كتابة حقول `note` و`rawVoiceText` وأسماء الفئات.
- **الأثر:** تنفيذ معادلات غير مصرح بها أو روابط خبيثة (`=HYPERLINK(...)` أو أوامر DDE) عند فتح ملف CSV المصدر على الكمبيوتر.
- **الإصلاح المقترح:** تعديل `csvEscape` لتتحقق مما إذا كان الحقل النصي يبدأ بـ `=`, `+`, `-`, `@`, `\t`, `\r`، وإضافة فاصلة علوية مفردة `'` في بدايته قبل تغليفه بعلامات تنصيص.
- **معيار التحقق (Verification Criteria):** اختبار وحدة يصدر مصروفًا ملاحظته `"=1+1"` و`"-500+SUM(A1:A2)"` ويتأكد من خروجها في CSV بصيغة `"'=1+1"` و`"'-500+SUM(A1:A2)"`.

---

### 2. `[MONA-FIN-001]` — جمع المبالغ المالية عبر عملات مختلفة دون تصفية أو تجميع حسب `currencyCode`
- **الفئة:** Financial Engine & Data Integrity
- **الخطورة:** **High (P0)**
- **الملف والأسطر المتأثرة:**
  - [MainViewModel.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/MainViewModel.kt#L88-L152) (`todayTotal`, `weekTotal`, `monthTotal`, `filteredTotal`, `categoryBreakdown`, `dailyTotals`).
  - [ExpenseDao.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/local/ExpenseDao.kt#L29-L45) (`observeTotalInRange`, `observeCategoryTotalsInRange`).
  - [DashboardScreen.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt#L540) (`ExpenseListItemCard`).
- **الدليل التقني:** كل سجل في `ExpenseEntity` يحفظ `currencyCode` وقت إنشائه (مثلاً `"YER"`). عند تغيير العملة في الإعدادات إلى `"USD"` أو `"SAR"`, يستمر `allExpenses.filter { ... }.sumOf { it.expense.amount }` بجمع كل السجلات معًا بغض النظر عن `currencyCode`، وتُعرض كل البطاقات برمز العملة الجديد.
- **السبب الجذري:** افتراض وجود عملة واحدة ثابتة في استعلامات الجمع رغم تخزين `currencyCode` في كل صف.
- **الأثر:** أرقام وإحصائيات مالية خاطئة تمامًا بمجرد تبديل العملة في الإعدادات أو استيراد بيانات متعددة العملات.
- **الإصلاح المقترح:**
  1. عرض رمز العملة الخاص بكل مصروف (`CurrencyOption.fromCode(item.expense.currencyCode)`) في قائمة المعاملات، أو تصفية الإجماليات حسب العملة النشطة `preferences.currency.code` مع خيار واضح في الإعدادات ("تغيير رمز العملة لجميع السجلات السابقة أم للسجلات الجديدة فقط؟").
  2. تحديث استعلامات `ExpenseWidgetProvider` و`MainViewModel` لتلتزم بعملة التصفية المعتمدة.
- **معيار التحقق:** اختبار وحدة يدرج مصروفًا بـ `YER` وآخر بـ `USD` ويتحقق من عدم جمعهما معًا كرقم واحد تحت عملة واحدة دون تحويل أو تصفية.

---

### 3. `[MONA-FIN-002]` — استعادة النسخة الاحتياطية (`restoreJsonBackup`) تحذف البيانات خارج معاملة ذرية (`@Transaction`) ودون تحقق مسبق
- **الفئة:** Financial Data Integrity & Reliability
- **الخطورة:** **High (P0)**
- **الملف والأسطر المتأثرة:**
  - [ExpenseRepository.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt#L108-L122) (`replaceAllFromBackup`).
  - [BackupManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L100-L147) (`restoreJsonBackup`).
- **الدليل التقني:** تستدعي `replaceAllFromBackup` الدالة `expenseDao.deleteAll()` ثم `expenseDao.insertAll(expenses)` بدون `database.withTransaction`. كما أن `restoreJsonBackup` لا يتحقق من وجود `categoryId` في جدول الفئات قبل الإدراج ولا يرفض المبالغ السالبة أو `NaN`.
- **السبب الجذري:** تنفيذ عمليات حذف وإدراج متعددة متتابعة خارج معاملة قاعدة بيانات واحدة، مع وجود قيد `FOREIGN KEY` قد يفشل أثناء `insertAll(expenses)` بعد تنفيذ `deleteAll()`.
- **الأثر:** فقدان كامل ونهائي لجميع مصاريف المستخدم إذا فشل إدراج سجل واحد أثناء استعادة ملف JSON.
- **الإصلاح المقترح:**
  1. تمرير مرجع `ExpenseDatabase` إلى `ExpenseRepository` وتنفيذ `replaceAllFromBackup` داخل `database.withTransaction { ... }`.
  2. التحقق المسبق في `BackupManager` من صحة كل سجل (`amount > 0 && amount.isFinite()`)، وربط أي `categoryId` غير موجود بـ `DefaultCategories.OTHER` قبل بدء المعاملة.
- **معيار التحقق:** اختبار يحاول استعادة ملف JSON يحتوي على سجل تالف ويتأكد من بقاء البيانات الأصلية في قاعدة البيانات دون أي نقص (`Atomic Rollback`).

---

### 4. `[MONA-PLAY-001]` — تعارض معرف الحزمة (`com.myexpenses.app`) والاسم الحالي (`My Expenses`) مع العلامة المعتمدة `MONAVOCE` ومتجر Google Play
- **الفئة:** Brand Identity & Google Play Compliance
- **الخطورة:** **High (P0 — Play Store Blocker)**
- **الملف والأسطر المتأثرة:** [app/build.gradle.kts](file:///d:/My%20expensives/app/build.gradle.kts#L13-L14), [AndroidManifest.xml](file:///d:/My%20expensives/app/src/main/AndroidManifest.xml#L12-L74), [strings.xml](file:///d:/My%20expensives/app/src/main/res/values/strings.xml#L2).
- **الدليل التقني:** `applicationId = "com.myexpenses.app"` والاسم `"My Expenses | مصاريفي"`.
- **الأثر:** رفض محتمل في Google Play بسبب تشابه الاسم مع تطبيق `"My Expenses"` الشهير، وعدم تطابق الحزمة مع الهوية العالمية المعتمدة **MONAVOCE (موناڤوتشي)**.
- **الإصلاح المقترح:** تحديث `applicationId` و`namespace` إلى `com.monavoce.app`، واسم التطبيق إلى `MONAVOCE` / `MONAVOCE | موناڤوتشي`، وتحديث أيقونة التطبيق التكيفية وشاشة البداية وفق مواصفات [06-brand-identity.md](file:///d:/My%20expensives/docs/monavoce-audit/06-brand-identity.md).
- **معيار التحقق:** بناء ملف `.aab` و`.apk` والتحقق عبر `aapt2 dump badging` من أن `package: name='com.monavoce.app'` والاسم `MONAVOCE`.

---

### 5. `[MONA-PLAY-002]` — حزمة النموذج الصوتي العربي (`130 MB`) غير مدمجة عبر Play Asset Delivery داخل حزمة `.aab`
- **الفئة:** Google Play Packaging & Offline Readiness
- **الخطورة:** **High (P0 — Play Store Blocker)**
- **الملف والأسطر المتأثرة:** [build.gradle.kts](file:///d:/My%20expensives/build.gradle.kts), [settings.gradle.kts](file:///d:/My%20expensives/settings.gradle.kts), [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L66-L81).
- **الدليل التقني:** النموذج الصوتي الخارجي (`dist/MyExpenses-Offline-Arabic-Model.zip` بحجم `101.2 MiB`) غير مضمن كـ Asset Pack في بناء `.aab`، والكود يبحث فقط في `assets/offline_asr_ar/` (الفارغ في البناء لتجنب تضخم APK) أو في `getExternalFilesDir`.
- **الأثر:** المستخدم الذي يثبت التطبيق من متجر Google Play سيجد المحرك العربي الأوفلاين غير مثبت افتراضيًا وسيُطلب منه استيراد ملف ZIP لا يملكه على هاتفه!
- **الإصلاح المقترح:** إضافة وحدة Gradle من نوع `com.android.asset-pack` (`install-time` delivery) تضم الملفات الأربعة للنموذج داخل ملف `.aab`، بحيث يقرأها `SherpaOfflineArabicEngine` مباشرة من `context.assets` عند التثبيت من Google Play بدون إنترنت وبدون استيراد يدوي.
- **معيار التحقق:** بناء `./gradlew bundleRelease` والتحقق من احتواء ملف `.aab` على حزمة الأصول `arabic_asr_pack` ونجاح `SherpaOfflineArabicEngine.isModelAvailable(context)` فور التثبيت.

---

### 6. `[MONA-SEC-002]` — غياب سقف الحجم الأقصى عند فك ضغط نموذج ZIP أو قراءة ملف النسخ الاحتياطي JSON (CWE-409)
- **الفئة:** Security (`MASVS-CODE-2` / `CWE-409`)
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L101-L115), [BackupManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L101-L103).
- **الدليل التقني:** `zis.read(buffer)` ينسخ البايتات دون عداد حد أقصى لكل ملف، و`input.bufferedReader().readText()` يقرأ ملف JSON بالكامل في الذاكرة دون التحقق من حجمه.
- **الإصلاح المقترح:** فرض حد أقصى `150 MB` لكل ملف داخل `importModelFromZipUri` (و`200 MB` لإجمالي الأرشيف)، وحد أقصى `15 MB` لملف JSON في `restoreJsonBackup`.
- **معيار التحقق:** اختبار يمرر تيار بيانات يتجاوز الحد الأقصى ويتأكد من إيقاف القراءة وإرجاع `Result.failure` بأمان دون `OutOfMemoryError`.

---

### 7. `[MONA-SEC-003]` — النشاط المصدر `QuickVoiceActivity` يفتح الميكروفون تلقائيًا لأي `Intent` خارجي دون قيود
- **الفئة:** Security (`MASVS-PLATFORM-1`)
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [AndroidManifest.xml](file:///d:/My%20expensives/app/src/main/AndroidManifest.xml#L48-L75), [QuickVoiceActivity.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/QuickVoiceActivity.kt#L76-L79).
- **الدليل التقني:** `QuickVoiceActivity` مصدّر (`android:exported="true"`) بدون صلاحية، ويستدعي `startOrRequestMic()` فورًا في `LaunchedEffect(Unit)`.
- **الإصلاح المقترح:** التحقق من الفعل (`intent?.action`) ومصدر الاستدعاء، وإضافة اهتزاز خفيف/مؤشر واضح عند بدء التسجيل مع عدم تنفيذ أوامر الحذف الصوتية (`DeleteLastExpense`) تلقائيًا من نافذة القفل السريعة دون تأكيد بصري.
- **معيار التحقق:** التحقق من عدم إمكانية حذف مصروف عبر استدعاء خارجي صامت.

---

### 8. `[MONA-SEC-004]` — طباعة النصوص الصوتية المسجلة كاملة في `Logcat` في نسخة الإطلاق (CWE-532)
- **الفئة:** Security & Privacy (`MASVS-STORAGE-2` / `CWE-532`)
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [OfflineSpeechManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/OfflineSpeechManager.kt#L480) والسطر [628](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/OfflineSpeechManager.kt#L628)، [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L283)، و[proguard-rules.pro](file:///d:/My%20expensives/app/proguard-rules.pro).
- **الدليل التقني:** `Log.i(TAG, "Sherpa-ONNX offline decode done ...: '$rawText'")` يطبع كلام المستخدم المالي في سجلات النظام.
- **الإصلاح المقترح:** عدم طباعة محتوى `rawText` أو `best` في السجلات (الاكتفاء بطول النص وزمن المعالجة `decodeMs`)، وإضافة قاعدة `-assumenosideeffects class android.util.Log { public static int d(...); public static int i(...); public static int v(...); }` في `proguard-rules.pro`.
- **معيار التحقق:** فحص `adb logcat` أثناء تسجيل مصروف صوتي في نسخة `Release` والتأكد من عدم ظهور النص المنطوق في السجلات.

---

### 9. `[MONA-SEC-005]` — تخزين ملفات النموذج الصوتي في المساحة الخارجية للتطبيق (`getExternalFilesDir`) بدل المساحة الداخلية المحمية (`filesDir`)
- **الفئة:** Security (`MASVS-STORAGE-1`)
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L49-L53).
- **الدليل التقني:** `getModelDir` يفضل `context.getExternalFilesDir("offline_asr_ar")` على `context.filesDir`.
- **الإصلاح المقترح:** جعل المسار الأساسي لاستيراد وفك ضغط النموذج هو المساحة الداخلية المحمية `File(context.filesDir, "offline_asr_ar")`، مع الإبقاء على قراءة `getExternalFilesDir` كمسار توافق رجعي (Fallback) إذا كانت الملفات موجودة فيه مسبقًا.
- **معيار التحقق:** استيراد النموذج عبر الواجهة والتحقق من حفظه داخل `/data/data/com.monavoce.app/files/offline_asr_ar/`.

---

### 10. `[MONA-FIN-003]` — استخدام `Double` بدون تقريب عشري صارم (`BigDecimal` Rounding) قبل الحفظ والجمع
- **الفئة:** Financial Accuracy
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [ExpenseEntity.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/local/ExpenseEntity.kt#L28), [ExpenseRepository.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt#L46-L77).
- **الإصلاح المقترح:** إضافة دالة `CurrencyFormatter.roundMoney(amount: Double): Double` تستخدم `BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()` وتطبيقها عند إدراج أو تعديل أي مصروف وعند حساب الإجماليات.
- **معيار التحقق:** اختبار وحدة يجمع `0.1 + 0.2` ويتأكد من أن القيمة المخزنة والمصدرة في JSON هي `0.3` بالضبط وليست `0.30000000000000004`.

---

### 11. `[MONA-FIN-004]` — نقص `@ColumnInfo(defaultValue = "other")` على عمود `categoryId` في `ExpenseEntity`
- **الفئة:** Database Schema Integrity
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [ExpenseEntity.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/local/ExpenseEntity.kt#L12-L30).
- **الإصلاح المقترح:** إضافة `@ColumnInfo(defaultValue = DefaultCategories.OTHER)` إلى الحقل `categoryId`، وتغليف `deleteCategory` في [ExpenseRepository.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt#L38-L43) داخل `database.withTransaction`.
- **معيار التحقق:** نجاح حذف فئة مخصصة تحتوي على مصاريف مرتبطة بها وتحولها ذريًا إلى الفئة `"other"`.

---

### 12. `[MONA-FIN-005]` — تعطيل تصدير مخطط Room (`exportSchema = false`)
- **الفئة:** Database Maintainability
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [ExpenseDatabase.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt#L21), [app/build.gradle.kts](file:///d:/My%20expensives/app/build.gradle.kts).
- **الإصلاح المقترح:** تفعيل `exportSchema = true` وتحديد `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` في `app/build.gradle.kts`.
- **معيار التحقق:** تولد ملف `app/schemas/.../1.json` عند البناء.

---

### 13. `[MONA-VOX-001]` — تقسيم الجملة الصوتية بالخطأ عند وجود رقم كمية قبل اسم السلعة متبوعًا بالسعر
- **الفئة:** Voice NLP Parser
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [VoiceInputParser.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/domain/VoiceInputParser.kt#L244-L280) (`splitIntoExpenseClauses`).
- **الدليل التقني:** جملة `"اشتريت 2 حليب ب 1000 ريال"` تُقسم إلى مصروفين (`2 ريال` و`1000 ريال`).
- **الإصلاح المقترح:** تحسين `splitIntoExpenseClauses` للتعرف على أنماط الكمية والسعر (`<عدد صغير> <اسم سلعة> ب/بسعر/بمبلغ/for <السعر>`) ودمجها في مصروف واحد بالمبلغ الثاني.
- **معيار التحقق:** إضافة اختبار وحدة لجملة `"اشتريت 2 حليب ب 1000 ريال"` والتأكد من إرجاع مصروف واحد بمبلغ `1000.0` وملاحظة `"2 حليب"` في فئة `"food"`.

---

### 14. `[MONA-VOX-002]` — أمر `"عدل آخر مصروف"` يتطلب ذكر مبلغ جديد ويرفض تعديل الفئة وحدها
- **الفئة:** Voice NLP Parser
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [VoiceInputParser.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/domain/VoiceInputParser.kt#L27-L31) والأسطر [168-182](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/domain/VoiceInputParser.kt#L168-L182).
- **الإصلاح المقترح:** جعل `newAmount: Double?` اختياريًا في `VoiceCommandResult.UpdateLastExpense` بحيث يمكن للمستخدم تعديل المبلغ أو الفئة أو كليهما.
- **معيار التحقق:** اختبار وحدة للجملة `"عدل آخر مصروف إلى المواصلات"` يرجع `UpdateLastExpense(newAmount = null, newCategoryId = "transport", ...)`.

---

### 15. `[MONA-VOX-003]` — تحميل نموذج Sherpa-ONNX (`130 MB`) في `Application.onCreate()` حتى عند التحديث الخلفي للودجت
- **الفئة:** Performance & Memory Management
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [MyExpensesApp.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/MyExpensesApp.kt#L31-L33).
- **الإصلاح المقترح:** إزالة `warmUpAsync()` من `MyExpensesApp.onCreate()` واستدعاؤها في `MainActivity.onCreate()` و`QuickVoiceActivity.onCreate()`.
- **معيار التحقق:** عدم تهيئة `SherpaArabicEngine` في `Logcat` عند إرسال `APPWIDGET_UPDATE` والتطبيق مغلق.

---

### 16. `[MONA-UX-001]` — ألوان الوضع الليلي الصلبة في `VoiceOverlayPanel` تتجاهل الوضع النهاري (`Light Theme`)
- **الفئة:** UI/UX & Theme Consistency
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [VoiceOverlayPanel.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt#L80-L83) والأسطر [270-500](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt#L270-L500).
- **الإصلاح المقترح:** ربط ألوان خلفية اللوحة الصوتية والبطاقات الفرعية برموز ثيم **MONAVOCE** (`Obsidian Vault` / `Alabaster Prestige`).
- **معيار التحقق:** فتح اللوحة الصوتية في الوضع النهاري والوضع الليلي والتحقق من وضوح وتناسق جميع النصوص والأزرار.

---

### 17. `[MONA-UX-002]` — غياب `contentDescription` لمستخدمي TalkBack وصغر حجم بعض الأزرار عن `48.dp`
- **الفئة:** Accessibility (WCAG 2.1 / Android Accessibility)
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [VoiceOverlayPanel.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt#L603-L666), [DashboardScreen.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt#L138-L157) والسطر [552](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt#L552), [CategoriesScreen.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt#L150-L158).
- **الإصلاح المقترح:** إضافة `semantics { contentDescription = ...; role = Role.Button }` للكرة الصوتية، وتزويد كافة الأزرار الأيقونية بنصوص وصفية ثنائية اللغة، وتوسيع مساحة اللمس (`minimumInteractiveComponentSize` / `48.dp`).
- **معيار التحقق:** فحص الشاشات بأداة Android Accessibility Scanner أو TalkBack دون أخطاء `Missing speakable text` أو `Touch target size`.

---

### 18. `[MONA-PERF-001]` — تصفية وحساب إجماليات جدول المصاريف بالكامل في الذاكرة على الخيط الرئيسي في `MainViewModel`
- **الفئة:** Performance & Scalability
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [MainViewModel.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/ui/MainViewModel.kt#L75-L186).
- **الإصلاح المقترح:** تشغيل تحويلات `combine` الثقيلة على `Dispatchers.Default` (`flowOn(Dispatchers.Default)`) وتحسين التصفية لتجنب تكرار المسح الخطي للقائمة.
- **معيار التحقق:** اختبار الأداء مع `10,000` مصروف وهمي دون تجاوز زمن الإطار `16ms` على خيط الواجهة.

---

### 19. `[MONA-PLAY-003]` — خدمة `ExpenseStubRecognitionService` ترجع `ERROR_CLIENT` وقد تسترعي فحص Google Play
- **الفئة:** Google Play Policy & OS Integration
- **الخطورة:** **Medium (P1)**
- **الملف والأسطر المتأثرة:** [ExpenseAssistantServices.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/service/ExpenseAssistantServices.kt#L51-L65).
- **الإصلاح المقترح:** ربط الخدمة بمحرك التعرف المحلي أو توثيق دورها كخدمة مرافقة لـ `ExpenseVoiceInteractionService` في بيانات مراجعة Google Play Console.

---

### 20. الملاحظات المنخفضة والمعلوماتية (`Low / P2` & `Informational`)
- **`[MONA-SEC-006]` (Low):** إزالة التراجع الصامت إلى مفتاح `debug` عند بناء `release` في [app/build.gradle.kts](file:///d:/My%20expensives/app/build.gradle.kts#L51-L52) إذا كان البناء في بيئة CI.
- **`[MONA-FIN-006]` (Low):** جعل بداية الأسبوع قابلة للتخصيص في الإعدادات (`السبت / الأحد / الإثنين`) بدلاً من تثبيتها على السبت في [DateTimeUtils.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/DateTimeUtils.kt#L33).
- **`[MONA-VOX-004]` (Low):** قيد نظام أندرويد بخصوص 3 ضغطات زر الباور؛ تم حله عمليًا عبر `Press & Hold Assistant` + `Quick Settings Tile`.
- **`[MONA-UX-003]` (Low):** إضافة شاشة ترحيب أولية مختصرة (`MONAVOCE Onboarding`) عند فتح التطبيق لأول مرة.
- **`[MONA-UX-004]` (Low):** ترحيل النصوص الثابتة تدريجيًا إلى موارد `strings.xml` لتسهيل إضافة لغات ثالثة مستقبلاً.
- **`[MONA-ARCH-001]` (Low):** استخدام `goAsync()` في `ExpenseWidgetProvider.onUpdate` وتنظيف دوال `ExpenseDao` غير المستدعاة.
- **`[MONA-SEC-007]` (Informational — Positive):** توثيق خلو ملف البيان المدمج النهائي تمامًا من صلاحية `INTERNET`.
