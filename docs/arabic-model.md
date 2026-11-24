<div dir="rtl" lang="ar">

# نموذج التعرف على الصوت بالعربية

يستخدم MONAVOCE محرك Sherpa-ONNX مع نموذج FastConformer للهجة الخليجية. النموذج منفصل عن التطبيق.

## المصدر والترخيص

طابقت ملفات النموذج الأربعة في الحزمة المحلية نسخة `6e9acacc41528874b2b560c1776cac611939e256` من نموذج **vladadu / stt-ar-fastconformer-gulf-sherpa-onnx**. درّب ناشر النموذج النسخة الخليجية باستخدام SADA22 انطلاقًا من نموذج FastConformer العربي من NVIDIA. [بطاقة النموذج](https://huggingface.co/vladadu/stt-ar-fastconformer-gulf-sherpa-onnx)

الأوزان مرخّصة بموجب **CC BY-NC-SA 4.0**. يشترط الترخيص نسب المصدر، ويقصر الاستخدام وإعادة التوزيع على الأغراض غير التجارية، ويشترط الترخيص بالمثل عند توزيع نسخة معدّلة. للنشر التجاري، يلزم نموذج يسمح ترخيصه بذلك أو إذن منفصل. [نص الترخيص](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)

## الاستيراد

نزّل الملفات الأربعة من [نسخة النموذج المطابَقة](https://huggingface.co/vladadu/stt-ar-fastconformer-gulf-sherpa-onnx/tree/6e9acacc41528874b2b560c1776cac611939e256)، وضعها في ملف ZIP. بعدها اختر **استيراد نموذج العربية المحلي (.zip)** من إعدادات التطبيق.

</div>

| File | SHA-256 |
| --- | --- |
| `encoder.int8.onnx` | `26b3f302a3384a70dc25f264c05a023d72c5b00a1755303ea3d3b7d6c681bbab` |
| `decoder.int8.onnx` | `838f03f746a49deaa00b0ccab4c07acbe97101fb5dcaf6cb93fcc27042caf84e` |
| `joiner.int8.onnx` | `d8ee37bbbe77406ee9d99659747064047b3538f261f2c47431f0274ad3a8344f` |
| `tokens.txt` | `b5d39b52143723e71d997ed5dd8a68aa5facbd0a7c84a47669f83b0033d33164` |

<div dir="rtl" lang="ar">

الحزمة المحلية التي فُحصت اسمها `MyExpenses-Offline-Arabic-Model.zip` وحجمها 106,106,155 بايت. قد تختلف بصمة ملف ZIP عند إعادة ضغط الملفات نفسها.

</div>

```text
SHA-256: 580be8112133f54859bdf46c1e2894cbc5efae36148187eb6dbe45edc7e27aa8
```
