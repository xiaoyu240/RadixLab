# 签名说明（RadixLab）

> 解决的问题：手机安装 APK 时提示 **"没有安装证书" / "解析包错误" / "应用未签名"**。
> 根因：原来的 `app-debug.apk` 用的是 Android SDK 自带的**调试证书**，它由构建机器本地生成、
> 通用性差，很多国产 ROM（华为 / 小米 / OPPO / vivo 等）与部分 Android 高版本会直接拒绝安装。

---

## 一、已生成的正式证书

| 项目 | 值 |
| --- | --- |
| 证书文件 | `keystore/radixlab-release.jks`（本地私有，已 gitignore） |
| 别名 alias | `radixlab` |
| 库口令 / 密钥口令 | 见 `keystore.properties`（同样已 gitignore，勿上传） |
| 算法 | SHA256withRSA / 2048 位 |
| 有效期 | 2026-09-23 → 2056-09-15（30 年）|
| SHA256 指纹 | `EE:80:E2:29:63:19:EE:5A:03:B8:17:AF:50:1F:22:8C:A4:A5:9E:9D:E1:03:77:95:D0:D8:C3:AA:C1:12:54:08` |

> ⚠️ **务必备份** `radixlab-release.jks` 与两个口令。
> 证书一旦丢失，已安装的 App 将**无法再通过覆盖安装升级**（只能卸载重装，用户数据全丢）。

---

## 二、构建正式签名的 APK

工程已接入自动签名：打包 `release` 时会自动读取 `keystore.properties` 并签名。

```bash
cd C:\RadixLab\android
# 使用工程内自带的 JDK 17 + Gradle 8.10.2
java -cp "C:\RadixLab\.workbuddy\android-build\gradle-8.10.2\lib\gradle-launcher-8.10.2.jar" \
     org.gradle.launcher.GradleMain assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

> **踩坑提醒（已修复，勿再犯）**
> 1. `app/build.gradle.kts` 里解析证书路径**必须用 `rootProject.file(...)`**。
>    app 模块内的 `file(...)` 会以 `android/app/` 为相对根，导致找不到
>    `android/keystore/radixlab-release.jks`，构建会静默回退成 debug 签名
>    （APK 能装上，但证书是 `CN=Android Debug`，被手机拒装）。
> 2. Gradle 的**配置缓存**会缓存住旧配置。改完签名相关的脚本后，
>    加 `--no-configuration-cache`（或删除 `android/.gradle/`）再构建，
>    否则改动不生效。
> 3. 构建后务必复核：
>    ```
>    apksigner verify --print-certs app-release.apk
>    ```
>    看到 `DN: CN=RadixLab` 才算成功；`CN=Android Debug` 说明配置没吃到。

签名与口令写在 `keystore.properties`（本机私有文件，已被 gitignore）：

```properties
storeFile=keystore/radixlab-release.jks
storePassword=********
keyAlias=radixlab
keyPassword=********
```

该文件与 `*.jks` / `*.keystore` 均已在 `android/.gitignore` 中排除，
**不会上传到 GitHub**（遵守"只上传安卓源代码"的约定）。

---

## 三、给已有 APK 重新签名

如果手上已有别人给的 APK，或想单独重签，用一键脚本：

```bat
cd C:\RadixLab\android
sign-apk.bat  输入.apk  输出.apk
```

脚本会依次执行：`zipalign` 对齐 → `apksigner` 签名（V1+V2+V3）→ 校验并打印证书。

手动等价命令：

```bat
set BT=C:\RadixLab\.workbuddy\android-build\sdk\build-tools\35.0.0
set KS_PASS=你的口令（见 keystore.properties）
"%BT%\zipalign.exe" -p -f 4 in.apk aligned.apk
"%BT%\apksigner.bat" sign --ks keystore\radixlab-release.jks ^
  --ks-pass pass:%KS_PASS% --ks-key-alias radixlab --key-pass pass:%KS_PASS% ^
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true ^
  --out out.apk aligned.apk
"%BT%\apksigner.bat" verify --print-certs out.apk
```

---

## 四、安装失败的其他排查点

如果签名正确仍装不上，按顺序检查：

1. **卸载旧的 debug 版**。debug 版与 release 版签名不同，Android 不允许互相覆盖。
   ```
   adb uninstall com.radixlab.app
   ```
   或在手机上手动卸载"进制工坊"后重装。

2. **允许安装未知来源应用**。设置 → 安全 → 安装未知应用 → 允许你的文件管理器/浏览器。

3. **关闭"纯净模式"**。小米 / 华为等 ROM 有纯净模式，会拦截非应用商店来源的安装包。

4. **确认系统版本 ≥ Android 8.0（API 26）**。本 App 的 `minSdk = 26`。

5. **确认 APK 传输完整**。通过微信/QQ 传输可能被改名或压缩损坏，
   建议用数据线或 `adb install xxx.apk` 直接安装。

6. **查证书是否为正式证书**：
   ```
   "%BT%\apksigner.bat" verify --print-certs app-release.apk
   ```
   输出里的 `DN: CN=RadixLab` 即为正式证书；若是 `CN=Android Debug` 则为调试证书。

---

## 五、发布到应用商店

上架 Google Play 需用 `.aab` 格式：

```bash
java -cp "...gradle-launcher-8.10.2.jar" org.gradle.launcher.GradleMain bundleRelease
```

产物：`app/build/outputs/bundle/release/app-release.aab`

国内应用商店（华为/小米/OPPO 等）通常直接收 `.apk`，用上面的 `assembleRelease` 产物即可。
