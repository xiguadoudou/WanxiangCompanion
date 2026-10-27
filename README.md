# 万象弈伴 · GitHub 更新

Android 8.0+ 的个人万象棋阵容参考工具。当前版本 **0.1.5**，包名 `com.doudou.wxq`，版本号 6。

[下载最新安装包](https://github.com/xiguadoudou/WanxiangCompanion/releases/latest) · [GitHub 备用直接下载](https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/WanxiangCompanion-0.1.5.apk)

## 在软件内更新

打开「设置 → 软件更新」检查、下载并确认安装。更新包与旧版保持同一签名，可覆盖安装。首次可能需要允许本应用安装未知应用。

0.1.5 起从本仓库读取版本资料，优先下载 Releases 的安装包；连接失败时改用仓库内的同版本 APK。每个地址均验证相同的文件大小和 SHA-256，安装前还会检查包名、版本和原签名。不需要在 App 内登录 GitHub。

版本资料：[updates/latest.json](updates/latest.json)。统计资料来源仍为 [万象棋大数据](https://www.datawxq.com/)，软件更新与阵容数据检查独立。

## 发布后续版本

1. 在私有环境沿用原签名构建更高 versionCode 的 APK。
2. 将 APK 放入 `updates/`，更新 `updates/latest.json` 中的版本、文件地址、大小、SHA-256 和更新说明，同次提交至 main。
3. GitHub Actions 校验文件后自动创建并发布对应 Releases。公开版本不会被自动覆盖；同版本重复执行会检查现有附件后跳过。

仓库只保存预先签名的 APK、版本资料和发布脚本。签名私钥在私有构建环境保管；发布任务使用 GitHub 自动提供的短期凭据。
