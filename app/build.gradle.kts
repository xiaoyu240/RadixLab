import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---- 签名配置 ----
// 证书与口令写在 keystore.properties（已在 .gitignore 中排除，不会上传 GitHub）。
// 若该文件缺失，release 构建会自动回退到 debug 签名，便于本机直接跑通。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
// 注意：必须以 rootProject 为基准解析路径。
// app 模块里的 file(...) 会以 android/app 为相对根，而证书在 android/keystore/ 下。
val releaseKeystoreFile = keystoreProps.getProperty("storeFile")
    ?.let { rootProject.file(it) }

val hasReleaseKeystore = releaseKeystoreFile?.exists() == true

android {
    namespace = "com.radixlab.app"
    compileSdk = 35

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                // 同时启用 V1/V2/V3 签名，兼容各版本 Android
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.radixlab.app"
        minSdk = 26
        targetSdk = 35
        // 版本号规则：小更新只动第三位（1.0.0 → 1.0.1），大更新动第二位（1.0.x → 1.1.0）。
        // versionCode 每次发版必须 +1（Android 要求单调递增，与版本名无关）。
        // 1.3.0 = 中更新（用户定的「动第二位」口径）：
        //   全应用锁定竖屏；工具页改成独立路由，进入时不再显示底部导航栏；
        //   新增推入 / 弹出转场动画；首页返回改为双击退出，其他页面返回是回上一页。
        // 1.3.1 = 小更新：UI 全面对齐网站 v5「大字网格」——
        //   品牌色蓝 → 暖橙红；表面色改暖白 / 暖夜；全站直角 + 1px 细线、去阴影；
        //   数字与结果换 Azeret Mono（随包自带）；主按钮改反色、芯片选中态改反色填充。
        // 1.3.2 = 小更新：内部整理与稳定性改进（外观与可见交互保持不变）。
        // 1.3.3 = 小更新：修正一处手势热区问题，并微调界面布局与交互。
        versionCode = 8
        versionName = "1.3.3"

        // 无自定义测试 runner，保持工程零冗余依赖
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有正式证书就用正式证书，否则回退 debug 签名（保证本机可构建）
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // ---- 基础 ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ---- Compose（版本由 BOM 统一管理）----
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ---- 导航 / 数据 ----
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // ---- 调试期预览工具 ----
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ---- 测试 ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
