# Building and signing My Expenses

Use JDK 21 and Android SDK Platform 35. Configure the SDK in Android Studio or set `ANDROID_HOME`. Run the commands from the repository root.

## Verify a build

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease -PverifyUnsignedRelease=true
```

On macOS and Linux, use `sh ./gradlew` with the same arguments. This builds a Debug APK and an unsigned Release APK. The unsigned Release build checks minification and packaging. GitHub Actions runs the same tasks without access to a signing key.

- The Debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.
- The unsigned Release APK is at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Build a signed APK

Create a local `keystore.properties` at the repository root with the following keys. Use your existing release key if the APK will update an installed copy.

```properties
storeFile=keystore/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

The repository ignores the properties file and signing keys. Run the signed build without the unsigned verification flag.

```powershell
.\gradlew.bat assembleRelease
```

The signed APK is at `app/build/outputs/apk/release/app-release.apk`. Verify its certificate with [Android's apksigner](https://developer.android.com/tools/apksigner) before adding it to GitHub Releases. See the [Arabic model guide](arabic-model.md) for the model's separate license and verification information.
