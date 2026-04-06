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

/** 返回单词的原型，如果没有原型，就返回单词本身 */
fun getWordLemma(word: Word): String {
    word.exchange.split("/").forEach { exchange ->
        val pair = exchange.split(":")
        if (pair[0] == "0") return pair[1]
    }
    return word.value
}

@Composable
fun Summary(
    list: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    sort: String,
    showCard: Boolean,
    changeShowCard: (Boolean) -> Unit,
    changeSort: (String) -> Unit,
) {

    Column(Modifier.fillMaxWidth()) {
        val height = 61.dp
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(height).padding(start = 10.dp)
        ) {
            val summary = computeSummary(list, summaryVocabulary)
            Text(text = "共 ${list.size} 词  ", color = MaterialTheme.colors.onBackground)
            Text(text = "牛津5000核心词：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["oxford"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[0]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "四级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet4"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[1]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "六级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet6"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[2]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "GRE: ", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["gre"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[3]} 词", color = MaterialTheme.colors.onBackground)
            }


            var expanded by remember { mutableStateOf(false) }
            Box {
                val width = 195.dp
                val text = when (sort) {
                    "appearance" -> "按出现的顺序排序"
                    "bnc" -> "按 BNC 词频排序"
                    "alphabet" -> "按首字母排序"
                    else -> "按 COCA 词频排序"
                }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(width)
                        .padding(start = 10.dp)
                        .background(Color.Transparent)
                        .border(1.dp, Color.Transparent)
                ) {
                    Text(text = text)
                    Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(width)
                        .height(180.dp)
                ) {
                    val selectedColor = if (MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                    val backgroundColor = Color.Transparent
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("alphabet")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "bnc") selectedColor else backgroundColor)
                    ) {
                        Text("按首字母排序")
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("bnc")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "bnc") selectedColor else backgroundColor)
                    ) {
                        Text("按BNC词频排序")
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("coca")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "coca") selectedColor else backgroundColor)
                    ) {
                        Text("按COCA词频排序")

                    }


                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("appearance")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if (sort == "appearance") selectedColor else backgroundColor)
                    ) {
                        Text("按出现的顺序排序")
                    }

                }

            }
            IconButton(
                onClick = { changeShowCard(!showCard) },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Icon(
                    if (showCard) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                    contentDescription = "Localized description",
                    tint = MaterialTheme.colors.onBackground
                )
            }
        }
        Divider()
    }


}

/**
 * 计算摘要
 */
private fun computeSummary(
    list: List<Word>,
    vocabularySummary: Map<String, List<String>>
): List<Int> {
    var oxfordCount = 0
    var cet4Count = 0
    var cet6Count = 0
    var greCount = 0
    list.forEach { word ->
        if (vocabularySummary["oxford"]?.contains(word.value) == true) {
            oxfordCount++
        }
        if (vocabularySummary["cet4"]?.contains(word.value) == true) {
            cet4Count++
        }
        if (vocabularySummary["cet6"]?.contains(word.value) == true) {
            cet6Count++
        }
        if (vocabularySummary["gre"]?.contains(word.value) == true) {
            greCount++
        }
    }

    return listOf(oxfordCount, cet4Count, cet6Count, greCount)
}

/**
 * 载入摘要词库
 */
fun loadSummaryVocabulary(): Map<String, List<String>> {

    val oxford = loadVocabulary("vocabulary/牛津核心词/The_Oxford_5000.json").wordList
    val cet4 = loadVocabulary("vocabulary/大学英语/四级.json").wordList
    val cet6 = loadVocabulary("vocabulary/大学英语/六级.json").wordList
    val gre = loadVocabulary("vocabulary/出国/GRE.json").wordList

    val oxfordList = oxford.map { word -> word.value }
    val cet4List = cet4.map { word -> word.value }
    val cet6List = cet6.map { word -> word.value }
    val greList = gre.map { word -> word.value }

    val map = HashMap<String, List<String>>()
    map["oxford"] = oxfordList
    map["cet4"] = cet4List
    map["cet6"] = cet6List
    map["gre"] = greList

    return map
}

