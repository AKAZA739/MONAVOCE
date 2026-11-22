<div dir="rtl" lang="ar">

# 05 — تدقيق واجهة وتجربة المستخدم وإمكانية الوصول والتعريب (UI/UX, Accessibility & Localization Audit)

يفحص هذا التقرير التصميم البصري الحالي، وتجربة المستخدم عبر جميع الشاشات، ومعايير إمكانية الوصول (**WCAG 2.1 / Android Accessibility Guidelines**)، وجودة دعم اللغتين العربية (`RTL`) والإنجليزية (`LTR`).

---

## 1. التقييم البصري للشاشات الحالية (Screen-by-Screen UI/UX Inspection)

يعتمد التطبيق حاليًا في الإصدار `v1.3.0` على ثيم **Obsidian OLED Dark** ([Theme.kt](../../app/src/main/java/com/myexpenses/app/ui/theme/Theme.kt) و [Theme.kt](../../app/src/main/java/com/myexpenses/app/ui/theme/Theme.kt)) المستوحى من التصميمات المالية الداكنة الفاخرة مع لوحة صوتية كونية متوهجة (**Cosmic Neon Voice Orb**):

### أ. الشاشة الرئيسية (`DashboardScreen.kt`)
- **نقاط القوة:**
  - بطاقة نظرة عامة موحدة (`Overview Hero Card`) تجمع محدد الفترة الزمنية (`اليوم / الأسبوع / الشهر / السنة / الكل`)، ومتصفح التاريخ التفاعلي (`Date Navigator` مع أزرار السابق/التالي والعودة لليوم)، وإجمالي الإنفاق بخط عريض واضح (`34.sp, FontWeight.ExtraBold`)، ومخطط أعمدة مصغر لأسابيع الشهر (`W1..W5`)، وشريط أفقي لأعلى 3 فئات إنفاق مع نسبها المئوية.
  - شريط تنقل سفلي عائم بتصميم الكبسولة (`Floating Pill Dock` في [MainActivity.kt](../../app/src/main/java/com/myexpenses/app/MainActivity.kt)) يتوسطه زر المايكروفون المتوهج مع زر إضافة يدوية سريع.
- **نقاط الضعف:**
  - لا يوجد تمييز بصري في قائمة المعاملات لعملة السجل الفردي إذا كانت مختلفة عن العملة الافتراضية (`MONA-FIN-001`).
  - غياب شاشة تعريفية أولية (`First-Run Onboarding`) تشرح للمستخدم الجديد كيفية تفعيل زر التشغيل الجانبي أو كيفية استيراد النموذج الصوتي الأوفلاين (`MONA-UX-003`).

### ب. اللوحة الصوتية الكونية (`VoiceOverlayPanel.kt` و`QuickVoiceActivity.kt`)
- **نقاط القوة:**
  - تصميم بصري مبهر يستخدم `Canvas` لرسم هالات ضوئية متعددة الطبقات (`Cyan #00E5FF` -> `Royal Blue #3B82F6` -> `Magenta #D946EF`) تدور وتنبض في الوقت الفعلي بناءً على شدة صوت المستخدم (`voiceRmsLevel`).
  - عرض واضح لحالة المحرك (`Sherpa-ONNX 100% Offline Ready` أو زر استيراد ملف `.zip` بضغطة واحدة في حال عدم وجود النموذج).
- **نقاط الضعف (`MONA-UX-001`):**
  - الألوان في [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt) ثابتة على الوضع الليلي (`CosmicTopBg = Color(0xFF060714)`, `CosmicBottomBg = Color(0xFF0E122B)`)، وتجاهل كامل لاختيار المستخدم في حال تفعيل **الوضع النهاري (`Light Theme`)**، مما يخلق تناقضًا بصريًا حادًا بين البطاقات الداخلية واللوحة عند استخدام الثيم الفاتح.

### ج. شاشة الإحصائيات (`StatisticsScreen.kt`)
- **نقاط القوة:**
  - مخطط دائري مفرغ (`DonutChart`) مرسوم عبر `Canvas` مع إجمالي الإنفاق في المركز، ومخطط أعمدة لآخر 7 أيام، وشريط تقدم لكل فئة مع عدد العمليات والنسبة المئوية.
  - أزرار تصدير ومشاركة سريعة لملفات `CSV` و`JSON` في أعلى الشاشة.

### د. شاشة الفئات (`CategoriesScreen.kt`) وشاشة الإعدادات (`SettingsScreen.kt`)
- **نقاط القوة:**
  - شاشة الفئات توفر محررًا متكاملاً يتيح تخصيص الاسم العربي، الاسم الإنجليزي، الكلمات المفتاحية للتعرف الصوتي، الأيقونة (18 خيارًا)، واللون (12 لونًا).
  - شاشة الإعدادات توفر قسمًا تفاعليًا لإعداد زر التشغيل الجانبي (`Side Button Assistant`)، وإضافة اختصار الصوت للشاشة الرئيسية، وإدارة النموذج الصوتي الأوفلاين (`130 MB`)، والنسخ الاحتياطي، وتغيير اللغة والعملة والثيم.

