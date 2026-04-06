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

fun filterWords(
    inputWords: List<Word>,
    numberFilter: Boolean,
    bncNum: Int,
    bncNumFilter: Boolean,
    frqNum: Int,
    frqNumFilter: Boolean,
    bncZeroFilter: Boolean,
    frqZeroFilter: Boolean,
    replaceToLemma: Boolean,
    isBatchMKV: Boolean
): List<Word> {
    val resultList = ArrayList(inputWords)

    /**
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word, String>()

    /** 原型词 > 内部字幕列表 映射 */
    val captionsMap = HashMap<String, MutableList<Caption>>()

    /** 原型词 -> 外部字幕列表映射,批量生成 MKV 词库时，字幕保存在单词的外部字幕列表 */
    val externalCaptionsMap = HashMap<String, MutableList<ExternalCaption>>()

    /** 原型词 -> 例句列表映射 */
    val sentencesMap = HashMap<String, MutableList<String>>()

    inputWords.forEach { word ->

        if (numberFilter && (word.value.toDoubleOrNull() != null)) {
            // 过滤数字
            resultList.remove(word)
        } else if (bncNumFilter && (word.bnc!! in 1 until bncNum)) {
            // 过滤最常见的词
            resultList.remove(word)
        } else if (frqNumFilter && (word.frq!! in 1 until frqNum)) {
            // 过滤最常见的词
            resultList.remove(word)
        } else if (bncZeroFilter && word.bnc == 0) {
            // 过滤 BNC 词频为 0 的词
            resultList.remove(word)
        } else if (frqZeroFilter && word.frq == 0) {
            // 过滤 COCA 词频为 0 的词
            resultList.remove(word)
        }


        if (replaceToLemma) {
            val lemma = getWordLemma(word)
            if (lemma.isNotEmpty()) {
                lemmaMap[word] = lemma
                // 处理内部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                if (!isBatchMKV) {
                    if (captionsMap[lemma].isNullOrEmpty()) {
                        captionsMap[lemma] = word.captions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<Caption>()
                        list.addAll(captionsMap[lemma]!!)
                        for (caption in word.captions) {
                            if (list.size < 3) {
                                list.add(caption)
                            }
                        }
                        captionsMap[lemma] = list
                    }
                    // 处理外部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                } else {
                    if (externalCaptionsMap[lemma].isNullOrEmpty()) {
                        externalCaptionsMap[lemma] = word.externalCaptions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<ExternalCaption>()
                        list.addAll(externalCaptionsMap[lemma]!!)
                        for (externalCaption in word.externalCaptions) {
                            if (list.size < 3) {
                                list.add(externalCaption)
                            }
                        }
                        externalCaptionsMap[lemma] = list
                    }
                }

                // 处理例句,sentencesMap 最多只保留 3 个例句
                if (sentencesMap[lemma].isNullOrEmpty()) {
                    sentencesMap[lemma] = word.pos.split("\n").toMutableList()
                } else {
                    word.pos.split("\n").forEach {
                        if (sentencesMap[lemma]!!.size < 3) {
                            sentencesMap[lemma]!!.add(it)
                        }
                    }
                }

            }
        }
    }

    //替换原型需要特殊处理
    if (replaceToLemma) {
        // 查询单词原型
        val queryList = lemmaMap.values.toList()
        val lemmaList = Dictionary.queryList(queryList)
        val validLemmaMap = HashMap<String, Word>()
        lemmaList.forEach { lemmaWord ->
            // 处理内部字幕
            if (!isBatchMKV) {
                val captions = captionsMap[lemmaWord.value]!!
                lemmaWord.captions = captions
                // 处理外部字幕
            } else {
                val externalCaptions = externalCaptionsMap[lemmaWord.value]!!
                lemmaWord.externalCaptions = externalCaptions
            }
            // 处理例句
            val sentences = sentencesMap[lemmaWord.value]!!
            if(sentences.isNotEmpty()) {
                lemmaWord.pos = sentences.joinToString("\n")
            }
            validLemmaMap[lemmaWord.value] = lemmaWord
        }

        val toLemmaList = lemmaMap.keys
        for (word in toLemmaList) {
            val index = resultList.indexOf(word)
            // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
            if (index != -1) {
                val lemmaStr = lemmaMap[word]
                val validLemma = validLemmaMap[lemmaStr]
                if (validLemma != null) {
                    resultList.remove(word)
                    if (!resultList.contains(validLemma)) {
                        // 默认 add 为真
                        var add = true
                        // 但是，如果单词的词频为 0 或者是最常见的单词就不添加
                        if (bncNumFilter && (validLemma.bnc!! in 1 until bncNum)) {
                            add = false
                        } else if (frqNumFilter && (validLemma.frq!! in 1 until frqNum)) {
                            add = false
                        } else if (bncZeroFilter && validLemma.bnc == 0) {
                            add = false
                        } else if (frqZeroFilter && validLemma.frq == 0) {
                            add = false
                        }

                        if (add) {
                            resultList.add(index, validLemma)
                        }
                    }
                }

            }

        }
    }

    return resultList
}

