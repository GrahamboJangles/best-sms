@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo.
echo ==========================================
echo   BestSMS - One-Click Debug APK Builder
echo ==========================================
echo.

where java >nul 2>&1
if errorlevel 1 (
  for %%D in ("%ProgramFiles%\Android\Android Studio\jbr" "%ProgramFiles%\Android\Android Studio\jre" "%LOCALAPPDATA%\Programs\Android Studio\jbr") do (
    if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
  )
)

if not defined JAVA_HOME (
  where java >nul 2>&1
  if errorlevel 1 (
    echo Java was not found.
    echo Install Android Studio from https://developer.android.com/studio and run this file again.
    pause
    exit /b 1
  )
) else (
  set "PATH=!JAVA_HOME!\bin;!PATH!"
)

if not defined ANDROID_SDK_ROOT set "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_HOME set "ANDROID_HOME=%ANDROID_SDK_ROOT%"

if not exist "%ANDROID_SDK_ROOT%\platform-tools" (
  echo Android SDK was not found at:
  echo   %ANDROID_SDK_ROOT%
  echo.
  echo Install Android Studio, open SDK Manager, and install:
  echo   - Android SDK Platform 35
  echo   - Android SDK Build-Tools 35.0.0
  echo   - Android SDK Platform-Tools
  echo.
  echo Then run this file again.
  pause
  exit /b 1
)

>local.properties echo sdk.dir=%ANDROID_SDK_ROOT:\=/%

echo Building the debug APK. The first build may take several minutes...
call gradlew.bat :app:assembleDebug --no-daemon
if errorlevel 1 (
  echo.
  echo Build failed. Read the error above, or see BUILDING_WINDOWS.md for help.
  pause
  exit /b 1
)

if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo The build finished but the APK was not found.
  pause
  exit /b 1
)

copy /y "app\build\outputs\apk\debug\app-debug.apk" "BestSMS-debug.apk" >nul
echo.
echo SUCCESS! Your APK is here:
echo   %CD%\BestSMS-debug.apk
echo.
echo Copy BestSMS-debug.apk to your Android phone, open it, and allow installation.
echo You may need to select BestSMS as the default SMS app for full functionality.
pause
endlocal
