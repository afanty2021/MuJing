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

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Header(
    wordScreenState: WordScreenState,
    title:String,
    window: ComposeWindow,
    wordRequestFocus: () -> Unit,
    modifier: Modifier
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ){
        // macOS 的标题栏和 windows 不一样，需要特殊处理
        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center){
            // 记忆单词时的状态信息
            val text = when(wordScreenState.memoryStrategy){
                Normal -> { if(wordScreenState.vocabulary.size>0) "${wordScreenState.index + 1}/${wordScreenState.vocabulary.size}" else ""}
                Dictation -> { "听写单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.dictationWords.size}"}
                DictationTest -> {"听写测试   ${wordScreenState.dictationIndex + 1}/${wordScreenState.reviewWords.size}"}
                NormalReviewWrong -> { "复习错误单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
                DictationTestReviewWrong -> { "听写测试 - 复习错误单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
            }

            val top = if(wordScreenState.memoryStrategy != Normal) 0.dp else 12.dp
            Text(
                text = text,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .testTag("Header")
                    .padding(top = top )
            )
            if(wordScreenState.memoryStrategy != Normal){
                Spacer(Modifier.width(20.dp))
                val tooltip = when (wordScreenState.memoryStrategy) {
                    DictationTest, DictationTestReviewWrong -> {
                        "退出听写测试"
                    }
                    Dictation -> {
                        "退出听写"
                    }
                    else -> {
                        "退出复习"
                    }
                }
                ExitButton(
                    tooltip = tooltip,
                    onClick = {
                    wordScreenState.showInfo()
                    wordScreenState.clearInputtedState()
                    wordScreenState.memoryStrategy = Normal
                    if( wordScreenState.wrongWords.isNotEmpty()){
                        wordScreenState.wrongWords.clear()
                    }
                    if(wordScreenState.reviewWords.isNotEmpty()){
                        wordScreenState.reviewWords.clear()
                    }
                    wordRequestFocus()
                })
            }
        }
    }
}
