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

/**
 * 词型组件
 */
@Composable
fun Morphology(
    word: Word,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    searching: Boolean,
    morphologyVisible: Boolean,
    fontSize: TextUnit
) {
    if (morphologyVisible &&(isChangeVideoBounds || !isPlaying )) {
        val exchanges = word.exchange.split("/")
        var preterite = ""
        var pastParticiple = ""
        var presentParticiple = ""
        var third = ""
        var er = ""
        var est = ""
        var plural = ""
        var lemma = ""

        exchanges.forEach { exchange ->
            val pair = exchange.split(":")
            when (pair[0]) {
                "p" -> {
                    preterite = pair[1]
                }
                "d" -> {
                    pastParticiple = pair[1]
                }
                "i" -> {
                    presentParticiple = pair[1]
                }
                "3" -> {
                    third = pair[1]
                }
                "r" -> {
                    er = pair[1]
                }
                "t" -> {
                    est = pair[1]
                }
                "s" -> {
                    plural = pair[1]
                }
                "0" -> {
                    lemma = pair[1]
                }

            }
        }

        Column {
            SelectionContainer {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.height(IntrinsicSize.Max)
                        .width(if(searching) 600.dp else 554.dp)
                        .padding(start = if(searching) 0.dp else 50.dp)

                ) {
                    val textColor = MaterialTheme.colors.onBackground
                    val plainStyle = SpanStyle(
                        color = textColor,
                        fontSize = fontSize,
                    )


                    Text(
                        buildAnnotatedString {
                            if (lemma.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("原型 ")
                                }
                                withStyle(style = plainStyle.copy(color = Color.Magenta)) {
                                    append(lemma)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (preterite.isNotEmpty()) {
                                var color = textColor
                                if (!preterite.endsWith("ed")) {
                                    color = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

                                }
                                withStyle(style = plainStyle) {
                                    append("过去式 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(preterite)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (pastParticiple.isNotEmpty()) {
                                var color = textColor
                                if (!pastParticiple.endsWith("ed")) {
                                    color =
                                        if (MaterialTheme.colors.isLight) MaterialTheme.colors.primary else Color.Yellow
                                }
                                withStyle(style = plainStyle) {
                                    append("过去分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(pastParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (presentParticiple.isNotEmpty()) {
                                val color = if (presentParticiple.endsWith("ing")) textColor else Color(0xFF303F9F)
                                withStyle(style = plainStyle) {
                                    append("现在分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(presentParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (third.isNotEmpty()) {
                                val color = if (third.endsWith("s")) textColor else Color.Cyan
                                withStyle(style = plainStyle) {
                                    append("第三人称单数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(third)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }

                            if (er.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("比较级 $er;")
                                }
                            }
                            if (est.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("最高级 $est;")
                                }
                            }
                            if (plural.isNotEmpty()) {
                                val color = if (plural.endsWith("s")) textColor else Color(0xFFD84315)
                                withStyle(style = plainStyle) {
                                    append("复数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(plural)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                        }
                    )

                }
            }
            if(!searching){
                Divider(Modifier.padding(start = 50.dp))
            }
        }


    }

}


/**
 * 英语定义组件
 */
@Composable
fun Definition(
    word: Word,
    definitionVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    fontSize: TextUnit
) {
    if (definitionVisible && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.definition.length - word.definition.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.definition,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * 中文释义组件
 */
@Composable
fun Translation(
    translationVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (translationVisible && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.translation.length - word.translation.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.translation,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}


/**
 * 例句组件
 */
@Composable
fun Sentences(
    sentencesVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (sentencesVisible && word.pos.isNotEmpty() && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.pos.length - word.pos.replace("\n", "").length

        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(180.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 5) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.pos,
                        )
                    }
                }
                if (rows > 5) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }
            Divider(Modifier.padding(start = 50.dp))
        }

    }
}




