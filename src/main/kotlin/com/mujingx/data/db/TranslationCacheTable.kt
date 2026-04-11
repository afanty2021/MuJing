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

package com.mujingx.data.db

import java.sql.Connection

/**
 * 翻译缓存数据库表
 *
 * 管理翻译缓存的数据库操作，包括创建表、查询和插入/更新记录。
 */
object TranslationCacheTable {

    /**
     * 创建翻译缓存表
     *
     * 表结构：
     * - id: 主键，自增
     * - source_text: 源文本
     * - source_lang: 源语言代码
     * - target_lang: 目标语言代码
     * - translated_text: 翻译结果
     * - provider: 翻译提供商名称
     * - content_hash: 内容哈希（用于快速比对）
     * - created_at: 创建时间戳
     *
     * 唯一约束：(source_text, source_lang, target_lang)
     *
     * @param conn 数据库连接
     */
    fun createTable(conn: Connection) {
        val sql = """
            CREATE TABLE IF NOT EXISTS translation_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_text TEXT NOT NULL,
                source_lang TEXT NOT NULL,
                target_lang TEXT NOT NULL,
                translated_text TEXT NOT NULL,
                provider TEXT NOT NULL,
                content_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(source_text, source_lang, target_lang)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            stmt.execute(sql)
        }

        // 创建索引以提升查询性能
        val indexSql = """
            CREATE INDEX IF NOT EXISTS idx_translation_cache_lookup
            ON translation_cache(source_text, source_lang, target_lang)
        """.trimIndent()

        conn.createStatement().use { stmt ->
            stmt.execute(indexSql)
        }
    }

    /**
     * 查询缓存记录
     *
     * @param conn 数据库连接
     * @param text 源文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @return String? 缓存的翻译结果，如果不存在则返回 null
     */
    fun get(conn: Connection, text: String, from: String, to: String): String? {
        val sql = """
            SELECT translated_text
            FROM translation_cache
            WHERE source_text = ? AND source_lang = ? AND target_lang = ?
            LIMIT 1
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, text)
            stmt.setString(2, from)
            stmt.setString(3, to)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getString("translated_text")
                }
            }
        }

        return null
    }

    /**
     * 插入或替换缓存记录
     *
     * 使用 INSERT OR REPLACE 策略，如果唯一约束冲突则替换现有记录。
     * 自动计算内容哈希和创建时间戳。
     *
     * @param conn 数据库连接
     * @param text 源文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @param result 翻译结果
     * @param provider 翻译提供商名称
     */
    fun insertOrReplace(
        conn: Connection,
        text: String,
        from: String,
        to: String,
        result: String,
        provider: String
    ) {
        val contentHash = calculateContentHash(text, from, to)
        val createdAt = System.currentTimeMillis()

        val sql = """
            INSERT OR REPLACE INTO translation_cache
            (source_text, source_lang, target_lang, translated_text, provider, content_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, text)
            stmt.setString(2, from)
            stmt.setString(3, to)
            stmt.setString(4, result)
            stmt.setString(5, provider)
            stmt.setString(6, contentHash)
            stmt.setLong(7, createdAt)

            stmt.executeUpdate()
        }
    }

    /**
     * 计算内容哈希
     *
     * 使用简单的字符串拼接哈希，用于快速比对内容是否变化。
     *
     * @param text 源文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @return String 内容哈希值
     */
    private fun calculateContentHash(text: String, from: String, to: String): String {
        val input = "$text|$from|$to"
        // 使用简单的哈希算法
        return input.hashCode().toString()
    }
}

// 类型别名，保持与项目命名风格一致
val translationCacheTable = TranslationCacheTable
