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
fun TaskList(
    selectedFileList: List<File>,
    updateOrder: (List<File>) -> Unit,
    tasksState: Map<File, Boolean>,
    currentTask: File?,
    errorMessages: Map<File, String>,
    selectable: Boolean,
    checkedFileMap: Map<File, Boolean>,
    checkedChange: (Pair<File, Boolean>) -> Unit,
) {
    val viewList = selectedFileList.map { it }
    var items by remember (viewList){ mutableStateOf(viewList) }
    val lazyListState = rememberLazyListState()
    val state = rememberReorderableLazyListState(lazyListState){from,to ->
        items = items.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        updateOrder(items)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        // 性能优化：改进 key 参数使用文件绝对路径作为唯一标识符，添加 contentType 参数
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = items.size,
                key = { index -> items[index].absolutePath },
                contentType = { 0 }
            ) { index ->
                val item = items[index]
                ReorderableItem(state, key = item) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    Surface(elevation = elevation.value) {
                        Box(
                            modifier = Modifier
                                .clickable { }
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .longPressDraggableHandle()
                        ) {
                            Text(
                                text = item.nameWithoutExtension,
                                modifier = Modifier.align(Alignment.CenterStart).padding(top = 16.dp, bottom = 16.dp),
                                color = MaterialTheme.colors.onBackground
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                if (selectable) {
                                    val checked = checkedFileMap[item]

                                    Checkbox(
                                        checked = checked == true,
                                        onCheckedChange = { checkedChange(Pair(item, it)) }
                                    )
                                }


                                if (tasksState[item] == true) {
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                ),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = "完成", modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.TopCenter,
                                            alignment = Alignment.TopCenter,
                                            offset = DpOffset.Zero
                                        ),
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.TaskAlt,
                                                contentDescription = "",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                    }


                                } else if (item == currentTask) {
                                    CircularProgressIndicator(
                                        Modifier
                                            .padding(start = 8.dp, end = 16.dp).width(24.dp).height(24.dp)
                                    )
                                } else if (tasksState[item] == false) {

                                    val text = errorMessages[item].orEmpty()
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                                ),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = text, modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.CenterStart,
                                            alignment = Alignment.CenterStart,
                                            offset = DpOffset.Zero
                                        ),
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.Error,
                                                contentDescription = "",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }


                        }
                        Divider()
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }


}
