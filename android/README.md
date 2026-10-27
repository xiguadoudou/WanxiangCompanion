# 万象弈伴 Android 源码

这里是当前安装包对应的 Android 实现：Java 原生服务与 WebView 界面，最低 Android 8.0，当前目标 API 35。运行时源码与已发布安装包对应；公开构建脚本改用本地签名配置，仓库和公开源码包都不包含原签名私钥。

- `src/`：主程序、悬浮窗、资料读取、推荐查询、GitHub 软件更新。
- `assets/`：主界面、紧凑浮层、推荐逻辑、资料快照和阵营图片。
- `res/`、`AndroidManifest.xml`：安卓资源、组件和权限。
- `tests/`：保留的原生与界面回归检查，`fixtures/` 为历史公开数据响应测试资料，产品不使用这些测试响应代替在线推荐。

## 构建

需要 JDK 17（完整 JDK）、Python 3、bash、Android SDK Platform 35 和 Build Tools 35.0.0。支持 Linux/macOS 或 Windows 的 WSL 环境。

```bash
export ANDROID_HOME=/absolute/path/to/android-sdk
bash build.sh
```

默认生成本地调试签名，输出 `dist/WanxiangCompanion-<版本>-debug.apk`。它不具有官方签名，不能覆盖已安装的官方版。需要输出官方同签名更新包时，由维护者在私有环境提供原签名：

```bash
export WXQ_KEYSTORE=/private/path/original.jks
export WXQ_KEY_ALIAS=your_alias
# 在自己的终端安全设置 WXQ_STORE_PASSWORD，必要时设置 WXQ_KEY_PASSWORD。
bash build.sh
```

自定义 SDK 路径可使用 `WXQ_SDK`、`WXQ_BT`、`WXQ_PLATFORM`。签名文件、密码文件和环境配置不应提交到仓库。

## 验证

不依赖浏览器的前端逻辑检查：

```bash
node tests/logic.test.js
```

界面回归需要自行安装 Playwright 与 Chromium。在本目录安装 `playwright` 后，可逐个运行 `tests/*.test.cjs`，例如：

```bash
npm install --no-save playwright
npx playwright install chromium
mkdir -p build
node tests/round-reset.test.cjs
node tests/compact-details.test.cjs
```

若使用已有 Chromium，可设置 `WXQ_BROWSER_PATH`。`WXQ_PLAYWRIGHT_MODULE` 可指定 Playwright 模块路径。

原生纯逻辑测试需要主机可运行的 `org.json` JAR（Android SDK 的桩类不能用于主机运行）。提供该 JAR 路径后：

```bash
mkdir -p build/native-test
java -m jdk.compiler/com.sun.tools.javac.Main --release 8 -encoding UTF-8 \
  -cp "$WXQ_JSON_JAR" -d build/native-test \
  src/com/doudou/wxq/{BuildInfo,RoundSelection,OverlayGeometry,OverlayState,DragTapTracker,UpdateTransport,UpdateContract}.java \
  tests/{RoundSelectionTest,OverlayModelTest,UpdateContractTest,UpdateTransportTest}.java
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.RoundSelectionTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.OverlayModelTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.UpdateContractTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.UpdateTransportTest
```

## 发布与源码对应关系

每次功能更新都需要构建新的 APK，并同步这里的源码、资源、版本号和测试。根目录发布脚本会验证 Java/Manifest 版本、APK 中的全部 assets，以及版本资料中的 APK 摘要，再生成不含签名的源码 ZIP。Release 附件 `WanxiangCompanion-<版本>-source.zip` 是该版本固定源码，`SOURCE.json` 记录对应 APK 的 SHA-256。

0.1.9 的 Android 源码是在原 Release 建立后补充的，请下载明确命名的 `WanxiangCompanion-0.1.9-source.zip`；GitHub 自动生成的旧标签 “Source code” ZIP 不包含此次补充的 Android 目录。后续版本的发布标签会包含同步提交的源码。

资料来源为 https://www.datawxq.com/ 的公开资料，图标及游戏资料的相关权利归原权利方。软件不读取游戏画面或游戏账号。
