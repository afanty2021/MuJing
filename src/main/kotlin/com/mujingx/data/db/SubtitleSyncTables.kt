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

package com.mujingx.data.db

import java.sql.Connection

/**
 * 字幕同步数据库表定义
 *
 * 管理字幕时间偏移的数据库操作，包括全局偏移和单条字幕偏移。
 */
object SubtitleSyncTables {

    /**
     * 创建字幕同步相关表
     *
     * 创建两个表：
     * 1. subtitle_global_offset - 全局时间偏移表
     * 2. subtitle_caption_offset - 单条字幕时间偏移表
     *
     * @param connection 数据库连接
     */
    fun createTables(connection: Connection) {
        createGlobalOffsetTable(connection)
        createCaptionOffsetTable(connection)
    }

    /**
     * 创建全局时间偏移表
     *
     * 表结构：
     * - video_path: 视频文件路径（主键）
     * - offset_ms: 全局偏移量（毫秒），默认为 0
     * - updated_at: 更新时间戳
     *
     * @param connection 数据库连接
     */
    fun createGlobalOffsetTable(connection: Connection) {
        val sql = """
            CREATE TABLE IF NOT EXISTS subtitle_global_offset (
                video_path TEXT PRIMARY KEY,
                offset_ms INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

    /**
     * 创建单条字幕时间偏移表
     *
     * 表结构：
     * - video_path: 视频文件路径
     * - subtitle_file_hash: 字幕文件哈希（用于区分不同字幕版本）
     * - caption_index: 字幕索引
     * - offset_ms: 偏移量（毫秒），默认为 0
     *
     * 复合主键：(video_path, subtitle_file_hash, caption_index)
     *
     * @param connection 数据库连接
     */
    fun createCaptionOffsetTable(connection: Connection) {
        val sql = """
            CREATE TABLE IF NOT EXISTS subtitle_caption_offset (
                video_path TEXT NOT NULL,
                subtitle_file_hash TEXT NOT NULL,
                caption_index INTEGER NOT NULL,
                offset_ms INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (video_path, subtitle_file_hash, caption_index)
            )
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }

        // 创建索引以提升查询性能
        val indexSql = """
            CREATE INDEX IF NOT EXISTS idx_subtitle_caption_offset_video
            ON subtitle_caption_offset(video_path)
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.execute(indexSql)
        }
    }

    /**
     * 查询全局偏移量
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @return Long 偏移量（毫秒），如果不存在则返回 0
     */
    fun getGlobalOffset(connection: Connection, videoPath: String): Long {
        val sql = """
            SELECT offset_ms
            FROM subtitle_global_offset
            WHERE video_path = ?
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getLong("offset_ms")
                }
            }
        }

        return 0L
    }

    /**
     * 设置全局偏移量
     *
     * 使用 INSERT OR REPLACE 策略，如果主键冲突则替换现有记录。
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @param offsetMs 偏移量（毫秒）
     */
    fun setGlobalOffset(connection: Connection, videoPath: String, offsetMs: Long) {
        val sql = """
            INSERT OR REPLACE INTO subtitle_global_offset (video_path, offset_ms, updated_at)
            VALUES (?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setLong(2, offsetMs)
            stmt.setLong(3, System.currentTimeMillis())

            stmt.executeUpdate()
        }
    }

    /**
     * 查询指定字幕的偏移量
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @param subtitleFileHash 字幕文件哈希
     * @param captionIndex 字幕索引
     * @return Long 偏移量（毫秒），如果不存在则返回 0
     */
    fun getCaptionOffset(
        connection: Connection,
        videoPath: String,
        subtitleFileHash: String,
        captionIndex: Int
    ): Long {
        val sql = """
            SELECT offset_ms
            FROM subtitle_caption_offset
            WHERE video_path = ? AND subtitle_file_hash = ? AND caption_index = ?
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setString(2, subtitleFileHash)
            stmt.setInt(3, captionIndex)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getLong("offset_ms")
                }
            }
        }

        return 0L
    }

    /**
     * 批量查询视频的所有字幕偏移量
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @param subtitleFileHash 字幕文件哈希
     * @return Map<Int, Long> 字幕索引到偏移量的映射
     */
    fun getCaptionOffsets(
        connection: Connection,
        videoPath: String,
        subtitleFileHash: String
    ): Map<Int, Long> {
        val sql = """
            SELECT caption_index, offset_ms
            FROM subtitle_caption_offset
            WHERE video_path = ? AND subtitle_file_hash = ?
        """.trimIndent()

        val result = mutableMapOf<Int, Long>()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setString(2, subtitleFileHash)

            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val index = rs.getInt("caption_index")
                    val offset = rs.getLong("offset_ms")
                    result[index] = offset
                }
            }
        }

        return result
    }

    /**
     * 设置指定字幕的偏移量
     *
     * 使用 INSERT OR REPLACE 策略，如果复合主键冲突则替换现有记录。
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @param subtitleFileHash 字幕文件哈希
     * @param captionIndex 字幕索引
     * @param offsetMs 偏移量（毫秒）
     */
    fun setCaptionOffset(
        connection: Connection,
        videoPath: String,
        subtitleFileHash: String,
        captionIndex: Int,
        offsetMs: Long
    ) {
        val sql = """
            INSERT OR REPLACE INTO subtitle_caption_offset
            (video_path, subtitle_file_hash, caption_index, offset_ms)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setString(2, subtitleFileHash)
            stmt.setInt(3, captionIndex)
            stmt.setLong(4, offsetMs)

            stmt.executeUpdate()
        }
    }

    /**
     * 清除指定字幕的偏移量
     *
     * @param connection 数据库连接
     * @param videoPath 视频文件路径
     * @param subtitleFileHash 字幕文件哈希
     * @param captionIndex 字幕索引
     */
    fun clearCaptionOffset(
        connection: Connection,
        videoPath: String,
        subtitleFileHash: String,
        captionIndex: Int
    ) {
        val sql = """
            DELETE FROM subtitle_caption_offset
            WHERE video_path = ? AND subtitle_file_hash = ? AND caption_index = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setString(2, subtitleFileHash)
            stmt.setInt(3, captionIndex)

            stmt.executeUpdate()
        }
    }
}
