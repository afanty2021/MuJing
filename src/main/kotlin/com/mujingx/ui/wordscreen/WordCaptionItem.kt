/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mujingx.ui.wordscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.mujingx.data.Caption
import com.mujingx.data.ExternalCaption
import com.mujingx.data.Vocabulary
import com.mujingx.data.VocabularyType
import com.mujingx.data.Word
import com.mujingx.data.deepCopy
import com.mujingx.data.getFamiliarVocabularyFile
import com.mujingx.data.loadVocabulary
import com.mujingx.data.saveVocabulary
import com.mujingx.event.EventBus
import com.mujingx.event.WordScreenEventType
import com.mujingx.icons.AddNotes
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import com.mujingx.player.*
import com.mujingx.state.AppState
import com.mujingx.state.getResourcesFile
import com.mujingx.state.openVocabularyFile
import com.mujingx.theme.LocalCtrl
import com.mujingx.tts.AzureTTS
import com.mujingx.tts.rememberAzureTTS
import com.mujingx.ui.components.MacOSTitle
import com.mujingx.ui.components.RemoveButton
import com.mujingx.ui.components.Toolbar
import com.mujingx.ui.dialog.*
import com.mujingx.ui.wordscreen.MemoryStrategy.*
import com.mujingx.ui.util.computeVideoSize
import com.mujingx.ui.util.rememberMonospace
import com.mujingx.ui.util.shouldStartDragAndDrop
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.swing.JOptionPane
import kotlin.concurrent.schedule

/** 字幕列表组件
 * @param captionsVisible 字幕的可见性
 * @param playTripleMap 要显示的字幕。Map 的类型参数说明：
 * - Map 的 Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 * @param isPlaying 是否正在播放视频
 * @param volume 音量
 * @param setIsPlaying 设置是否正在播放视频播放的回调
 * @param word 单词
 * @param bounds 视频播放窗口的位置
 * @param textFieldValueList 用户输入的字幕列表
 * @param typingResultMap 用户输入字幕的结果 Map
 * @param putTypingResultMap 添加当前的字幕到结果Map
 * @param checkTyping 检查用户输入的回调
 * @param playKeySound 当用户输入字幕时播放敲击键盘音效的回调
 * @param modifier 修改器
 */
