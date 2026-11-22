package com.myexpenses.app.voice

internal enum class SystemRecognitionMode { ON_DEVICE, STANDARD, UNAVAILABLE }

internal fun selectSystemRecognitionMode(
    onDeviceAvailable: Boolean,
    localLanguageInstalled: Boolean?,
    standardAvailable: Boolean,
    allowOnlineFallback: Boolean
): SystemRecognitionMode = when {
    onDeviceAvailable && (localLanguageInstalled != false || !allowOnlineFallback || !standardAvailable) ->
        SystemRecognitionMode.ON_DEVICE
    allowOnlineFallback && standardAvailable -> SystemRecognitionMode.STANDARD
    else -> SystemRecognitionMode.UNAVAILABLE
}
