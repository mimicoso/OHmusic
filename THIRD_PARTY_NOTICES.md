# 第三方开源声明 / Third-Party Notices

OHMusic 使用通过 Google Maven 和 Maven Central 分发的开源组件。本文件记录项目直接依赖的主要组件、用途和许可证。各组件的著作权、商标、许可证及 `NOTICE` 仍归原项目及贡献者所有。

OHMusic uses open-source components distributed through Google Maven and Maven Central. This document records the project's direct dependencies, their purposes, and their licenses. Copyrights, trademarks, license terms, and upstream `NOTICE` files remain with their respective projects and contributors.

## 应用运行时与编译依赖 / Application dependencies

| 项目 / Project | 直接依赖及版本 / Direct artifacts and versions | 用途 / Purpose | 许可证 / License |
| --- | --- | --- | --- |
| [AndroidX / Jetpack Compose](https://github.com/androidx/androidx) | Compose BOM `2025.12.00`; Compose UI, Graphics, Material 3, Foundation, Tooling Preview, Material Icons Extended | Compose 用户界面 / Compose UI | Apache License 2.0 |
| [AndroidX Core](https://developer.android.com/jetpack/androidx/releases/core) | `androidx.core:core-ktx:1.19.0` | Android 核心扩展 / Android core extensions | Apache License 2.0 |
| [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) | `lifecycle-runtime-ktx:2.11.0`, `lifecycle-viewmodel-compose:2.11.0` | 生命周期与状态管理 / Lifecycle and state | Apache License 2.0 |
| [AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity) | `activity-compose:1.13.0` | Compose Activity 集成 / Compose Activity integration | Apache License 2.0 |
| [AndroidX Navigation](https://developer.android.com/jetpack/androidx/releases/navigation) | `navigation-compose:2.9.8` | 页面导航 / Navigation | Apache License 2.0 |
| [AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room) | `room-runtime:2.8.4`, `room-ktx:2.8.4`, `room-compiler:2.8.4` | 本地数据库 / Local database | Apache License 2.0 |
| [AndroidX Hilt](https://developer.android.com/jetpack/androidx/releases/hilt) | `hilt-navigation-compose:1.4.0` | Hilt 与 Compose 导航集成 / Hilt navigation integration | Apache License 2.0 |
| [AndroidX Media3](https://developer.android.com/jetpack/androidx/releases/media3) | `media3-exoplayer:1.10.1`, `media3-session:1.10.1` | 音频播放与系统媒体会话 / Playback and media sessions | Apache License 2.0 |
| [Dagger / Hilt](https://github.com/google/dagger) | `hilt-android:2.60.1`, `hilt-android-compiler:2.60.1` | 依赖注入 / Dependency injection | Apache License 2.0 |
| [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | `io.github.kyant0:backdrop:2.0.0` | 液态玻璃背景效果 / Liquid-glass backdrop | Apache License 2.0 |
| [Kyant Shapes](https://github.com/Kyant0/Shapes) | `io.github.kyant0:shapes:1.2.0` | Compose 图形与形状 / Compose shapes | Apache License 2.0 |
| [Coil](https://github.com/coil-kt/coil) | `io.coil-kt.coil3:coil-compose:3.5.0` | 本地封面解码与显示 / Local artwork decoding and display | Apache License 2.0 |
| [Kotlin](https://github.com/JetBrains/kotlin) and [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Versions selected by the Kotlin and AndroidX dependency graphs | Kotlin 运行时与协程 / Kotlin runtime and coroutines | Apache License 2.0 |

## 构建与测试依赖 / Build and test dependencies

以下组件用于构建、代码生成或测试，通常不会作为独立功能代码打入正式 APK。

The following components are used for builds, code generation, or tests and are generally not packaged as standalone application functionality in the release APK.

| 项目 / Project | 版本 / Version | 许可证 / License |
| --- | --- | --- |
| [Gradle](https://github.com/gradle/gradle) | `9.4.1` | Apache License 2.0 |
| [Android Gradle Plugin](https://android.googlesource.com/platform/tools/base/) | `9.2.1` | Apache License 2.0 |
| [Kotlin Compose compiler plugin](https://github.com/JetBrains/kotlin) | `2.3.0` | Apache License 2.0 |
| [Kotlin Symbol Processing (KSP)](https://github.com/google/ksp) | `2.3.9` | Apache License 2.0 |
| [Dagger Hilt Gradle plugin](https://github.com/google/dagger) | `2.59` | Apache License 2.0 |
| [JUnit 4](https://github.com/junit-team/junit4) | `4.13.2` | Eclipse Public License 1.0; test scope only |
| [AndroidX Test](https://github.com/android/android-test) | JUnit `1.3.0`, Core `1.7.0`, Runner `1.7.0`, Room Testing `2.8.4` | Apache License 2.0; test scope only |

## 传递依赖 / Transitive dependencies

直接依赖会继续引入 Kotlin 标准库、AndroidX 基础模块、协程、注解及其他支持组件。它们的准确版本由 Gradle 锁定的依赖图决定，可使用以下命令查看正式版本的完整解析结果：

Direct dependencies also bring in the Kotlin standard library, AndroidX foundation modules, coroutines, annotations, and other support components. Their exact resolved versions are determined by Gradle. Inspect the complete release dependency graph with:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

项目未复制或修改上述库的源代码。随上游制品提供的许可证和 `NOTICE` 文件继续有效。本声明不替代任何第三方许可证原文；发布二进制版本前，应重新核对解析后的依赖图及上游声明。

OHMusic does not vendor or modify the source of the libraries listed above. License and `NOTICE` files shipped with upstream artifacts remain effective. This document does not replace upstream license texts; review the resolved dependency graph and upstream notices before each binary release.
