# Arabic Speech Recognition Model

MONAVOCE uses the Sherpa-ONNX engine with a Gulf Arabic FastConformer model. The model is distributed separately from the app.

## Source and License

The four model files in the local bundle match revision `6e9acacc41528874b2b560c1776cac611939e256` of **vladadu / stt-ar-fastconformer-gulf-sherpa-onnx**. The model publisher trained the Gulf variant using SADA22 starting from NVIDIA's Arabic FastConformer model. [Model card](https://huggingface.co/vladadu/stt-ar-fastconformer-gulf-sherpa-onnx)

The weights are licensed under **CC BY-NC-SA 4.0**. The license requires attribution, restricts use and redistribution to non-commercial purposes, and requires ShareAlike licensing when distributing a modified version. For commercial distribution, a model with a permissive license or separate permission is required. [License text](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)

## Import

Download the four files from the [matched model revision](https://huggingface.co/vladadu/stt-ar-fastconformer-gulf-sherpa-onnx/tree/6e9acacc41528874b2b560c1776cac611939e256) and place them into a ZIP file. Then choose **Import Offline Arabic Model (.zip)** in the app settings.

| File | SHA-256 |
| --- | --- |
| `encoder.int8.onnx` | `26b3f302a3384a70dc25f264c05a023d72c5b00a1755303ea3d3b7d6c681bbab` |
| `decoder.int8.onnx` | `838f03f746a49deaa00b0ccab4c07acbe97101fb5dcaf6cb93fcc27042caf84e` |
| `joiner.int8.onnx` | `d8ee37bbbe77406ee9d99659747064047b3538f261f2c47431f0274ad3a8344f` |
| `tokens.txt` | `b5d39b52143723e71d997ed5dd8a68aa5facbd0a7c84a47669f83b0033d33164` |

The tested local bundle is named `MyExpenses-Offline-Arabic-Model.zip` and has a size of 106,106,155 bytes. The ZIP file's hash may differ if the same files are re-compressed.

```text
SHA-256: 580be8112133f54859bdf46c1e2894cbc5efae36148187eb6dbe45edc7e27aa8
```
