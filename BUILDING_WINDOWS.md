# Build BestSMS on Windows: Beginner Guide

This guide assumes you have never built an Android app before. You do not need to understand Kotlin or Gradle to create a test APK.

## The easiest method: Android Studio

1. Install [Android Studio](https://developer.android.com/studio). Use the standard installation options.
2. Open Android Studio once and allow the setup wizard to install the recommended Android SDK.
3. In Android Studio, open **More Actions → SDK Manager**. Confirm that **Android SDK Platform 35**, **Android SDK Build-Tools**, and **Android SDK Platform-Tools** are installed.
4. Download or clone this repository. On GitHub, click **Code → Download ZIP**, then extract the ZIP to a normal folder such as `C:\Projects\best-sms`. Do not build from inside the ZIP preview.
5. Open the extracted `best-sms` folder in Android Studio and wait for Gradle sync to finish. The first sync can take several minutes.
6. For the simplest build, close Android Studio and double-click **`build_debug.bat`** in the project folder.
7. When the script says `SUCCESS`, the APK will be in the project folder as **`BestSMS-debug.apk`**. It is also stored at `app\build\outputs\apk\debug\app-debug.apk`.

## Building from Android Studio instead

Open the project, wait for indexing and Gradle sync to finish, then choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Click the notification link when Android Studio reports that the APK was generated. Choose the `debug` variant if Android Studio asks which variant to build.

## Installing the APK

Copy `BestSMS-debug.apk` to your Android phone, open it from the Files app, and approve installation if Android asks. You may need to enable **Install unknown apps** for the Files app or browser used to open the APK. Launch BestSMS, grant the requested permissions, and select it as the default SMS application when prompted. Full SMS/MMS provider access requires the default-SMS role on modern Android versions.

## If the one-click script fails

| Message | Fix |
|---|---|
| `Java was not found` | Install Android Studio, or install a Java Development Kit and reopen Command Prompt. Android Studio normally includes its own Java runtime. |
| `Android SDK was not found` | Open Android Studio → **More Actions → SDK Manager**, install the required SDK packages, then run the script again. |
| `SDK location not found` | The script normally detects `%LOCALAPPDATA%\Android\Sdk`. If your SDK is elsewhere, set `ANDROID_SDK_ROOT` to that folder before running the script. |
| Gradle download or network error | Check your internet connection and run the script again. The Gradle wrapper downloads its required Gradle version automatically. |
| `License not accepted` | Open Android Studio’s SDK Manager, install the requested package, and accept the licenses. |
| APK will not install | Uninstall an older debug build if it was signed differently, then install the new APK. Do not uninstall your production messaging app unless you intend to change the default SMS application. |
| Build fails after changing files | Run `git restore .` only if you want to discard local changes. Otherwise read the first error in the build output; later errors are often consequences of the first one. |

## Command-line build for experienced users

From a Command Prompt opened in the project folder:

```bat
gradlew.bat :app:assembleDebug
```

The output is `app\build\outputs\apk\debug\app-debug.apk`. The included `build_debug.bat` additionally detects common Android Studio installations, writes `local.properties`, copies the APK to the project root, and prints the exact output path.

## Safe files and files to avoid committing

Do not commit `local.properties`, signing keys, API keys, carrier credentials, personal SMS backups, or private phone numbers. The repository’s `.gitignore` already excludes the local Android SDK configuration and common IDE/build files.
