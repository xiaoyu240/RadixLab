# 进制工坊 · RadixLab

`RadixLab` 仓库的源码。Kotlin + Jetpack Compose + Material 3，纯本地工具，
**不申请任何权限（包括网络权限）**。

- 包名：`com.radixlab.app`
- 显示名：进制工坊
- minSdk 26 / targetSdk 35 / compileSdk 35
- 官网：http://uk.frp.one:52600/

---

## 一、目录结构

```
android/
├── settings.gradle.kts                 模块与仓库声明
├── build.gradle.kts                    顶层构建脚本
├── gradle.properties                   JVM 参数 / AndroidX 开关
├── .gitignore
├── README.md
├── gradle/
│   ├── libs.versions.toml              版本目录（Version Catalog）
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts                模块构建脚本
    ├── proguard-rules.pro              R8 混淆规则
    └── src/main/
        ├── AndroidManifest.xml         ★ 无任何权限声明；锁定竖屏 + 开启预测式返回
        ├── java/com/radixlab/app/
        │   ├── MainActivity.kt         唯一 Activity，边到边 + 主题分发
        │   ├── RadixLabApp.kt          根组件：底部导航（仅顶层页显示）+ 双击退出
        │   ├── navigation/
        │   │   ├── AppNavHost.kt       导航图 + 推入/弹出转场（含 tool/{key} 子页面路由）
        │   │   └── BottomNavItem.kt    首页 / 我的
        │   ├── ui/theme/               设计 Token（与网站语义色共用）
        │   │   ├── Color.kt            颜色
        │   │   ├── Theme.kt            主题 + 深浅色模式
        │   │   ├── Type.kt             字体（等宽用于进制结果）
        │   │   └── Shape.kt            圆角 + 间距 Token
        │   ├── ui/screens/
        │   │   ├── HomeScreen.kt       首页：品牌头部 + 正方形工具卡片宫格
        │   │   ├── ToolScreen.kt       工具页外壳：返回行 + 内容（无底部导航栏）
        │   │   ├── ConverterContent.kt 进制转换
        │   │   ├── CalculatorContent.kt 大数计算器（含 π）
        │   │   ├── IpToolContent.kt    IP 转换
        │   │   └── ProfileScreen.kt    历史记录 + 收藏 + 外观 + 关于
        │   ├── ui/components/
        │   │   ├── BaseSelector.kt     进制选择器（2-36）
        │   │   ├── ResultCard.kt       结果卡 / 说明卡
        │   │   ├── HistoryItem.kt      历史记录条目
        │   │   └── BrandLogo.kt        品牌图标（复用启动图标图形）
        │   ├── viewmodel/
        │   │   ├── ConverterViewModel.kt
        │   │   ├── CalculatorViewModel.kt
        │   │   └── IpViewModel.kt
        │   ├── data/
        │   │   ├── ConversionEngine.kt 2-36 进制互转（BigInteger）
        │   │   ├── CalculatorEngine.kt 有理数精确四则运算
        │   │   ├── PiEngine.kt         Chudnovsky 公式 + 二进制分裂
        │   │   ├── IpUtils.kt          IPv4 / IPv6 工具
        │   │   ├── model/ConversionRecord.kt
        │   │   └── repository/HistoryRepository.kt   DataStore
        │   └── utils/ClipboardUtils.kt
        └── res/
            ├── values/{strings,colors,themes}.xml
            ├── values-night/{colors,themes}.xml   （深色模式窗口背景）
            ├── xml/{backup_rules,data_extraction_rules}.xml
            ├── drawable/ic_launcher_foreground.xml
            └── mipmap-anydpi-v26/ic_launcher.xml
```

---

## 二、构建与运行

### 1. 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Android SDK | Platform 35 + Build-Tools 35.x |
| Gradle | 8.9（wrapper 已配置） |
| AGP | 8.7.2 |
| Kotlin | 2.0.21 |

### 2. 首次打开

用 **Android Studio（Ladybug 2024.2 及以上）** 直接 `Open` 本目录（`android/`），
IDE 会自动补全 `gradle-wrapper.jar` 与 `local.properties`。

