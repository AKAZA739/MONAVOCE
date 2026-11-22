<div dir="rtl" lang="ar">

# 12 — قائمة التحقق النهائية للإطلاق (MONAVOCE Production Release Checklist)

تُصنف كل نقطة في قائمة التحقق هذه بإحدى الحالات الأربع المعتمدة:
- **`PASS`**: تم التحقق منه فعليًا بالدليل (مع ذكر الدليل).
- **`FAIL`**: لم يجتز الفحص ويمثل عائقًا أو خللًا مسجلاً (مع ذكر معرف الخلل `MONA-*`).
- **`NOT TESTED`**: لم يُختبر بسبب قيود البيئة الحالية.
- **`NOT APPLICABLE`**: لا ينطبق على معمارية التطبيق.

---

## 1. البناء والتكوين والتوقيع (Build, Configuration & Signing)

| البند | الحالة | الدليل / المرجع |
|---|---|---|
| نجاح بناء نسخة التطوير (`assembleDebug`) بدون أخطاء | **`PASS`** | تم بناء `dist/MyExpenses-v1.3.0-debug.apk` (`57,440,274` بايت) بنجاح. |
| نجاح بناء نسخة الإطلاق النهائية (`assembleRelease`) مع `R8` و`lintVitalRelease` | **`PASS`** | تم بناء `dist/MyExpenses-v1.3.0-release.apk` (`40,507,911` بايت) مع `minifyReleaseWithR8` بنجاح 100%. |
| استهداف `targetSdk = 35` (Android 15) و`minSdk = 26` (Android 8.0) | **`PASS`** | مثبت في [app/build.gradle.kts](../../app/build.gradle.kts) وملف البيان المدمج. |
| تفعيل تقليص الكود والموارد (`isMinifyEnabled = true`, `isShrinkResources = true`) | **`PASS`** | مثبت في [app/build.gradle.kts](../../app/build.gradle.kts). |
| اعتماد معرف الحزمة النهائي `com.monavoce.app` والاسم التجاري `MONAVOCE` | **`FAIL`** | لا يزال يحمل `com.myexpenses.app` والاسم `My Expenses | مصاريفي` (`MONA-PLAY-001` — P0). |
| تضمين نموذج الصوت العربي (`130 MB`) داخل `.aab` عبر Play Asset Delivery | **`FAIL`** | النموذج حاليًا في ملف `.zip` منفصل (`MONA-PLAY-002` — P0). |

---

## 2. الأمان والخصوصية (Security & Privacy)

| البند | الحالة | الدليل / المرجع |
|---|---|---|
| خلو التطبيق تمامًا من صلاحية الإنترنت (`android.permission.INTERNET`) | **`PASS`** | تم التحقق من [بيان المصدر](../../app/src/main/AndroidManifest.xml) (`MONA-SEC-007`). |
| تعطيل النسخ الاحتياطي السحابي غير المشفر (`android:allowBackup="false"`) | **`PASS`** | مثبت في السطر 40 من ملف البيان المدمج لنسخة الإطلاق. |
| حماية `FileProvider` (`exported="false"` + `grantUriPermissions="true"`) | **`PASS`** | ورد في التدقيق السابق. البيان الحالي يستخدم واجهة منتقي الملفات في أندرويد. |
| حماية فك ضغط النموذج من ثغرة تجاوز المسار (`Zip Slip Path Traversal`) | **`PASS`** | يستخدم `File(entry.name).name` مع قائمة بيضاء `expectedFiles` في [SherpaOfflineArabicEngine.kt](../../app/src/main/java/com/myexpenses/app/voice/SherpaOfflineArabicEngine.kt). |
| حماية ملفات `CSV` المصدرة من حقن المعادلات (`CSV Formula Injection`) | **`FAIL`** | الدالة `csvEscape` لا تحيد `=`, `+`, `-`, `@` (`MONA-SEC-001` — P0). |
| فرض حد أقصى لحجم فك ضغط `ZIP` وقراءة `JSON` لمنع استنزاف الذاكرة/التخزين | **`FAIL`** | غياب عداد البايتات الأقصى في حلقة القراءة (`MONA-SEC-002` — P1). |
| خلو سجلات `Logcat` في نسخة الإطلاق من نصوص الكلام المالي للمستخدم | **`FAIL`** | `Log.i` يطبع النص الصوتي في `OfflineSpeechManager` و`SherpaOfflineArabicEngine` (`MONA-SEC-004` — P1). |
| أمان الاتصالات الشبكية وتثبيت الشهادات (`TLS / Certificate Pinning`) | **`NOT APPLICABLE`** | التطبيق يعمل أوفلاين 100% ولا يجري أي اتصالات شبكية. |

---

## 3. المحرك المالي وسلامة قاعدة البيانات (Financial Engine & Database Integrity)

