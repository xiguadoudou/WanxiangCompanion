# 万象弈伴 Android 源码

这里是当前安装包对应的 Android 实现：Java 原生服务与 WebView 界面，最低 Android 8.0，当前目标 API 35。运行时源码与已发布安装包对应；公开构建脚本改用本地签名配置，仓库和公开源码包都不包含原签名私钥。

- `src/`：主程序、悬浮窗、资料读取、推荐查询、GitHub 软件更新。
- `assets/`：主界面、紧凑浮层、推荐逻辑、资料快照和阵营图片。
- `res/`、`AndroidManifest.xml`：安卓资源、组件和权限。
- `tests/`：保留的原生与界面回归检查，`fixtures/` 为历史公开数据响应测试资料，产品不使用这些测试响应代替在线推荐。

## 0.1.20 实际资源路由与页面左滑

`WorkshopRoutes` 是 Android 本地资源服务与离线浏览器检查共同采用的路径规则，包含导出需要的 `/port-manifest.json`，修正原先浏览器任意文件服务掩盖 Android 404 的问题。`WorkshopRoutesTest` 检查配置、捕获模块、路径隔离；`workshop-local.test.cjs` 调用同一 Java 规则生成可访问路径列表后启动本地模拟。

`navigation-gesture.js` 在首页和模块间共享，只有从普通内容开始的触摸才注册可取消的移动监听。向左超过 56 CSS px 且方向明显为水平时，在松手后打开导航；棋盘、拖动源、水平滚动容器、输入框、滑块等保持自身事件。下一次触摸立即解除兼容点击抑制，纵向和多点触摸取消导航判定。MainActivity 仅保留已打开抽屉的原生关闭拖动，移除边缘右滑打开的旧限制。资料库工具区 position 改为 static。

## 0.1.19 页面融合、导出与动画

模块自己的 HTML 顶栏和正文共用同一 body 背景；`WorkshopView` 不再叠加独立原生工具栏。顶栏固定动作使用 `__WXQ_APP_ACTION__` 提示回调，Android 校验当前 WebView、模块 URL 与动作白名单后处理导航、本机方案和刷新；PNG 仍走单独限定的保存回调。不注入通用原生桥。

`port.js` 复用已打包的 html2canvas（保留原许可），直接捕获 `.board-capture-shell`，克隆中只更换浅色导出背景、格子及标签颜色，保留实际图像与附属区域。横向导出从实际角色 DOM 创建临时捕获区，结束即移除。`tools/cache_special_images.py` 根据基础记录解析并缓存原模拟器使用的特殊单位图片，重建脚本调用同一函数。离线 UI 回归检查导出范围、像素颜色、机械仆从图像和横向捕获区回收。

`MainActivity` 原生抽屉使用 ValueAnimator 与边缘触摸判定实现跟手拖动和回弹；页面切换使用短淡入位移，软件设置/详情/本地菜单提供对应 CSS/WAAPI 过渡。系统禁用动画时原生立即完成，Web 遵守 prefers-reduced-motion。首页更新箭头入口移除，更新流程仍可由齿轮设置进入。

## 0.1.18 外观和路由统一

`port.css` 将两套原站组件主题映射到首页的薄荷绿与磨砂卡片，并移除两套外壳导航和底部占位。`WorkshopView` 工具栏统一标识、间距、圆角工具按钮；标题说明随当前模块显示。`port.js` 在 Vue 挂载后注册当前模块路由守卫，对网站外壳设置 hidden/inert，并限制跨模块链接、History 和新窗口；`SiteModules.owns` 在 Android 导航与草稿存储中校验模块归属，切换功能只经过原生侧边栏。

离线 UI 回归增加实际 Vue router.push、链接与 History 越界尝试，验证 320/390/844 像素下导航不可见且不可聚焦、查询参数更新和原交互仍正常。

## 0.1.17 本地模块

`WorkshopView` 替换并移除了在线 `SiteModuleView`。`LocalWorkshop` 校验并读取 APK 中的授权移植组件 ZIP，仅在虚拟 app-asset 原点提供本地 HTML/JS/CSS/JSON；CSP 禁止远程脚本及账号 API。基础图片从包内读取，其他图片复用现有磁盘缓存。原站组件数据字面量已剥离到 `library-data.json`，`WorkshopData` 在已有更新链路中解析字面记录并原子保存，原站脚本不会在模块中执行。

授权移植来源、未改动文件摘要、基础数据与图片表在 `assets/workshop/site.zip` 内；本地适配源码在同目录 index.html/port.js/port.css。`tools/build_local_modules.py` 和 `tools/PrepareWorkshop.java` 重建公共模块，`tools/repack_workshop.py` 重打适配器。source_package.py 只对这个指定 ZIP 放行，并检查每个内部文件的类型、路径、体积与敏感数据，其他私钥和嵌套归档仍禁止。

