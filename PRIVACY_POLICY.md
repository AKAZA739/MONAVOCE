# Privacy Policy | سياسة الخصوصية — My Expenses | مصاريفي

_Last updated / آخر تحديث: 2026-11-22_

---

## العربية

تطبيق **My Expenses | مصاريفي** مصمّم بمبدأ **الخصوصية أولًا والعمل دون إنترنت (Offline-First & Privacy-First)**.

1. **عدم طلب صلاحية الإنترنت أصلًا:** التطبيق لا يطلب إذن `android.permission.INTERNET` في ملف الـ Manifest، ولا يتصل بأي خادم خارجي من تلقاء نفسه، ولا يحتوي على أي إعلانات أو أدوات تتبع أو تحليلات (Analytics).
2. **تخزين محلي 100%:** جميع مصروفاتك، الفئات، الكلمات المفتاحية، والإعدادات تُحفظ محليًا على هاتفك فقط داخل قاعدة بيانات Room (SQLite) وتفضيلات الجهاز الخاصة بالتطبيق.
3. **الميكروفون والتعرف الصوتي:**
   - تُطلب صلاحية الميكروفون (`RECORD_AUDIO`) فقط عندما تبدأ التسجيل الصوتي بفعلك المباشر.
   - لا يعمل الميكروفون أبدًا في الخلفية أو دون فتح نافذة التسجيل.
   - عند استخدام المحرك المحلي (Sherpa-ONNX للعربية أو حزمة الجهاز للإنجليزية)، تتم معالجة الصوت بالكامل داخل جهازك بدون أي اتصال بالشبكة.
   - إذا اختار المستخدم تفعيل «البديل السحابي الاختياري» على أجهزة لا تتوفر فيها حزم محلية لخدمة النظام، تتم المعالجة عبر خدمة التعرف الصوتي للنظام (Google Speech Services) بموافقة المستخدم الصريحة فقط.
4. **النسخ الاحتياطي والتحكم الكامل ببياناتك:** يمكنك تصدير مصروفاتك بصيغة CSV أو أخذ نسخة احتياطية JSON محلية واستعادتها أو حذفها في أي وقت.

---

## English

**My Expenses | مصاريفي** is built from the ground up on **Offline-First & Strict Privacy** principles.

1. **Zero Internet Permission:** The app does **not** declare `android.permission.INTERNET` in its manifest, never sends telemetry or analytics, contains zero ads, and requires no account or login.
2. **100% Local Storage:** All expenses, categories, keywords, and preferences are stored strictly on your device inside a local Room (SQLite) database and private SharedPreferences.
3. **Microphone & Speech Recognition:**
   - `RECORD_AUDIO` permission is requested only when you explicitly initiate voice input.
   - The microphone never runs passively or in the background.
   - With the on-device speech engine (Sherpa-ONNX for Arabic / on-device pack for English), audio is processed entirely on your phone CPU with zero network transmission.
   - Only if you explicitly opt in to the optional system speech fallback will audio be routed through your device's system `SpeechRecognizer` provider.
4. **Data Portability:** You have full ownership of your data via local CSV export and JSON backup/restore.