---

## 2. الملاحظات المسجلة في تجربة المستخدم وإمكانية الوصول (UI/UX & Accessibility Findings)

### 1. `[MONA-UX-001]` (Medium) — ألوان داكنة صلبة (Hardcoded Dark Colors) في `VoiceOverlayPanel` تتجاهل الوضع النهاري
- **الموقع الدقيق:** [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt) والأسطر [270-303](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt).
- **الدليل:** تستخدم اللوحة تدرجًا ثابتًا من `#060714` إلى `#0E122B` ونصوصًا بيضاء `Color.White`، بينما البطاقات الداخلية (مثل بطاقة استيراد النموذج أو نتيجة الخطأ في الأسطر 363 و487) تستخدم أحيانًا `MaterialTheme.colorScheme.errorContainer` أو `Color.White.copy(alpha = 0.05f)`.
- **الإصلاح المقترح:** توحيد نظام رموز الألوان (Design Tokens) في ثيم **MONAVOCE** بحيث تحافظ الكرة الصوتية على توهجها الداكن الفخم داخل حاوية متناغمة مع الوضعين الليلي والنهاري دون تضارب في تباين النصوص.

---

### 2. `[MONA-UX-002]` (Medium) — فجوات في إمكانية الوصول (TalkBack Accessibility & <48dp Touch Targets)
- **المواقع الدقيقة:**
  1. **غياب `contentDescription` لقارئات الشاشة (TalkBack):**
     - الكرة الصوتية الرئيسية (`CosmicVoiceOrb` في [VoiceOverlayPanel.kt](../../app/src/main/java/com/myexpenses/app/ui/components/VoiceOverlayPanel.kt)) تستخدم `.clickable { onClick() }` على `Box` بدون `Role.Button` وبدون `semantics { contentDescription = ... }` (`Icon(..., contentDescription = null)` في السطر 665). مستخدمو قارئ الشاشة المكفوفون—وهم الفئة الأكثر استفادة من تطبيق مالي صوتي!—لا يستطيعون معرفة وظيفة الزر الرئيسي في اللوحة الصوتية.
     - أزرار تعديل وحذف الفئات في [CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt) تمرر `contentDescription = null`.
     - أزرار التنقل بين التواريخ في [DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt) تمرر `contentDescription = null`.
  2. **مساحات لمس أصغر من الحد الأدنى القياسي (`48.dp x 48.dp`):**
     - زر حذف المصروف في بطاقة المعاملة ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)): `Modifier.size(26.dp)` — صغير جدًا وعرضة للنقرات الخاطئة على فتح التعديل بدل الحذف.
     - أزرار تقديم وتأخير التاريخ ([DashboardScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/DashboardScreen.kt)): `Modifier.size(30.dp)`.
     - دوائر اختيار لون الفئة ([CategoriesScreen.kt](../../app/src/main/java/com/myexpenses/app/ui/screens/CategoriesScreen.kt)): `Modifier.size(30.dp)`.

---

### 3. `[MONA-UX-003]` (Low) — غياب رحلة الترحيب والإعداد الأولي للمستخدم الجديد (First-Run Onboarding Flow)
- **التحليل:** عند تثبيت التطبيق لأول مرة، يدخل المستخدم مباشرة إلى الشاشة الرئيسية الفارغة دون معرفة:
  1. كيفية صياغة الجمل الصوتية المدعومة (مثل إمكانية قول عدة مصاريف في جملة واحدة أو قول `"احذف آخر مصروف"`).
  2. حالة محرك الصوت العربي الأوفلاين وكيفية تفعيله في ثوانٍ.
  3. كيفية ربط زر التشغيل الجانبي (`Side Button`) لفتح النافذة الصوتية فورًا من خارج التطبيق.

---

### 4. `[MONA-UX-004]` (Low) — تضمين النصوص ثنائية اللغة شرطيًا داخل كود Compose (`if (isArabic) ... else ...`) بدل ملفات الموارد `strings.xml`
- **الموقع الدقيق:** جميع ملفات `ui/screens/*.kt` و`ui/components/*.kt`.
- **التحليل:** تبديل اللغة عبر `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)` في [MainActivity.kt](../../app/src/main/java/com/myexpenses/app/MainActivity.kt) يتميز بسرعة استجابة فورية (Zero Activity Recreation)، لكن كتابة النصوص داخل الكود المصدري عبر `if (isArabic) "..." else "..."` يصعّب إضافة لغات عالمية جديدة (مثل الفرنسية، الإسبانية، التركية، الأردية) في التوسع العالمي لعلامة **MONAVOCE**.

</div>
