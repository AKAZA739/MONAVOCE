# Room Entities, DAOs, and Query Projections
-keep class com.myexpenses.app.data.local.** { *; }

# Sherpa-ONNX JNI classes (accessed via JNI reflection / native pointers)
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn android.speech.**
