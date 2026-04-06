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

/**
 * 应用程序的核心组件，记忆单词界面
 * @param appState 应用程序的全局状态
 * @param wordScreenState 记忆单词界面的状态容器
 * @param videoBounds 视频播放窗口的位置和大小
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WordScreen(
    window: ComposeWindow,
    title: String,
    appState: AppState,
    wordScreenState: WordScreenState,
    videoBounds: Rectangle,
    showPlayer :(Boolean) -> Unit,
    openVideo: (String, String) -> Unit,
    showContext :(MediaInfo) -> Unit,
    eventBus: EventBus
) {

    // 拖放处理函数
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // 处理拖放事件
                val transferable = event.awtTransferable
                // 获取拖放的文件列表，并过滤出 File 类型，避免类型转换警告和异常
                val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.filterIsInstance<File>() ?: emptyList()
                if(files.isNotEmpty()){
                    val file = files[0]
                    if(file.isDirectory){
                        JOptionPane.showMessageDialog(window, "不能拖放目录，请拖放文件。")
                        return false
                    }
                    if(file.extension == "json"){
                        // 如果是词库文件，打开词库
                        if (wordScreenState.vocabularyPath != file.absolutePath) {
                            val index = appState.findVocabularyIndex(file)
                            appState.changeVocabulary(file,wordScreenState,index)
                        } else {
                            JOptionPane.showMessageDialog(window, "词库已打开")
                        }
                    }else if(file.extension == "mkv" || file.extension == "mp4" ){
                        openVideo(file.absolutePath, wordScreenState.vocabularyPath)
                    }else{
                        JOptionPane.showMessageDialog(window, "只支持拖放词库文件或视频文件。")
                    }
                    return true
                }

                return false
            }
        }
    }
    Box(Modifier
        .background(MaterialTheme.colors.background)
        .dragAndDropTarget(
            shouldStartDragAndDrop =shouldStartDragAndDrop,
            target = dropTarget
        )
    ) { ->
        /** 单词输入框的焦点请求器*/
        val wordFocusRequester = remember { FocusRequester() }
        /** 当前正在记忆的单词 */
        val currentWord = if(wordScreenState.vocabulary.wordList.isNotEmpty()){
            wordScreenState.getCurrentWord()
        }else  null

        val  wordRequestFocus: () -> Unit = {
            if(currentWord != null){
                wordFocusRequester.requestFocus()
            }
        }
        var showFilePicker by remember {mutableStateOf(false)}
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        var documentWindowVisible by remember { mutableStateOf(false) }
        var generateVocabularyListVisible by remember { mutableStateOf(false) }
        val openChooseVocabulary:(String) ->Unit = { path ->
            openVocabularyFile(path, appState, wordScreenState)
        }

        Row {
            val dictationState = rememberDictationState()
            val azureTTS = rememberAzureTTS()
            WordScreenSidebar(
                appState = appState,
                wordScreenState = wordScreenState,
                dictationState = dictationState,
                wordRequestFocus = wordRequestFocus,
                azureTTS = azureTTS
                )

            Box(Modifier.fillMaxSize()) {
                if (currentWord != null) {
                    MainContent(
                        appState =appState,
                        wordScreenState = wordScreenState,
                        dictationState = dictationState,
                        azureTTS = azureTTS,
                        currentWord = currentWord,
                        videoBounds = videoBounds,
                        wordFocusRequester = wordFocusRequester,
                        window = window,
                        openVocabulary = { showFilePicker = true },
                        showContext = showContext,
                        eventBus = eventBus
                    )
                } else {
                    VocabularyEmpty(
                        openVocabulary = { showFilePicker = true },
                        openBuiltInVocabulary = {showBuiltInVocabulary = true},
                        generateVocabulary = {generateVocabularyListVisible = true},
                        openDocument = {documentWindowVisible = true},
                        parentWindow = window,
                        openChooseVocabulary = openChooseVocabulary
                    )
                }

                Header(
                    wordScreenState = wordScreenState,
                    title = title,
                    window = window,
                    wordRequestFocus = wordRequestFocus,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                )
            }
        }

        Row( modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = appState.openSidebar,
                setIsOpen = {
                    appState.openSidebar = it
                    if(!it && currentWord != null){
                        wordFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier,
                globalState = appState.global,
                saveGlobalState = {appState.saveGlobalState()},
                showPlayer = showPlayer,
                openSearch = appState.openSearch,
            )
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shortcutText = if (isMacOS()) "$ctrl O" else "$ctrl+O"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "打开词库文件  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText)
                            }
                        }
                    }
                },
                delayMillis = 50,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset.Zero
                )
            ) {

                IconButton(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.padding(top = if (isMacOS()) 44.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            RemoveButton(onClick = {
                wordScreenState.index = 0
                wordScreenState.vocabulary.size = 0
                wordScreenState.vocabulary.name = ""
                wordScreenState.vocabulary.relateVideoPath = ""
                wordScreenState.vocabulary.wordList.clear()
                wordScreenState.vocabularyName = ""
                wordScreenState.vocabularyPath = ""
                wordScreenState.saveWordScreenState()
            }, toolTip = "关闭当前词库")
            val extensions = if(isMacOS()) listOf("public.json") else listOf("json")

            FilePicker(
                show = showFilePicker,
                fileExtensions = extensions,
                initialDirectory = ""){pfile ->
                if(pfile != null){
                    if(pfile.path.isNotEmpty()){
                        openChooseVocabulary(pfile.path)
                    }
                }

                showFilePicker = false
            }
        }


        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            openChooseVocabulary = openChooseVocabulary,
        )

        GenerateVocabularyListDialog(
            appState = appState,
            show = generateVocabularyListVisible,
            close = {generateVocabularyListVisible = false}
        )

        var currentPage by remember { mutableStateOf("features") }
        if(documentWindowVisible){
            DocumentWindow(
                close = {documentWindowVisible = false},
                currentPage = currentPage,
                setCurrentPage = {currentPage = it}
            )
        }
    }

}


class CustomAlert(){
    var visible: Boolean by mutableStateOf(false)
    var message: String by mutableStateOf("")
    var isError: Boolean by mutableStateOf(false)
    var title:String by mutableStateOf("")

    fun clear(){
        visible = false
        message = ""
        isError = false
        title = ""
    }
}

