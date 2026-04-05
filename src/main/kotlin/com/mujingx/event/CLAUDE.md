# Event 模块 - 事件总线

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **event**

## 模块职责

Event 模块负责幕境应用的事件系统：
- **事件总线**：全局事件发布和订阅
- **快捷键处理**：窗口级快捷键事件
- **组件通信**：跨组件事件传递

## 入口与启动

### 事件总线
```kotlin
// EventBus.kt - 事件总线
object EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun publish(event: Event)
    fun <T : Event> subscribe(): Flow<T>
}

// WindowKeyEvent.kt - 快捷键事件
data class WindowKeyEvent(
    val key: Key,
    val modifiers: KeyboardModifiers = KeyboardModifiers(0)
)
```

## 对外接口

### 事件发布
```kotlin
// 发布事件
EventBus.publish(VocabularyChangedEvent(vocabulary))

// 订阅事件
LaunchedEffect(Unit) {
    EventBus.events<VocabularyChangedEvent>().collect { event ->
        // 处理事件
    }
}
```

## 相关文件清单

### 核心文件
- `EventBus.kt` - 事件总线
- `WindowKeyEvent.kt` - 快捷键事件

## 变更记录 (Changelog)

### 2026-04-05 - 创建 event 模块文档
- 📚 初始化模块文档
- 🗂️ 记录事件系统接口

---

**依赖关系**：
- 被 `ui` 模块依赖（所有界面）
- 被 `player` 模块依赖（播放器控制）
