# 02 — تدقيق الأمان والخصوصية الشامل (OWASP MASVS / MASTG & Privacy Audit)

يستند هذا التقرير إلى معايير **OWASP Mobile Application Security Verification Standard (MASVS v2.1)** ودليل اختبار أمان تطبيقات الهواتف **(MASTG)** وتصنيفات الثغرات العالمية **(MITRE CWE)**، مع فحص الكود المصدري وملف البيان المدمج للنسخة النهائية ([merged_manifest/release/AndroidManifest.xml](file:///d:/My%20expensives/app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml)).

---

## 1. نموذج التهديد الأمني لتطبيق MONAVOCE (Threat Model)

بصفته تطبيقًا لإدارة المصاريف الشخصية بالصوت يعمل بدون إنترنت:
1. **الأصول الحساسة (Protected Assets):**
   - السجلات المالية للمستخدم (المبالغ، الفئات، التواريخ، الملاحظات، النص الصوتي الخام `rawVoiceText`) المخزنة في قاعدة بيانات Room SQLite (`my_expenses_db`).
   - البصمة الصوتية اللحظية للمستخدم أثناء التحدث عبر الميكروفون (`android.permission.RECORD_AUDIO`).
   - ملفات النسخ الاحتياطي المصدرة والمستوردة (`JSON` و`CSV`).
   - ملفات النموذج العصبي للتعرف الصوتي (`encoder.int8.onnx`, `decoder.int8.onnx`, `joiner.int8.onnx`, `tokens.txt`).
2. **جهات التهديد المحتملة (Threat Actors):**
   - تطبيقات خبيثة أخرى مثبتة على نفس الهاتف تحاول استغلال الأنشطة والخدمات المصدرة (`android:exported="true"`) أو قراءة وحدة التخزين المشتركة.
   - ملفات استيراد خبيثة (`Backup JSON` أو `Model ZIP`) يصممها مهاجم لاستغلال فك الضغط (Zip Bomb / Zip Slip) أو استنزاف الذاكرة (`OOM`).
   - فتح ملفات `CSV` المصدرة داخل برامج جداول البيانات (Excel / Google Sheets / LibreOffice Calc) وما يرتبط بها من هجمات حقن المعادلات (CSV Formula Injection).
   - الوصول الفيزيائي أو عبر `adb logcat` إلى سجلات النظام التي قد تسرب ما قاله المستخدم صوتيًا.

---

## 2. فحص ملف البيان المدمج والمكونات المصدرة (Manifest & Component Attack Surface)

تم فحص ملف البيان المصدري ([app/src/main/AndroidManifest.xml](file:///d:/My%20expensives/app/src/main/AndroidManifest.xml)) وملف البيان المدمج لنسخة الإطلاق ([merged_manifest/release/AndroidManifest.xml](file:///d:/My%20expensives/app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml)):

### أ. الصلاحيات المطلوبة (Permissions Audit)
- `android.permission.RECORD_AUDIO` (السطر 5 في ملف البيان): ضرورية للوظيفة الأساسية (التسجيل الصوتي للمصاريف)، وتُطلب في وقت التشغيل (Runtime Permission) قبل بدء الجلسة الصوتية.
- **غياب تام لصلاحية الإنترنت (`MONA-SEC-007` - Positive Verification):** لا يوجد `android.permission.INTERNET` ولا `ACCESS_NETWORK_STATE` ولا `READ_EXTERNAL_STORAGE` ولا `WRITE_EXTERNAL_STORAGE`. هذا يقضي جذريًا على مخاطر تسريب البيانات عبر الشبكة (Zero Exfiltration Attack Surface).
- **تعطيل النسخ الاحتياطي السحابي:** `android:allowBackup="false"` (السطر 10 في ملف البيان المصدري، والسطر 40 في المدمج)، مما يمنع استخراج قاعدة البيانات عبر `adb backup` أو رفعها غير مشفرة تلقائيًا.

### ب. تحليل المكونات المصدرة (`android:exported="true"`)

| المكون (Component) | النوع | الحماية / الصلاحية المطلوبة | التقييم الأمني |
|---|---|---|---|
| `.MainActivity` | Activity | `MAIN` / `LAUNCHER` + `ACTION_QUICK_VOICE` | آمن كمدخل رئيسي للتطبيق. |
| `.QuickVoiceActivity` | Activity | **بدون صلاحية (`exported="true"`)** | **متوسط الخطورة (`MONA-SEC-003`)**: يقبل `ACTION_ASSIST` و`com.myexpenses.app.ACTION_QUICK_VOICE` من أي تطبيق خارجي ويبدأ التسجيل تلقائيًا. |
| `.service.ExpenseVoiceInteractionService` | Service | `android.permission.BIND_VOICE_INTERACTION` | **آمن**: محمي بصلاحية نظام (`signature|privileged`) لا يملكها إلا نظام أندرويد. |
| `.service.ExpenseVoiceInteractionSessionService` | Service | `android.permission.BIND_VOICE_INTERACTION` | **آمن**: محمي بصلاحية نظام (`signature`). |
| `.service.ExpenseStubRecognitionService` | Service | `android.permission.BIND_RECOGNITION_SERVICE` | **آمن أمنيًا** (يرفض الجلسة فورًا بـ `ERROR_CLIENT`)، لكنه يحتاج مراجعة لمتطلبات Google Play (`MONA-PLAY-003`). |
| `.service.QuickVoiceTileService` | Service | `android.permission.BIND_QUICK_SETTINGS_TILE` | **آمن**: محمي بصلاحية النظام لمربعات الإعدادات السريعة. |
| `.widget.ExpenseWidgetProvider` | Receiver | `android.appwidget.action.APPWIDGET_UPDATE` | **منخفض الخطورة**: يستمع فقط لتحديثات الودجت ويعيد قراءة إجمالي اليوم من قاعدة البيانات المحلية. |

---

## 3. الثغرات والملاحظات الأمنية المؤكدة بالأدلة (Confirmed Security Findings)

### 1. `[MONA-SEC-001]` (High) — ثغرة حقن المعادلات في ملفات CSV المصدرة (CSV Formula Injection / CWE-1236)
- **الموقع الدقيق:** [BackupManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L29-L66) والدالة `csvEscape` في الأسطر [149-156](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L149-L156):
  ```kotlin
  private fun csvEscape(value: String): String {
      val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
      if (!needsQuotes) return value
      return "\"${value.replace("\"", "\"\"")}\""
  }
  ```
- **الدليل والسبب الجذري:** تقوم الدالة بتغليف الفواصل وعلامات التنصيص والأسطر الجديدة فقط، ولكنها لا تفحص ما إذا كانت القيمة (`note` أو `rawVoiceText` أو اسم الفئة المخصصة `categoryNameAr`/`categoryNameEn`) تبدأ بأحد محارف تشغيل المعادلات في برامج الجداول: `=`, `+`, `-`, `@`, `\t`, `\r`.
- **الأثر الأمني:** إذا أدخل المستخدم ملاحظة أو استورد ملف JSON يحتوي على نص مثل `=cmd|' /C calc'!A0` أو `-500+HYPERLINK(...)` ثم قام بتصدير التقرير بصيغة CSV وفتحه على Microsoft Excel أو Google Sheets، سيقوم برنامج الجداول بتنفيذ المعادلة تلقائيًا.
- **الإصلاح المقترح:** تعديل `csvEscape` لإضافة فاصلة علوية مفردة `'` (أو مسافة بادئة داخل علامات تنصيص) أمام أي حقل نصي يبدأ بـ `=`, `+`, `-`, `@`, `\t`, `\r`.

---

### 2. `[MONA-SEC-002]` (Medium) — غياب حدود الحجم الأقصى عند فك ضغط نموذج ZIP واستيراد ملفات JSON (Unbounded Decompression & Memory Exhaustion / CWE-409)
- **الموقع الدقيق 1 (استيراد ZIP):** [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L83-L132) (`importModelFromZipUri`).
  - *ملاحظة إيجابية:* الكود يحمي من ثغرة **Zip Slip (Path Traversal)** عبر أخذ اسم الملف المجرد فقط `val baseName = File(entry.name).name` والتحقق من وجوده داخل القائمة البيضاء `expectedFiles = setOf(ENCODER_NAME, DECODER_NAME, JOINER_NAME, TOKENS_NAME)` (الأسطر 88–94).
  - *الثغرة:* حلقة النسخ `while (zis.read(buffer).also { read = it } > 0)` (الأسطر 103–105) لا تفرض حدًا أقصى لعدد البايتات المكتوبة لكل ملف (مثلاً `MAX_ENTRY_BYTES = 200L * 1024 * 1024`). إذا اختار المستخدم ملف ZIP خبيثًا (Zip Bomb) يحتوي على ملف باسم `encoder.int8.onnx` مضغوط بحجم 50 ميغابايت ولكنه ينفك إلى 20 غيغابايت، فسيؤدي ذلك إلى ملء مساحة تخزين الهاتف بالكامل (`Disk Exhaustion DoS`).
- **الموقع الدقيق 2 (استيراد JSON):** [BackupManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/util/BackupManager.kt#L100-L106) (`restoreJsonBackup`):
  ```kotlin
  val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
      input.bufferedReader().readText()
  } ?: return@withContext Result.failure(...)
  ```
  - *الثغرة:* قراءة الملف بالكامل إلى الذاكرة (`readText()`) دون فحص حجم الملف أولاً أو فرض سقف قراءة (مثلاً `10 MB`) يعرض التطبيق لانهيار فوري بـ `OutOfMemoryError` إذا اختار المستخدم ملفًا ضخمًا بالخطأ.

---

### 3. `[MONA-SEC-003]` (Medium) — تشغيل الميكروفون تلقائيًا عند استدعاء `QuickVoiceActivity` المصدرة بدون تقييد هوية المستدعي (Exported Activity Auto-Mic Activation / MASVS-PLATFORM-1)
- **الموقع الدقيق:** [AndroidManifest.xml](file:///d:/My%20expensives/app/src/main/AndroidManifest.xml#L48-L75) و [QuickVoiceActivity.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/QuickVoiceActivity.kt#L76-L79).
- **الدليل والسبب الجذري:** النشاط `QuickVoiceActivity` مصدّر (`android:exported="true"`) ليتمكن النظام واختصارات سامسونج (`RegiStar` / `Side Button`) من فتحه عبر `com.myexpenses.app.ACTION_QUICK_VOICE` و`android.intent.action.ASSIST`. وعند فتح النشاط، يقوم الكود التالي في السطر 77 ببدء التسجيل الصوتي فورًا دون انتظار نقرة إضافية:
  ```kotlin
  LaunchedEffect(Unit) {
      startOrRequestMic()
  }
  ```
- **الأثر الأمني:** أي تطبيق آخر مثبت على الهاتف (حتى لو لم يكن يملك صلاحية الميكروفون) يمكنه إرسال `Intent("com.myexpenses.app.ACTION_QUICK_VOICE")` في الخلفية أو المقدمة لإظهار نافذة `QuickVoiceActivity`، والتي ستفتح الميكروفون تلقائيًا وتُسجل ما يقوله المستخدم كعملية مصروف جديدة في قاعدة البيانات.
- **الإصلاح المقترح:** التحقق من مصدر الـ `Intent`، أو إضافة خيار/حماية تتحقق من أن الشاشة تفاعلية، أو فصل الـ Action المخصص للاختصارات عن الـ Action العام مع عدم الحفظ التلقائي إلا إذا نطق المستخدم صيغة مصروف صريحة أو ضغط زر التأكيد عند الاستدعاء الخارجي غير الموثوق.

---

### 4. `[MONA-SEC-004]` (Medium) — تسريب النصوص الصوتية والمالية الحساسة إلى سجلات النظام `Logcat` في نسخة الإطلاق (Sensitive Data in System Logs / CWE-532 / MASVS-STORAGE-2)
- **المواقع الدقيقة:**
  - [OfflineSpeechManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/OfflineSpeechManager.kt#L480):
    `Log.i(TAG, "onResults(localeTag=$localeTag, count=${matches.size}, best='$best')")`
  - [OfflineSpeechManager.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/OfflineSpeechManager.kt#L628):
    `Log.i(TAG, "Sherpa-ONNX offline Arabic result: '$text'")`
  - [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L283):
    `Log.i(TAG, "Sherpa-ONNX offline decode done in ${decodeMs}ms (audio=${seconds}s): '$rawText'")`
  - [proguard-rules.pro](file:///d:/My%20expensives/app/proguard-rules.pro#L1-L32): لا يحتوي على قاعدة `-assumenosideeffects class android.util.Log` لحذف استدعاءات `Log.d` و`Log.i` في نسخة `release`.
- **الأثر الأمني:** كل جملة ينطقها المستخدم (بما في ذلك المبالغ المالية وتفاصيل مشترياته الخاصة) تُطبع نصيًا في حلقة `logcat` الخاصة بالنظام حتى في نسخة `Release APK`، مما يسمح لأي أداة متصلة عبر ADB أو خدمة نظام تملك `READ_LOGS` بقراءة السجل المالي الصوتي للمستخدم.
- **الإصلاح المقترح:** إخفاء النصوص الحساسة في السجلات إلا في وضع `BuildConfig.DEBUG`، وإضافة قاعدة إزالة `Log.d`/`Log.i`/`Log.v` في `proguard-rules.pro` لنسخة الإطلاق.

---

### 5. `[MONA-SEC-005]` (Medium) — تخزين نموذج الذكاء الاصطناعي في المساحة الخارجية للتطبيق وقاعدة البيانات بدون تشفير SQLCipher (Storage & Model Integrity / MASVS-STORAGE-1)
- **الموقع الدقيق:**
  1. [SherpaOfflineArabicEngine.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/speech/SherpaOfflineArabicEngine.kt#L49-L51):
     ```kotlin
     fun getModelDir(context: Context): File {
         val base = context.getExternalFilesDir("offline_asr_ar") ?: File(context.filesDir, "offline_asr_ar")
     ```
     مسار `getExternalFilesDir` يقع في `/sdcard/Android/data/com.myexpenses.app/files/offline_asr_ar/`. رغم أن أندرويد 11+ يفرض Scoped Storage، فإن هذا المسار يظل قابلاً للوصول والتعديل عبر `adb shell` أو تطبيقات إدارة الملفات ذات الصلاحيات الخاصة، على عكس المسار الداخلي المحمي بنظام صلاحيات لينكس `/data/data/com.monavoce.app/files/offline_asr_ar/` (`context.filesDir`). استبدال ملف `.onnx` بملف تالف قد يسبب انهيارًا في الكود الأصلي (`libonnxruntime.so`).
  2. [ExpenseDatabase.kt](file:///d:/My%20expensives/app/src/main/java/com/myexpenses/app/data/local/ExpenseDatabase.kt#L39-L43): قاعدة البيانات `my_expenses_db` مخزنة بصيغة SQLite قياسية غير مشفرة على مستوى التطبيق (تعتمد فقط على تشفير ملفات النظام FBE في أندرويد). على الأجهزة المكسورة الحماية (Rooted Devices)، يمكن قراءة ملف قاعدة البيانات مباشرة.

---

### 6. `[MONA-SEC-006]` (Low) — التراجع الصامت إلى مفتاح التوقيع التجريبي (`debug`) في حال غياب `keystore.properties` أثناء بناء نسخة الإطلاق
- **الموقع الدقيق:** [app/build.gradle.kts](file:///d:/My%20expensives/app/build.gradle.kts#L51-L52):
  ```kotlin
  signingConfig = signingConfigs.findByName("release")
      ?.takeIf { keystorePropertiesFile.exists() }
      ?: signingConfigs.getByName("debug")
  ```
- **الأثر الأمني:** في بيئات البناء الآلي (CI/CD)، إذا نسي المطور تمرير ملف `keystore.properties`، لن يفشل البناء بل سينتج ملف `release.apk` موقعًا بمفتاح `debug` غير صالح للنشر على Google Play دون تنبيه واضح.

---

## 4. مصفوفة الامتثال لمعايير OWASP MASVS v2.1

| مجال MASVS | الحالة | الدليل والتفاصيل |
|---|---|---|
| **MASVS-STORAGE-1** (تخزين البيانات الحساسة بأمان) | **PASS (جزئي)** | قاعدة البيانات داخل `/data/data/` مع `allowBackup="false"`؛ يوصى بنقل مجلد النموذج الصوتي إلى `context.filesDir` بعد اعتماد Play Asset Delivery. |
| **MASVS-STORAGE-2** (منع تسريب البيانات الحساسة) | **FAIL (`MONA-SEC-004`)** | النصوص الصوتية المسجلة تُطبع في `Log.i` في نسخة الإطلاق؛ يجب حذفها عبر ProGuard/R8 وشرط `DEBUG`. |
| **MASVS-CRYPTO-1 / 2** (التشفير وإدارة المفاتيح) | **N/A / INFO** | التطبيق لا يخزن كلمات مرور أو بطاقات بنكية؛ في حال إضافة قفل حيوي أو تشفير SQLCipher مستقبلاً يُستخدم `AndroidKeyStore`. |
| **MASVS-AUTH-1** (المصادقة المحلية) | **INFO** | لا يوجد قفل ببصمة الإصبع (`BiometricPrompt`) عند فتح التطبيق؛ مقترح كميزة اختيارية في `P2`. |
| **MASVS-NETWORK-1 / 2** (أمان الاتصالات) | **PASS (100%)** | التطبيق لا يطلب `INTERNET` مطلقًا ولا يفتح أي Socket شبكي (`MONA-SEC-007`). |
| **MASVS-PLATFORM-1** (أمان مكونات النظام وIPC) | **WARN (`MONA-SEC-003`)** | خدمات المساعد الصوتي محمية بـ `BIND_VOICE_INTERACTION`، لكن `QuickVoiceActivity` بحاجة لضبط الاستدعاء الخارجي. |
| **MASVS-CODE-1 / 2** (سلامة المدخلات والتبعيات) | **FAIL (`MONA-SEC-001`, `002`)** | يلزم تحييد المعادلات في CSV (`csvEscape`) ووضع سقف لحجم فك ضغط ZIP واستيراد JSON. |
| **MASVS-RESILIENCE-1** (تعتيم الكود وحماية الإطلاق) | **PASS** | `isMinifyEnabled = true` و`isShrinkResources = true` مفعلان ويعملان بنجاح مع R8. |
