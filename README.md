<div dir="rtl" lang="ar">

# MONAVOCE — سجّل مصروفاتك بالصوت

[English](#english) · [سياسة الخصوصية](PRIVACY_POLICY.md) · [سجل التدقيق](docs/monavoce-audit/README.md)

MONAVOCE تطبيق أندرويد لتسجيل المصروفات بالصوت. قل «صرفت ٥٠ ريال على البقالة» أو أضف العملية يدويًا. تقدر بعدها تراجع مصروفاتك حسب التاريخ والفئة، وتشوف إجمالي كل عملة على حدة.

## ماذا يقدم التطبيق؟

- تسجيل المصروفات بالعربية والإنجليزية، وفهم التواريخ النسبية والأوامر مثل «كم صرفت اليوم؟».
- تعرف صوتي محلي بالعربية بعد استيراد النموذج، وبالإنجليزية على الأجهزة التي تتوفر فيها خدمة التعرف المحلية وحزمة اللغة.
- تشغيل سريع من الإعدادات السريعة أو الودجت أو اختصار الشاشة الرئيسية. يتوفر تشغيل الزر الجانبي على الأجهزة التي تدعم ربطه بالمساعد الرقمي.
- حفظ المصروفات على الجهاز، وتصدير CSV، واستعادة نسخة JSON بعد التحقق منها وتأكيد الاستبدال.

## لقطات من التطبيق

<p dir="rtl">
  <img src="docs/screenshots/history-ar.png" width="320" alt="سجل المصروفات بالعربية مع البحث والتصفية حسب التاريخ والفئة">
</p>

## التثبيت

1. افتح [صفحة الإصدارات](https://github.com/AKAZA739/MONAVOCE/releases) ونزّل ملف APK الموقّع إذا كان الإصدار متاحًا. يتطلب الإصدار جهاز ARM يعمل بأندرويد 8.0 أو أحدث.
2. للتعرف المحلي بالعربية، جهّز ملف ZIP للنموذج حسب [دليل النموذج العربي](docs/arabic-model.md). افتح **الإعدادات** ثم **استيراد نموذج العربية المحلي (.zip)** واختر الملف. حجم الحزمة التي اختُبرت حوالي 106 MB.
3. للتعرف المحلي بالإنجليزية، استخدم حزمة لغة تدعمها خدمة التعرف على جهازك. يتطلب مسار Android المحلي أندرويد 12 أو أحدث.
4. تشغيل الزر الجانبي يعتمد على الجهاز وإصدار النظام. إذا ظهر خيار **المساعد الرقمي** ضمن إعدادات الزر، اختر MONAVOCE كمساعد افتراضي ثم اربط الضغط المطوّل به.

البديل السحابي مغلق افتراضيًا. تقدر تستخدم الإدخال اليدوي مباشرة إذا كانت حزمة الصوت المحلية غير متوفرة.

## البناء والاختبار

تحتاج JDK 21 وحزمة Android SDK Platform 35. اضبط `JAVA_HOME` على مسار JDK عندك إذا احتجت، وحدد مسار Android SDK عبر Android Studio أو `ANDROID_HOME`.

**Windows**

<div dir="ltr">

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

</div>

**macOS وLinux**

<div dir="ltr">

```sh
sh ./gradlew testDebugUnitTest lintDebug assembleDebug
```

</div>

راجع [دليل بناء الإصدار وتوقيعه](docs/BUILD.md) و[ترخيص كود التطبيق](LICENSE).

</div>

## English

[Privacy policy](PRIVACY_POLICY.md) · [Audit archive](docs/monavoce-audit/README.md)

MONAVOCE is an Android app for logging daily spending by voice or by hand. Say “I spent 50 riyals on groceries” (Arabic: «صرفت ٥٠ ريال على البقالة») or add an entry manually. Browse your expenses by date and category, and see a separate total for each currency.

### Features

- Arabic and English expense entry, understanding relative dates and commands such as “How much did I spend today?”
- On-device Arabic speech recognition after importing the model, and on-device English recognition on devices with a supported local recognition service and language pack.
- Quick launch from Quick Settings, a home screen widget, or a home screen shortcut. Side-button launch is available on devices that support binding it to the digital assistant.
- On-device expense storage, CSV export, and JSON backup restore with validation and replacement confirmation.

### Screenshots

<p>
  <img src="docs/screenshots/dashboard-en.png" width="320" alt="Expenses dashboard showing Saudi riyal and US dollar totals separately">
</p>

### Install

1. Open the [Releases page](https://github.com/AKAZA739/MONAVOCE/releases) and download the signed APK if a release is available. The release requires an ARM device running Android 8.0 or later.
2. For on-device Arabic recognition, prepare the model ZIP file following the [Arabic model guide](docs/arabic-model.md). Open **Settings**, then **Import Offline Arabic Model (.zip)**, and select the file. The tested bundle is about 106 MB.
3. For on-device English recognition, use a language pack supported by your device's speech recognition service. The Android on-device path requires Android 12 or later.
4. Side-button launch depends on the device and OS version. If the **Digital assistant** option appears under your button settings, set MONAVOCE as the default assistant and bind long-press to it.

Online speech fallback is off by default. You can use manual entry directly if an on-device voice pack is unavailable.

### Build and Test

You need JDK 21 and Android SDK Platform 35. Set `JAVA_HOME` to your JDK path if needed, and configure the Android SDK path via Android Studio or `ANDROID_HOME`.

**Windows**

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

**macOS and Linux**

```sh
sh ./gradlew testDebugUnitTest lintDebug assembleDebug
```

See the [release build and signing guide](docs/BUILD.md) and the [app source license](LICENSE).