| البند | الحالة | الدليل / المرجع |
|---|---|---|
| عمليات إضافة وتعديل وحذف المصاريف مع التراجع (`Undo`) | **`PASS`** | تم التحقق منها في الكود ([ExpenseRepository.kt](../../app/src/main/java/com/myexpenses/app/data/repository/ExpenseRepository.kt)) وعلى الهاتف الحقيقي. |
| بذر الفئات الافتراضية الثمانية ثنائية اللغة تلقائيًا | **`PASS`** | مثبت في [CategoryEntity.kt](../../app/src/main/java/com/myexpenses/app/data/local/entity/CategoryEntity.kt) و`ExpenseDatabase.seedDefaultsIfEmpty()`. |
| تصدير ملفات `CSV` بترميز `UTF-8 BOM` لدعم الحروف العربية في Excel | **`PASS`** | يكتب `\uFEFF` في بداية الملف ([BackupManager.kt](../../app/src/main/java/com/myexpenses/app/data/backup/BackupManager.kt)). |
| صحة جمع المبالغ عند تغيير العملة الافتراضية أو تعدد العملات | **`FAIL`** | يتم جمع كل السجلات بدون تصفية `currencyCode` (`MONA-FIN-001` — P0). |
| ذرية استعادة النسخة الاحتياطية (`Atomic Backup Restore with Rollback`) | **`FAIL`** | `replaceAllFromBackup` تحذف البيانات خارج `@Transaction` (`MONA-FIN-002` — P0). |
| تصدير مخطط Room (`exportSchema = true`) لدعم الترحيلات المستقبلية | **`FAIL`** | مضبوط حاليًا على `exportSchema = false` (`MONA-FIN-005` — P1). |

---

## 4. التعرف الصوتي الأوفلاين والتشغيل السريع (Voice ASR & Hardware Quick-Launch)

| البند | الحالة | الدليل / المرجع |
|---|---|---|
| عمل التعرف الصوتي العربي 100% بدون إنترنت (وضع الطيران) على Samsung A54 5G | **`PASS`** | تم اختباره على `SM-A546E` في وضع الطيران بزمن فك تشفير `179ms–252ms`. |
| زوال خطأ `ERROR_SERVER_DISCONNECTED (11)` في محرك الصوت | **`PASS`** | تم التحقق منه عبر `Logcat` بعد توحيد دورة حياة المحرك على الخيط الرئيسي ودمج `Sherpa-ONNX`. |
| اجتياز اختبارات الوحدة لمحلل الأوامر الصوتية (`VoiceInputParserTest`) | **`PASS`** | **16/16 اختبارًا ناجحًا** في `0.633s` ([اختبارات المحلل](../../app/src/test/java/com/myexpenses/app/VoiceInputParserTest.kt)). |
| فتح نافذة الصوت السريعة عبر زر التشغيل الجانبي (`Press & Hold` / `Double Press`) | **`PASS`** | تم التحقق منه فعليًا على `SM-A546E` عبر `ExpenseVoiceInteractionService` و`QuickVoiceActivity`. |
| إغلاق الميكروفون تلقائيًا عند إطفاء الشاشة أو الخروج (`onStop`) | **`PASS`** | مثبت في [QuickVoiceActivity.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/QuickVoiceActivity.kt). |
| تحليل جملة تحتوي على رقم كمية وسعر بدون حرف عطف (`"2 حليب ب 1000"`) | **`FAIL`** | تُقسم حاليًا إلى مصروفين (`MONA-VOX-001` — P1). |
| عدم تحميل نموذج `130 MB` في الذاكرة عند تحديث الودجت في الخلفية | **`FAIL`** | `warmUpAsync()` تُستدعى في `Application.onCreate()` (`MONA-VOX-003` — P1). |

---

## 5. واجهة المستخدم وإمكانية الوصول (UI/UX & Accessibility)

| البند | الحالة | الدليل / المرجع |
|---|---|---|
| دعم التبديل الفوري بين العربية (`RTL`) والإنجليزية (`LTR`) | **`PASS`** | يعمل عبر `LocalLayoutDirection` في [Theme.kt](../../app/src/main/java/com/myexpenses/app/ui/theme/Theme.kt). |
| تناغم ألوان اللوحة الصوتية (`VoiceOverlayPanel`) مع الوضع النهاري (`Light Theme`) | **`FAIL`** | الألوان الداكنة صلبة داخل `VoiceOverlayPanel.kt` (`MONA-UX-001` — P1). |
| توفر `contentDescription` لجميع الأزرار ومساحة لمس `>= 48.dp` | **`FAIL`** | الكرة الصوتية وأزرار الحذف/التاريخ تفتقر لوصف TalkBack أو أصغر من `48.dp` (`MONA-UX-002` — P1). |
| اختبار التوافق مع الأجهزة اللوحية والشاشات القابلة للطي (`Foldables / Tablets`) | **`NOT TESTED`** | تم الاختبار الفيزيائي على شاشة هاتف `SM-A546E` (`1080 x 2340`). |

</div>
