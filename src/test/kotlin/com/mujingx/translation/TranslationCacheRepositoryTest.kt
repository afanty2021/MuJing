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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * TranslationCacheRepository 测试
 *
 * 测试翻译缓存仓库的基本功能：
 * - 查询不存在的缓存返回 null
 * - 写入和查询缓存
 * - 覆盖已存在的缓存
 * - 不同语言对的缓存隔离
 */
class TranslationCacheRepositoryTest {

    private lateinit var repository: TranslationCacheRepository
    private lateinit var tempDbFile: File

    /**
     * 设置测试环境
     *
     * 创建临时 SQLite 数据库文件用于测试。
     */
    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        // 创建临时数据库文件
        tempDbFile = File.createTempFile("translation_cache_test_", ".db")
        repository = TranslationCacheRepository(tempDbFile.absolutePath)
    }

    /**
     * 清理测试环境
     *
     * 关闭数据库连接并删除临时文件。
     */
    @AfterEach
    fun cleanup() {
        repository.close()
        if (tempDbFile.exists()) {
            tempDbFile.delete()
        }
    }

    @Test
    fun `get returns null for non-cached text`() {
        // Arrange & Act
        val result = repository.get("Hello", "en", "zh")

        // Assert
        assertNull(result, "查询不存在的缓存应该返回 null")
    }

    @Test
    fun `put and get returns cached translation`() = runTest {
        // Arrange
        val text = "Hello"
        val from = "en"
        val to = "zh"
        val result = "你好"
        val provider = "TestProvider"

        // Act
        repository.put(text, from, to, result, provider)
        val cached = repository.get(text, from, to)

        // Assert
        assertEquals(result, cached, "缓存应该返回正确的翻译结果")
    }

    @Test
    fun `put overwrites existing cache`() = runTest {
        // Arrange
        val text = "Hello"
        val from = "en"
        val to = "zh"
        val firstResult = "你好"
        val secondResult = "您好"
        val provider = "TestProvider"

        // Act
        repository.put(text, from, to, firstResult, provider)
        repository.put(text, from, to, secondResult, provider)
        val cached = repository.get(text, from, to)

        // Assert
        assertEquals(secondResult, cached, "第二次写入应该覆盖第一次的结果")
    }

    @Test
    fun `different language pairs are cached separately`() = runTest {
        // Arrange
        val text = "Hello"
        val enToZhResult = "你好"
        val enToJaResult = "こんにちは"
        val provider = "TestProvider"

        // Act
        repository.put(text, "en", "zh", enToZhResult, provider)
        repository.put(text, "en", "ja", enToJaResult, provider)

        val enToZhCached = repository.get(text, "en", "zh")
        val enToJaCached = repository.get(text, "en", "ja")

        // Assert
        assertEquals(enToZhResult, enToZhCached, "英译中缓存应该独立")
        assertEquals(enToJaResult, enToJaCached, "英译日缓存应该独立")
    }
}
