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

package com.mujingx.translation

import com.mujingx.data.db.translationCacheTable
import java.sql.Connection

/**
 * 翻译缓存仓库
 *
 * 提供翻译结果的持久化缓存功能，使用 SQLite 数据库存储。
 * 支持根据源文本、源语言和目标语言的组合查询和更新缓存。
 *
 * @property dbPath 数据库文件路径
 */
class TranslationCacheRepository(
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
        translationCacheTable.createTable(conn)
        conn
    }

    /**
     * 从缓存获取翻译结果
     *
     * @param text 源文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @return String? 缓存的翻译结果，如果不存在则返回 null
     */
    fun get(text: String, from: String, to: String): String? {
        return try {
            translationCacheTable.get(connection, text, from, to)
        } catch (e: Exception) {
            // 记录错误但不抛出异常，允许降级到无缓存模式
            System.err.println("TranslationCache get error: ${e.message}")
            null
        }
    }

    /**
     * 将翻译结果写入缓存
     *
     * 使用 INSERT OR REPLACE 策略，如果已存在相同 (text, from, to) 的记录，
     * 则更新翻译结果和提供商信息。
     *
     * @param text 源文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @param result 翻译结果
     * @param provider 翻译提供商名称
     */
    fun put(text: String, from: String, to: String, result: String, provider: String) {
        try {
            translationCacheTable.insertOrReplace(connection, text, from, to, result, provider)
        } catch (e: Exception) {
            // 记录错误但不抛出异常，允许降级到无缓存模式
            System.err.println("TranslationCache put error: ${e.message}")
        }
    }

    /**
     * 关闭数据库连接
     *
     * 应在应用退出或不再需要缓存时调用，以释放资源。
     */
    fun close() {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: Exception) {
            System.err.println("TranslationCache close error: ${e.message}")
        }
    }
}
