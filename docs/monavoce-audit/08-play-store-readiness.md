<div dir="rtl" lang="ar">

# 08 — تدقيق جاهزية النشر على متجر جوجل بلاي (Google Play Store Readiness & Policy Compliance Audit)

يفحص هذا التقرير مدى توافق التطبيق مع متطلبات وسياسات **Google Play Console (2025/2026)** تمهيدًا لإطلاقه العالمي تحت علامة **MONAVOCE (موناڤوتشي)**.

---

## 1. جدول التحقق من متطلبات Google Play الأساسية

| المتطلب في Google Play | الحالة الحالية في الكود | التقييم والتوصية |
|---|---|---|
| **1. مستوى الواجهة البرمجية المستهدف (`targetSdk`)** | `targetSdk = 35` (Android 15) / `compileSdk = 35` | **PASS (ممتاز)**: يتجاوز الحد الأدنى الإلزامي لمتجر Google Play (`API 34/35`). |
| **2. دعم معمارية 64-bit (`arm64-v8a`, `x86_64`)** | مدعوم بالكامل داخل `sherpa-onnx-1.10.45.aar` | **PASS**: يحتوي على مكتبات `arm64-v8a` و`x86_64` و`armeabi-v7a`. |
| **3. توافق حجم صفحة الذاكرة `16 KB Page Size` (Android 15+)** | يتطلب فحص محاذاة `libonnxruntime.so` و`libsherpa-onnx-jni.so` | **WARN**: بدءًا من أواخر 2025/2026 يشترط Google Play محاذاة مكتبات `.so` على حدود `16 KB` (`android.bundle.enableUncompressedNativeLibs=false` أو تحديث NDK/Sherpa-ONNX). |
| **4. صيغة النشر (`Android App Bundle .aab`)** | البناء يدعم `bundleRelease` | **FAIL (`MONA-PLAY-002`)**: التطبيق الأساسي جاهز، لكن نموذج الصوت (`130 MB`) موضوع حاليًا كملف `.zip` خارجي ولن يُثبت تلقائيًا مع التطبيق من المتجر إلا عبر **Play Asset Delivery (PAD)**. |
| **5. تفرد معرف الحزمة (`applicationId`) والاسم** | `com.myexpenses.app` / `"My Expenses | مصاريفي"` | **FAIL (`MONA-PLAY-001`)**: يتعارض اسم `"My Expenses"` مع تطبيق عريق في المتجر؛ يجب الترحيل إلى **`com.monavoce.app`** والاسم **`MONAVOCE`**. |
| **6. تصريح الصلاحيات الحساسة (`RECORD_AUDIO`)** | `RECORD_AUDIO` فقط (بدون `INTERNET`) | **PASS**: استخدام الميكروفون واضح ومباشر في صميم وظيفة التطبيق (Voice Expense Tracker). |
| **7. خدمات المساعد الصوتي (`BIND_VOICE_INTERACTION`)** | `ExpenseVoiceInteractionService` + `ExpenseStubRecognitionService` | **WARN (`MONA-PLAY-003`)**: خدمة `ExpenseStubRecognitionService` ترجع `ERROR_CLIENT` مباشرة؛ قد تعترض عليها روبوتات فحص Google Play. |
| **8. نموذج أمان البيانات (`Data Safety Form`)** | لا يجمع ولا يشارك أي بيانات (`Zero-Internet`) | **PASS (ميزة تسويقية كبرى)**: سيحصل التطبيق على شارة **"No data collected / No data shared with third parties"** في المتجر. |
| **9. رابط سياسة الخصوصية (`Privacy Policy URL`)** | غير موجود حاليًا داخل `SettingsScreen` أو موقع خارجي | **REQUIRED**: يشترط Google Play رابط سياسة خصوصية عامًا (`HTTPS`) حتى للتطبيقات التي تعمل بدون إنترنت. |

---

## 2. تحليل العوائق الخاصة بمتجر Google Play وكيفية حلها

### 1. `[MONA-PLAY-001]` (High / Release Blocker) — ترحيل معرف الحزمة والاسم التجاري إلى `MONAVOCE` (`com.monavoce.app`)
- **الموقع الدقيق:** [app/build.gradle.kts](../../app/build.gradle.kts) و [strings.xml](../../app/src/main/res/values/strings.xml).
- **التحليل:**
  - معرف الحزمة `applicationId` في Google Play نهائي ولا يمكن تغييره أبدًا بعد رفع أول إصدار.
  - اسم `"My Expenses"` مستخدم منذ أكثر من 10 سنوات على المتجر (`org.totschnig.myexpenses`)، واستخدامه قد يؤدي إلى رفض التطبيق بسبب سياسة انتحال الهوية أو التعدي على العلامات التجارية (Impersonation / IP Policy).
- **الحل الهندسي:** تغيير `namespace` و`applicationId` إلى `com.monavoce.app`، ومزود الملفات إلى `com.monavoce.app.fileprovider`، والأفعال المخصصة إلى `com.monavoce.app.ACTION_QUICK_VOICE`، واسم التطبيق المعروض إلى **`MONAVOCE`** (في الإنجليزية) و**`MONAVOCE | موناڤوتشي`** (في العربية).

---

