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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VocabularyEmpty(
    openVocabulary: () -> Unit,
    openBuiltInVocabulary: () -> Unit = {},
    generateVocabulary: () -> Unit = {},
    openDocument: () -> Unit = {},
    parentWindow : ComposeWindow,
    openChooseVocabulary: (String) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
                    .width(450.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "打开词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openVocabulary() })
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "使用手册",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openDocument() })
                            .width(78.dp)
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "生成词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {generateVocabulary()  })
                            .padding(5.dp)
                    )
                }

                var visible by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .onPointerEvent(PointerEventType.Exit) { visible = false }
                ){
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp).onPointerEvent(PointerEventType.Enter) { visible = true }
                    ) {
                        Text(
                            text = "内置词库",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                .padding(5.dp)
                        )
                    }
                    val scope = rememberCoroutineScope()
                    AnimatedVisibility(visible = visible){
                        var selectedFile by remember { mutableStateOf<File?>(null) }
                        // 文件选择器
                        val launcher = rememberFileSaverLauncher(
                            dialogSettings = FileKitDialogSettings.createDefault()
                        ) {  platformFile ->
                            scope.launch (Dispatchers.IO){
                                platformFile?.let{
                                    val fileToSave = platformFile.file
                                    selectedFile?.let {
                                        try{
                                            fileToSave.writeBytes(selectedFile!!.readBytes())
                                            openChooseVocabulary(fileToSave.absolutePath)
                                        }catch (e:Exception){
                                            e.printStackTrace()
                                            JOptionPane.showMessageDialog(parentWindow,"保存失败，错误信息：\n${e.message}")
                                        }
                                    }

                                }
                            }

                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 5.dp)
                        ) {
                            Text(
                                text = "四级",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/四级.json")
                                    selectedFile = file
                                    launcher.launch(file.nameWithoutExtension,"json")
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "六级",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/六级.json")
                                    selectedFile = file
                                    launcher.launch(file.nameWithoutExtension,"json")
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "牛津核心3000词",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/牛津核心词/The_Oxford_3000.json")
                                    selectedFile = file
                                    launcher.launch(file.nameWithoutExtension,"json")
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "高中词库乱序版",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/高中英语/高中英语-乱序版.json")
                                    selectedFile = file
                                    launcher.launch(file.nameWithoutExtension,"json")
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "更多",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                    .padding(5.dp)
                            )
                        }
                    }
                }

            }
        }


    }
}
