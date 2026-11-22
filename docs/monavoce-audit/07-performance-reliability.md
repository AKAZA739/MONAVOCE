<div dir="rtl" lang="ar">

# 07 — تدقيق الأداء والاعتمادية والقياسات الفعلية (Performance, Reliability & Benchmarks Audit)

يعتمد هذا التقرير على قياسات فعلية أُجريت على الهاتف الحقيقي المتصل (**Samsung Galaxy A54 5G — `SM-A546E` — Exynos 1380 `arm64-v8a` — Android 16 / One UI 8.5**) وعلى مخرجات بناء النسخة النهائية (`Release` و`Debug`).

---

## 1. القياسات الفعلية المسجلة (Measured Hardware & Build Benchmarks)

| المؤشر المقاس (Metric) | القيمة الفعلية المقاسة | الأداة / المصدر | التقييم الهندسي |
|---|---|---|---|
| **حجم ملف الإطلاق (`Release APK` مع R8)** | **`40,507,911` بايت (`38.63 MiB`)** | `dist/MyExpenses-v1.3.0-release.apk` | ممتاز مقارنة بتضمين مكتبات JNI لـ `arm64-v8a` و`armeabi-v7a` و`x86_64`. |
| **حجم ملف الإطلاق عند تقسيم المعماريات (`ABI Split arm64-v8a`)** | **`~15.8 MiB` (تقديري عبر AAB)** | تحليل حجم `libonnxruntime.so` + `libsherpa-onnx-jni.so` | عند الرفع بصيغة `.aab` على Google Play، سيحمّل هاتف المستخدم مكتبة معماريته فقط (`arm64-v8a`) مما يخفض حجم التطبيق الأساسي بأكثر من 58%. |
| **حجم ملف التطوير (`Debug APK`)** | **`57,440,274` بايت (`54.78 MiB`)** | `dist/MyExpenses-v1.3.0-debug.apk` | طبيعي لنسخة التطوير غير المضغوطة بـ R8. |
| **حجم حزمة النموذج العربي الأوفلاين (`Model ZIP`)** | **`106,106,155` بايت (`101.19 MiB`)** (ينفك إلى `~124.3 MiB`) | `dist/MyExpenses-Offline-Arabic-Model.zip` | مناسب جدًا لنموذج `FastConformer int8` عالي الدقة. |
| **زمن تنفيذ اختبارات الوحدة (`16 Unit Tests`)** | **`0.633s`** (`100% PASS`) | `TEST-com.myexpenses.app.VoiceInputParserTest.xml` | سريع جدًا ومستقر. |
| **زمن تهيئة محرك Sherpa-ONNX لأول مرة (`Cold Engine Init`)** | **`1,150ms – 1,368ms`** | `Logcat` على `SM-A546E` (`SherpaArabicEngine`) | يتم في خيط خلفي (`Dispatchers.Default`) ولا يجمد واجهة المستخدم. |
| **زمن فك التشفير الصوتي العربي (`Offline ASR Decode Latency`)** | **`179ms – 252ms`** لجملة من 3–5 ثوانٍ | `Logcat` على `SM-A546E` في وضع الطيران | استجابة فورية (أقل من ربع ثانية بعد انتهاء الكلام). |
| **زمن فتح نافذة الصوت السريعة (`QuickVoiceActivity Warm Launch`)** | **`~140ms – 210ms`** | `ActivityTaskManagerDisplayed` على `SM-A546E` | فوري بفضل خفة وزن النشاط الشفاف. |

---

## 2. تحليل استهلاك الذاكرة والبطارية والملاحظات الأدائية (Performance & Memory Findings)

### 1. `[MONA-PERF-001]` (Medium) — تحميل جدول المصاريف بالكامل إلى الذاكرة وإجراء التصفية والجمع على الخيط الرئيسي في `MainViewModel`
- **الموقع الدقيق:** [MainViewModel.kt](../../app/src/main/java/com/myexpenses/app/ui/viewmodel/MainViewModel.kt):
  ```kotlin
  val dashboardState: StateFlow<DashboardUiState> = combine(
      repo.observeAllExpenses(),
      repo.observeCategories(),
      prefs.preferencesFlow,
      selectedPeriod,
      selectedDate
  ) { allExpenses, categories, preferences, period, anchorDate ->
      val todayTotal = allExpenses.filter { ... }.sumOf { ... }
      val weekTotal = allExpenses.filter { ... }.sumOf { ... }
      val monthTotal = allExpenses.filter { ... }.sumOf { ... }
      val filteredByPeriod = allExpenses.filter { ... }
      ...
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
  ```
