# MyPurse — 极简个人记账

一款**极致轻量、零广告、纯本地**的 Android 个人记账应用。

## 技术栈

| 维度 | 详情 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture（Data/Domain/UI） |
| DI | Hilt |
| 数据库 | Room（SQLite） |
| 最低版本 | Android 7.0 (API 24) |

## 快速开始

```bash
# 调试安装
./gradlew installDebug

# 构建 Release APK（需要 mypurse-release.jks 密钥库）
./gradlew assembleRelease
```

**注意**：`mypurse-release.jks` 在项目根目录，受 `.gitignore` 保护不会提交到 Git。此文件丢失后无法对已发布的包更新签名，请妥善备份。

## 文档

完整项目文档在 `docs/` 目录：[文档索引](docs/README.md)

接手开发前建议按以下顺序阅读：
1. `docs/1-项目概览/项目介绍.md` — 架构、技术栈、安全修改指南
2. `docs/3-开发过程/开发日志-精简版.md` — 开发历程速查
3. `docs/3-开发过程/问题记录-精简版.md` — 已知坑点速查
4. `docs/3-开发过程/开发阶段.md` — 当前进度
5. `docs/4-规范与指南/MyPurse开发规范.md` — 编码规范

## 更新日志

见 [CHANGELOG.md](CHANGELOG.md)

## 许可证

个人项目，保留所有权利。