@ExperimentalComposeUiApi
@Composable
fun Captions(
    captionsVisible: Boolean,
    playTripleMap: Map<Int, Triple<Caption, String, Int>>,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    setPlayingMedia: (MediaInfo) -> Unit,
    plyingIndex: Int,
    setPlayingIndex: (Int) -> Unit,
    volume: Float,
    word: Word,
    bounds: Rectangle,
    textFieldValueList: List<String>,
    typingResultMap: Map<Int, MutableList<Pair<Char, Boolean>>>,
    putTypingResultMap: (Int, MutableList<Pair<Char, Boolean>>) -> Unit,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    modifier: Modifier,
    focusRequesterList:List<FocusRequester>,
    jumpToWord: () -> Unit,
    openSearch: () -> Unit,
    fontSize: TextUnit,
    isWriteSubtitles:Boolean,
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
                val scope = rememberCoroutineScope()
                playTripleMap.forEach { (index, playTriple) ->
                    var captionContent = playTriple.first.content
                    if(!isWriteSubtitles){
                        if (captionContent.endsWith("\r\n")) {
                            captionContent = captionContent.dropLast(2)
                        } else if (captionContent.endsWith("\n")) {
                            captionContent = captionContent.dropLast(1)
                        }
                    }else{
                        if (captionContent.contains("\r\n")) {
                            captionContent = captionContent.replace("\r\n", " ")
                        } else if (captionContent.contains("\n")) {
                            captionContent = captionContent.replace("\n", " ")
                        }
                    }
                    // 当前的字幕是否获得焦点
                    var focused by remember { mutableStateOf(false) }
                    var textFieldValue = textFieldValueList[index]
                    if(!isWriteSubtitles){
                        textFieldValue = captionContent
                    }
                    var typingResult = typingResultMap[index]
                    if (typingResult == null) {
                        typingResult = mutableListOf()
                        putTypingResultMap(index, typingResult)
                    }
                    var isPlayFailed by remember { mutableStateOf(false) }
                    var failedMessage by remember { mutableStateOf("") }
                    val playCurrentCaption:()-> Unit = {
                        if (!isPlaying) {
                            setPlayingIndex(index+1)
                            setIsPlaying(true)
                            val playMedia = MediaInfo(
                                playTriple.first,
                                playTriple.second,
                                playTriple.third,
                            )
                            setPlayingMedia(playMedia)
                        }
                    }
                    var selectable by remember { mutableStateOf(false) }
                    val focusMoveUp:() -> Unit = {
                        if(index == 0){
                            jumpToWord()
                        }else{
                            focusRequesterList[index-1].requestFocus()
                        }
                    }
                    val focusMoveDown:() -> Unit = {
                        if(index<2 && index + 1 < playTripleMap.size){
                            focusRequesterList[index+1].requestFocus()
                        }
                    }
                    val captionKeyEvent:(KeyEvent) -> Boolean = {
                        val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                        when {
                            (it.type == KeyEventType.KeyDown
                                    && it.key != Key.ShiftRight
                                    && it.key != Key.ShiftLeft
                                    && it.key != Key.CtrlRight
                                    && it.key != Key.CtrlLeft
                                    ) -> {
                                scope.launch { playKeySound() }
                                true
                            }
                            (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                scope.launch { selectable = !selectable }
                                true
                            }
                            (it.key == Key.Tab && it.type == KeyEventType.KeyUp) -> {
                                scope.launch {  playCurrentCaption() }
                                true
                            }
                            (it.key == Key.DirectionDown && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            (it.key == Key.DirectionUp && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            else -> false
                        }
                    }

                    Caption(
                        isPlaying = isPlaying,
                        isWriteSubtitles = isWriteSubtitles,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        index = index,
                        playingIndex = plyingIndex,
                        focusRequester = focusRequesterList[index],
                        focused = focused,
                        focusChanged = { focused = it },
                        playCurrentCaption = {playCurrentCaption()},
                        captionKeyEvent = {captionKeyEvent(it)},
                        selectable = selectable,
                        setSelectable = {selectable = it},
                        resetPlayState = {isPlayFailed = false },
                        isPlayFailed = isPlayFailed,
                        failedMessage = failedMessage,
                        openSearch = {openSearch()},
                        fontSize = fontSize
                    )
                }

            }
        }
        if (!isPlaying && (word.captions.isNotEmpty() || word.externalCaptions.isNotEmpty()))
            Divider(Modifier.padding(start = 50.dp))
    }
}


/**
 * 字幕组件
 * @param isPlaying 是否正在播放
 * @param isWriteSubtitles 是否抄写字幕
 * @param captionContent 字幕的内容
 * @param textFieldValue 输入的字幕
 * @param typingResult 输入字幕的结果
 * @param checkTyping 输入字幕后被调用的回调
 * @param index 当前字幕的索引
 * @param playingIndex 正在播放的字幕索引
 * @param focusRequester 焦点请求器
 * @param focused 是否获得焦点
 * @param focusChanged 处理焦点变化的函数
 * @param playCurrentCaption 播放当前字幕的函数
 * @param captionKeyEvent 处理当前字幕的快捷键函数
 * @param selectable 是否可选择复制
 * @param setSelectable 设置是否可选择
 * @param isPlayFailed 是否路径错误
 */
