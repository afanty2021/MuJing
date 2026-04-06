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
@OptIn(ExperimentalFoundationApi::class)

@Composable
fun SelectFile(
    type: VocabularyType,
    selectedFileList: List<File>,
    selectedFilePath: String,
    setSelectedFilePath: (String) -> Unit,
    selectedSubtitle: String,
    setSelectedSubtitle: (String) -> Unit,
    setRelateVideoPath: (String) -> Unit,
    relateVideoPath: String,
    trackList: List<Pair<Int, String>>,
    selectedTrackId: Int,
    setSelectedTrackId: (Int) -> Unit,
    showTaskList: Boolean,
    showTaskListEvent: () -> Unit,
    analysis: (String, Int) -> Unit,
    batchAnalysis: (String) -> Unit,
    selectable: Boolean,
    changeSelectable: () -> Unit,
    selectAll: () -> Unit,
    delete: () -> Unit,
    chooseText: String,
    openFile: () -> Unit,
    openRelateVideo: () -> Unit,
    started: Boolean,
    showEnablePhrases: Boolean,
    enablePhrases: Boolean,
    changeEnablePhrases: (Boolean) -> Unit,
) {

    Column(Modifier.height(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ) {
            Text(chooseText, color = MaterialTheme.colors.onBackground)
            if (type == SUBTITLES || type == DOCUMENT) {
                Spacer(Modifier.width(75.dp))
            } else if (type == MKV) {
                Spacer(Modifier.width(24.dp))
            }
            BasicTextField(
                value = selectedFilePath,
                onValueChange = {
                    setSelectedFilePath(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                textStyle = TextStyle(
                    lineHeight = 29.sp,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onBackground
                ),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
                    ) {
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .width(300.dp)
                    .padding(start = 8.dp, end = 10.dp)
                    .height(35.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            OutlinedButton(onClick = { openFile() }) {
                Text("打开", fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))
            val startEnable = if (type != MKV) {
                selectedFilePath.isNotEmpty()
            } else selectedSubtitle != "    " || selectedFileList.isNotEmpty()

            OutlinedButton(
                enabled = startEnable,
                onClick = {
                    if (selectedFileList.isEmpty()) {
                        analysis(selectedFilePath, selectedTrackId)
                    } else {
                        batchAnalysis("English")
                    }

                }) {
                Text("开始", fontSize = 12.sp)
            }
            Spacer(Modifier.width(20.dp))
            if (showEnablePhrases) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("处理词组 ", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default)
                    Checkbox(
                        checked = enablePhrases,
                        onCheckedChange = {
                            changeEnablePhrases(it)
                            // 如果已经开始了，就重新开始
                            if (started) {
                                if (selectedFileList.isEmpty()) {
                                    analysis(selectedFilePath, selectedTrackId)
                                } else {
                                    batchAnalysis("English")
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp, 30.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))
            if (chooseText != "选择词库") {
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "帮助文档", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 50,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    var documentWindowVisible by remember { mutableStateOf(false) }
                    var currentPage by remember { mutableStateOf("document") }
                    IconButton(onClick = {
                        documentWindowVisible = true
                        currentPage = when (type) {
                            DOCUMENT -> "document"
                            SUBTITLES -> "subtitles"
                            MKV -> "matroska"
                        }
                    }) {
                        Icon(
                            Icons.Filled.Help,
                            contentDescription = "Localized description",
                            tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                        )
                    }


                    if (documentWindowVisible) {
                        DocumentWindow(
                            close = { documentWindowVisible = false },
                            currentPage = currentPage,
                            setCurrentPage = { currentPage = it }

                        )
                    }
                }
            }

        }

        if ((selectedFilePath.isNotEmpty() || selectedFileList.isNotEmpty()) && type == MKV) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                if (trackList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.width(IntrinsicSize.Max)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(282.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(
                                    text = selectedSubtitle, fontSize = 12.sp,
                                )
                                Icon(
                                    Icons.Default.ExpandMore, contentDescription = "Localized description",
                                    modifier = Modifier.size(20.dp, 20.dp)
                                )
                            }
                            val dropdownMenuHeight = (trackList.size * 40 + 20).dp
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(282.dp)
                                    .height(dropdownMenuHeight)
                            ) {
                                trackList.forEach { (index, description) ->
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            setSelectedSubtitle(description)
                                            setSelectedTrackId(index)
                                        },
                                        modifier = Modifier.width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = "$description ", fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }


                            }

                        }

                    }
                } else {
                    // 批量处理，现在只能批量处理英语字幕，所以就写死了。
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier
                                .width(282.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(
                                text = "英语", fontSize = 12.sp,
                            )
                        }
                    }
                }


                if (selectedFileList.isNotEmpty()) {
                    OutlinedButton(onClick = { showTaskListEvent() }) {
                        Text("任务列表", fontSize = 12.sp)
                    }
                    if (showTaskList) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { changeSelectable() }) {
                            Text("选择", fontSize = 12.sp)
                        }
                    }
                    if (selectable) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { selectAll() }) {
                            Text("全选", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { delete() }) {
                            Text("删除", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (type == SUBTITLES && selectedFilePath.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                Text("选择对应的视频(可选)", color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = relateVideoPath,
                    onValueChange = setRelateVideoPath,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = 29.sp,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(start = 8.dp, end = 10.dp)
                        .height(35.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
                OutlinedButton(onClick = { openRelateVideo() }) {
                    Text("打开")
                }
            }
        }
        Divider()
    }
}