- **التحليل والسبب الجذري:**
  - يستمع `MainViewModel` إلى `repo.observeAllExpenses()` الذي يجلب **جميع** الصفوف من جدول `expenses` عبر SQLite، ثم يمرر القائمة الكاملة عبر أكثر من **8 حلقات `.filter { ... }` و`.sumOf { ... }` و`.groupBy { ... }`** داخل كتلة `combine` التي تعمل افتراضيًا على الخيط الرئيسي (`viewModelScope` -> `Dispatchers.Main.immediate`).
  - اللافت أن ملف [ExpenseDao.kt](../../app/src/main/java/com/myexpenses/app/data/local/dao/ExpenseDao.kt) يحتوي أصلاً على استعلامات SQL مفهرسة وجاهزة (`observeInRange`, `observeTotalInRange`, `observeCategoryTotalsInRange`)، لكن لم يتم استخدامها بعد إضافة ميزة تصفح التواريخ (`selectedDate`)!
- **الأثر الأدائي:** مع 100–500 مصروف لا يشعر المستخدم بأي بطء، لكن بعد استخدام التطبيق لعدة سنوات (مثلاً `10,000+` عملية)، فإن كل حرف يكتبه المستخدم في شريط البحث (`searchQuery`) أو كل تغيير للتاريخ سيعيد تخصيص آلاف الكائنات في الذاكرة على خيط الواجهة الرئيسي (`UI Jank / Frame Drops`).
- **الإصلاح المقترح:**
  1. نقل عمليات الحساب والتصفية الثقيلة إلى `.flowOn(Dispatchers.Default)` وفصل تصفية `searchQuery` عن إعادة حساب إجماليات اليوم والأسبوع والشهر.
  2. الاستفادة من استعلامات النطاق الزمني المفهرسة في `ExpenseDao` (`INDEX(timestampMillis)`).

---

### 2. `[MONA-VOX-003]` (Medium) — استهلاك `~220 MB` من الذاكرة الأصلية (`Native Heap`) عند تشغيل التطبيق في الخلفية بسبب التحميل المسبق غير المشروط لـ Sherpa-ONNX
- **الموقع الدقيق:** [MyExpensesApp.kt](../../app/src/main/java/com/myexpenses/app/MyExpensesApp.kt) (`container.sherpaArabicEngine.warmUpAsync()`).
- **التحليل:** كما وُثق في تقرير الصوت، فإن تحميل `OfflineRecognizer` في `Application.onCreate()` يحجز حوالي `200–250 MB` من الذاكرة الأصلية (`C++ ONNX Runtime Arena`) حتى لو كان النظام قد أيقظ عملية التطبيق في الخلفية فقط لتنفيذ `ExpenseWidgetProvider.onUpdate()`.
- **الإصلاح المقترح:** تفعيل `warmUpAsync()` بشكل كسول (Lazy Warm-up) فقط عند إنشاء `MainActivity` أو `QuickVoiceActivity` أو `ExpenseVoiceInteractionSessionService`، مع إمكانية تحرير المحرك (`release()`) عند استلام `Application.onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` وتوفير ذاكرة الهاتف.

---

### 3. `[MONA-ARCH-001]` (Low) — كود غير مستخدم (Dead/Unused DAO Queries) وإنشاء `CoroutineScope` غير مهيكل في `ExpenseWidgetProvider`
- **المواقع الدقيقة:**
  1. [ExpenseDao.kt](../../app/src/main/java/com/myexpenses/app/data/local/dao/ExpenseDao.kt) والسطر [57](../../app/src/main/java/com/myexpenses/app/data/local/dao/ExpenseDao.kt): الدوال `observeInRange`, `getInRange`, `observeTotalInRange`, `observeCountInRange`, `observeCategoryTotalsInRange`, `getAllRaw` غير مستدعاة حاليًا.
  2. [ExpenseWidgetProvider.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/ExpenseWidgetProvider.kt): ينشئ `CoroutineScope(Dispatchers.IO).launch` جديدًا في كل مرة يُستدعى فيها `onUpdate` أو `updateAllWidgets` دون استخدام `goAsync()` في `BroadcastReceiver.onUpdate`، مما قد يسمح للنظام بإنهاء العملية قبل اكتمال قراءة قاعدة البيانات إذا كان الجهاز تحت ضغط ذاكرة عالٍ.

---

## 3. تقييم الاعتمادية ومقاومة الانهيار (Crash & Lifecycle Resilience)

- **دورة حياة الميكروفون (`AudioRecord` & `SpeechRecognizer`):**
  - محمية بشكل ممتاز بـ `activeSessionId` ذري (`AtomicInteger`)، وتُغلق الجلسة فورًا في `QuickVoiceActivity.onStop()` وفي `MainViewModel.onCleared()`، مما يمنع بقاء الميكروفون مفتوحًا في الخلفية.
- **تدوير الشاشة (`Configuration Changes`) وإغلاق العملية (`Process Death`):**
  - يستخدم التطبيق `ViewModel` لإدارة الحالة، وتُحفظ الإعدادات في `DataStore` + `SharedPreferences` والمصاريف في `Room SQLite`.
  - يوصى بحفظ النص الجاري كتابته في حوار الإضافة اليدوية عبر `rememberSaveable` بدلاً من `remember` العادي في [DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt) حتى لا يضيع النص إذا دوّر المستخدم الشاشة أثناء كتابة مصروف يدوي.

</div>
