# 万象弈伴 Android 源码

这里是当前安装包对应的 Android 实现：Java 原生服务与 WebView 界面，最低 Android 8.0，当前目标 API 35。运行时源码与已发布安装包对应；公开构建脚本改用本地签名配置，仓库和公开源码包都不包含原签名私钥。

- `src/`：主程序、悬浮窗、资料读取、推荐查询、GitHub 软件更新。
- `assets/`：主界面、紧凑浮层、推荐逻辑、资料快照和阵营图片。
- `res/`、`AndroidManifest.xml`：安卓资源、组件和权限。
- `tests/`：保留的原生与界面回归检查，`fixtures/` 为历史公开数据响应测试资料，产品不使用这些测试响应代替在线推荐。

## 0.1.12 悬浮窗菜单

工具栏“菜单”新增返回软件、退出软件两个按钮；先确认再退出，取消或按返回键均保留助手。返回主程序自动隐藏浮层。退出时持久保存禁用状态，立即移除窗口、通知，结束已有页面，再结束本应用进程以停止查询/图片预载/更新线程；缓存文件保持原样。Android 系统和游戏进程不受影响。

`tests/overlay-actions.test.cjs` 覆盖 250×170、280×240、392×230、560×300 的按钮可达性、取消/返回键、返回软件和单次确认派发。原签名 APK 可覆盖升级；实际设备的后台启动限制仍以系统行为为准。

## 0.1.11 软件更新加速

- 软件版本检查优先访问 GHFast 的公开 GitHub 文件中转，失败后依次检查 GitHub Raw 官方地址和 GitHub Pages 的版本资料镜像。元数据使用独立连接/读取超时，返回错误网页、校验不通过或较旧版本信息时会尝试下一条线路。
- APK 优先尝试加速后的版本附件与仓库备用文件，再回到 GitHub 官方下载。版本资料保留官方原始 URL，客户端只为本仓库的公开 Release/updates 文件生成加速地址，不传递账号或访问令牌。
- 每条下载线路都核验文件大小、SHA-256、包名、版本与原签名。保留平台 HTTPS 校验，任何线路验证失败都不能直接安装；可以换下一条线路重试。
- 设置中的软件更新区可打开下载页；错误信息区分域名解析、超时、HTTPS 校验和无效版本资料。
- 发布时要求 `docs/latest.json` 与 `updates/latest.json` 逐字节一致，保持 Pages 备用检查源随新版本更新。
- 公共加速服务的可达性会随运营与网络变化，无法保证每个地区/运营商持续可用。旧版若已经无法访问 GitHub Raw，需要先手动覆盖安装一次新客户端；只有新客户端会使用加速线路。

## 0.1.10 悬浮兼容与图片缓存

- 图标始终留在同一个原生系统窗口，展开与收起只更新窗口尺寸、可见性和内容，不再在系统窗口与面板之间迁移图标。点击动作在当前触摸事件结束后执行。
- 面板创建或窗口扩展失败时退回可点击的小图标；窗口整体被系统拒绝时安排一次恢复尝试，保留通知中的重新启用入口。不会因为一次面板打开异常直接停止服务。
- Android 11+ 使用关联显示器的 Overlay WindowContext 获取尺寸和创建视图；异常时使用兼容路径。保留横屏贴边、右下角调整和原有自适应。
- 在软件中点击启用，先完成悬浮和可选通知权限流程，服务在主程序内以不可见且不接收触摸的方式准备窗口。系统窗口添加成功回执到达后才打开桌面；失败、超时、取消或旧回执不会触发返回桌面。主程序内仍不显示浮层。
- 处理 WebView 渲染进程终止：清理失效实例、保留原生悬浮图标并允许重建；主页面提供原生重新加载入口，避免共享渲染进程异常拖走整个助手。
- 第一次运行后台预载目录中的棋手、英雄、装备与阵营图片，和两个 WebView 共用应用私有目录中的磁盘缓存。同 URL 并发请求合并，重启和刷新优先使用本地图片；图片地址变化立即使用新缓存项。
- 图片缓存上限约 48 MB，单图上限 2 MB。7 天后用 ETag/Last-Modified 检查更新，304 复用原图片；离线优先使用旧图并延迟失败重试。损坏缓存会重新下载，按最近使用回收空间。原生图片下载失败允许 WebView 自行加载，不把网络兼容问题变成永久占位图。
- 资料快照和匹配条件缓存仍保留，网页资料模块改为应用私有文件目录缓存；阵容查询保持联网更新，缓存不伪造新统计。
- 已完成窗口故障/启动回执模型、图片缓存并发/离线/过期/容量检查和界面回归。未在具体故障手机的系统 WindowManager 和启动器实测，OEM 后台限制或权限撤销仍可能阻止显示。

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
  src/com/doudou/wxq/{BuildInfo,RoundSelection,OverlayGeometry,OverlayState,DragTapTracker,OverlayWindowSession,OverlayStartGate,ImageDiskCache,UpdateTransport,UpdateContract,UpdateRoutes,UpdateCheck}.java \
  tests/{RoundSelectionTest,OverlayModelTest,OverlayReliabilityTest,ImageDiskCacheTest,UpdateContractTest,UpdateTransportTest,UpdateRoutesTest}.java
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.RoundSelectionTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.OverlayModelTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.OverlayReliabilityTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.ImageDiskCacheTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.UpdateContractTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.UpdateTransportTest
java -cp "build/native-test:$WXQ_JSON_JAR" com.doudou.wxq.UpdateRoutesTest
```

## 发布与源码对应关系

每次功能更新都需要构建新的 APK，并同步这里的源码、资源、版本号和测试。根目录发布脚本会验证 Java/Manifest 版本、APK 中的全部 assets，以及版本资料中的 APK 摘要，再生成不含签名的源码 ZIP。Release 附件 `WanxiangCompanion-<版本>-source.zip` 是该版本固定源码，`SOURCE.json` 记录对应 APK 的 SHA-256。

0.1.9 的 Android 源码是在原 Release 建立后补充的，请下载明确命名的 `WanxiangCompanion-0.1.9-source.zip`；GitHub 自动生成的旧标签 “Source code” ZIP 不包含此次补充的 Android 目录。后续版本的发布标签会包含同步提交的源码。

资料来源为 https://www.datawxq.com/ 的公开资料，图标及游戏资料的相关权利归原权利方。软件不读取游戏画面或游戏账号。