@OptIn(
    ExperimentalFoundationApi::class,
)
@Composable
fun Caption(
    isPlaying: Boolean,
    isWriteSubtitles: Boolean,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    index: Int,
    playingIndex: Int,
    focusRequester:FocusRequester,
    focused: Boolean,
    focusChanged:(Boolean) -> Unit,
    playCurrentCaption:()-> Unit,
    captionKeyEvent:(KeyEvent) -> Boolean,
    selectable:Boolean,
    setSelectable:(Boolean) -> Unit,
    isPlayFailed:Boolean,
    resetPlayState:() -> Unit,
    failedMessage:String,
    openSearch: () -> Unit,
    fontSize: TextUnit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        // 字幕的行数
        val row = if(isWriteSubtitles) 1 else captionContent.split("\n").size
        val rowHeight = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                24.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                20.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            else -> 16.dp * 2 * row + 4.dp
        }
        val background = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary.copy(alpha = 0.05f) else MaterialTheme.colors.background
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(rowHeight).width(IntrinsicSize.Max).background(background)
        ) {
            val dropMenuFocusRequester = remember { FocusRequester() }
            Box(Modifier.width(IntrinsicSize.Max)) {
                val textHeight = rowHeight -4.dp
                CustomTextMenuProvider {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            checkTyping(index, input, captionContent)
                        },
                        singleLine = isWriteSubtitles,
                        readOnly = !isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = LocalTextStyle.current.copy(
                            color = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary else  MaterialTheme.colors.onBackground,
                            fontSize = fontSize
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(textHeight)
                            .align(Alignment.CenterStart)
                            .focusRequester(focusRequester)
                            .onFocusChanged {focusChanged(it.isFocused)}
                            .onKeyEvent { captionKeyEvent(it) }
                    )
                }

                if(isWriteSubtitles){
                    Text(
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).height(textHeight),
                        overflow = TextOverflow.Ellipsis,
                        text = buildAnnotatedString(captionContent, typingResult, fontSize)
                    )
                }


                DropdownMenu(
                    expanded = selectable,
                    onDismissRequest = { setSelectable(false) },
                    offset = DpOffset(0.dp, (if(isWriteSubtitles)-30 else -70).dp)
                ) {
                    // 增加一个检查，检查字幕的字符长度，有的字幕是机器生成的，一段可能会有很多字幕，
                    // 可能会超出限制，导致程序崩溃。
                    val content = if(captionContent.length>400){
                       captionContent.substring(0,400)
                    }else captionContent

                    BasicTextField(
                        value = content,
                        onValueChange = {},
                        singleLine = isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle =  LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                            fontSize = fontSize,
                        ),
                        modifier = Modifier.focusable()
                            .focusRequester(dropMenuFocusRequester)
                            .onKeyEvent {
                                val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                                if (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                    scope.launch { setSelectable(!selectable) }
                                    true
                                }else if (isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                    scope.launch { openSearch() }
                                    true
                                } else false
                            }
                    )
                    LaunchedEffect(Unit) {
                        dropMenuFocusRequester.requestFocus()
                    }

                }
            }

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val plus = if (isMacOS()) "" else "+"
                        val shortcutText: Any = when (index) {
                            0 -> "$ctrl$plus 1"
                            1 -> "$ctrl$plus 2"
                            2 -> "$ctrl$plus 3"
                            else -> println("字幕数量超出范围")
                        }
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "播放  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText.toString())
                            }
                        }
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
                    playCurrentCaption()
                },
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    val tint = if(isPlaying && playingIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = tint
                    )
                }
            }
            if (isPlayFailed) {
                Text(failedMessage, color = Color.Red)
                Timer("恢复状态", false).schedule(2000) {
                    resetPlayState()
                }
            }
        }
    }


}


@Composable
fun buildAnnotatedString(
    captionContent:String,
    typingResult:List<Pair<Char, Boolean>>,
    fontSize: TextUnit,
):AnnotatedString{
    return buildAnnotatedString {
        typingResult.forEach { (char, correct) ->
            if (correct) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colors.primary,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
                    )
                ) {
                    append(char)
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
                    )
                ) {
                    if (char == ' ') {
                        append("_")
                    } else {
                        append(char)
                    }

                }
            }
        }

        if (!(typingResult.isNotEmpty() && captionContent.length < typingResult.size)) {
            var remainChars = captionContent.substring(typingResult.size)
            // 增加一个检查，检查字幕的字符长度，有的字幕是机器生成的，一段可能会有很多字幕，
            // 可能会超出限制，导致程序崩溃。
            if (remainChars.length > 400) {
                remainChars = remainChars.substring(0, 400)
            }

            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = fontSize,
                    letterSpacing = LocalTextStyle.current.letterSpacing,
                    fontFamily = LocalTextStyle.current.fontFamily,
                )
            ) {
                append(remainChars)
            }
        }

    }
}
