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

@Composable
fun VocabularyFilter(
    vocabularyFilterList: List<File>,
    vocabularyFilterListAdd: (File) -> Unit,
    vocabularyFilterListRemove: (File) -> Unit,
    recentList: List<RecentItem>,
    removeInvalidRecentItem: (RecentItem) -> Unit,
    familiarVocabulary: MutableVocabulary,
    updateFamiliarVocabulary: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {

        Column(Modifier.width(180.dp).fillMaxHeight().background(MaterialTheme.colors.background)) {

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        getResourcesFile("vocabulary/大学英语/四级.json").let {
                            if (!vocabularyFilterList.contains(it)) {
                                vocabularyFilterListAdd(it)
                            }
                        }
                    }
            ) {
                Text("四级", color = MaterialTheme.colors.onBackground)
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        getResourcesFile("vocabulary/大学英语/六级.json").let {
                            if (!vocabularyFilterList.contains(it)) {
                                vocabularyFilterListAdd(it)
                            }
                        }
                    }
            ) {
                Text("六级", color = MaterialTheme.colors.onBackground)
            }
            var expanded by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().height(40.dp)
                .background(MaterialTheme.colors.background)
                .clickable { expanded = true }
            ) {
                Text(
                    text = "内置词库",
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )
                BuiltInVocabularyMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    addVocabulary = { file ->
                        if (!vocabularyFilterList.contains(file)) {
                            vocabularyFilterListAdd(file)
                        }
                    }
                )
            }


            var showDialog by remember { mutableStateOf(false) }
            if (showDialog) {
                FamiliarDialog(
                    close = {
                        showDialog = false
                        updateFamiliarVocabulary()
                    },

                    )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        if (familiarVocabulary.wordList.isEmpty()) {
                            val result = JOptionPane.showConfirmDialog(
                                null,
                                "熟悉词库现在还没有单词，是否导入单词到熟悉词库",
                                "",
                                JOptionPane.YES_NO_OPTION
                            )
                            if (result == 0) {
                                showDialog = true
                            }
                        } else {
                            val familiarFile = getFamiliarVocabularyFile()
                            vocabularyFilterListAdd(File(familiarFile.absolutePath))
                        }
                    }
            ) {
                if (familiarVocabulary.wordList.isNotEmpty()) {
                    BadgedBox(badge = {
                        Badge {
                            val badgeNumber = "${familiarVocabulary.wordList.size}"
                            Text(
                                badgeNumber,
                                modifier = Modifier.semantics {
                                    contentDescription = "$badgeNumber new notifications"
                                }
                            )
                        }
                    }) {
                        Text(text = "熟悉词库", color = MaterialTheme.colors.onBackground)
                    }
                } else {
                    Text(text = "熟悉词库", color = MaterialTheme.colors.onBackground)
                }
            }

            if (recentList.isNotEmpty()) {
                var expandRecent by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth().height(40.dp).clickable { expandRecent = true }) {
                    Text(
                        text = "最近词库",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    val dropdownMenuHeight = if (recentList.size <= 10) (recentList.size * 40 + 20).dp else 420.dp
                    DropdownMenu(
                        expanded = expandRecent,
                        onDismissRequest = { expandRecent = false },
                        offset = DpOffset(20.dp, 0.dp),
                        modifier = Modifier
                            .widthIn(min = 300.dp, max = 700.dp)
                            .width(IntrinsicSize.Max)
                            .height(dropdownMenuHeight)
                    ) {
                        Box(Modifier.fillMaxWidth().height(dropdownMenuHeight)) {
                            val stateVertical = rememberScrollState(0)
                            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                                Column {
                                    recentList.forEach { recentItem ->

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                                .clickable {
                                                    val recentFile = File(recentItem.path)
                                                    if (recentFile.exists()) {
                                                        vocabularyFilterListAdd(recentFile)
                                                    } else {
                                                        // 文件可能被删除了
                                                        removeInvalidRecentItem(recentItem)
                                                        JOptionPane.showMessageDialog(
                                                            null,
                                                            "文件地址错误：\n${recentItem.path}"
                                                        )
                                                    }

                                                }
                                        ) {
                                            Text(
                                                text = recentItem.name,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                                color = MaterialTheme.colors.onBackground,
                                                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                                            )

                                        }

                                    }

                                }
                            }

                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = stateVertical
                                )
                            )
                        }

                    }
                }
            }
            val singleLauncher = rememberFilePickerLauncher(
                title = "选择词库",
                type = FileKitType.File(extensions = listOf("json")),
                mode = FileKitMode.Single,
            ) { file ->
                scope.launch(Dispatchers.IO){
                    file?.let {
                        vocabularyFilterListAdd(file.file)
                    }
                }

            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clickable {
                        singleLauncher.launch()
                    }
            ) {
                Text("选择词库", color = MaterialTheme.colors.onBackground)
            }

        }
        Divider(Modifier.width(1.dp).fillMaxHeight())
        Column(
            Modifier.width(270.dp).fillMaxHeight()
                .background(MaterialTheme.colors.background)
        ) {
            SelectedList(
                vocabularyFilterList,
                removeFile = {
                    vocabularyFilterListRemove(it)
                })
        }
    }
}