> 本仓库刻意不提交 `gradle-wrapper.jar` 与 `local.properties`：
> 前者是二进制文件，后者包含你本机的 SDK 路径，都不应该进版本库。
> 首次打开时 IDE 会提示「Gradle wrapper jar missing」，点 OK 自动生成即可。
> 如果习惯命令行，也可以在 `android/` 下执行一次 `gradle wrapper --gradle-version 8.9`。

### 3. 命令行构建

```bash
cd android

# 调试包（可直接安装，无需签名配置）
./gradlew assembleDebug      # Windows: gradlew.bat assembleDebug

# 正式包（需要先配置签名，见下）
./gradlew assembleRelease

# 安装到已连接设备
./gradlew installDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 4. 配置正式签名

1. 生成密钥库：

```bash
keytool -genkeypair -v -keystore radixlab.jks -alias radixlab \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. 在 `android/` 下新建 `keystore.properties`（已在 `.gitignore` 中，不会提交）：

```properties
storeFile=radixlab.jks
storePassword=你的密码
keyAlias=radixlab
keyPassword=你的密码
```

3. 在 `app/build.gradle.kts` 的 `android { }` 中加入：

```kotlin
val keystoreProps = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

signingConfigs {
    create("release") {
        storeFile = file(keystoreProps.getProperty("storeFile") ?: "radixlab.jks")
        storePassword = keystoreProps.getProperty("storePassword")
        keyAlias = keystoreProps.getProperty("keyAlias")
        keyPassword = keystoreProps.getProperty("keyPassword")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

## 三、需要随环境更新的地址

用户名 `xiaoyu240` 与仓库地址都已确定为真实值，不再是占位符。唯一需要在
「官网换域名」时同步的是 `res/values/strings.xml` 里的 4 条链接：

| 资源名 | 当前值 | 说明 |
| --- | --- | --- |
| `url_github` | `https://github.com/xiaoyu240/RadixLab` | 开源项目入口 |
| `url_website_repo` | `https://github.com/xiaoyu240/RadixLab` | 源码仓库（原 `RadixLab-Website` 仓库不存在，已统一） |
| `url_website` | `http://uk.frp.one:52600/` | 官方网站（ChmlFrp 隧道） |
| `url_privacy` | `http://uk.frp.one:52600/privacy.html` | 隐私政策 |
| `url_issues` | `https://github.com/xiaoyu240/RadixLab/issues` | 问题反馈 |
| `url_releases` | `https://github.com/xiaoyu240/RadixLab/releases` | 版本发布页 |

> 这些链接通过 `Intent.ACTION_VIEW` 交给系统浏览器打开，因此用 `http://`（明文）
> 也不受应用自身 cleartext 策略限制。换成自有域名或 HTTPS 后，改完需重新打包 APK。

替换后「关于」页的两个按钮（`Intent.ACTION_VIEW`）才会指向真实地址。

---

## 四、推送与发布

### 步骤 1：新建仓库

在 GitHub 新建 **public** 仓库 `RadixLab`（不要初始化 README）。

### 步骤 2：推送代码

```bash
cd android
git init -b main
git add .
git commit -m "feat: 进制工坊 Android 首发"
git remote add origin https://github.com/xiaoyu240/RadixLab.git
git push -u origin main
```

### 步骤 3：发布 APK 到 Releases（供官网下载）

```bash
# 先在 app/build.gradle.kts 里配置好签名，然后
./gradlew assembleRelease

# 用 gh CLI 发布（首次需 gh auth login）
gh release create v1.3.0 \
  app/build/outputs/apk/release/app-release.apk#RadixLab-1.3.0.apk \
  --title "进制工坊 v1.3.0" \
  --notes "全应用锁定竖屏；工具页改为独立路由，进入后不再显示底部导航栏，并带推入/弹出转场动画；首页返回改为双击退出。"
```

发布后，官网的下载按钮即可使用永久链接：

```
https://github.com/xiaoyu240/RadixLab/releases/latest/download/RadixLab-1.3.0.apk
```

---

## 五、功能与实现要点

