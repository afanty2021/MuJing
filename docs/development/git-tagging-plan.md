# Git 标签方案

> 创建日期：2026-04-05
> 当前版本：v2.12.3

---

## 标签策略

项目使用语义化版本（Semantic Versioning）：`v主版本.次版本.修订版本`

### 版本规则

- **主版本（Major）**：不兼容的 API 变更
- **次版本（Minor）**：向后兼容的功能新增
- **修订版本（Patch）**：向后兼容的 Bug 修复

---

## 需要添加的标签

根据 git log，以下版本需要添加标签：

| 版本 | 提交 SHA | 说明 | 优先级 |
|------|---------|------|--------|
| **v2.12.3** | 9ca8c14 | 当前版本 | P0 |
| **v2.12.2** | a0c819b | 上一版本 | P1 |
| **v2.12.1** | c2197fd | 历史版本 | P2 |
| **v2.12.0** | 017b822 | 历史版本 | P2 |

---

## 添加标签的命令

### 本地添加标签

```bash
# 为当前版本添加标签
git tag -a v2.12.3 -m "Release v2.12.3

- 添加 Detekt 代码质量检查
- 添加 CHANGELOG.md
- 添加多个模块的单元测试
- 优化构建配置
"

# 为历史版本添加标签
git tag -a v2.12.2 a0c819b -m "Release v2.12.2"
git tag -a v2.12.1 c2197fd -m "Release v2.12.1"
git tag -a v2.12.0 017b822 -m "Release v2.12.0"
```

### 推送标签到远程

```bash
# 推送所有标签
git push origin --tags

# 推送单个标签
git push origin v2.12.3
```

### 查看标签

```bash
# 列出所有标签
git tag

# 查看标签信息
git show v2.12.3

# 查看标签对应的提交
git log v2.12.3 --oneline -1
```

---

## 标签命名规范

### 格式

```
v<主版本>.<次版本>.<修订版本>[-<预发布标识>]
```

### 示例

| 标签 | 说明 |
|------|------|
| `v2.12.3` | 正式发布版本 |
| `v2.13.0-rc.1` | 候选版本 1 |
| `v2.13.0-beta.1` | 测试版本 1 |
| `v2.13.0-alpha` | 内部测试版本 |

---

## 自动化标签创建

### 使用 GitHub Actions

```yaml
# .github/workflows/release.yml
name: Create Release Tag

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          body: |
            ## 更新内容
            查看 CHANGELOG.md 获取详细更新内容。
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 标签管理

### 删除标签

```bash
# 删除本地标签
git tag -d v2.12.3

# 删除远程标签
git push origin --delete v2.12.3
```

### 修改标签

```bash
# 删除旧标签
git tag -d v2.12.3
git push origin --delete v2.12.3

# 创建新标签
git tag -a v2.12.3 -m "Release v2.12.3"
git push origin v2.12.3
```

---

## 最佳实践

1. **每次发布创建标签**
   - 发布新版本时立即创建标签
   - 标签信息包含主要更新内容

2. **使用带注释的标签**
   - 使用 `-a` 参数创建带注释的标签
   - 注释说明版本的主要变更

3. **标签与 CHANGELOG 同步**
   - 标签创建后更新 CHANGELOG.md
   - CHANGELOG 中引用对应的标签

4. **保护已发布的标签**
   - 已发布的标签不应修改或删除
   - 如需修正，创建新的修订版本

---

## 检查清单

创建标签前确认：

- [ ] build.gradle.kts 中的版本号已更新
- [ ] CHANGELOG.md 已更新
- [ ] 所有测试通过
- [ ] 发布说明已准备
- [ ] 标签格式正确

---

*本方案由 Claude Code 创建*
