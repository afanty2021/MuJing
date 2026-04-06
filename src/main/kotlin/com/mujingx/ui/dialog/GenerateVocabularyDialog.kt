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

package com.mujingx.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.mujingx.data.*
import com.mujingx.data.VocabularyType.*
import com.mujingx.player.isWindows
import com.mujingx.player.parseTrackList
import com.mujingx.state.AppState
import com.mujingx.state.getResourcesFile
import com.mujingx.ui.components.BuiltInVocabularyMenu
import com.mujingx.ui.components.SaveButton
import com.mujingx.ui.dialog.FilterState.*
import com.mujingx.ui.edit.SaveOtherVocabulary
import com.mujingx.ui.util.*
import com.mujingx.ui.window.windowBackgroundFlashingOnCloseFixHack
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.math.max
import kotlin.math.min

/**
 * 生成词库
 * @param state 应用程序状态
 * @param title 标题
 * @param type 词库类型
 */
@OptIn(
    ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun GenerateVocabularyDialog(
    state: AppState,
    title: String,
    type: VocabularyType
) {
    val windowWidth = if (type == MKV) 1320.dp else 1285.dp
    DialogWindow(
        title = title,
        onCloseRequest = {
            onCloseRequest(state, title)
        },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(windowWidth, 850.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        val scope = rememberCoroutineScope()

        var started by remember { mutableStateOf(false) }

        /**
         * 选择的文件列表,用于批量处理
         */
        val selectedFileList = remember { mutableStateListOf<File>() }

        /**
         * 显示任务列表,用于拖拽多个文件进入窗口时，自动为真，
         * 分析完字幕，自动为假，单击【任务列表】显示-隐藏。
         */
        var showTaskList by remember { mutableStateOf(false) }

        /**
         * 任务列表的状态
         */
        val tasksState = remember { mutableStateMapOf<File, Boolean>() }

        /**
         * 正在处理的文件
         */
        var currentTask by remember { mutableStateOf<File?>(null) }

        /**
         * 批处理时的错误信息
         */
        val errorMessages = remember { mutableStateMapOf<File, String>() }

        /**
         * 批量处理时，选择文件，用于删除
         */
        var selectable by remember { mutableStateOf(false) }

        /**
         * 勾选的文件，用于批量删除
         */
        val checkedFileMap = remember { mutableStateMapOf<File, Boolean>() }

        /**
         * 是否全选
         */
        var isSelectedAll by remember { mutableStateOf(false) }

        /**
         * 选择的文件的绝对路径
         */
        var selectedFilePath by remember { mutableStateOf("") }

        /**
         * 选择的字幕名称
         */
        var selectedSubtitlesName by remember { mutableStateOf("    ") }

        /**
         * 预览的单词
         */
        val previewList = remember { mutableStateListOf<Word>() }


        val parsedList = remember { mutableStateListOf<Word>() }

        /**
         * 用字幕生成单词 -> 相关视频的地址
         */
        var relateVideoPath by remember { mutableStateOf("") }

        /**
         * 字幕的轨道 ID
         */
        var selectedTrackId by remember { mutableStateOf(0) }

        /**
         * 需要过滤的词库的类型
         */
        var filteringType by remember { mutableStateOf(DOCUMENT) }

        /**
         * 字幕轨道列表
         */
        val trackList = remember { mutableStateListOf<Pair<Int, String>>() }

        /**
         * 这个 filterState 有四个状态：Idle、Parse、Filtering、End
         */
        var filterState by remember { mutableStateOf(Idle) }

        /**
         * 摘要词库
         */
        val summaryVocabulary = loadSummaryVocabulary()

        /**
         * 用于过滤的词库列表
         */
        val vocabularyFilterList = remember { mutableStateListOf<File>() }

        /**
         * 是否过滤词组
         */
        var enablePhrases by remember { mutableStateOf(false) }

        /**
         * 过滤单词
         */
        var filter by remember { mutableStateOf(true) }

        /**
         * 包含单词
         */
        var include by remember { mutableStateOf(false) }

        /**
         * 是否过滤所有的数字
         */
        var numberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频前 1000 的单词，最常见的 1000 词
         */
        var bncNumberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频前 1000 的单词，最常见的 1000 词
         */
        var frqNumFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频为0的单词
         */
        var bncZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频为0的单词
         */
        var frqZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否替换索引派生词
         */
        var replaceToLemma by remember { mutableStateOf(false) }

        /** 熟悉词库 */
        val familiarVocabulary = remember { loadMutableVocabularyByName("FamiliarVocabulary") }

        /** 用鼠标删除的单词列表 */
        val removedWords = remember { mutableStateListOf<Word>() }

        var progressText by remember { mutableStateOf("") }

        var loading by remember { mutableStateOf(false) }

        var sort by remember { mutableStateOf("appearance") }

        var showCard by remember { mutableStateOf(true) }

        /** 文件选择器的标题 */
        val chooseText = when (title) {
            "过滤词库" -> "选择词库"
            "用文档生成词库" -> "选择文档"
            "用字幕生成词库" -> "选择字幕"
            "用 MKV 视频生成词库" -> "选择 MKV 文件"
            else -> ""
        }

        /** 拖放的文件和文件选择器选择的文件都使用这个函数处理 */
        val parseImportFile: (List<File>) -> Unit = { files ->
            scope.launch(Dispatchers.Default) {
                if (files.size == 1) {
                    val file = files.first()
                    when (file.extension) {
                        "pdf", "txt", "md", "java", "cs", "cpp", "c", "kt", "js", "py", "ts" -> {
                            if (type == DOCUMENT) {
                                selectedFilePath = file.absolutePath
                                selectedSubtitlesName = "    "
                            } else {
                                JOptionPane.showMessageDialog(
                                    window,
                                    "如果你想用 ${file.nameWithoutExtension} 文档生成词库，\n请重新选择：词库 -> 用文档生成词库，再拖放文件到这里。"
                                )
                            }
                        }

                        "srt", "ass" -> {
                            if (type == SUBTITLES) {
                                selectedFilePath = file.absolutePath
                                selectedSubtitlesName = "    "
                            } else {
                                JOptionPane.showMessageDialog(
                                    window,
                                    "如果你想用 ${file.nameWithoutExtension} 字幕生成词库，\n请重新选择：词库 -> 用字幕生成词库，再拖放文件到这里。"
                                )
                            }
                        }

                        "mkv", "mp4" -> {
                            when (type) {
                                MKV -> {
                                    // 第一次拖放
                                    if (selectedFilePath.isEmpty() && selectedFileList.isEmpty()) {
                                        loading = true
                                        parseTrackList(
                                            window,
                                            state.videoPlayerWindow,
                                            file.absolutePath,
                                            setTrackList = {
                                                trackList.clear()
                                                trackList.addAll(it)
                                                if (it.isNotEmpty()) {
                                                    selectedFilePath = file.absolutePath
                                                    relateVideoPath = file.absolutePath
                                                    selectedSubtitlesName = "    "
                                                }else{
                                                    JOptionPane.showMessageDialog(
                                                        window,
                                                        "这个视频没有字幕轨道，无法生成词库"
                                                    )
                                                }
                                            }
                                        )

                                        loading = false
                                    } else { // 窗口已经有文件了
                                        // 已经有一个相同的 MKV 视频，不再添加
                                        if (file.absolutePath == selectedFilePath) {
                                            return@launch
                                        }
                                        // 批量生成词库暂时不支持 MP4 格式
                                        if (file.extension == "mp4") {
                                            JOptionPane.showMessageDialog(
                                                window,
                                                "批量生成词库暂时不支持 MP4 格式"
                                            )
                                            return@launch
                                        }
                                        // 如果之前有一个 MKV 视频,把之前的视频加入到 selectedFileList
                                        if (selectedFilePath.isNotEmpty() && selectedFileList.isEmpty()) {
                                            val f = File(selectedFilePath)
                                            if (f.extension == "mp4") {
                                                JOptionPane.showMessageDialog(
                                                    window,
                                                    "即将进入批量生成词库模式\n" +
                                                            "批量生成词库暂时不支持 MP4 格式\n" +
                                                            "${f.nameWithoutExtension} 不会被添加到列表"
                                                )

                                            } else {
                                                selectedFileList.add(f)
                                            }
                                            trackList.clear()
                                            selectedSubtitlesName = "    "
                                            selectedFilePath = ""
                                            relateVideoPath = ""
                                        }
                                        // 列表里面没有这个文件，就添加
                                        if (!selectedFileList.contains(file)) {
                                            selectedFileList.add(file)
                                            selectedFileList.sortBy { it.nameWithoutExtension }
                                            if (selectedFileList.isNotEmpty()) showTaskList = true
                                        }


                                    }

                                }

                                SUBTITLES -> {
                                    relateVideoPath = file.absolutePath
                                }

                                else -> {
                                    JOptionPane.showMessageDialog(
                                        window,
                                        "如果你想用 ${file.nameWithoutExtension} 视频生成词库，\n请重新选择：词库 -> 用 MKV 视频生成词库，再拖放文件到这里。"
                                    )
                                }
                            }
                        }

                        "json" -> {
                            if (title == "过滤词库") {
                                selectedFilePath = file.absolutePath
                            }
                        }

                        else -> {
                            JOptionPane.showMessageDialog(window, "格式不支持")
                        }
                    }


                } else if (files.size == 2 && type == SUBTITLES) {
                    val first = files.first()
                    val last = files.last()
                    if (first.extension == "srt" && (last.extension == "mp4" || last.extension == "mkv")) {
                        selectedFilePath = first.absolutePath
                        selectedSubtitlesName = "    "
                        relateVideoPath = last.absolutePath
                        selectedTrackId = -1
                    } else if (last.extension == "srt" && (first.extension == "mp4" || first.extension == "mkv")) {
                        selectedFilePath = last.absolutePath
                        selectedSubtitlesName = "    "
                        relateVideoPath = first.absolutePath
                        selectedTrackId = -1
                    } else if (first.extension == "srt" && last.extension == "srt") {
                        JOptionPane.showMessageDialog(
                            window,
                            "不能接收两个 srt 字幕文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                        )
                    } else if (first.extension == "mp4" && last.extension == "mp4") {
                        JOptionPane.showMessageDialog(
                            window,
                            "不能接收两个 mp4 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                        )
                    } else if (first.extension == "mkv" && last.extension == "mkv") {
                        JOptionPane.showMessageDialog(
                            window,
                            "不能接收两个 mkv 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                        )
                    } else if (first.extension == "mkv" && last.extension == "mp4") {
                        JOptionPane.showMessageDialog(
                            window,
                            "不能接收两个视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                        )
                    } else {
                        JOptionPane.showMessageDialog(
                            window,
                            "格式错误，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                        )
                    }

                } else if (files.size in 2..100 && type == MKV) {
                    var extensionWrong = ""
                    files.forEach { file ->
                        if (file.extension == "mkv") {
                            if (!selectedFileList.contains(file)) {
                                selectedFileList.add(file)
                            }

                        } else {
                            extensionWrong = extensionWrong + file.name + "\n"
                        }
                    }
                    selectedFileList.sortBy { it.nameWithoutExtension }
                    if (selectedFileList.isNotEmpty()) showTaskList = true
                    if (extensionWrong.isNotEmpty()) {
                        JOptionPane.showMessageDialog(window, "以下文件不是 mkv 格式\n$extensionWrong")
                    }

                } else if (files.size > 100 && type == MKV) {
                    JOptionPane.showMessageDialog(window, "批量处理最多不能超过 100 个文件")
                } else {
                    JOptionPane.showMessageDialog(window, "文件不能超过两个")
                }
            }
        }

        val filePicker = when (title) {
            "过滤词库" -> rememberFilePickerLauncher(
                title = "选择词库文件",
                mode = FileKitMode.Single,
                type = FileKitType.File(extensions = listOf("json")),
                dialogSettings = FileKitDialogSettings.createDefault(),
                onResult = { platformFile ->
                    platformFile?.let{
                        parseImportFile(listOf(platformFile.file))
                    }
                }
            )
            "用文档生成词库" -> rememberFilePickerLauncher(
                title = "选择文件",
                mode = FileKitMode.Single,
                type = FileKitType.File(extensions = listOf("pdf", "txt", "md", "java", "cs", "cpp", "c", "kt", "js", "py", "ts")),
                dialogSettings = FileKitDialogSettings.createDefault(),
                onResult = { platformFile ->
                    platformFile?.let{
                        parseImportFile(listOf(platformFile.file))
                    }
                }
            )
            "用字幕生成词库" -> rememberFilePickerLauncher(
                title = "选择字幕文件",
                mode = FileKitMode.Single,
                type = FileKitType.File(extensions = listOf("srt", "ass")),
                dialogSettings = FileKitDialogSettings.createDefault(),
                onResult = { platformFile ->
                    platformFile?.let{
                        parseImportFile(listOf(platformFile.file))
                    }
                }
            )

            "用视频生成词库" -> rememberFilePickerLauncher(
                title = "选择视频文件",
                mode = FileKitMode.Multiple(maxItems = 2),
                type = FileKitType.File(extensions = listOf("mkv", "mp4")),
                dialogSettings = FileKitDialogSettings.createDefault(),
                onResult = { platformFileList ->
                    platformFileList?.let{
                        val files =  platformFileList.map { it.file }
                        parseImportFile(files)
                    }
                }
            )

            else -> null
        }

        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                parseImportFile(files)
            }
        }

        /** 全选 */
        val selectAll: () -> Unit = {
            if (!isSelectedAll) {
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = true
                }
                isSelectedAll = true
            } else {
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = false
                }
                isSelectedAll = false
            }
        }

        /** 删除 */
        val delete: () -> Unit = {
            val list = mutableListOf<File>()
            checkedFileMap.forEach { (file, checked) ->
                if (checked) {
                    list.add(file)
                }
            }
            list.forEach { file ->
                checkedFileMap.remove(file)
                selectedFileList.remove(file)
                tasksState.remove(file)
                errorMessages.remove(file)
                if (currentTask == file) {
                    currentTask = null
                }
            }
        }

        /** 打开关联的视频 */
        val relateVideoPicker =  rememberFilePickerLauncher(
            title = "选择视频文件",
            mode = FileKitMode.Single,
            type = FileKitType.File(extensions = listOf("mkv", "mp4")),
            dialogSettings = FileKitDialogSettings.createDefault(),
            onResult = { platformFile ->
                if(platformFile != null){
                    relateVideoPath = platformFile.file.absolutePath
                }
            }
        )

        /** 改变了左边过滤区域的状态，如有有一个为真，或者选择了一个词库，就开始过滤 */
        val shouldApplyFilters: () -> Boolean = {
            numberFilter || bncNumberFilter || frqNumFilter ||
                    bncZeroFilter || frqZeroFilter || replaceToLemma ||
                    vocabularyFilterList.isNotEmpty()
        }

        /** 分析文件里的单词 */
        val analysis: (String, Int) -> Unit = { pathName, trackId ->
            started = true
            filterState = Parsing
            previewList.clear()
            parsedList.clear()
            scope.launch(Dispatchers.Default) {
                val words = when (type) {
                    DOCUMENT -> {
                        if (title == "过滤词库") {
                            val vocabulary = loadVocabulary(pathName)
                            filteringType = vocabulary.type
                            relateVideoPath = vocabulary.relateVideoPath
                            selectedTrackId = vocabulary.subtitlesTrackId
                            vocabulary.wordList
                        } else {
                            parseDocument(
                                pathName = pathName,
                                enablePhrases = enablePhrases,
                                sentenceLength = state.global.maxSentenceLength,
                                setProgressText = { progressText = it })
                        }

                    }

                    SUBTITLES -> {
                        val extension = File(pathName).extension
                        if (extension == "srt") {
                            parseSRT(
                                pathName = pathName,
                                enablePhrases = enablePhrases,
                                setProgressText = { progressText = it }
                            )
                        } else {
                            parseASS(
                                pathName = pathName,
                                enablePhrases = enablePhrases,
                                setProgressText = { progressText = it }
                            )
                        }
                    }

                    MKV -> {
                        parseVideo(
                            pathName = pathName,
                            enablePhrases = enablePhrases,
                            trackId = trackId,
                            setProgressText = { progressText = it }
                        )
                    }
                }
                parsedList.addAll(words)
                filterState = if (shouldApplyFilters()) {
                    Filtering
                } else {
                    // 不用过滤
                    previewList.addAll(words)
                    End
                }
            }
        }

        /** 批量分析文件 MKV 视频里的单词 */
        val batchAnalysis: (String) -> Unit = { language ->
            started = true
            previewList.clear()
            parsedList.clear()
            scope.launch(Dispatchers.Default) {
                val words = batchReadMKV(
                    language = language,
                    enablePhrases = enablePhrases,
                    selectedFileList = selectedFileList,
                    setCurrentTask = { currentTask = it },
                    setErrorMessages = {
                        errorMessages.clear()
                        errorMessages.putAll(it)
                    },
                    updateTaskState = {
                        tasksState[it.first] = it.second
                    }
                )
                if (words.isNotEmpty()) {
                    showTaskList = false
                    selectable = false
                }
                parsedList.addAll(words)
                filterState = if (shouldApplyFilters()) {
                    Filtering
                } else {
                    // 不用过滤
                    previewList.addAll(words)
                    End
                }

                if (errorMessages.isNotEmpty()) {
                    val string = "有 ${errorMessages.size} 个文件解析失败，请点击 [任务列表] 查看详细信息"
                    JOptionPane.showMessageDialog(window, string)
                }
            }

        }

        /**
         * 手动点击删除的单词，一般都是熟悉的词，
         * 所有需要添加到熟悉词库
         */
        val removeWord: (Word) -> Unit = { word ->
            val tempWord = word.deepCopy()
            // 如果是过滤词库，同时过滤的是熟悉词库，要把删除的单词从内存中的熟悉词库删除
            if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                familiarVocabulary.wordList.remove(tempWord)
            } else {
                // 用字幕生成的词库和用 MKV 生成的词库，需要把内部字幕转换为外部字幕
                if (tempWord.captions.isNotEmpty()) {
                    tempWord.captions.forEach { caption ->
                        val externalCaption = ExternalCaption(
                            relateVideoPath = relateVideoPath,
                            subtitlesTrackId = selectedTrackId,
                            subtitlesName = File(selectedFilePath).nameWithoutExtension,
                            start = caption.start,
                            end = caption.end,
                            content = caption.content
                        )
                        tempWord.externalCaptions.add(externalCaption)
                    }
                    tempWord.captions.clear()
                }

                // 把单词添加到熟悉词库
                if (!familiarVocabulary.wordList.contains(tempWord)) {
                    familiarVocabulary.wordList.add(tempWord)
                }
            }

            try {
                familiarVocabulary.size = familiarVocabulary.wordList.size
                val familiarFile = getFamiliarVocabularyFile()
                saveVocabulary(familiarVocabulary.serializeVocabulary, familiarFile.absolutePath)
                previewList.remove(word)
                removedWords.add(word)
            } catch (e: Exception) {
                // 回滚
                if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                    familiarVocabulary.wordList.add(tempWord)
                } else {
                    familiarVocabulary.wordList.remove(tempWord)
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "保存熟悉词库失败,错误信息：\n${e.message}")
            }

        }


        Box(Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
            .dragAndDropTarget(
                shouldStartDragAndDrop = shouldStartDragAndDrop,
                target = dropTarget
            )
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(bottom = 60.dp)
                    .background(MaterialTheme.colors.background)
            ) {
                Divider()
                Row(Modifier.fillMaxWidth()) {
                    // 左边的过滤区
                    val width = if (vocabularyFilterList.isEmpty()) 380.dp else 450.dp
                    Column(Modifier.width(width).fillMaxHeight()) {
                        BasicFilter(
                            filter = filter,
                            changeFilter = {
                                filter = it
                                scope.launch(Dispatchers.Default) {
                                    include = !include
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }

                            },
                            include = include,
                            changeInclude = {
                                include = it
                                scope.launch(Dispatchers.Default) {
                                    filter = !filter
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }

                            },
                            showMaxSentenceLength = (type == DOCUMENT && title != "过滤词库"),
                            numberFilter = numberFilter,
                            changeNumberFilter = {
                                numberFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            bncNum = state.global.bncNum,
                            setBncNum = { state.global.bncNum = it },
                            maxSentenceLength = state.global.maxSentenceLength,
                            setMaxSentenceLength = { state.global.maxSentenceLength = it },
                            bncNumFilter = bncNumberFilter,
                            changeBncNumFilter = {
                                bncNumberFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            frqNum = state.global.frqNum,
                            setFrqNum = { state.global.frqNum = it },
                            frqNumFilter = frqNumFilter,
                            changeFrqFilter = {
                                frqNumFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            bncZeroFilter = bncZeroFilter,
                            changeBncZeroFilter = {
                                bncZeroFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            frqZeroFilter = frqZeroFilter,
                            changeFrqZeroFilter = {
                                frqZeroFilter = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            replaceToLemma = replaceToLemma,
                            setReplaceToLemma = {
                                replaceToLemma = it
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                        )
                        VocabularyFilter(
                            vocabularyFilterList = vocabularyFilterList,
                            vocabularyFilterListAdd = {
                                if (!vocabularyFilterList.contains(it)) {
                                    vocabularyFilterList.add(it)
                                    if (started) {
                                        filterState = Filtering
                                    }
                                }
                            },
                            vocabularyFilterListRemove = {
                                vocabularyFilterList.remove(it)
                                if (started) {
                                    filterState = Filtering
                                }
                            },
                            recentList = state.recentList,
                            removeInvalidRecentItem = {
                                state.removeRecentItem(it)
                            },
                            familiarVocabulary = familiarVocabulary,
                            updateFamiliarVocabulary = {
                                val wordList = loadMutableVocabularyByName("FamiliarVocabulary").wordList
                                familiarVocabulary.wordList.addAll(wordList)
                            }
                        )
                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())
                    // 生成词库区
                    Column(
                        Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colors.background)
                    ) {

                        SelectFile(
                            type = type,
                            selectedFileList = selectedFileList,
                            selectedFilePath = selectedFilePath,
                            setSelectedFilePath = { selectedFilePath = it },
                            selectedSubtitle = selectedSubtitlesName,
                            setSelectedSubtitle = { selectedSubtitlesName = it },
                            setRelateVideoPath = { relateVideoPath = it },
                            relateVideoPath = relateVideoPath,
                            trackList = trackList,
                            selectedTrackId = selectedTrackId,
                            setSelectedTrackId = { selectedTrackId = it },
                            showTaskList = showTaskList,
                            showTaskListEvent = {
                                showTaskList = !showTaskList
                                if (!showTaskList) {
                                    selectable = false
                                }
                            },
                            analysis = { pathName, trackId ->
                                analysis(pathName, trackId)
                            },
                            batchAnalysis = { batchAnalysis(it) },
                            selectable = selectable,
                            changeSelectable = { selectable = !selectable },
                            selectAll = { selectAll() },
                            delete = { delete() },
                            chooseText = chooseText,
                            openFile = { filePicker?.launch() },
                            openRelateVideo = {relateVideoPicker.launch() },
                            started = started,
                            showEnablePhrases = title != "过滤词库",
                            enablePhrases = enablePhrases,
                            changeEnablePhrases = {
                                enablePhrases = it
                                filterState = Filtering
                            },
                        )

                        // 单词预览和任务列表
                        Box(Modifier.fillMaxSize()) {
                            if (started) {
                                when (filterState) {
                                    Parsing -> {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.width(60.dp).padding(bottom = 60.dp)
                                            )
                                            Text(text = progressText, color = MaterialTheme.colors.onBackground)
                                        }
                                    }

                                    Filtering -> {
                                        CircularProgressIndicator(
                                            Modifier.width(60.dp).align(Alignment.Center)
                                        )
                                        scope.launch(Dispatchers.Default) {
                                            // 是否应该执行过滤或包含
                                            if (shouldApplyFilters()) {
                                                // 过滤词库
                                                if (filter) {
                                                    // 根据词频或原型过滤单词
                                                    val basicFilteredList = filterWords(
                                                        parsedList,
                                                        numberFilter,
                                                        state.global.bncNum,
                                                        bncNumberFilter,
                                                        state.global.frqNum,
                                                        frqNumFilter,
                                                        bncZeroFilter,
                                                        frqZeroFilter,
                                                        replaceToLemma,
                                                        selectedFileList.isNotEmpty()
                                                    )
                                                    // 根据选择的词库过滤单词
                                                    val filteredList = filterSelectVocabulary(
                                                        selectedFileList = vocabularyFilterList,
                                                        basicFilteredList = basicFilteredList
                                                    )
                                                    // 过滤手动删除的单词
                                                    filteredList.removeAll(removedWords)
                                                    previewList.clear()
                                                    previewList.addAll(filteredList)
                                                    filterState = End
                                                } else {// 包含词库
                                                    // 根据词频或原型包含单词
                                                    val basicIncludeList = includeWords(
                                                        parsedList,
                                                        numberFilter,
                                                        state.global.bncNum,
                                                        bncNumberFilter,
                                                        state.global.frqNum,
                                                        frqNumFilter,
                                                        bncZeroFilter,
                                                        frqZeroFilter,
                                                        replaceToLemma,
                                                        selectedFileList.isNotEmpty()
                                                    )
                                                    // 根据选择的词库包含单词
                                                    val includeList = includeSelectVocabulary(
                                                        selectedFileList = vocabularyFilterList,
                                                        parsedList = parsedList
                                                    )
                                                    includeList.addAll(basicIncludeList)

                                                    // 过滤手动删除的单词
                                                    includeList.removeAll(removedWords)
                                                    previewList.clear()
                                                    previewList.addAll(includeList)
                                                    filterState = End
                                                }
                                            } else {
                                                // 不用过滤或包含
                                                previewList.clear()
                                                previewList.addAll(parsedList)
                                                filterState = End
                                            }

                                        }
                                    }

                                    End -> {
                                        PreviewWords(
                                            previewList = previewList,
                                            summaryVocabulary = summaryVocabulary,
                                            removeWord = { removeWord(it) },
                                            sort = sort,
                                            changeSort = { sort = it },
                                            showCard = showCard,
                                            changeShowCard = { showCard = it }
                                        )
                                    }

                                    Idle -> {}
                                }
                            } else {
                                val text = when (type) {
                                    DOCUMENT -> {
                                        if (title !== "过滤词库") {
                                            "可以拖放文档到这里"
                                        } else {
                                            "可以拖放词库到这里"
                                        }
                                    }

                                    SUBTITLES -> "可以拖放 SRT 或 ASS 字幕到这里"
                                    MKV -> "可以拖放 MKV 或 MP4 视频到这里"
                                }
                                if (!loading) {
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colors.onBackground,
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            if (loading) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                ) {
                                    CircularProgressIndicator(
                                        Modifier.width(60.dp).padding(bottom = 60.dp)
                                    )
                                    val text = if (selectedFileList.isNotEmpty()) {
                                        "正在读取第一个视频的字幕轨道列表"
                                    } else {
                                        "正在读取字幕轨道列表"
                                    }
                                    Text(text = text, color = MaterialTheme.colors.onBackground)
                                }
                            }
                            if (showTaskList) {
                                TaskList(
                                    selectedFileList = selectedFileList,
                                    updateOrder = {
                                        scope.launch {
                                            selectedFileList.clear()
                                            selectedFileList.addAll(it)
                                        }
                                    },
                                    tasksState = tasksState,
                                    currentTask = currentTask,
                                    errorMessages = errorMessages,
                                    selectable = selectable,
                                    checkedFileMap = checkedFileMap,
                                    checkedChange = {
                                        checkedFileMap[it.first] = it.second
                                    }
                                )
                            }
                        }
                    }
                }

            }
            // Bottom
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colors.background)
            ) {
                val fileName = File(selectedFilePath).nameWithoutExtension
                val saveEnabled = previewList.isNotEmpty()
                var saveOtherFormats by remember { mutableStateOf(false) }
                val vType = if (title == "过滤词库") {
                    filteringType
                } else if (selectedFileList.isNotEmpty()) {
                    DOCUMENT
                } else type
                Divider()
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {

                    val launcher = rememberFileSaverLauncher(
                        dialogSettings = FileKitDialogSettings.createDefault()
                    ) {  platformFile ->
                        scope.launch(Dispatchers.IO){
                            platformFile?.let{
                                val selectedFile = platformFile.file
                                val vocabularyDirPath = Paths.get(getResourcesFile("vocabulary").absolutePath)
                                val savePath = Paths.get(selectedFile.absolutePath)
                                if (savePath.startsWith(vocabularyDirPath)) {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "不能把词库保存到应用程序安装目录，因为软件更新或卸载时，生成的词库会被删除"
                                    )
                                } else {
                                    val vocabulary = Vocabulary(
                                        name = selectedFile.nameWithoutExtension,
                                        type = vType,
                                        language = "english",
                                        size = previewList.size,
                                        relateVideoPath = relateVideoPath,
                                        subtitlesTrackId = selectedTrackId,
                                        wordList = previewList
                                    )
                                    try {
                                        saveVocabulary(vocabulary, selectedFile.absolutePath)
                                        state.saveToRecentList(vocabulary.name, selectedFile.absolutePath, 0)

                                        // 清理状态
                                        selectedFileList.clear()
                                        started = false
                                        showTaskList = false
                                        tasksState.clear()
                                        currentTask = null
                                        errorMessages.clear()
                                        selectedFilePath = ""
                                        selectedSubtitlesName = ""
                                        previewList.clear()
                                        parsedList.clear()
                                        relateVideoPath = ""
                                        selectedTrackId = 0
                                        filteringType = DOCUMENT
                                        trackList.clear()
                                        filterState = Idle
                                        vocabularyFilterList.clear()
                                        numberFilter = false
                                        frqNumFilter = false
                                        bncNumberFilter = false
                                        bncZeroFilter = false
                                        frqZeroFilter = false
                                        replaceToLemma = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        JOptionPane.showMessageDialog(
                                            window,
                                            "保存词库失败,错误信息：\n${e.message}"
                                        )
                                    }


                                }
                            }
                        }

                    }


                    SaveButton(
                        enabled = saveEnabled,
                        saveClick = {
                            launcher.launch(fileName, "json")
                        },
                        otherClick = { saveOtherFormats = true }
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = {
                        onCloseRequest(state, title)
                    }) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(10.dp))
                }

                if (saveOtherFormats) {
                    SaveOtherVocabulary(
                        fileName = fileName,
                        wordList = previewList,
                        vocabularyType = vType,
                        colors = state.colors,
                        close = { saveOtherFormats = false }
                    )
                }
            }
        }

    }
}

/** FilterState 有四个状态：Idle、"Parse"、"Filtering"、"End" */
enum class FilterState {
    /** 空闲状态，预览区为空 */
    Idle,

    /** 正在解析文档或字幕 */
    Parsing,

    /** 正在过滤单词 */
    Filtering,

    /** 单词过滤完成，可以显示了*/
    End
}

@OptIn(ExperimentalSerializationApi::class)
private fun onCloseRequest(state: AppState, title: String) {
    when (title) {
        "过滤词库" -> state.filterVocabulary = false
        "用文档生成词库" -> state.generateVocabularyFromDocument = false
        "用字幕生成词库" -> state.generateVocabularyFromSubtitles = false
        "用视频生成词库" -> state.generateVocabularyFromVideo = false
    }

}