### 2. `[MONA-PLAY-002]` (High / Release Blocker) — استراتيجية تسليم النموذج الصوتي العربي (`130 MB`) عبر Google Play Asset Delivery (PAD)
- **المشكلة:**
  - حاليًا يعتمد التطبيق على وجود ملفات النموذج (`encoder.int8.onnx` ~115 MB، `decoder.int8.onnx` ~7.5 MB، `joiner.int8.onnx` ~1.7 MB، `tokens.txt`) في مجلد خارجي أو استيراد ملف `MyExpenses-Offline-Arabic-Model.zip` (`101.2 MiB`) يدويًا.
  - المستخدم الذي يحمّل تطبيق **MONAVOCE** من متجر Google Play يتوقع أن يعمل التعرف الصوتي العربي بدون إنترنت **فور فتح التطبيق لأول مرة (Zero-Setup Out of the Box)** دون البحث عن ملف ZIP خارجي!
  - في الوقت نفسه، الحد الأقصى لحجم Base APK داخل حزمة `.aab` في Google Play هو `200 MB` (أو `150 MB` في بعض الإعدادات)، وتضمين 130 MB داخل `app/src/main/assets` مباشرة سيضخم Base APK لكل المعماريات.
- **الحل المعماري الأمثل لـ Google Play (مع الحفاظ على `Zero-Internet Permission`):**
  - إنشاء وحدة **Play Asset Delivery (`install-time` Asset Pack)** باسم `:arabic_asr_pack` في مشروع Gradle:
    ```kotlin
    // arabic_asr_pack/build.gradle.kts
    plugins { id("com.android.asset-pack") }
    assetPack {
        packName.set("arabic_asr_pack")
        dynamicDelivery { deliveryType.set("install-time") }
    }
    ```
  - يسمح Google Play لحزمة `install-time` بحجم يصل إلى **1 غيغابايت (1 GB)**، ويقوم متجر Google Play بتنزيلها وتثبيتها تلقائيًا مع التطبيق، وتكون ملفاتها متاحة فورًا عبر `context.assets` دون الحاجة إلى `android.permission.INTERNET` ودون أي إعداد يدوي من المستخدم!
  - كما يظل خيار استيراد `.zip` اليدوي موجودًا كمسار احتياطي لمن يثبتون ملف `.apk` مباشرة خارج المتجر (Sideloading).

---

### 3. `[MONA-PLAY-003]` (Medium) — مراجعة تصريح `ExpenseStubRecognitionService` و`VoiceInteractionService`
- **الموقع الدقيق:** [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml) و [ExpenseAssistantServices.kt](../../app/src/main/java/com/myexpenses/app/quicklaunch/ExpenseAssistantServices.kt).
- **التحليل:**
  - يشترط نظام أندرويد في `voice_interaction_service.xml` الإشارة إلى `recognitionService` حتى يظهر التطبيق في قائمة **"تطبيق المساعد الرقمي الافتراضي" (Default Digital Assistant App)** لتشغيله بزر التشغيل الجانبي (`Side Button Press & Hold`).
  - حاليًا، `ExpenseStubRecognitionService` يرجع `callback?.error(SpeechRecognizer.ERROR_CLIENT)` مباشرة.
  - لتجنب أي ملاحظة من الفحص الآلي في Google Play Console (أو تعطل الإملاء الصوتي في لوحة المفاتيح إذا اختار المستخدم التطبيق كخدمة تعرف صوتي افتراضية للنظام كله)، يوصى إما بربط `ExpenseStubRecognitionService` بمحرك `SherpaOfflineArabicEngine` الفعلي، أو الاكتفاء بـ `<intent-filter><action android:name="android.intent.action.ASSIST" /></intent-filter>` على `QuickVoiceActivity` إذا تبيّن أن One UI يقبلها بدون `VoiceInteractionService`، أو توضيح وظيفة المساعد المالي الصوتي في فيديو المراجعة (App Review Instructions) لفريق Google Play.

---

## 3. متطلبات بطاقة المتجر والإفصاحات (Store Listing & Data Safety Disclosures)

1. **إجابات نموذج أمان البيانات (Google Play Data Safety Section):**
   - *Does your app collect or share any of the required user data types?* -> **`No`** (لأن التطبيق لا يملك `android.permission.INTERNET` ولا يرسل أي بيانات خارج جهاز المستخدم).
2. **تصنيف الفئة (Category):** `Finance` (المالية).
3. **التصنيف العمري (Content Rating / IARC):** `Everyone / PEGI 3 / USK 0` (خالي من أي محتوى مقيد أو عمليات شراء داخلية أو إعلانات).
4. **الأصول البصرية المطلوبة للمتجر (Store Graphic Assets):**
   - أيقونة عالية الدقة: `512 x 512 px` (32-bit PNG) بشعار **MONAVOCE**.
   - صورة Feature Graphic: `1024 x 500 px` تعرض الهوية الداكنة `Obsidian Vault` والكرة الصوتية `Sonic Voice Orb` مع شعار *"Speak Your Spend. 100% Offline."*.
   - 6 لقطات شاشة للهاتف (3 بالعربية و3 بالإنجليزية): لوحة التحكم `Overview`، الكرة الصوتية أثناء الالتقاط، الإحصائيات `DonutChart`، إدارة الفئات، وإعدادات زر التشغيل الجانبي.

</div>
