# 收尾合规检查清单（详细版）

> 源规范：`docs/4-规范与指南/通用开发规范.md` 0.3 节 + 6.0 节
> 源规范：`docs/4-规范与指南/MyPurse开发规范.md` 13.2.3 节

## MyPurse 文档路径速查

| 用途 | 文件路径 |
|------|---------|
| 开发日志 | `docs/3-开发过程/开发日志.md` |
| 开发日志（精简版） | `docs/3-开发过程/开发日志-精简版.md` |
| 问题记录 | `docs/3-开发过程/问题记录.md` |
| 问题记录（精简版） | `docs/3-开发过程/问题记录-精简版.md` |
| 进度追踪 | `docs/3-开发过程/开发阶段.md` |
| 需求规格（V1.0 封版） | `docs/2-需求规格/requirements.md` |
| 需求规格（V1.x 迭代） | `docs/2-需求规格/requirements-v1x.md` |
| 项目介绍 | `docs/1-项目概览/项目介绍.md` |
| CHANGELOG | `CHANGELOG.md` |

## 9 条收尾检查

### ☐ 1. 编译验证
```bash
./gradlew assembleDebug
```
编译失败 → 修代码，不继续。

### ☐ 2. 更新开发日志
- [ ] 在 `开发日志.md` 末尾追加本次会话记录
- [ ] 同步到 `开发日志-精简版.md`
- 模板见 `log-template.md`

### ☐ 3. 更新问题记录（如有 Bug）
- [ ] 在 `问题记录.md` 末尾追加
- [ ] 同步到 `问题记录-精简版.md`
- 模板见 `issue-template.md`

### ☐ 4. 更新进度追踪
- [ ] 打开 `docs/3-开发过程/开发阶段.md`
- [ ] 涉及的任务项状态列打 ✅

### ☐ 5. 更新需求文档（如有变更）
- [ ] 正文替换
- [ ] 第 10 章变更记录追加

### ☐ 6. 清理临时文件
检查并删除：`build_output.txt`、`compile_errors.txt`、`compile_result.txt` 等。

### ☐ 7. Git 提交
```bash
git add -A
git commit -m "type(scope): 描述"
git push
```
- type: feat/fix/refactor/docs/style/test/chore/perf
- scope 可选，如 `(ui)`, `(data)`

### ☐ 8. 告知 commit hash
回复中说明：commit hash + 简要内容

### ☐ 9. 告知文档改动
列出本次修改了哪些文档（无改动也需说明"本次无文档改动"）

## 常见遗漏场景

| 遗漏项 | 高频场景 |
|--------|---------|
| 第 4 条（打勾） | 代码写完了但 `开发阶段.md` 表里没打 ✅ |
| 第 3 条（问题记录） | 修了 Bug 但没记录 |
| 第 6 条（临时文件） | 编译排查后忘删 build_output.txt |
| 第 8 条（告知 hash） | commit 了但没告诉用户 |
