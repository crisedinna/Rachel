# 我的学习冒险岛 Android 外壳

此项目用 Android WebView 加载 Mac 局域网里的 `index.html`。学习数据继续存在学习机 WebView 的 `localStorage` 中；Mac 只提供网页文件，不保存孩子的学习记录。

## 在 Mac 上运行网页服务

先停止之前运行的 `python3 -m http.server`（在对应终端按 Ctrl+C），再在项目目录运行：

```bash
python3 serve_lan.py
```

默认地址是 `http://192.168.22.230:8000/index.html`。如果 Mac 的局域网 IP 变化，可在 APK 顶栏点齿轮修改地址。建议在路由器中为 Mac 设置 DHCP 地址保留，避免 IP 变化后学习机把它当成新的网页来源。

Mac 需要开机、保持服务运行，学习机也要连同一个局域网。AndroidManifest 已允许 HTTP 明文流量，以便连接本地 Mac 服务。

## 编译 APK

### 不在本机安装 SDK：使用 GitHub Actions 云端编译

项目已包含 `.github/workflows/build-apk.yml`。GitHub Actions 会在云端准备 JDK、Gradle 和 Android SDK，构建 APK 后提供下载。

1. 在 GitHub 新建一个**私有仓库**。
2. 把 `android-app` 文件夹里的项目文件上传到仓库根目录，包含 `.github/workflows/build-apk.yml`。不要上传旁边的文章 PDF 和截图。
3. 打开仓库的 **Actions**，选择 **Build Learning Adventure APK**，点 **Run workflow**。
4. 编译完成后打开这次运行，在 **Artifacts** 区域下载 `learning-adventure-apk`，解压后把 `app-debug.apk` 传到学习机安装。

GitHub 只会编译和暂存 APK 文件，不会连到 Mac 的局域网服务。学习机安装后仍要能访问 Mac 上运行的网页服务。工作流产物设置为保留 14 天；请及时下载。

### 用 Android Studio 编译

1. 在装有 Android Studio 的电脑上打开 `android-app` 文件夹。
2. 等待 Gradle 同步完成。
3. 选择 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。
4. 默认调试 APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。
5. 把 APK 传到学习机安装。若设备由学校或家长模式管理，可能需要允许安装此来源的应用。

该项目使用 Android Gradle Plugin 8.7.3、JDK 17 和 Android SDK 35。当前开发环境没有 Java、Gradle 或 Android SDK，因此尚未在这里编译 APK。
