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

package com.mujingx.subtitle

/**
 * 字幕同步服务接口
 *
 * 提供字幕时间偏移管理功能，包括全局偏移和单条字幕偏移。
 * 用于解决视频与字幕不同步的问题。
 */
interface SubtitleSyncService {
    /**
     * 获取视频的全局时间偏移量（毫秒）
     *
     * @param videoPath 视频文件路径
     * @return Long 偏移量（毫秒），默认为 0
     */
    fun getGlobalOffset(videoPath: String): Long

    /**
     * 设置视频的全局时间偏移量（毫秒）
     *
     * @param videoPath 视频文件路径
     * @param offsetMs 偏移量（毫秒），正数表示延后，负数表示提前
     */
    fun setGlobalOffset(videoPath: String, offsetMs: Long)

    /**
     * 获取指定字幕的时间偏移量（毫秒）
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     * @return Long 偏移量（毫秒），默认为 0
     */
    fun getCaptionOffset(videoPath: String, captionIndex: Int): Long

    /**
     * 设置指定字幕的时间偏移量（毫秒）
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     * @param offsetMs 偏移量（毫秒），正数表示延后，负数表示提前
     */
    fun setCaptionOffset(videoPath: String, captionIndex: Int, offsetMs: Long)

    /**
     * 计算字幕的有效显示时间
     *
     * 有效时间 = 原始时间 + 全局偏移 + 字幕偏移
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     * @param originalStart 原始开始时间（毫秒）
     * @return Long 有效显示时间（毫秒）
     */
    fun getEffectiveTime(videoPath: String, captionIndex: Int, originalStart: Long): Long

    /**
     * 清除指定字幕的时间偏移量
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     */
    fun clearCaptionOffset(videoPath: String, captionIndex: Int)
}
