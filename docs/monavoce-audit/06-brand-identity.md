<div dir="rtl" lang="ar">

# 06 — نظام الهوية البصرية والعلامة التجارية العالمية (MONAVOCE Brand Identity & Material 3 Design System)

يقدم هذا المستند المواصفات التصميمية والهندسية الكاملة لتحويل التطبيق من هويته التجريبية الحالية (`My Expenses | مصاريفي`) إلى العلامة التجارية العالمية المعتمدة:

# **MONAVOCE**
### **موناڤوتشي**

---

## 1. الاستراتيجية والتموضع وهوية العلامة (Brand Strategy & Positioning)

### أ. أصل الاسم وفلسفته (Etymology & Core Concept)
- **MONA** (*Moneta*): الجذر اللاتيني للمال، العملة، والقيمة المالية المحفوظة.
- **VOCE** (*Voce*): الكلمة الإيطالية/اللاتينية للصوت البشري، النبرة، والتعبير الحي.
- **المعنى المركب:** **"صوت المال" (The Voice of Money)** — إدارة مالية شخصية فورية تقودها نبرة صوتك، بخصوصية مطلقة على جهازك ودون اتصال بالإنترنت.

### ب. الشخصية والشعار اللفظي (Brand Personality & Taglines)
- **شخصية العلامة:** فاخرة (Premium)، عالمية (International)، رصينة وبسيطة (Minimal)، موثوقة (Trustworthy)، متقنة (Sophisticated)، ومبتكرة (Innovative).
- **الشعار اللفظي بالإنجليزية (Primary English Taglines):**
  - *Speak Your Spend. Private by Design.*
  - *Your Financial Voice. 100% On-Device.*
- **الشعار اللفظي بالعربية (Primary Arabic Taglines):**
  - **موناڤوتشي — صوتك المالي، بخصوصية مطلقة.**
  - **تحدّث بمصروفك.. يُحفظ في لحظة دون إنترنت.**

### ج. فحص المشهد التنافسي للاسم (Naming Landscape Verification)
- أظهر البحث في متجر Google Play والسوق العالمي عدم وجود أي تطبيق مالي يحمل الاسم المطابق **`MONAVOCE`** أو **`موناڤوتشي`** (توجد فقط أسماء متقاربة صوتيًا مثل `Monavo` و`MonAi` و`Monivo`).したがって، فإن اسم **MONAVOCE** متفرد ويمتلك قابلية عالية للتسجيل كعلامة تجارية مميزة، شريطة تغيير معرف الحزمة البرمجي من `com.myexpenses.app` إلى **`com.monavoce.app`** (`MONA-PLAY-001`).

---

## 2. الاتجاه البصري للشعار والأيقونة (Logo & Symbolic Direction)

### مفهوم الشعار: **"موجة العملة الصوتية" (The Sonic Coin / Monogram MV Wave)**
بدلاً من أيقونة الميكروفون التقليدية أو رمز الدولار المستهلك، يدمج شعار **MONAVOCE** بين:
1. **حرف `M` الهندسي المتماثل** الذي تتشكل قمتاه الوسطيان على هيئة **نبضة موجية صوتية (Acoustic Waveform)** تنحدر لتشكل حرف **`V`** في القلب.
2. **الحلقة السيادية المحيطة (Sovereign Vault Ring):** دائرة فائقة النعومة تحيط بالموجة، ترمز في آنٍ واحد إلى **العملة النقدية (Moneta)**، و**الدرع المحلي المغلق (Zero-Cloud Privacy Vault)**، و**الهالة الصوتية النابضة (Voce Orb)**.

---

## 3. نظام الألوان المعتمد (MONAVOCE Color Palette & Material 3 Tokens)

يحافظ نظام الألوان المقترح لـ **MONAVOCE** على الهوية الفخمة التي اختارها المستخدم (**Obsidian OLED + Soft Periwinkle + Sonic Cyan/Violet Orb**) مع صقلها بمعايير التباين العالمية (**WCAG AAA / AA**) وتوفير وضع نهاري متكامل (**Alabaster Vault Light Theme**) يعالج الخلل `MONA-UX-001`.

### أ. الوضع الليلي الرئيسي — **"Obsidian Vault"** (Default Premium Dark Theme)

| الرمز (Token Name) | كود HEX | الاستخدام المعماري في Material 3 / Compose |
|---|---|---|
| `MonaObsidianBg` | `#07080D` | `background` — خلفية الشاشات الرئيسية المتوافقة مع شاشات AMOLED. |
| `MonaSurface1` | `#11131C` | `surface` — خلفية البطاقات القياسية (`ExpenseListItemCard`, `CategoryCard`). |
| `MonaSurface2` | `#171A26` | `surfaceVariant` — بطاقة النظرة العامة الرئيسية (`Overview Hero Card`) وشريط التنقل العائم. |
| `MonaSurfaceElevated` | `#1F2333` | الحاويات الفرعية، أزرار التبديل الزمنية (`Segmented Pills`)، وحقول البحث. |
| `MonaBorderSubtle` | `#272B3F` | `outlineVariant` — حدود البطاقات الرفيعة (`1.dp`) لإبراز العمق الزجاجي. |
| `MonaPrimaryPeriwinkle` | `#8E8FFA` | `primary` — اللون الرئيسي للعلامة (الأعمدة البيانية، المؤشرات النشطة، الأزرار الرئيسية). |
| `MonaPrimaryContainer` | `#23254B` | `primaryContainer` — خلفية العناصر النشطة في شريط التنقل السفلي. |
| `MonaSonicCyan` | `#00E5FF` | `secondary` — اللون الصوتي النابض لحلقة الاستماع النشطة ومؤشر الأوفلاين. |
| `MonaSonicMagenta` | `#D946EF` | `tertiary` — الطيف اللوني المكمل لهالة الكرة الصوتية (`Cosmic Voice Orb`). |
| `MonaGoldSovereign` | `#F5B041` | لون التمييز المالي للعملات والميزانيات والتنبيهات القيّمة. |
| `MonaEmeraldSuccess` | `#10B981` | حالة جاهزية النموذج الصوتي (`100% Offline Ready`) ورسائل الحفظ الناجح. |
| `MonaCoralDanger` | `#F43F5E` | `error` — حالة الحذف، تجاوز الميزانية، أو أخطاء الميكروفون. |
| `MonaTextPrimary` | `#F4F5FA` | `onBackground` / `onSurface` — الأرقام المالية والعناوين الرئيسية (تباين `17.8:1` AAA). |
| `MonaTextSecondary` | `#9CA3C2` | `onSurfaceVariant` — التسميات الفرعية، التواريخ، والنسب المئوية (تباين `7.4:1` AAA). |

