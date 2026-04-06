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
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

@Composable
fun PreviewWords(
    previewList: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    removeWord: (Word) -> Unit,
    sort: String,
    changeSort: (String) -> Unit,
    showCard: Boolean,
    changeShowCard: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        // 显示方式：卡片或列表

        Summary(
            previewList,
            summaryVocabulary,
            sort = sort, changeSort = { changeSort(it) },
            showCard = showCard, changeShowCard = changeShowCard
        )

        val sortedList = when (sort) {
            "alphabet" -> {
                val sorted = previewList.sortedBy { it.value }
                sorted
            }

            "bnc" -> {
                val sorted = previewList.sortedBy { it.bnc }
                val zeroBnc = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for (word in sorted) {
                    if (word.bnc == 0) {
                        zeroBnc.add(word)
                    } else {
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroBnc)
                greaterThanZero
            }

            "coca" -> {
                val sorted = previewList.sortedBy { it.frq }
                val zeroFrq = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for (word in sorted) {
                    if (word.frq == 0) {
                        zeroFrq.add(word)
                    } else {
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroFrq)
                greaterThanZero
            }

            else -> previewList
        }


        if (showCard) {
            val listGridState = rememberLazyGridState()
            Box(Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    contentPadding = PaddingValues(15.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                    state = listGridState
                ) {
                    itemsIndexed(sortedList) { _: Int, word ->

                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape,
                                ) {
                                    Column(Modifier.padding(5.dp).width(200.dp)) {
                                        Text(
                                            text = word.value,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        val lemma = getWordLemma(word)
                                        Text(text = "原型:$lemma", fontSize = 12.sp)
                                        Row {
                                            Text(
                                                text = "BNC  ",
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(end = 2.dp)
                                            )
                                            Text(text = ":${word.bnc}", fontSize = 12.sp)
                                        }

                                        Text(text = "COCA:${word.frq}", fontSize = 12.sp)
                                        Divider()
                                        Text(
                                            text = word.translation,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                        )
                                    }
                                }
                            },
                            delayMillis = 50,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomStart,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(7.5.dp),
                                elevation = 3.dp
                            ) {
                                var closeVisible by remember { mutableStateOf(false) }
                                Box(Modifier.size(width = 130.dp, height = 65.dp)
                                    .onPointerEvent(PointerEventType.Enter) {
                                        closeVisible = true
                                    }
                                    .onPointerEvent(PointerEventType.Exit) {
                                        closeVisible = false
                                    }) {
                                    Text(
                                        text = word.value,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    if (closeVisible) {
                                        Icon(
                                            Icons.Filled.Close, contentDescription = "",
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .clickable {
                                                    scope.launch(Dispatchers.Default) {
                                                        removeWord(word)
                                                    }
                                                }
                                        )
                                    }


                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    style = LocalScrollbarStyle.current.copy(
                        shape = if (isWindows()) RectangleShape else RoundedCornerShape(
                            4.dp
                        )
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listGridState
                    )
                )


            }
        } else {
            val listState = rememberLazyListState()
            var shiftPressed by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                            shiftPressed = true
                        }
                    } else if (event.type == KeyEventType.KeyUp) {
                        if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                            shiftPressed = false
                        }
                    }
                    false
                }
            ) {
                val selectedList = remember { mutableStateListOf<Word>() }
                var latestSelectedIndex by remember { mutableStateOf(-1) }
                val topPadding = if (selectedList.isNotEmpty()) 0.dp else 0.dp
                // 性能优化：添加 key 参数使用单词的 value 作为唯一标识符，添加 contentType 参数
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(top = topPadding).fillMaxSize()
                ) {
                    items(
                        count = sortedList.size,
                        key = { index -> sortedList[index].value },
                        contentType = { 0 }
                    ) { index ->
                        val word = sortedList[index]
                        val selected = selectedList.contains(word)

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                .background(if (selected) MaterialTheme.colors.onBackground.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    if (shiftPressed) {
                                        // 如果最后一次选择不存在（第一次选择），
                                        // 就选中当前的,并且把多选的开始设置为列表的第一个
                                        if (latestSelectedIndex == -1) {
                                            latestSelectedIndex = index
                                            val subList = sortedList.subList(0, index + 1)
                                            selectedList.addAll(subList)
                                        } else {
                                            // 如果最后一次选择存在，就选中最后一次选择到当前的
                                            val start = min(latestSelectedIndex, index)
                                            val end = max(latestSelectedIndex, index)
                                            val subList = sortedList.subList(start, end + 1)
                                            selectedList.addAll(subList)
                                        }
                                    } else {
                                        latestSelectedIndex = index
                                        if (!selected) {
                                            selectedList.add(word)
                                        } else {
                                            selectedList.remove(word)
                                        }
                                    }

                                },

                            ) {
                            Text(
                                text = word.value,
                                modifier = Modifier.padding(start = 20.dp).width(130.dp),
                                color = MaterialTheme.colors.onBackground,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(20.dp))
                            Text(
                                text = word.translation.replace("\n", "  "),
                                color = MaterialTheme.colors.onBackground,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (selectedList.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch(Dispatchers.Default) {
                                selectedList.forEach { removeWord(it) }
                                selectedList.clear()
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }

                VerticalScrollbar(
                    style = LocalScrollbarStyle.current.copy(
                        shape = if (isWindows()) RectangleShape else RoundedCornerShape(
                            4.dp
                        )
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listState
                    )
                )
            }

        }

    }

}

