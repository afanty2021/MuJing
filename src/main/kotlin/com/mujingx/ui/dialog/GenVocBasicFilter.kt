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
fun BasicFilter(
    filter: Boolean,
    changeFilter: (Boolean) -> Unit,
    include: Boolean,
    changeInclude: (Boolean) -> Unit,
    showMaxSentenceLength: Boolean,
    numberFilter: Boolean,
    changeNumberFilter: (Boolean) -> Unit,
    bncNum: Int,
    setBncNum: (Int) -> Unit,
    maxSentenceLength: Int,
    setMaxSentenceLength: (Int) -> Unit,
    bncNumFilter: Boolean,
    changeBncNumFilter: (Boolean) -> Unit,
    frqNum: Int,
    setFrqNum: (Int) -> Unit,
    frqNumFilter: Boolean,
    changeFrqFilter: (Boolean) -> Unit,
    bncZeroFilter: Boolean,
    changeBncZeroFilter: (Boolean) -> Unit,
    frqZeroFilter: Boolean,
    changeFrqZeroFilter: (Boolean) -> Unit,
    replaceToLemma: Boolean,
    setReplaceToLemma: (Boolean) -> Unit,
) {
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (showMaxSentenceLength) {
                var maxLengthFieldValue by remember { mutableStateOf(TextFieldValue("$maxSentenceLength")) }
                Text(
                    "单词所在句子的最大单词数 ",
                    color = MaterialTheme.colors.onBackground,
                    fontFamily = FontFamily.Default
                )
                BasicTextField(
                    value = maxLengthFieldValue,
                    onValueChange = { maxLengthFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = maxLengthFieldValue.text.toIntOrNull()
                                if (input != null && input >= 10) {
                                    setMaxSentenceLength(input)
                                } else {
                                    setMaxSentenceLength(10)
                                    maxLengthFieldValue = TextFieldValue("10")
                                    JOptionPane.showMessageDialog(null, "单词所在句子的最大单词数不能小于 10")
                                }
                            }
                        }
                        .width(40.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
            }
        }
        Divider()
        val textWidth = 320.dp

        val textColor = MaterialTheme.colors.onBackground
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(61.dp)
        ) {
            Text("包含", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default)
            Checkbox(
                checked = include,
                onCheckedChange = {
                    changeInclude(it)
                },
                modifier = Modifier.size(30.dp, 30.dp)
            )

            Spacer(Modifier.width(10.dp))
            Text("过滤", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default)
            Checkbox(
                checked = filter,
                onCheckedChange = {
                    changeFilter(it)
                },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        // 过滤词频
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                    Text("过滤 ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                    Text("包含 ", color = MaterialTheme.colors.onBackground)
                }
                Text(
                    "BNC", color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(end = 1.dp)
                )
                Text("   词频前 ", color = MaterialTheme.colors.onBackground)
                var bncNumFieldValue by remember { mutableStateOf(TextFieldValue("$bncNum")) }
                BasicTextField(
                    value = bncNumFieldValue,
                    onValueChange = { bncNumFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = bncNumFieldValue.text.toIntOrNull()
                                if (input != null && input >= 0) {
                                    setBncNum(input)
                                } else {
                                    bncNumFieldValue = TextFieldValue("$bncNum")
                                    JOptionPane.showMessageDialog(null, "数字解析错误，将设置为默认值")
                                }
                            }
                        }
                        .width(50.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
                Text(" 的单词", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = bncNumFilter,
                onCheckedChange = { changeBncNumFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                    Text("过滤 ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                    Text("包含 ", color = MaterialTheme.colors.onBackground)
                }
                Text("COCA 词频前 ", color = MaterialTheme.colors.onBackground)
                var frqNumFieldValue by remember { mutableStateOf(TextFieldValue("$frqNum")) }
                BasicTextField(
                    value = frqNumFieldValue,
                    onValueChange = { frqNumFieldValue = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = LocalTextStyle.current.lineHeight,
                        fontSize = LocalTextStyle.current.fontSize,
                        color = MaterialTheme.colors.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusable()
                        .onFocusChanged {
                            if (!it.isFocused) {
                                val input = frqNumFieldValue.text.toIntOrNull()
                                if (input != null && input >= 0) {
                                    setFrqNum(input)
                                } else {
                                    frqNumFieldValue = TextFieldValue("$frqNum")
                                    JOptionPane.showMessageDialog(null, "数字解析错误，将设置为默认值")
                                }
                            }
                        }
                        .width(50.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                )
                Text(" 的单词", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = frqNumFilter,
                onCheckedChange = { changeFrqFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {


            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                    Text("过滤 ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                    Text("包含 ", color = MaterialTheme.colors.onBackground)
                }
                Text("所有 ", color = MaterialTheme.colors.onBackground)

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "英国国家语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {

                    Text("BNC", color = blueColor,
                        modifier = Modifier
                            .clickable {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                        .isSupported(Desktop.Action.BROWSE)
                                ) {
                                    Desktop.getDesktop().browse(URI("https://www.natcorp.ox.ac.uk/"))
                                }
                            }
                            .pointerHoverIcon(Hand)
                            .padding(end = 3.dp))
                }

                Text("   语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = bncZeroFilter,
                onCheckedChange = { changeBncZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                    Text("过滤 ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                    Text("包含 ", color = MaterialTheme.colors.onBackground)
                }
                Text("所有 ", color = MaterialTheme.colors.onBackground)
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "美国当代英语语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {
                    Text("COCA", color = blueColor,
                        modifier = Modifier.clickable {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.BROWSE)
                            ) {
                                Desktop.getDesktop().browse(URI("https://www.english-corpora.org/coca/"))
                            }
                        }
                            .pointerHoverIcon(Hand))
                }

                Text(" 语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = frqZeroFilter,
                onCheckedChange = { changeFrqZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                AnimatedVisibility(visible = filter) {
                    Text("过滤 ", color = MaterialTheme.colors.onBackground)
                }
                AnimatedVisibility(visible = include) {
                    Text("包含 ", color = MaterialTheme.colors.onBackground)
                }
                Text("所有数字 ", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = numberFilter,
                onCheckedChange = { changeNumberFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "词形还原，例如：\ndid、done、doing、does 全部替换为 do",
                fontFamily = FontFamily.Default,
                color = textColor,
                modifier = Modifier.width(textWidth)
            )
            Checkbox(
                checked = replaceToLemma,
                enabled = filter,
                onCheckedChange = { setReplaceToLemma(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
    }

}

