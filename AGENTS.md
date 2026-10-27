# Android release maintenance

- Keep the complete Android implementation in `android/` in sync with every APK release. Commit its Java sources, packaged assets/resources, manifest, and tests alongside the matching `updates/latest.json` and pre-signed APK.
- Publish through `.github/workflows/publish-update.yml`. Every release includes the versioned APK and corresponding `WanxiangCompanion-<version>-source.zip`; the workflow validates matching versions and packaged assets.
- Never upload a private workspace archive containing signing keys. The public source ZIP is generated only from the allowlisted `android/` source tree. Keep original signing keys/passwords outside Git and use environment-based signing for local release builds.
- Published APKs and source attachments are immutable. Increment the app version for subsequent implementation changes; do not replace an existing version with different bytes. The one-time 0.1.9 source backfill leaves its APKs intact.
- GitHub Pages uses `main:/docs`. Retain its download links and current public update feed when maintaining the Android source.

