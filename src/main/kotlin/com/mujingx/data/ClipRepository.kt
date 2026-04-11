/*
 * MuJing: 幕境 - Contextual Vocabulary Learning
 * Copyright (C) 2025 MuJing Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mujingx.data

import com.mujingx.data.db.ClipTables
import java.sql.Connection
import java.sql.SQLException

/**
 * Clip 数据仓库
 *
 * 提供视频片段（Clip）的持久化存储功能，使用 SQLite 数据库。
 * 支持片段的保存、查询和删除操作。
 *
 * @property dbPath 数据库文件路径
 */
class ClipRepository(
    private val dbPath: String
) {
    /**
     * 懒加载的数据库连接
     *
     * 首次访问时初始化数据库表结构，后续访问复用同一连接。
     */
    private val connection: Connection by lazy {
        val conn = java.sql.DriverManager.getConnection("jdbc:sqlite:$dbPath")
        // 初始化数据库表
        ClipTables.createTables(conn)
        // 创建默认集合
        ensureDefaultCollection(conn)
        conn
    }

    /**
     * 确保默认集合存在
     *
     * 如果不存在默认集合，则创建一个。
     */
    private fun ensureDefaultCollection(conn: Connection) {
        val checkStmt = conn.prepareStatement(
            "SELECT id FROM clip_collections WHERE id = ?"
        )
        checkStmt.setString(1, DEFAULT_COLLECTION_ID)
        val rs = checkStmt.executeQuery()

        if (!rs.next()) {
            // 创建默认集合
            val insertStmt = conn.prepareStatement(
                "INSERT INTO clip_collections (id, name, created_at) VALUES (?, ?, ?)"
            )
            insertStmt.setString(1, DEFAULT_COLLECTION_ID)
            insertStmt.setString(2, "Default Collection")
            insertStmt.setLong(3, System.currentTimeMillis())
            insertStmt.executeUpdate()
            insertStmt.close()
        }

        rs.close()
        checkStmt.close()
    }

    companion object {
        private const val DEFAULT_COLLECTION_ID = "default-collection"
    }

    /**
     * 保存或更新片段
     *
     * 使用 INSERT OR REPLACE 策略，如果已存在相同 id 的记录，
     * 则更新所有字段。片段将被添加到默认集合中。
     *
     * @param clip 要保存的片段对象
     * @throws SQLException 如果数据库操作失败
     */
    fun saveClip(clip: Clip) {
        try {
            val preparedStatement = connection.prepareStatement(
                """
                INSERT OR REPLACE INTO clips (
                    id, video_path, start_time, end_time, subtitle_text,
                    translated_text, note, video_clip_path, collection_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )

            preparedStatement.setString(1, clip.id)
            preparedStatement.setString(2, clip.videoPath)
            preparedStatement.setLong(3, clip.startTime)
            preparedStatement.setLong(4, clip.endTime)
            preparedStatement.setString(5, clip.subtitleText)
            preparedStatement.setString(6, clip.translatedText)
            preparedStatement.setString(7, clip.note)
            preparedStatement.setString(8, clip.videoClipPath)
            preparedStatement.setString(9, DEFAULT_COLLECTION_ID)
            preparedStatement.setLong(10, clip.createdAt)

            preparedStatement.executeUpdate()
            preparedStatement.close()
        } catch (e: SQLException) {
            throw SQLException("Failed to save clip with id: ${clip.id}", e)
        }
    }

    /**
     * 根据视频路径查询所有片段
     *
     * @param videoPath 视频文件路径
     * @return List<Clip> 按开始时间排序的片段列表
     * @throws SQLException 如果数据库操作失败
     */
    fun getClipsByVideo(videoPath: String): List<Clip> {
        return try {
            val preparedStatement = connection.prepareStatement(
                """
                SELECT id, video_path, start_time, end_time, subtitle_text,
                       translated_text, note, video_clip_path, created_at
                FROM clips
                WHERE video_path = ?
                ORDER BY start_time
                """.trimIndent()
            )

            preparedStatement.setString(1, videoPath)
            val resultSet = preparedStatement.executeQuery()

            val clips = mutableListOf<Clip>()
            while (resultSet.next()) {
                clips.add(
                    Clip(
                        id = resultSet.getString("id"),
                        videoPath = resultSet.getString("video_path"),
                        startTime = resultSet.getLong("start_time"),
                        endTime = resultSet.getLong("end_time"),
                        subtitleText = resultSet.getString("subtitle_text"),
                        translatedText = resultSet.getString("translated_text"),
                        note = resultSet.getString("note"),
                        tags = mutableListOf(), // Tags are stored in a separate table
                        createdAt = resultSet.getLong("created_at"),
                        videoClipPath = resultSet.getString("video_clip_path")
                    )
                )
            }

            resultSet.close()
            preparedStatement.close()
            clips
        } catch (e: SQLException) {
            throw SQLException("Failed to get clips for video: $videoPath", e)
        }
    }

    /**
     * 根据ID删除片段
     *
     * @param id 片段的唯一标识符
     * @throws SQLException 如果数据库操作失败
     */
    fun deleteClip(id: String) {
        try {
            val preparedStatement = connection.prepareStatement(
                "DELETE FROM clips WHERE id = ?"
            )

            preparedStatement.setString(1, id)
            preparedStatement.executeUpdate()
            preparedStatement.close()
        } catch (e: SQLException) {
            throw SQLException("Failed to delete clip with id: $id", e)
        }
    }

    /**
     * 关闭数据库连接
     *
     * 应在应用退出或不再需要仓库时调用，以释放资源。
     */
    fun close() {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: Exception) {
            System.err.println("ClipRepository close error: ${e.message}")
        }
    }
}
