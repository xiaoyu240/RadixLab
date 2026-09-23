@echo off
REM ============================================================
REM  RadixLab 一键重新签名脚本
REM  用途：给已有的未签名/任意 APK 用正式证书重新签名
REM  用法：sign-apk.bat 输入.apk 输出.apk
REM  口令从 keystore.properties 读取（该文件已被 gitignore，不入库）
REM ============================================================
setlocal

set "JDK=C:\RadixLab\.workbuddy\android-build\jdk\jdk-17.0.20.1+1"
set "BT=C:\RadixLab\.workbuddy\android-build\sdk\build-tools\35.0.0"
set "KS=%~dp0keystore\radixlab-release.jks"
set "KS_ALIAS=radixlab"

REM ---- 从 keystore.properties 读取口令 ----
for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0keystore.properties") do (
  if "%%a"=="storePassword" set "KS_PASS=%%b"
  if "%%a"=="keyPassword"   set "KEY_PASS=%%b"
)
if "%KS_PASS%"=="" (
  echo [错误] 读不到口令：请确认 %~dp0keystore.properties 存在
  exit /b 1
)

if "%~1"=="" (
  echo 用法: sign-apk.bat 输入.apk 输出.apk
  exit /b 1
)
if "%~2"=="" (
  echo 用法: sign-apk.bat 输入.apk 输出.apk
  exit /b 1
)

if not exist "%KS%" (
  echo [错误] 找不到证书: %KS%
  exit /b 1
)

echo [1/3] 对齐 (zipalign)...
"%BT%\zipalign.exe" -p -f 4 "%~1" "%~dpn2.aligned.apk"
if errorlevel 1 goto :fail

echo [2/3] 签名 (apksigner)...
call "%BT%\apksigner.bat" sign ^
  --ks "%KS%" --ks-pass pass:%KS_PASS% --ks-key-alias %KS_ALIAS% ^
  --key-pass pass:%KEY_PASS% ^
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true ^
  --out "%~2" "%~dpn2.aligned.apk"
if errorlevel 1 goto :fail

echo [3/3] 校验签名...
call "%BT%\apksigner.bat" verify --print-certs "%~2"
if errorlevel 1 goto :fail

del /q "%~dpn2.aligned.apk" 2>nul
echo.
echo 完成: %~2
exit /b 0

:fail
echo.
echo [失败] 签名过程出错，请检查上面的输出。
if exist "%~dpn2.aligned.apk" del /q "%~dpn2.aligned.apk" 2>nul
exit /b 1
