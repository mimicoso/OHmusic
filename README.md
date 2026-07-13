# OHMusic

简体中文 | [English](README_EN.md)

[![Android CI](https://github.com/linkyplus-sys/OHMusic/actions/workflows/android.yml/badge.svg)](https://github.com/linkyplus-sys/OHMusic/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-brightgreen.svg)](#系统要求)

OHMusic 是一款使用 Kotlin 和 Jetpack Compose 开发的轻量 Android 本地音乐播放器。它专注于播放设备中已有的音乐，不包含账号、广告、数据统计或联网功能。

## 主要特点

- 扫描 Android MediaStore 中超过 2 分钟的本地歌曲
- 使用 Media3 `MediaLibraryService` 提供可靠的后台播放
- 支持通知栏、锁屏、耳机和蓝牙设备的系统媒体控制
- 支持顺序播放、单曲循环和随机播放
- 支持收藏歌曲，并自动恢复播放队列和进度
- 优先读取歌曲内嵌封面，无封面时显示内置默认封面
- 适配 Android 12+ 动态主题图标和主流厂商媒体通知样式
- 无搜索框、无自定义开屏、无网络权限，保持界面和启动流程轻量

## 系统要求

- Android 10（API 29）或更高版本
- 需要授予“音乐和音频”访问权限

OHMusic 会在扫描时过滤时长小于或等于 2 分钟的音频，避免把提示音、铃声和较短的录音加入音乐库。

## 安装与使用

正式版本发布后，可在仓库的 [Releases](https://github.com/linkyplus-sys/OHMusic/releases) 页面下载 APK。当前也可以按照下方开发说明自行构建。

安装后：

1. 授予本地音乐读取权限。
2. 应用会自动扫描符合条件的歌曲。
3. 点击音乐库中的歌曲开始播放。
4. 使用底部胶囊在“正在播放”和“音乐库”之间切换。

通知栏和状态栏播放器的最终外观由手机厂商系统决定。OHMusic 使用 Android 官方 Media3 接口，不模拟或破解厂商的灵动胶囊、原子通知等私有能力。

## 隐私说明

OHMusic 只在获得 Android 授权后读取本地音频文件及其元数据。歌曲信息、收藏状态、播放队列和进度均保存在设备本地。

应用不申请网络访问权限，不包含广告、账号、分析统计或在线封面服务。详细说明见 [PRIVACY.md](PRIVACY.md)。

## 开发环境

- Android Studio
- JDK 17 或更高版本
- Android SDK 37

如果 Android SDK 未被自动识别，请在项目根目录创建不会被 Git 跟踪的 `local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk
```

### Windows

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --no-parallel
.\gradlew.bat lintDebug --no-parallel
```

### Linux / macOS

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest --no-parallel
./gradlew lintDebug --no-parallel
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug 版本的应用 ID 为 `com.ohmusic.app.debug`，可以与正式版本同时安装。

## 正式构建

未配置签名时，可以验证并生成未签名 Release 产物：

```powershell
.\gradlew.bat lintRelease --no-parallel
.\gradlew.bat assembleRelease bundleRelease --no-parallel
```

正式签名通过以下环境变量配置，禁止将密钥或密码提交到仓库：

- `OHMUSIC_KEYSTORE_FILE`
- `OHMUSIC_KEYSTORE_PASSWORD`
- `OHMUSIC_KEY_ALIAS`
- `OHMUSIC_KEY_PASSWORD`

完整发布约定见 [CONTRIBUTING.md](CONTRIBUTING.md#release-signing)。

## 项目结构

```text
Compose UI -> ViewModel -> MusicPlayerManager -> MediaController
                                          \-> MediaLibraryService -> ExoPlayer
Compose UI -> ViewModel -> MusicRepository -> Room / MediaStore
```

更多信息：

- [架构说明](docs/ARCHITECTURE.md)
- [设备兼容性](docs/DEVICE_COMPATIBILITY.md)
- [隐私政策](PRIVACY.md)
- [更新记录](CHANGELOG.md)

## 参与贡献

欢迎提交 Issue 和 Pull Request。参与前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，安全问题请按照 [SECURITY.md](SECURITY.md) 中的方式报告。

## 开源许可

OHMusic 使用 [Apache License 2.0](LICENSE) 开源。第三方组件及其许可信息见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