fun includeWords(
    inputWords: List<Word>,
    numberFilter: Boolean,
    bncNum: Int,
    bncNumFilter: Boolean,
    frqNum: Int,
    frqNumFilter: Boolean,
    bncZeroFilter: Boolean,
    frqZeroFilter: Boolean,
    replaceToLemma: Boolean,
    isBatchMKV: Boolean
): List<Word> {
    val resultList = ArrayList<Word>()

    /**
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word, String>()

    /** 原型词 > 内部字幕列表 映射 */
    val captionsMap = HashMap<String, MutableList<Caption>>()

    /** 原型词 -> 外部字幕列表映射,批量生成 MKV 词库时，字幕保存在单词的外部字幕列表 */
    val externalCaptionsMap = HashMap<String, MutableList<ExternalCaption>>()

    inputWords.forEach { word ->

        if (numberFilter && (word.value.toDoubleOrNull() != null)) {
            // 包含数字
            resultList.add(word)
        } else if (bncNumFilter && (word.bnc!! in 1 until bncNum)) {
            // 包含最常见的词
            resultList.add(word)
        } else if (frqNumFilter && (word.frq!! in 1 until frqNum)) {
            // 包含最常见的词
            resultList.add(word)
        } else if (bncZeroFilter && word.bnc == 0) {
            // 包含 BNC 词频为 0 的词
            resultList.add(word)
        } else if (frqZeroFilter && word.frq == 0) {
            // 包含 COCA 词频为 0 的词
            resultList.add(word)
        }


    }

    if (replaceToLemma) {
        resultList.forEach { word ->

            val lemma = getWordLemma(word)
            if (lemma.isNotEmpty()) {
                lemmaMap[word] = lemma
                // 处理内部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                if (!isBatchMKV) {
                    if (captionsMap[lemma].isNullOrEmpty()) {
                        captionsMap[lemma] = word.captions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<Caption>()
                        list.addAll(captionsMap[lemma]!!)
                        for (caption in word.captions) {
                            if (list.size < 3) {
                                list.add(caption)
                            }
                        }
                        captionsMap[lemma] = list
                    }
                    // 处理外部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
                } else {
                    if (externalCaptionsMap[lemma].isNullOrEmpty()) {
                        externalCaptionsMap[lemma] = word.externalCaptions
                    } else {
                        // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                        val list = mutableListOf<ExternalCaption>()
                        list.addAll(externalCaptionsMap[lemma]!!)
                        for (externalCaption in word.externalCaptions) {
                            if (list.size < 3) {
                                list.add(externalCaption)
                            }
                        }
                        externalCaptionsMap[lemma] = list
                    }
                }
            }

        }


        // 查询单词原型
        val queryList = lemmaMap.values.toList()
        val lemmaList = Dictionary.queryList(queryList)
        val validLemmaMap = HashMap<String, Word>()
        lemmaList.forEach { word ->
            // 处理内部字幕
            if (!isBatchMKV) {
                val captions = captionsMap[word.value]!!
                word.captions = captions
                // 处理外部字幕
            } else {
                val externalCaptions = externalCaptionsMap[word.value]!!
                word.externalCaptions = externalCaptions
            }
            validLemmaMap[word.value] = word
        }

        val toLemmaList = lemmaMap.keys
        for (word in toLemmaList) {
            val index = resultList.indexOf(word)
            // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
            if (index != -1) {
                val lemmaStr = lemmaMap[word]
                val validLemma = validLemmaMap[lemmaStr]
                if (validLemma != null) {
                    resultList.remove(word)
                    if (!resultList.contains(validLemma)) {
                        // 默认 add 为真
                        var add = true
                        // 但是，如果单词的词频为 0 或者是最常见的单词就不添加
                        if (bncNumFilter && (validLemma.bnc!! in 1 until bncNum)) {
                            add = false
                        } else if (frqNumFilter && (validLemma.frq!! in 1 until frqNum)) {
                            add = false
                        } else if (bncZeroFilter && validLemma.bnc == 0) {
                            add = false
                        } else if (frqZeroFilter && validLemma.frq == 0) {
                            add = false
                        }

                        if (add) {
                            resultList.add(index, validLemma)
                        }
                    }
                }

            }

        }
    }


    return resultList
}

fun filterSelectVocabulary(
    selectedFileList: List<File>,
    basicFilteredList: List<Word>
): MutableList<Word> {
    val list = ArrayList(basicFilteredList)
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)
            list.removeAll(vocabulary.wordList.toSet())
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }

    }
    return list
}

fun includeSelectVocabulary(
    selectedFileList: List<File>,
    parsedList: List<Word>
): MutableList<Word> {
    val list = ArrayList(parsedList)
    val includeSet = mutableSetOf<Word>()
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)

            includeSet.addAll(vocabulary.wordList)
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }
    }
    // 交集 list 和 includeSet 的交集
    list.retainAll(includeSet)
    return list
}