`LocalPlan` 验证、导入、导出软件方案，`WorkshopView` 管理最多 12 个命名方案和持续草稿；`port.js` 本地 Canvas 绘图通过限定的 PNG 保存提示回调交给 `LocalPngExporter`，Android 10+ 写入图库，旧系统写入应用图片目录。模块不暴露通用 Native JS 桥。软件方案码与游戏内阵容码不同。

`workshop-local.test.cjs` 将所有非打包网络请求中止，验证桌面/手机启动、英雄与装备、鼠标/触摸站位、方案恢复、PNG 与六类资料。`WorkshopModelTest` 验证完整记录、纯数据刷新及方案码约束；已有侧边栏与更新 UI 回归通过。

## 0.1.16 侧边栏与在线模块（历史实现）

`MainActivity` 内的原生侧边栏统一切换推荐、模拟、资料库，`ModuleTabs` 延迟创建并保留三个页面；切换不触发 Activity 前后台重建或重置推荐条件。原本 AppView 独立放入 mainPane，显示失败的重新加载不会移除其他模块。旋转保留页面并调整侧边栏大小。

`SiteModuleView` 是独立 WebView，不使用 AppView、不添加 JavaScript Native 接口、不开放文件读取、内容 URI、混合内容和第三方 Cookie。顶层仅允许数据站 HTTPS 原点，图片读取共享 ImageDiskCache，页面缓存遵循网站 Cache-Control。加载进度、HTTP/网络/SSL/30 秒超时及 rendererGone 有原生重试入口。销毁 Activity 会清理所有模块 WebView；软件内浏览模块仍隐藏浮层。

原站模拟 `/team-builder` 和手机资料库 `/h5/database` 作为在线模块提供，未复制或打包站点源码；原站更新自动生效。导出、分享等网站附属能力依赖系统 WebView 支持，离线功能不作承诺。`SiteModulesTest` 验证路由、原点隔离、200 次切换复用和失败创建；`navigation.test.cjs` 检查侧边栏桥接、推荐保留、设置入口与三种屏幕宽度。

## 0.1.15 简易与点击透传

`OverlayAppearance` 将空值/旧默认 88% 一次迁移为 50%，保留自定义及以后手动选择。简易卡片固定 64 dp 宽、约 86 dp 高，装备组整体居中；新版独立宽度偏好不沿用旧版拉宽值。

右侧锁定按钮经 Native 设置 `FLAG_NOT_TOUCHABLE`，独立解锁窗口放在面板外。先成功挂载解锁按钮，再使主窗口不接收触摸；挂载失败则取消锁定。Android 12+ 查询 `InputManager.getMaximumObscuringOpacityForTouch()`，将主窗口 alpha 限制在系统阈值以内，背景 alpha 反向补偿，解除锁定恢复正常。解锁窗口仅占 28 dp 方形，关闭/回主程序/显示失败会清理。

竖屏读取状态栏、导航条和刘海 Insets，启动、拖动、尺寸变化与旋转都限制垂直安全范围；横屏不引入左右边距。`OverlayTouchTest` 验证锁定状态、alpha 策略、默认迁移及安全位置，`lineup-strip.test.cjs` 检查默认 50%、简易命名、固定列宽、装备组居中、锁定桥接与触摸滑动。

## 0.1.14 触摸滑动修复

装备列允许横向手势交给整条容器，保持纵向装备列表自己的边界控制。补充移动端真实 touchStart/touchMove/touchEnd 回归，检查从英雄区和装备区开始均能横向滚动。

## 0.1.13 固定阵容长条

详情标题旁“长条”固定选中阵容的英雄和装备快照，切换成约 98 dp 高的横向卡片条；列表、筛选、指标等完全隐藏，宽度不足时横向滚动。每个英雄独立显示头像、名字和推荐装备，空装备显示暂无装备；超过三件的资料仍可在对应装备列内滑动。后台结果不会改变长条，回主程序或新局清除固定状态。

与完整面板使用同一个系统窗口，保留拖动/透明度。左侧恢复图标点击回完整面板，右下角只改变长条宽度并独立保存，不覆盖原面板宽高。原生状态与 WebView 通过 `stripMode`/`overlayMode` 同步；切换失败退回小图标，重新展开为完整面板。

`tests/lineup-strip.test.cjs` 验证非默认阵容及对应装备、四种宽度的完整访问、后台刷新锁定、原生恢复、返回键及新局清理；`OverlayModelTest` 覆盖长条恢复和前后台转换。

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
