# 更新日志

本文档记录幕境 (MuJing) 项目的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增
- HF_ENDPOINT 环境变量支持，可使用 HuggingFace 国内镜像下载 Whisper 模型

### 修复
- 跳过 Homebrew JDK 供应商检查，修复 macOS 上打包失败问题
- 统一 kotlinx-serialization 版本为 1.7.3，消除与 filekit 0.12.0 的版本冲突
- 添加 FSRS schemaVersion 安全校验，防止潜在的 SQL 注入风险

### 测试
- 新增 70 个单元测试（state、event、lyric、tts 模块）
- 修复 testLast 测试套件失败问题

## [2.12.3] - 2025-04-XX

### 新增
- AI 上下文文档系统（CLAUDE.md）
- 模块导航面包屑和 Mermaid 结构图

### 变更
- 优化 Build Package.yml 中的 macOS 支持和 artifacts 字段

## [2.12.2] - 2025-XX-XX

### 变更
- 优化 Build Package.yml 中的版本号处理

## [2.12.1] - 2025-XX-XX

### 变更
- 优化 Build Package.yml 中的 artifacts 字段格式

## [2.12.0] - 2025-XX-XX

### 新增
- 自定义文本菜单提供者以增强字幕翻译功能
- 词典查询增加复制和网络查词按钮，支持 macOS 系统词典

### 优化
- 字幕索引切换：点击单词即可切换当前字幕索引
- 字幕行渲染逻辑：支持根据可见性动态激活悬停功能
- 通知显示位置与字幕浏览器位置一致

### 修复
- 菜单闪烁问题

### 依赖升级
- Kotlin 至 2.2.20
- Compose 至 1.9.3
- FileKit 至 0.12.0

## [2.11.x] 及更早版本

请查看 Git 提交历史获取详细信息。

---

## 版本说明

- **[Unreleased]**: 即将发布版本的变更
- **[X.Y.Z]**: 已发布版本
  - **X**: 主版本号（不兼容的 API 变更）
  - **Y**: 次版本号（向下兼容的功能新增）
  - **Z**: 修订号（向下兼容的问题修复）

## 变更类型

- **新增**: 新功能
- **变更**: 现有功能的变更
- **弃用**: 即将移除的功能
- **移除**: 已移除的功能
- **修复**: Bug 修复
- **安全**: 安全性相关的修复
