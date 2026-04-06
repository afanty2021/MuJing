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
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import javax.swing.JOptionPane
import kotlin.concurrent.schedule

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainContent(
    appState: AppState,
    wordScreenState: WordScreenState,
    dictationState: DictationState,
    azureTTS: AzureTTS,
    currentWord: Word,
    videoBounds: Rectangle,
    wordFocusRequester:FocusRequester,
    window: ComposeWindow,
    openVocabulary: () -> Unit,
    showContext:(MediaInfo) -> Unit,
    eventBus: EventBus
){
    var nextButtonVisible by remember{ mutableStateOf(false) }
        /** 协程构建器 */
        val scope = rememberCoroutineScope()

        /** 单词输入错误*/
        var isWrong by remember { mutableStateOf(false) }

        /** 是否正在播放视频 */
        var isPlaying by remember { mutableStateOf(false) }

        /** 正在播放的视频信息 */
        var playMedia :MediaInfo? by remember { mutableStateOf(null) }

        /** 正在使用快捷键播放字幕的索引 */
        var plyingIndex by remember { mutableStateOf(1) }

        /** 显示填充后的书签图标 */
        var showBookmark by remember { mutableStateOf(false) }

        /** 显示删除对话框 */
        var showDeleteDialog by remember { mutableStateOf(false) }

        /** 显示把当前单词加入到熟悉词库的确认对话框 */
        var showFamiliarDialog by remember { mutableStateOf(false) }


        /** 第一条字幕输入框的焦点请求器 */
        val focusRequester1 = remember { FocusRequester() }
        /** 第二条字幕输入框的焦点请求器 */
        val focusRequester2 = remember { FocusRequester() }
        /** 第三条字幕输入框的焦点请求器 */
        val focusRequester3 = remember { FocusRequester() }

        /** 当前单词输入框是否有焦点 */
        var wordFocused by remember { mutableStateOf(false) }

        /** 是否应该跳转到单词输入框,
         * 播放字幕片段后，通过这个属性决定是否把焦点切换到单词
         * */
        var shouldJumpToWord by remember { mutableStateOf(false) }

        /** 等宽字体*/
        val monospace  = rememberMonospace()

        val audioPlayerComponent = LocalAudioPlayerComponent.current

        val clipboardManager = LocalClipboardManager.current

        var alert by remember { mutableStateOf(CustomAlert()) }

        /** 是否正在播放单词发音 */
        var isPlayingAudio by remember { mutableStateOf(false) }
        /** 删除当前单词 */
        val deleteWord:() -> Unit = {
            val index = wordScreenState.index
            wordScreenState.vocabulary.wordList.removeAt(index)
            wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
            if(wordScreenState.vocabulary.name == "HardVocabulary"){
                appState.hardVocabulary.wordList.remove(currentWord)
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }
            try{
                wordScreenState.saveCurrentVocabulary()
                wordScreenState.clearInputtedState()
            }catch (e:IOException){
                // 回滚
                wordScreenState.vocabulary.wordList.add(index,currentWord)
                wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    appState.hardVocabulary.wordList.add(currentWord)
                    appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "删除单词失败,错误信息:\n${e.message}")
            }
        }

        /** 把当前单词加入到熟悉词库 */
        val addToFamiliar:() -> Unit = {
            val file = getFamiliarVocabularyFile()
            val familiar = loadVocabulary(file.absolutePath)
            val familiarWord = currentWord.deepCopy()
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (wordScreenState.vocabulary.type == VocabularyType.MKV ||
                wordScreenState.vocabulary.type == VocabularyType.SUBTITLES
            ) {
                familiarWord.captions.forEach{ caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                        subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                        subtitlesName = wordScreenState.vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    familiarWord.externalCaptions.add(externalCaption)
                }
                familiarWord.captions.clear()

            }
            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }
            if(!familiar.wordList.contains(familiarWord)){
                familiar.wordList.add(familiarWord)
                familiar.size = familiar.wordList.size
            }
            try{
                saveVocabulary(familiar, file.absolutePath)
                deleteWord()
            }catch(e:IOException){
                // 回滚
                if(familiar.wordList.contains(familiarWord)){
                    familiar.wordList.remove(familiarWord)
                    familiar.size = familiar.wordList.size
                }

                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "保存熟悉词库失败,错误信息:\n${e.message}")
            }
            showFamiliarDialog = false
        }

        /** 处理加入到困难词库的函数 */
        val bookmarkClick :() -> Unit = {
            val hardWord = currentWord.deepCopy()
            val contains = appState.hardVocabulary.wordList.contains(currentWord)
            val index = appState.hardVocabulary.wordList.indexOf(currentWord)
            if(contains){
                appState.hardVocabulary.wordList.removeAt(index)
                // 如果当前词库是困难词库，说明用户想把单词从困难词库（当前词库）删除
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    wordScreenState.clearInputtedState()
                    wordScreenState.vocabulary.wordList.remove(currentWord)
                    wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                    try{
                        wordScreenState.saveCurrentVocabulary()
                    }catch (e:IOException){
                        // 回滚
                        appState.hardVocabulary.wordList.add(index,currentWord)
                        appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                        wordScreenState.vocabulary.wordList.add(wordScreenState.index,currentWord)
                        wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size

                        e.printStackTrace()
                        JOptionPane.showMessageDialog(window, "保存当前词库失败,错误信息:\n${e.message}")
                    }

                }
            }else{
                val relateVideoPath = wordScreenState.vocabulary.relateVideoPath
                val subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId
                val subtitlesName =
                    if (wordScreenState.vocabulary.type == VocabularyType.SUBTITLES) wordScreenState.vocabulary.name else ""

                currentWord.captions.forEach { caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath,
                        subtitlesTrackId,
                        subtitlesName,
                        caption.start,
                        caption.end,
                        caption.content
                    )
                    hardWord.externalCaptions.add(externalCaption)
                }
                hardWord.captions.clear()
                appState.hardVocabulary.wordList.add(hardWord)
            }
            try{
                appState.saveHardVocabulary()
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }catch(e:IOException){
                // 回滚
                if(contains){
                    appState.hardVocabulary.wordList.add(index,hardWord)
                }else{

                    appState.hardVocabulary.wordList.remove(hardWord)
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "保存困难词库失败,错误信息:\n${e.message}")
            }

        }

        /** 焦点请求 */
        val focusRequest:(Int) -> Unit = { index ->
            try {
                if(wordScreenState.subtitlesVisible){
                    when(index){
                        1 -> focusRequester1.requestFocus()
                        2 -> focusRequester2.requestFocus()
                        3 -> focusRequester3.requestFocus()
                        else -> wordFocusRequester.requestFocus()
                    }
                }else{
                    wordFocusRequester.requestFocus()
                }
            } catch (_: IllegalStateException) {
                // FocusRequester 未初始化时的安全处理
                println("FocusRequester not initialized, skipping focus request for index: $index")
                // 尝试使用主要的 wordFocusRequester 作为后备
                try {
                    wordFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                    println("wordFocusRequester also not initialized, skipping focus request")
                }
            }
        }

        /** 用快捷键播放视频时被调用的函数， */
        val handleMediaPlay: (
                index:Int
                ) -> Unit = { index ->
            plyingIndex = index
            val mediaInfo =  when {
                // 先从 MKV 或 SUBTITLES 类型的词库获取 MediaInfo
                (wordScreenState.getCurrentWord().captions.size >= index) -> {
                    val caption = currentWord.captions[index -1]
                    MediaInfo(
                        caption,
                        wordScreenState.vocabulary.relateVideoPath,
                        wordScreenState.vocabulary.subtitlesTrackId
                    )
                }
                // 混合词库 DOCUMENT 从 ExternalCaptions 获取 MediaInfo
                (wordScreenState.getCurrentWord().externalCaptions.size >= index) -> {
                    getMediaInfoFromExternalCaption(currentWord.externalCaptions, index - 1)
                }
                else -> null
            }

            if(mediaInfo != null){
                playMedia = mediaInfo
                isPlaying = true

            }

            // 如果现在焦点在单词输入框，播放完字幕后，跳回单词输入框
            shouldJumpToWord = wordFocused

        }

        /** 显示本单元已经完成对话框 */
        var showUnitFinishedDialog by remember { mutableStateOf(false) }

        /** 显示整个词库已经学习完成对话框 */
        var isVocabularyFinished by remember { mutableStateOf(false) }

        /** 播放整个单元完成时音效 */
        val playUnitFinished = {
            if (wordScreenState.isPlaySoundTips) {
                playSound("audio/Success!!.wav", wordScreenState.soundTipsVolume)
            }
        }

        /**
         * 在听写模式，闭着眼睛听写单词时，刚拼写完单词，就播放这个声音感觉不好，
         * 在非听写模式下按Enter键就不会有这种感觉，因为按Enter键，
         * 自己已经输入完成了，有一种期待，预测到了将会播放提示音。
         */
        val delayPlaySound:() -> Unit = {
            Timer("playUnitFinishedSound", false).schedule(1000) {
                playUnitFinished()
            }
            showUnitFinishedDialog = true
        }


        /** 增加复习错误单词时的索引 */
        val increaseWrongIndex:() -> Unit = {
            if (wordScreenState.dictationIndex + 1 == wordScreenState.wrongWords.size) {
                delayPlaySound()
            } else wordScreenState.dictationIndex++
        }


        /** 切换到下一个单词 */
        val toNext: () -> Unit = {
            scope.launch {
                wordScreenState.clearInputtedState()
                when (wordScreenState.memoryStrategy) {
                    Normal -> {
                        when {
                            (wordScreenState.index == wordScreenState.vocabulary.size - 1) -> {
                                isVocabularyFinished = true
                                playUnitFinished()
                                showUnitFinishedDialog = true
                            }
                            ((wordScreenState.index + 1) % 20 == 0) -> {
                                playUnitFinished()
                                showUnitFinishedDialog = true
                            }
                            else -> wordScreenState.index += 1
                        }
                        wordScreenState.saveWordScreenState()
                    }
                    Dictation -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.dictationWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    DictationTest -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.reviewWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    NormalReviewWrong -> { increaseWrongIndex() }
                    DictationTestReviewWrong -> { increaseWrongIndex() }
                }

                wordFocusRequester.requestFocus()

            }
        }

        /** 切换到上一个单词,听写时不允许切换到上一个单词 */
        val previous :() -> Unit = {
            scope.launch {
                // 正常记忆单词
                if(wordScreenState.memoryStrategy == Normal){
                    wordScreenState.clearInputtedState()
                    if((wordScreenState.index) % 20 != 0 ){
                        wordScreenState.index -= 1
                        wordScreenState.saveWordScreenState()
                    }
                    // 复习错误单词
                }else if (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong ){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.dictationIndex > 0 ){
                        wordScreenState.dictationIndex -= 1
                    }
                }
                wordFocusRequester.requestFocus()
            }
        }

        // 处理键盘事件
        LaunchedEffect(currentWord) {
            eventBus.events.collect { event ->
                if (event is WordScreenEventType) {
                    when (event) {
                        WordScreenEventType.FOCUS_ON_WORD -> {
                            wordFocusRequester.requestFocus()
                        }
                        WordScreenEventType.NEXT_WORD -> {
                            toNext()
                        }

                        WordScreenEventType.PREVIOUS_WORD -> {
                            previous()
                        }

                        WordScreenEventType.OPEN_SIDEBAR -> {
                            appState.openSidebar = !appState.openSidebar
                        }

                        WordScreenEventType.OPEN_VOCABULARY -> {
                            openVocabulary()
                        }
                        WordScreenEventType.SHOW_WORD -> {
                            wordScreenState.wordVisible = !wordScreenState.wordVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.showUnderline = !dictationState.showUnderline
                                    dictationState.saveDictationState()
                                }
                            }
                        }

                        WordScreenEventType.SHOW_PRONUNCIATION -> {
                            wordScreenState.phoneticVisible = !wordScreenState.phoneticVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.phoneticVisible = wordScreenState.phoneticVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }

                        WordScreenEventType.SHOW_LEMMA -> {
                            wordScreenState.morphologyVisible = !wordScreenState.morphologyVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.morphologyVisible = wordScreenState.morphologyVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }

                        WordScreenEventType.SHOW_DEFINITION -> {
                            wordScreenState.definitionVisible = !wordScreenState.definitionVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.definitionVisible = wordScreenState.definitionVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }

                        WordScreenEventType.SHOW_TRANSLATION -> {
                            wordScreenState.translationVisible = !wordScreenState.translationVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.translationVisible = wordScreenState.translationVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }

                        WordScreenEventType.SHOW_SENTENCES -> {
                            wordScreenState.sentencesVisible = !wordScreenState.sentencesVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.sentencesVisible = wordScreenState.sentencesVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }

                        WordScreenEventType.SHOW_SUBTITLES -> {
                            wordScreenState.subtitlesVisible = !wordScreenState.subtitlesVisible
                            launch (Dispatchers.IO){
                                wordScreenState.saveWordScreenState()
                                if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                                    dictationState.subtitlesVisible = wordScreenState.subtitlesVisible
                                    dictationState.saveDictationState()
                                }
                            }

                        }


                        WordScreenEventType.PLAY_AUDIO -> {
                            val audioPath = getAudioPath(
                                word = currentWord.value,
                                audioSet = appState.localAudioSet,
                                addToAudioSet = { appState.localAudioSet.add(it) },
                                pronunciation = wordScreenState.pronunciation,
                                azureTTS = azureTTS
                            )
                            playAudio(
                                word = currentWord.value,
                                audioPath = audioPath,
                                pronunciation = wordScreenState.pronunciation,
                                volume = appState.global.audioVolume,
                                audioPlayerComponent = audioPlayerComponent,
                                changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                            )
                        }

                        WordScreenEventType.DELETE_WORD -> {
                            showDeleteDialog = true
                        }

                        WordScreenEventType.ADD_TO_FAMILIAR -> {
                            if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                                JOptionPane.showMessageDialog(window, "不能把熟悉词库的单词添加到熟悉词库")
                            }else{
                                showFamiliarDialog = true
                            }
                        }

                        WordScreenEventType.ADD_TO_DIFFICULT -> {
                            scope.launch {
                                bookmarkClick()
                                showBookmark = true
                            }
                        }

                        WordScreenEventType.COPY_WORD -> {
                            scope.launch {
                            clipboardManager.setText(AnnotatedString(currentWord.value))
                            }
                        }

                        WordScreenEventType.PLAY_FIRST_CAPTION -> {
                            scope.launch (Dispatchers.Default){
                                handleMediaPlay(1)
                            }
                        }

                        WordScreenEventType.PLAY_SECOND_CAPTION -> {
                            scope.launch (Dispatchers.Default){
                            handleMediaPlay(2)
                            }
                        }

                        WordScreenEventType.PLAY_THIRD_CAPTION -> {
                            scope.launch (Dispatchers.Default){
                            handleMediaPlay(3)
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
                .onPointerEvent(PointerEventType.Move){nextButtonVisible = true}
                .onPointerEvent(PointerEventType.Exit){nextButtonVisible = false}
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .background(MaterialTheme.colors.background)
                .focusable(true)
                .align(Alignment.Center)
                .padding(end = 0.dp,bottom = 58.dp)
        ) {

            /** 听写模式的错误单词 */
            val dictationWrongWords = remember { mutableStateMapOf<Word, Int>()}

            /** 显示编辑单词对话框 */
            var showEditWordDialog by remember { mutableStateOf(false) }

            /** 清空听写模式存储的错误单词 */
            val resetUnitTime: () -> Unit = {
                dictationWrongWords.clear()
            }


            /** 播放错误音效 */
            val playBeepSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/beep.wav", wordScreenState.soundTipsVolume)
                }
            }

            /** 播放成功音效 */
            val playSuccessSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/hint.wav", wordScreenState.soundTipsVolume)
                }
            }


            /** 播放按键音效 */
            val playKeySound = {
                if (appState.global.isPlayKeystrokeSound) {
                    playSound("audio/keystroke.wav", appState.global.keystrokeVolume)
                }
            }

            /**
             * 当用户在听写测试按 enter 调用的函数，
             * 在听写测试跳过单词也算一次错误
             */
            val dictationSkipCurrentWord: () -> Unit = {
                if (wordScreenState.wordCorrectTime == 0) {
                    val dictationWrongTime = dictationWrongWords[currentWord]
                    if (dictationWrongTime == null) {
                        dictationWrongWords[currentWord] = 1
                    }
                }
            }

            /** 焦点切换到单词输入框 */
            val jumpToWord:() -> Unit = {
                wordFocusRequester.requestFocus()
            }

            /** 焦点切换到抄写字幕 */
            val jumpToCaptions:() -> Unit = {
                if((wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest) &&
                    wordScreenState.subtitlesVisible && (currentWord.captions.isNotEmpty() || currentWord.externalCaptions.isNotEmpty())
                ){
                    focusRequester1.requestFocus()
                }
            }

            /** 检查输入的单词 */
            val checkWordInput: (String) -> Unit = { input ->
                if(!isWrong){
                    wordScreenState.wordTextFieldValue = input
                    wordScreenState.wordTypingResult.clear()
                    var done = true
                    /**
                     *  防止用户粘贴内容过长，如果粘贴的内容超过 word.value 的长度，
                     * 会改变 BasicTextField 宽度，和 Text 的宽度不匹配
                     */
                    if (input.length > currentWord.value.length) {
                        wordScreenState.wordTypingResult.clear()
                        wordScreenState.wordTextFieldValue = ""
                    } else {
                        val inputChars = input.toList()
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar == wordChar) {
                                wordScreenState.wordTypingResult.add(Pair(inputChar, true))
                            } else {
                                // 字母输入错误
                                wordScreenState.wordTypingResult.add(Pair(inputChar, false))
                                done = false
                                playBeepSound()
                                isWrong = true
                                wordScreenState.wordWrongTime++
                                // 如果是听写测试，或独立的听写测试，需要汇总错误单词
                                if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                    val dictationWrongTime = dictationWrongWords[currentWord]
                                    if (dictationWrongTime != null) {
                                        dictationWrongWords[currentWord] = dictationWrongTime + 1
                                    } else {
                                        dictationWrongWords[currentWord] = 1
                                    }
                                }
//                                // 再播放一次单词发音
                                if (wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }

                            }
                        }
                        // 用户输入的单词完全正确
                        if (wordScreenState.wordTypingResult.size == currentWord.value.length && done) {
                            // 输入完全正确
                            playSuccessSound()
                            wordScreenState.wordCorrectTime++
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                Timer("input correct to next", false).schedule(50) {
                                    toNext()
                                }
                            }else if (wordScreenState.isAuto && wordScreenState.wordCorrectTime == wordScreenState.repeatTimes ) {
                                Timer("input correct to next", false).schedule(50) {
                                    toNext()
                                }
                            } else {
                                Timer("input correct clean InputChar", false).schedule(50){
                                    wordScreenState.wordTypingResult.clear()
                                    wordScreenState.wordTextFieldValue = ""
                                }

                                // 再播放一次单词发音
                                if (wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }
                            }
                        }
                    }
                }else{
                    // 输入错误后继续输入
                    if(input.length > wordScreenState.wordTypingResult.size){
                        // 如果不截取字符串，用户长按某个按键，程序可能会崩溃
                        val inputStr = input.substring(0,wordScreenState.wordTypingResult.size)
                        val inputChars = inputStr.toList()
                        isWrong = false
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar != wordChar) {
                                playBeepSound()
                                isWrong = true
                            }
                        }
                        if(!isWrong){
                            wordScreenState.wordTextFieldValue = inputStr
                        }
                    }else if(input.length == wordScreenState.wordTypingResult.size-1){
                        // 输入错误后按退格键删除错误字母
                        isWrong = false
                            wordScreenState.wordTypingResult.removeLast()
                            wordScreenState.wordTextFieldValue = input
                    }else if(input.isEmpty()){
                        // 输入错误后 Ctrl + A 全选后删除全部输入
                        wordScreenState.wordTextFieldValue = ""
                        wordScreenState.wordTypingResult.clear()
                        isWrong = false
                    }

                }

            }


            /** 检查输入的字幕 */
            val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                when(index){
                    0 -> wordScreenState.captionsTextFieldValue1 = input
                    1 -> wordScreenState.captionsTextFieldValue2 = input
                    2 -> wordScreenState.captionsTextFieldValue3 = input
                }
                val typingResult = wordScreenState.captionsTypingResultMap[index]
                typingResult!!.clear()
                val inputChars = input.toMutableList()
                for (i in inputChars.indices) {
                    val inputChar = inputChars[i]
                    if(i<captionContent.length){
                        val captionChar = captionContent[i]
                        if (inputChar == captionChar) {
                            typingResult.add(Pair(captionChar, true))
                        }else if (inputChar == ' ' && (captionChar == '[' || captionChar == ']')) {
                            typingResult.add(Pair(captionChar, true))
                            // 音乐符号不好输入，所以可以使用空格替换
                        }else if (inputChar == ' ' && (captionChar == '♪')) {
                            typingResult.add(Pair(captionChar, true))
                            // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                            inputChars.add(i,'♪')
                            inputChars.removeAt(i+1)
                            val textFieldValue = String(inputChars.toCharArray())
                            when(index){
                                0 -> wordScreenState.captionsTextFieldValue1 = textFieldValue
                                1 -> wordScreenState.captionsTextFieldValue2 = textFieldValue
                                2 -> wordScreenState.captionsTextFieldValue3 = textFieldValue
                            }
                        } else {
                            typingResult.add(Pair(inputChar, false))
                        }
                    }else{
                        typingResult.add(Pair(inputChar, false))
                    }

                }

            }

            /** 索引递减 */
            val decreaseIndex = {
                if(wordScreenState.index == wordScreenState.vocabulary.size - 1){
                    val mod = wordScreenState.vocabulary.size % 20
                    wordScreenState.index -= (mod-1)
                }else if (wordScreenState.vocabulary.size > 19) wordScreenState.index -= 19
                else wordScreenState.index = 0
            }

            /** 计算正确率 */
            val correctRate: () -> Float = {
                val size = if(wordScreenState.memoryStrategy == Dictation ) wordScreenState.dictationWords.size else wordScreenState.reviewWords.size
                var rate =  (size - dictationWrongWords.size).div(size.toFloat()) .times(1000)
                rate = rate.toInt().toFloat().div(10)
                rate
            }

            /** 重复学习本单元 */
            val learnAgain: () -> Unit = {
                decreaseIndex()
                resetUnitTime()
                wordScreenState.saveWordScreenState()
                showUnitFinishedDialog = false
                isVocabularyFinished = false
            }


            /** 复习错误单词 */
            val reviewWrongWords: () -> Unit = {
                val reviewList = dictationWrongWords.keys.toList()
                if (reviewList.isNotEmpty()) {
                    wordScreenState.showInfo(clear = false)
                    if (wordScreenState.memoryStrategy == DictationTest ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ) {
                        wordScreenState.memoryStrategy = DictationTestReviewWrong
                    }else{
                        wordScreenState.memoryStrategy = NormalReviewWrong
                    }
                    if( wordScreenState.wrongWords.isEmpty()){
                        wordScreenState.wrongWords.addAll(reviewList)
                    }
                    wordScreenState.dictationIndex = 0
                    showUnitFinishedDialog = false
                }
            }

            /** 下一单元 */
            val nextUnit: () -> Unit = {

                if (wordScreenState.memoryStrategy == NormalReviewWrong ||
                    wordScreenState.memoryStrategy == DictationTestReviewWrong
                ) {
                    wordScreenState.wrongWords.clear()
                }

                if( wordScreenState.memoryStrategy == Dictation){
                    wordScreenState.showInfo()
                }

                wordScreenState.index += 1
                wordScreenState.unit++
                resetUnitTime()
                wordScreenState.memoryStrategy = Normal
                wordScreenState.saveWordScreenState()
                showUnitFinishedDialog = false
            }


            /** 正常记忆单词，进入到听写测试，需要的单词 */
            val shuffleNormal:() -> Unit = {
                val wordValue = wordScreenState.getCurrentWord().value
                val shuffledList = wordScreenState.generateDictationWords(wordValue)
                wordScreenState.dictationWords.clear()
                wordScreenState.dictationWords.addAll(shuffledList)
            }
            /** 从独立的听写测试再次进入到听写测试时，需要的单词 */
            val shuffleDictationReview:() -> Unit = {
                var shuffledList = wordScreenState.reviewWords.shuffled()
                // 如果打乱顺序的列表的第一个单词，和当前单元的最后一个词相等，就不会触发重组
                while(shuffledList.first() == currentWord){
                    shuffledList = wordScreenState.reviewWords.shuffled()
                }
                wordScreenState.reviewWords.clear()
                wordScreenState.reviewWords.addAll(shuffledList)
            }
            /** 进入听写模式 */
            val enterDictation: () -> Unit = {
                scope.launch {
                    wordScreenState.saveWordScreenState()
                    when(wordScreenState.memoryStrategy){
                        // 从正常记忆单词第一次进入到听写测试
                        Normal -> {
                            shuffleNormal()
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 正常记忆单词时选择再次听写
                        Dictation ->{
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                        }
                        // 从复习错误单词进入到听写测试，这里有两种情况：
                        // 一种是从正常记忆单词进入到复习错误单词，复习完毕后，再次听写
                        NormalReviewWrong ->{
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.wrongWords.clear()
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 一种是从独立的听写测试进入到复习错误单词，复习完毕后，再次听写
                        DictationTestReviewWrong ->{
                            wordScreenState.memoryStrategy = DictationTest
                            wordScreenState.wrongWords.clear()
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 在独立的听写测试时选择再次听写
                        DictationTest ->{
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                        }
                    }
                    wordFocusRequester.requestFocus()
                    resetUnitTime()
                    showUnitFinishedDialog = false
                    isVocabularyFinished = false
                }
            }

            /** 文件选择器 */
            val launcher = rememberFileSaverLauncher(
                dialogSettings = FileKitDialogSettings.createDefault()
            ) {  platformFile ->
                platformFile?.let{
                    val selectedFile = platformFile.file
                    val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                    val savePath = Paths.get(selectedFile.absolutePath)
                    if(savePath.startsWith(vocabularyDirPath)){
                        JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，词库会被重置或者被删除")
                    }else{
                        wordScreenState.vocabulary.wordList.shuffle()
                        val shuffledList = wordScreenState.vocabulary.wordList
                        val vocabulary = Vocabulary(
                            name = selectedFile.nameWithoutExtension,
                            type = VocabularyType.DOCUMENT,
                            language = "english",
                            size = wordScreenState.vocabulary.size,
                            relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                            subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                            wordList = shuffledList
                        )

                        try {
                            saveVocabulary(vocabulary, selectedFile.absolutePath)
                            appState.changeVocabulary(selectedFile, wordScreenState, 0)
                            // changeVocabulary 会把内置词库保存到最近列表，
                            // 保存后，如果再切换列表，就会有两个名字相同的词库，
                            // 所以需要把刚刚添加的词库从最近列表删除
                            for (i in 0 until appState.recentList.size) {
                                val recentItem = appState.recentList[i]
                                if (recentItem.name == wordScreenState.vocabulary.name) {
                                    appState.removeRecentItem(recentItem)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            JOptionPane.showMessageDialog(window, "保存词库失败,错误信息:\n${e.message}")
                        }


                    }
                }
            }

            /**
             * 重置索引
             * 参数 isShuffle 是否打乱词库
             */
            val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->
                // 如果要打乱顺序
                if (isShuffle) {
                    // 内置词库的地址
                    val path = getResourcesFile("vocabulary").absolutePath
                    // 如果要打乱的词库是内置词库，要选择一个地址，保存打乱后的词库，
                    // 如果不选择地址的话，软件升级后词库会被重置。
                    if(wordScreenState.vocabularyPath.startsWith(path)){
                        val fileName = File(wordScreenState.vocabularyPath).nameWithoutExtension
                        launcher.launch(fileName, "json")
                    }else{
                        try{
                            wordScreenState.vocabulary.wordList.shuffle()
                            wordScreenState.saveCurrentVocabulary()
                        }catch(e:Exception){
                            e.printStackTrace()
                            JOptionPane.showMessageDialog(window, "保存词库失败,错误信息:\n${e.message}")
                        }

                    }

                }

                wordScreenState.index = 0
                wordScreenState.unit = 1
                wordScreenState.saveWordScreenState()
                resetUnitTime()
                showUnitFinishedDialog = false
                isVocabularyFinished = false
            }
            val wordKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                when {
                    ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || it.key == Key.DirectionRight)
                            && it.type == KeyEventType.KeyUp) -> {
                        toNext()
                        if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                            dictationSkipCurrentWord()
                        }
                        true
                    }
                    ((it.key == Key.PageUp || it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                        previous()
                        true
                    }

                    (isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }

                    (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }
                    (it.type == KeyEventType.KeyDown
                            && it.key != Key.ShiftRight
                            && it.key != Key.ShiftLeft
                            && it.key != Key.CtrlRight
                            && it.key != Key.CtrlLeft
                            && it.key != Key.AltLeft
                            && it.key != Key.AltRight
                            && it.key != Key.Escape
                            && it.key != Key.Enter
                            && it.key != Key.NumPadEnter
                            ) -> {
                        playKeySound()
                        true
                    }
                    else -> false
                }
            }


            LaunchedEffect(appState.vocabularyChanged){
                if(appState.vocabularyChanged){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.memoryStrategy == NormalReviewWrong ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ){
                        wordScreenState.wrongWords.clear()
                    }
                    if (wordScreenState.memoryStrategy == Dictation) {
                        wordScreenState.showInfo()
                        resetUnitTime()
                    }

                    if(wordScreenState.memoryStrategy == DictationTest) wordScreenState.memoryStrategy = Normal


                    appState.vocabularyChanged = false
                }
            }

            var activeMenu by remember { mutableStateOf(false) }
            Box(
                Modifier.onPointerEvent(PointerEventType.Exit) { activeMenu = false }
            ) {
                /** 动态菜单，鼠标移动到单词区域时显示 */
                if (activeMenu) {
                    Row(modifier = Modifier.align(Alignment.TopCenter)) {
                        val contains = appState.hardVocabulary.wordList.contains(currentWord)
                        DeleteButton(onClick = { showDeleteDialog = true })
                        EditButton(onClick = { showEditWordDialog = true })
                        FamiliarButton(onClick = {
                            if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                                JOptionPane.showMessageDialog(window, "不能把熟悉词库的单词添加到熟悉词库")
                            }else{
                                showFamiliarDialog = true
                            }

                        })
                        HardButton(
                            onClick = { bookmarkClick() },
                            contains = contains,
                            fontFamily = monospace
                        )
                        CopyButton(wordValue = currentWord.value)
                    }
                }else if(showBookmark){
                    val contains = appState.hardVocabulary.wordList.contains(currentWord)
                    // 这个按钮只显示 0.3 秒后消失
                    BookmarkButton(
                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 96.dp),
                        contains = contains,
                        disappear = {showBookmark = false}
                    )
                }

                Row(Modifier.align(Alignment.Center).padding(top = 48.dp)){
                    Word(
                        word = currentWord,
                        global = appState.global,
                        wordVisible = wordScreenState.wordVisible,
                        pronunciation = wordScreenState.pronunciation,
                        azureTTS = azureTTS,
                        playTimes = wordScreenState.playTimes,
                        isPlaying = isPlayingAudio,
                        setIsPlaying = { isPlayingAudio = it },
                        isDictation = (wordScreenState.memoryStrategy == Dictation ||wordScreenState.memoryStrategy == DictationTest),
                        showUnderline = dictationState.showUnderline,
                        fontFamily = monospace,
                        audioSet = appState.localAudioSet,
                        addToAudioSet = {appState.localAudioSet.add(it) },
                        correctTime = wordScreenState.wordCorrectTime,
                        wrongTime = wordScreenState.wordWrongTime,
                        textFieldValue = wordScreenState.wordTextFieldValue,
                        typingResult = wordScreenState.wordTypingResult,
                        checkTyping = { checkWordInput(it) },
                        focusRequester = wordFocusRequester,
                        updateFocusState = {wordFocused = it},
                        textFieldKeyEvent = {wordKeyEvent(it)},
                        showMenu = {activeMenu = it}
                    )
                }

            }


            Phonetic(
                word = currentWord,
                phoneticVisible = wordScreenState.phoneticVisible,
                fontSize = appState.global.detailFontSize
            )
            Morphology(
                word = currentWord,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                searching = false,
                morphologyVisible = wordScreenState.morphologyVisible,
                fontSize = appState.global.detailFontSize
            )
            Definition(
                word = currentWord,
                definitionVisible = wordScreenState.definitionVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Translation(
                word = currentWord,
                translationVisible = wordScreenState.translationVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Sentences(
                word = currentWord,
                sentencesVisible = wordScreenState.sentencesVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )

            val startPadding = if ( isPlaying && !wordScreenState.isChangeVideoBounds) 0.dp else 50.dp
            val captionsModifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(bottom = 0.dp, start = startPadding)
                .onKeyEvent {
                    when {
                        ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || (it.key == Key.DirectionRight && !it.isShiftPressed))
                                && it.type == KeyEventType.KeyUp
                                ) -> {
                            toNext()
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                dictationSkipCurrentWord()
                            }
                            true
                        }
                        ((it.key == Key.PageUp  ||  (it.key == Key.DirectionLeft && !it.isShiftPressed)) && it.type == KeyEventType.KeyUp) -> {
                            previous()
                            true
                        }
                        else -> false
                    }
                }
            Captions(
                captionsVisible = wordScreenState.subtitlesVisible,
                playTripleMap = getPlayTripleMap(wordScreenState.vocabulary.type,wordScreenState.vocabulary.subtitlesTrackId,wordScreenState.vocabulary.relateVideoPath,  currentWord),
                isPlaying = isPlaying,
                plyingIndex = plyingIndex,
                setPlayingIndex = {plyingIndex = it},
                volume = appState.global.videoVolume,
                setIsPlaying = { isPlaying = it },
                setPlayingMedia = { playMedia = it },
                word = currentWord,
                bounds = videoBounds,
                textFieldValueList = listOf(wordScreenState.captionsTextFieldValue1,wordScreenState.captionsTextFieldValue2,wordScreenState.captionsTextFieldValue3),
                typingResultMap = wordScreenState.captionsTypingResultMap,
                putTypingResultMap = { index, list ->
                    wordScreenState.captionsTypingResultMap[index] = list
                },
                checkTyping = { index, input, captionContent ->
                    checkCaptionsInput(index, input, captionContent)
                },
                playKeySound = { playKeySound() },
                modifier = captionsModifier,
                focusRequesterList = listOf(focusRequester1,focusRequester2,focusRequester3),
                jumpToWord = {jumpToWord()},
                openSearch = {appState.openSearch()},
                fontSize = appState.global.detailFontSize,
                isWriteSubtitles = wordScreenState.isWriteSubtitles,
            )

            if (showDeleteDialog) {
                ConfirmDialog(
                    message = "确定要删除单词 ${currentWord.value} ?",
                    confirm = {
                        scope.launch {
                            deleteWord()
                            showDeleteDialog = false
                        }
                    },
                    close = { showDeleteDialog = false }
                )
            }
            if(showFamiliarDialog){
                ConfirmDialog(
                    message = "确定要把 ${currentWord.value} 加入到熟悉词库？\n" +
                            "加入到熟悉词库后，${currentWord.value} 会从当前词库删除。",
                    confirm = { scope.launch { addToFamiliar() } },
                    close = { showFamiliarDialog = false }
                )

            }
            if (showEditWordDialog) {
                EditWordDialog(
                    word = currentWord,
                    title = "编辑单词",
                    appState = appState,
                    vocabulary = wordScreenState.vocabulary,
                    vocabularyDir = wordScreenState.getVocabularyDir(),
                    save = { newWord ->
                        scope.launch {
                            val index = wordScreenState.index
                            // 触发重组
                            wordScreenState.vocabulary.wordList.removeAt(index)
                            wordScreenState.vocabulary.wordList.add(index, newWord)
                            try{
                                wordScreenState.saveCurrentVocabulary()
                                showEditWordDialog = false
                            }catch(e:Exception){
                                // 回滚
                                wordScreenState.vocabulary.wordList.removeAt(index)
                                wordScreenState.vocabulary.wordList.add(index, currentWord)
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window, "保存当前词库失败,错误信息:\n${e.message}")
                            }

                        }
                    },
                    close = { showEditWordDialog = false }
                )
            }

            /** 显示独立的听写测试的选择单元对话框 */
            var showUnitDialog by remember { mutableStateOf(false) }
            /** 打开独立的听写测试的选择单元对话框 */
            val openReviewDialog:() -> Unit = {
                showUnitFinishedDialog = false
                showUnitDialog = true
                resetUnitTime()
            }

            if(showUnitDialog){
                SelectUnitDialog(
                    close = {showUnitDialog = false},
                    wordScreenState = wordScreenState,
                    wordRequestFocus = {
                        wordFocusRequester.requestFocus()
                    },
                    isMultiple = true
                )
            }

            /** 关闭当前单元结束时跳出的对话框 */
            val close: () -> Unit = {
                showUnitFinishedDialog = false
                if(isVocabularyFinished) isVocabularyFinished = false
            }
            if (showUnitFinishedDialog) {
                UnitFinishedDialog(
                    close = { close() },
                    isVocabularyFinished = isVocabularyFinished,
                    correctRate = correctRate(),
                    memoryStrategy = wordScreenState.memoryStrategy,
                    openReviewDialog = {openReviewDialog()},
                    isReviewWrong = (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong),
                    dictationWrongWords = dictationWrongWords,
                    enterDictation = { enterDictation() },
                    learnAgain = { learnAgain() },
                    reviewWrongWords = { reviewWrongWords() },
                    nextUnit = { nextUnit() },
                    resetIndex = { resetIndex(it) }
                )
            }
        }

            // 视频播放器
            if(isPlaying && playMedia != null){
                val videoPlayerSize by remember(appState.global.size){
                    derivedStateOf {
                        computeVideoSize(appState.global.size)
                    }
                }

                // 验证视频文件的路径
                val resolvedPath =  resolveMediaPath(
                    playMedia!!.mediaPath,
                    wordScreenState.getVocabularyDir()
                )
                if(resolvedPath != ""){
                    playMedia!!.mediaPath = resolvedPath
                }else{
                    alert.message = if(playMedia!!.mediaPath.isEmpty())"视频地址为空" else "视频地址错误,视频可能已经被移动或删除"
                    alert.title = "无法播放视频"
                    alert.isError = true
                    alert.visible = true

                }

                MiniVideoPlayer(
                    modifier = Modifier.align(Alignment.Center),
                    size = videoPlayerSize,
                    stop = {
                        isPlaying = false
                        if(shouldJumpToWord){
                            wordFocusRequester.requestFocus()
                        }else{
                            focusRequest(plyingIndex)
                        }
                    },
                    volume = appState.global.videoVolume,
                    externalPlayingState = isPlaying,
                    showContextButton = true,
                    showContext ={ playMedia?.let {
                        showContext(it)
                        isPlaying = false
                        focusRequest(plyingIndex)
                    } },
                    mediaInfo = playMedia,
                    externalSubtitlesVisible = wordScreenState.externalSubtitlesVisible,
                    showTitle = true,
                    settingOffset = DpOffset((-56).dp,(-96).dp)
                )
            }


        if (nextButtonVisible) {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "上一个")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { previous() },
                    modifier = Modifier.testTag("PreviousButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "下一个")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterStart,
                    alignment = Alignment.CenterStart,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { toNext()},
                    modifier = Modifier.testTag("NextButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }


    }

        if(alert.visible){
            val titleColor = if(alert.isError) MaterialTheme.colors.error else MaterialTheme.colors.onSurface
            AlertDialog(
                onDismissRequest = { alert.clear() },
                title = { Text(alert.title,color = titleColor) },
                text = { Text(alert.message,color = MaterialTheme.colors.onSurface) },
                confirmButton = {
                    OutlinedButton(onClick = { alert.clear() }) {
                        Text("确定",color = MaterialTheme.colors.onSurface)
                    }
                },
                modifier = Modifier.background(MaterialTheme.colors.surface),
            )
        }
}