### ب. الوضع النهاري — **"Alabaster Prestige"** (Harmonized Light Theme)

| الرمز (Token Name) | كود HEX | الاستخدام المعماري في Material 3 / Compose |
|---|---|---|
| `MonaLightBg` | `#F6F7FB` | `background` — خلفية نهارية دافئة مريحة للعين. |
| `MonaLightSurface1` | `#FFFFFF` | `surface` — بطاقات المعاملات البيضاء النقية. |
| `MonaLightSurface2` | `#ECEEFA` | `surfaceVariant` — الحاويات الفرعية ومحددات الفترات. |
| `MonaLightBorder` | `#DCE0F2` | `outlineVariant` — حدود البطاقات النهارية. |
| `MonaLightPrimary` | `#4F46E5` | `primary` — نيلي ملكي عميق يحقق تباين `7.2:1` على الخلفية الفاتحة. |
| `MonaLightTextPrimary` | `#0D1021` | `onBackground` — الأرقام المالية والعناوين الداكنة. |
| `MonaLightTextSecondary` | `#52587A` | `onSurfaceVariant` — النصوص الفرعية والتواريخ. |

---

## 4. نظام الخطوط والطباعة المالية (Typography & Numeric System)

في تطبيق مالي ثنائي اللغة، يجب أن تصطف الأرقام عموديًا بدقة (`Tabular Figures`) وألا تتغير سماكة الحروف بين العربية والإنجليزية:
- **الخط العربي المقترح:** **`IBM Plex Sans Arabic`** أو **`Cairo`** (مدمج كـ downloadable/bundled font في `res/font/`).
- **الخط اللاتيني والأرقام:** **`Plus Jakarta Sans`** أو **`Inter`** مع تفعيل ميزة `fontFeatureSettings = "tnum"` (Tabular Numbers) لجميع المبالغ المالية والنسب المئوية.
- **مقياس الأحجام المعتمد (`MonaTypography`):**
  - `Hero Financial Total`: `34.sp`, `FontWeight.ExtraBold`, `letterSpacing = (-0.5).sp`, `fontFeatureSettings = "tnum"`.
  - `Screen Title`: `20.sp`, `FontWeight.Bold`.
  - `Card Amount`: `16.sp`, `FontWeight.Bold`, `fontFeatureSettings = "tnum"`.
  - `Voice Live Transcript`: `18.sp`, `FontWeight.Medium`, `lineHeight = 26.sp`.
  - `Metadata & Badges`: `12.sp`, `FontWeight.SemiBold`.

---

## 5. مواصفات إعادة تصميم الشاشات تحت هوية MONAVOCE

1. **الترويسة العلوية (`MONAVOCE Brand Header`):**
   - استبدال النص المؤقت `"My Expenses"` في شاشة الإعدادات وترويسة التطبيق بشعار **MONAVOCE** مع شارة حالة الخصوصية:
     `🔒 100% On-Device Voice Vault` / `🔒 خزنة صوتية محلية 100%`.
2. **اللوحة الصوتية (`MONAVOCE Sonic Sheet`):**
   - الاحتفاظ بالكرة الصوتية المتوهجة (`CosmicVoiceOrb`) التي أحبها المستخدم، مع إضافة:
     - شرائح أمثلة تفاعلية قابلة للنقر أو القراءة (`"قهوة 500 وبنزين 3000"`, `"احذف آخر مصروف"`).
     - دعم كامل لقارئ الشاشة TalkBack (`contentDescription` واضح لكل حالة: `جاري الاستماع`، `جاري التحليل`، `اضغط للتحدث`).
     - توافق ألوان البطاقات الفرعية مع الوضعين الليلي والنهاري.
3. **أيقونة التطبيق التكيفية (`Adaptive Launcher Icon` — Android 8.0+ / Android 13+ Monochrome):**
   - **الطبقة الخلفية (`ic_launcher_background.xml`):** تدرج قطري عميق من `#07080D` إلى `#131629` مع توهج مركزي خفيف `#1E2246`.
   - **الطبقة الأمامية (`ic_launcher_foreground.xml`):** رمز **MV Sonic Wave** محاطًا بحلقة متدرجة من `#00E5FF` عبر `#8E8FFA` إلى `#D946EF`.
   - **الطبقة أحادية اللون (`ic_launcher_monochrome.xml`):** نسخة خطية نظيفة من رمز **MV Sonic Wave** لدعم أيقونات Material You الملونة في Android 13–16.
4. **شاشة البداية (`Android 12+ SplashScreen API`):**
   - خلفية `#07080D` مع أيقونة **MONAVOCE** في المركز بدون تأخير مصطنع، تفتح الشاشة الرئيسية فورًا في أقل من `350ms`.

</div>
