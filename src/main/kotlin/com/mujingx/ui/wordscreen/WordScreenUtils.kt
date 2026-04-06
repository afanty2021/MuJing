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

import com.mujingx.data.Caption
import com.mujingx.data.ExternalCaption
import com.mujingx.data.VocabularyType
import com.mujingx.data.Word
import com.mujingx.player.MediaInfo
import com.mujingx.player.isWindows
import java.time.Duration

fun replaceSeparator(path: String): String {
    val absPath = if (isWindows()) {
        path.replace('/', '\\')
    } else {
        path.replace('\\', '/')
    }
    return absPath
}

/**
 * 获取字幕
 * @return Map 的类型参数说明：
 * Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 */
fun getPlayTripleMap(
    vocabularyType: VocabularyType,
    subtitlesTrackId: Int,
    relateVideoPath: String,
    word: Word
): MutableMap<Int, Triple<Caption, String, Int>> {

    val playTripleMap = mutableMapOf<Int, Triple<Caption, String, Int>>()
    if (vocabularyType == VocabularyType.DOCUMENT) {
        if (word.externalCaptions.isNotEmpty()) {
            word.externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val playTriple =
                    Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }
        }
    } else {
        if (word.captions.isNotEmpty()) {
            word.captions.forEachIndexed { index, caption ->
                val playTriple =
                    Triple(caption, relateVideoPath, subtitlesTrackId)
                playTripleMap[index] = playTriple
            }

        }
    }
    return playTripleMap
}


fun getMediaInfo(
    vocabularyType: VocabularyType,
    subtitlesTrackId: Int,
    relateVideoPath: String,
    captions: List<Caption>,
    externalCaptions: List<ExternalCaption>,
): MutableMap<Int, MediaInfo> {

    val mediaInfoMap = mutableMapOf<Int, MediaInfo>()
    if (vocabularyType == VocabularyType.DOCUMENT) {
        if (externalCaptions.isNotEmpty()) {
            externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val mediaInfo = MediaInfo(
                    caption,
                    externalCaption.relateVideoPath,
                    externalCaption.subtitlesTrackId
                )
                mediaInfoMap[index] = mediaInfo

            }
        }
    } else {
        if (captions.isNotEmpty()) {
            captions.forEachIndexed { index, caption ->
                val mediaInfo = MediaInfo(
                    caption,
                    relateVideoPath,
                    subtitlesTrackId
                )
                mediaInfoMap[index] = mediaInfo
            }

        }
    }
    return mediaInfoMap
}

fun secondsToString(seconds: Double): String {
    val duration = Duration.ofMillis((seconds * 1000).toLong())
    return String.format(
        "%02d:%02d:%02d.%03d",
        duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart()
    )
}

/**
 * 从外部字幕中获取媒体信息
 * @param externalCaptions 外部字幕列表
 * @param index links 的 index
 * @return MediaInfo? 如果 index 小于 currentWord.externalCaptions.size，则返回 MediaInfo，否则返回 null
 */
fun getMediaInfoFromExternalCaption(externalCaptions: MutableList<ExternalCaption>, index: Int): MediaInfo? {
    return if (index < externalCaptions.size) {
        val externalCaption = externalCaptions[index]
        val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
        MediaInfo(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
    } else {
        null
    }
}
