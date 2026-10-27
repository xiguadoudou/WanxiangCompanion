# 万象弈伴 · GitHub 更新

Android 8.0+ 的个人万象棋阵容参考工具。当前版本 **0.1.9**，包名 `com.doudou.wxq`，版本号 10。

[查看 Android 源码](android/) · [下载当前源码包](https://github.com/xiguadoudou/WanxiangCompanion/releases/download/v0.1.9/WanxiangCompanion-0.1.9-source.zip)

[下载最新安装包](https://github.com/xiguadoudou/WanxiangCompanion/releases/latest) · [GitHub 备用直接下载](https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/WanxiangCompanion-0.1.9.apk)

## 下载页面（GitHub Pages）

页面源码位于 [docs/](docs/)，采用深色磨砂与薄荷绿按钮，适配手机及桌面。首次启用：

1. 打开 [仓库 Pages 设置](https://github.com/xiguadoudou/WanxiangCompanion/settings/pages)。
2. 在 **Build and deployment → Source** 选择 **Deploy from a branch**。
3. Branch 选择 **main**，目录选择 **/docs**，点击 **Save**。

GitHub 完成首次构建后，可访问 **https://xiguadoudou.github.io/WanxiangCompanion/**。

网页每次加载优先读取本仓库已发布的最新 Releases，失败时读取 `updates/latest.json`，显示版本号、更新内容、主下载与备用下载。资料加载失败或禁用 JavaScript 时，下载入口仍指向 Releases 最新版本的固定名 `WanxiangCompanion.apk`。0.1.9 起发布脚本同时上传固定名与版本名 APK，两者内容一致。网页没有账户、无统计脚本，也不保存用户选择。

本仓库连接不包含 Pages 管理权限，首次开关需要仓库所有者设置一次。后续不需要重复开关；发布新 Releases 后，页面读取新版本即可更新，不必逐次修改 HTML。

## 小窗口完整资料

0.1.8 起，小窗口详情区也包含样本说明和全队装备速览，向下滑动即可查看；窗口尺寸只改变排版，不再隐藏这些资料。首页和设置页末尾标注“软件由 ChatGPT 构建”。

每次进入主程序会默认选择**常小娥**（也称嫦小娥），清空禁用阵营和当前阵容并自动查询，保留透明度、窗口大小及统计设置；游戏内展开/收起悬浮窗不清空。

默认阵容最小样本为 **10 场**，可在设置中改为其他值并保存。

## 阵容数据同步与阵营图标

打开或返回软件时检查资料、选择默认棋手并清空禁用；也可切换棋手和禁用后查询当前推荐，网站新增的普通阵容组合无需重新安装 App，按棋手、禁用、样本量和排序自动参与查询。新增英雄资料和阵营列表也随网站资料读取；接口或玩法结构变化时可能需要客户端升级。

首页的河洛、逐鹿、日落海、三分之地、大河流域按钮使用数据网站对应的原始图标，内置五张图片，离线也可显示。阵容推荐需要数据源正常可用；失败时保留对应条件的缓存并显示错误。候选有分页/数量限制，详情以实际数据范围为准。

## 启动更新弹窗

0.1.6 起打开软件会自动检查 GitHub。有新版本时展示新旧版本号、更新内容和“立即下载”；支持“稍后再说”，同次启动不会反复提示相同版本。下载时显示进度，校验后可以在弹窗中安装。无更新或检查失败时不弹窗。

已是最新版本时不会显示更新弹窗；需要手动查看时使用设置中的更新入口。

## 在软件内更新

打开「设置 → 软件更新」检查、下载并确认安装。更新包与旧版保持同一签名，可覆盖安装。首次可能需要允许本应用安装未知应用。

0.1.5 起从本仓库读取版本资料，优先下载 Releases 的安装包；连接失败时改用仓库内的同版本 APK。每个地址均验证相同的文件大小和 SHA-256，安装前还会检查包名、版本和原签名。不需要在 App 内登录 GitHub。

版本资料：[updates/latest.json](updates/latest.json)。统计资料来源仍为 [万象棋大数据](https://www.datawxq.com/)，软件更新与阵容数据检查独立。

## 源码与发布维护

Android 源码位于 [android/](android/)，包括 Java 实现、主页面和悬浮窗界面、资源、构建脚本及回归测试。构建方法见 [android/README.md](android/README.md)。后续每次发布 APK，都同步提交对应源码，并在 Releases 提供可直接下载的版本源码 ZIP。

0.1.9 的源码是在已有 Release 上补充的，请使用明确命名的 `WanxiangCompanion-0.1.9-source.zip` 附件；该旧标签自动生成的 “Source code” ZIP 不包含此次新增的 Android 目录。以后在包含源码的提交上创建新版本标签。

1. 修改 `android/` 中的完整实现，更新 Manifest 和 `BuildInfo.java` 的版本号；在私有环境沿用原签名构建 APK。
2. 将正式 APK 放入 `updates/`，更新 `updates/latest.json` 的版本、地址、大小、SHA-256 和更新说明，与本版源码在同次提交中更新到 main。
3. GitHub Actions 校验 APK 摘要、源码版本和 APK 的全部 assets，生成只包含公开源码的 ZIP，并一同发布 APK、版本资料与源码附件。缺失或不匹配的源码会阻止发布。
4. 每版包含 `WanxiangCompanion-<版本>-source.zip`，并附带固定名 `WanxiangCompanion-source.zip`，可通过 Releases 最新版本下载。`SOURCE.json` 记录对应 APK 摘要和源码文件摘要。

已公开的 APK 和源码附件不自动覆盖；同版本重跑逐字节检查现有文件后跳过。0.1.9 首次补充源码时保留原有 APK、版本资料和更新说明，只增加源码附件。

签名私钥、密码、运行缓存、账号凭据不发布。公开构建默认生成自己的调试签名；只有维护者在私有环境提供原签名才能构建可覆盖官方版本的更新包。仓库不保存用户令牌；发布使用 GitHub 自动提供的短期凭据。