| 功能 | 实现 |
| --- | --- |
| 通用进制转换 | `ConversionEngine`，`BigInteger` 按权展开 → 除基取余，2-36 进制 |
| 非法字符校验 | 逐字符比对，错误信息精确到「第 N 位」并给出合法字符范围 |
| 转换步骤 | `Result.Success.steps`，界面可折叠展示；超长数字自动省略中间步骤 |
| IP 转换 | `IpUtils`：IPv4 四种表示互转，每段 0-255 严格校验；IPv6 压缩/展开 + 内嵌 IPv4 |
| 大数计算器 | `CalculatorEngine`，`BigInteger` 有理数精确运算：四则、括号、乘方、取余；除不尽保留 10 位并标记为近似值 |
| π 无限计算 | `PiEngine`：Chudnovsky 公式 + 二进制分裂，自研整数开方 `isqrt`（Android 8–12 没有 `BigInteger.sqrt()`）；50 位首次警告，此后每涨 1 MB 提醒一次，10 MB 强制停 |
| 历史记录 | `HistoryRepository` + DataStore（JSON 序列化），去重、裁剪至 300 条、收藏优先保留 |
| 收藏 | 记录上的 `favorite` 标记，「我的」页可筛选 |
| 深浅色 | `RadixLabTheme` + `ThemeMode`（跟随系统/浅色/深色），持久化在 DataStore |
| 一键复制 | `ClipboardUtils`，只写不读剪贴板 |
| 打开链接 | 「我的」页关于卡中 `Intent.ACTION_VIEW`，异常时 Toast 兜底 |
| 竖屏锁定 | `AndroidManifest.xml`：`screenOrientation="portrait"` + `resizeableActivity="false"`（Android 12L+ 的大屏 / 折叠屏也不会被拉横或分屏） |
| 页面转场 | `AppNavHost` 统一配 `enter/exit/popEnter/popExit`：推入时新页从右滑入、旧页左移 1/3 淡出；返回正好相反。Tab 之间只做淡入淡出 |
| 子页隐藏底栏 | 工具页是独立路由 `tool/{key}`（不属于 `BottomNavItem`）；`RadixLabApp` 只在顶层路由渲染 `bottomBar`，切换时底部栏整体滑下 / 滑上 |
| 返回行为 | `enableOnBackInvokedCallback="true"` 走系统预测式返回手势；工具页侧滑 = 回上一页；首页用 `BackHandler` 双击退出（2 秒窗口，首次弹 Toast） |

### 设计规范

App 侧的全部颜色 / 圆角 / 间距 / 字体 Token 定义在 `ui/theme/` 下：

| App（Kotlin） | 值 |
| --- | --- |
| `BrandPrimary` / `BrandPrimaryDark` | `#2563EB` / `#1D4ED8` |
| `BrandPrimaryLight` | `#DBEAFE` |
| `BrandAccent` | `#06B6D4` |
| `BrandSuccess` / `BrandError` / `BrandWarning` | `#10B981` / `#EF4444` / `#F59E0B` |
| `LightBg` / `DarkBg` | `#F8FAFC` / `#0B1220` |
| `LightSurface` / `DarkSurface` | `#FFFFFF` / `#111827` |
| `LightTextPrimary` / `DarkTextPrimary` | `#0F172A` / `#F1F5F9` |
| `LightTextSecondary` / `DarkTextSecondary` | `#64748B` / `#94A3B8` |
| `LightBorder` / `DarkBorder` | `#E2E8F0` / `#1E293B` |
| `RadiusSmall/Medium/Large` | `8 / 12 / 20 dp` |
| `Dimens.Space1..8` | `4 / 8 / 12 / 16 / 24 / 32 dp` |
| `MonoFontFamily` | JetBrains Mono → 系统等宽 |

> **注意**：品牌色与语义色两端一致，但**版面语言已经分开**。
> App 保持 Material 3（实色面 + 标准 elevation）；
> 网站从 v1.2.0 起改为「渐变玻璃拟态」（光斑背景 + 半透明磨砂卡 + 渐变描边 + 大圆角），
> 相关 Token 见 `web/css/style.css` 第 1/2 节与末尾第 20 节。
> 改网站皮肤不需要同步改 App。

---

## 六、隐私

- `AndroidManifest.xml` 中 **零权限声明**，`allowBackup=false`；
- 无网络代码路径，无第三方统计 / 广告 SDK；
- 记录通过 `backup_rules.xml` 与 `data_extraction_rules.xml` 排除云备份。

详见 http://uk.frp.one:52600/privacy.html
