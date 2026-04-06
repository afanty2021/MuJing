# Git 提交信息规范

> 基于 Conventional Commits 规范
> 创建日期：2026-04-05

---

## 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 示例

```
feat(player): 添加弹幕时间轴同步功能

实现弹幕与视频字幕的时间轴精确同步，
支持多轨道弹幕显示和交互。

Closes #123
```

---

## 类型（Type）

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | feat(auth): 添加 OAuth 登录 |
| `fix` | Bug 修复 | fix(player): 修复视频暂停时崩溃 |
| `docs` | 文档更新 | docs(readme): 更新安装说明 |
| `style` | 代码格式 | style(ui): 统一代码缩进 |
| `refactor` | 重构 | refactor(data): 优化词库加载逻辑 |
| `perf` | 性能优化 | perf(scrolling): 优化列表滚动性能 |
| `test` | 测试相关 | test(fsrs): 添加 FSRS 算法测试 |
| `chore` | 构建/工具 | chore(deps): 升级 Kotlin 版本 |
| `ci` | CI/CD | ci(github): 添加自动测试 |
| `revert` | 回滚 | revert: 回滚 commit abc123 |

---

## 范围（Scope）

范围标识提交影响的模块，使用以下值：

- **player** - 视频播放器
- **fsrs** - 间隔重复算法
- **data** - 数据模型和词典
- **ui** - 用户界面
- **tts** - 文字转语音
- **ffmpeg** - 视频处理
- **lyric** - 歌词/字幕处理
- **state** - 应用状态
- **event** - 事件总线
- **theme** - 主题配置
- **build** - 构建配置
- **docs** - 文档

---

## 主题（Subject）

使用中文描述，遵循以下规则：

- 使用动词开头（添加、修复、更新等）
- 不使用句号结尾
- 长度不超过 72 字符
- 首字母小写

**正确示例**：
- ✅ `添加弹幕时间轴同步功能`
- ✅ `修复视频暂停时崩溃问题`
- ✅ `优化词库加载性能`

**错误示例**：
- ❌ `Add danmaku sync` (应使用中文)
- ❌ `添加弹幕功能。` (不应有句号)
- ❌ `实现了用户登录注册和权限管理以及邮件验证功能` (太长)

---

## 正文（Body）

详细描述提交的内容，遵循以下规则：

- 每行不超过 100 字符
- 说明 **what** 和 **why**，不是 **how**
- 可以包含多个段落

**示例**：

```
实现弹幕与视频字幕的时间轴精确同步，
支持多轨道弹幕显示和交互。

问题：
- 之前弹幕时间与字幕不同步
- 多个弹幕轨道时显示混乱

解决方案：
- 使用统一的时钟源
- 添加弹幕轨道管理器
```

---

## 页脚（Footer）

用于关联 Issue 或 breaking changes。

**关联 Issue**：
```
Closes #123
Fixes #456
Refs #789
```

**Breaking Changes**：
```
BREAKING CHANGE: 词库文件格式已更新，旧版本无法读取
```

---

## 完整示例

### 新功能

```
feat(fsrs): 添加间隔重复调度算法

基于 FSRS 算法实现智能复习调度，
根据用户的记忆表现动态调整复习间隔。

功能：
- 计算下次复习时间
- 支持难度评级
- 记忆曲线可视化

Closes #42
```

### Bug 修复

```
fix(player): 修复 VLC 播放器内存泄漏

问题：
VLC 媒体播放器在关闭时没有正确释放资源，
导致多次打开后内存占用持续增长。

解决方案：
- 在 DisposableEffect 中添加清理逻辑
- 确保 MediaPlayer 正确释放
```

### 文档更新

```
docs(readme): 更新 macOS 安装说明

添加 macOS 14+ 系统的安装步骤，
修复 Homebrew 路径变化问题。
```

### 重构

```
refactor(data): 优化词库加载性能

将词库加载从同步改为异步，
使用 Flow 实现渐进式加载。

性能提升：
- 首屏加载时间减少 60%
- 内存占用降低 40%
```

---

## 提交前检查清单

- [ ] 类型是否正确
- [ ] 范围是否明确
- [ ] 主题是否简洁清晰
- [ ] 是否包含必要的正文说明
- [ ] 是否关联相关 Issue
- [ ] 是否通过本地测试

---

## 工具支持

### Commitlint

项目已配置 Commitlint，提交时会自动检查格式：

```bash
# 安装 commitlint CLI
npm install -g @commitlint/cli @commitlint/config-conventional

# 手动检查提交信息
echo "feat: add feature" | commitlint
```

### Git Alias

添加便捷的 Git 别名：

```bash
# 添加到 ~/.gitconfig
[alias]
    cm = commit -m
    ca = commit -a
    amend = commit --amend --no-edit
```

---

*本规范由 Claude Code 基于 Conventional Commits 创建*
