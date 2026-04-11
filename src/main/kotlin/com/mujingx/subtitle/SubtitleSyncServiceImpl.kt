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

import com.mujingx.data.db.SubtitleSyncTables
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ConcurrentHashMap

/**
 * 字幕同步服务实现
 *
 * 提供字幕时间偏移的持久化存储和查询功能，使用内存缓存提升性能。
 *
 * @param dbPath 数据库文件路径
 */
class SubtitleSyncServiceImpl(
    private val dbPath: String
) : SubtitleSyncService {

    // 内存缓存
    private val globalOffsetCache = ConcurrentHashMap<String, Long>()
    private val captionOffsetCache = ConcurrentHashMap<String, MutableMap<Int, Long>>()

    // 懒加载的数据库连接
    private val connection: Connection by lazy {
        initDatabase()
        DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    /**
     * 初始化数据库
     *
     * 确保数据库目录存在，并创建必要的表。
     */
    private fun initDatabase() {
        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()

        // 创建临时连接用于初始化表
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            SubtitleSyncTables.createTables(conn)
        }
    }

    /**
     * 获取视频的全局时间偏移量（毫秒）
     *
     * 先查询内存缓存，如果缓存未命中则从数据库加载。
     *
     * @param videoPath 视频文件路径
     * @return Long 偏移量（毫秒），默认为 0
     */
    override fun getGlobalOffset(videoPath: String): Long {
        return globalOffsetCache.getOrPut(videoPath) {
            SubtitleSyncTables.getGlobalOffset(connection, videoPath)
        }
    }

    /**
     * 设置视频的全局时间偏移量（毫秒）
     *
     * 同时更新内存缓存和数据库。
     *
     * @param videoPath 视频文件路径
     * @param offsetMs 偏移量（毫秒）
     */
    override fun setGlobalOffset(videoPath: String, offsetMs: Long) {
        globalOffsetCache[videoPath] = offsetMs
        SubtitleSyncTables.setGlobalOffset(connection, videoPath, offsetMs)
    }

    /**
     * 获取指定字幕的时间偏移量（毫秒）
     *
     * 从内存缓存获取，如果缓存未命中则批量加载该视频的所有字幕偏移。
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     * @return Long 偏移量（毫秒），默认为 0
     */
    override fun getCaptionOffset(videoPath: String, captionIndex: Int): Long {
        val cacheKey = videoPath
        val videoCache = captionOffsetCache.getOrPut(cacheKey) { mutableMapOf() }

        return videoCache.getOrPut(captionIndex) {
            // 缓存未命中，批量加载该视频的所有字幕偏移
            loadCaptionOffsets(videoPath)
            videoCache.getOrDefault(captionIndex, 0L)
        }
    }

    /**
     * 设置指定字幕的时间偏移量（毫秒）
     *
     * 同时更新内存缓存和数据库。
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     * @param offsetMs 偏移量（毫秒）
     */
    override fun setCaptionOffset(videoPath: String, captionIndex: Int, offsetMs: Long) {
        val cacheKey = videoPath
        val videoCache = captionOffsetCache.getOrPut(cacheKey) { mutableMapOf() }
        videoCache[captionIndex] = offsetMs

        // 使用简单的哈希作为 subtitle_file_hash
        val subtitleFileHash = videoPath.hashCode().toString()
        SubtitleSyncTables.setCaptionOffset(connection, videoPath, subtitleFileHash, captionIndex, offsetMs)
    }

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
    override fun getEffectiveTime(videoPath: String, captionIndex: Int, originalStart: Long): Long {
        val globalOffset = getGlobalOffset(videoPath)
        val captionOffset = getCaptionOffset(videoPath, captionIndex)
        return originalStart + globalOffset + captionOffset
    }

    /**
     * 清除指定字幕的时间偏移量
     *
     * 同时从内存缓存移除和数据库删除。
     *
     * @param videoPath 视频文件路径
     * @param captionIndex 字幕索引
     */
    override fun clearCaptionOffset(videoPath: String, captionIndex: Int) {
        val cacheKey = videoPath
        captionOffsetCache[cacheKey]?.remove(captionIndex)

        val subtitleFileHash = videoPath.hashCode().toString()
        SubtitleSyncTables.clearCaptionOffset(connection, videoPath, subtitleFileHash, captionIndex)
    }

    /**
     * 批量加载视频的所有字幕偏移量
     *
     * 从数据库加载并更新内存缓存。
     *
     * @param videoPath 视频文件路径
     */
    private fun loadCaptionOffsets(videoPath: String) {
        val subtitleFileHash = videoPath.hashCode().toString()
        val offsets = SubtitleSyncTables.getCaptionOffsets(connection, videoPath, subtitleFileHash)

        val cacheKey = videoPath
        val videoCache = captionOffsetCache.getOrPut(cacheKey) { mutableMapOf() }
        videoCache.putAll(offsets)
    }
}
