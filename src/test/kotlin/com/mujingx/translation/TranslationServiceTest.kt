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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * TranslationService 测试
 *
 * 测试 MultiProviderTranslationService 的基本行为：
 * - 使用第一个已配置的提供商
 * - 第一个提供商失败时自动 fallback
 * - 批量翻译功能
 *
 * 注意：MultiProviderTranslationService 在 Task 6 中实现，
 * 因此此测试在当前阶段预期会编译失败。
 */
class TranslationServiceTest {

    /**
     * Mock 翻译提供商，用于测试
     */
    class MockProvider(
        override val name: String,
        private val result: String,
        private val configured: Boolean = true,
        private val shouldFail: Boolean = false
    ) : TranslationProvider {
        override suspend fun translate(text: String, from: String, to: String): String {
            if (shouldFail) {
                throw RuntimeException("Mock provider failure: $name")
            }
            return result
        }

        override fun isConfigured(): Boolean = configured
    }

    @Test
    fun `translate should return first configured provider result`() = runTest {
        // Arrange
        val provider1 = MockProvider("Provider1", "Translation 1", configured = true)
        val provider2 = MockProvider("Provider2", "Translation 2", configured = true)
        val service = MultiProviderTranslationService(listOf(provider1, provider2))

        // Act
        val result = service.translate("Hello", "en", "zh")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals("Translation 1", result.getOrNull())
    }

    @Test
    fun `translate should fallback when first provider fails`() = runTest {
        // Arrange
        val provider1 = MockProvider(
            name = "FailingProvider",
            result = "",
            configured = true,
            shouldFail = true
        )
        val provider2 = MockProvider("FallbackProvider", "Fallback Translation", configured = true)
        val service = MultiProviderTranslationService(listOf(provider1, provider2))

        // Act
        val result = service.translate("Hello", "en", "zh")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals("Fallback Translation", result.getOrNull())
    }

    @Test
    fun `translateBatch should translate all texts`() = runTest {
        // Arrange
        val provider = MockProvider("BatchProvider", "Translated", configured = true)
        val service = MultiProviderTranslationService(listOf(provider))
        val texts = listOf("Hello", "World", "Test")

        // Act
        val result = service.translateBatch(texts, "en", "zh")

        // Assert
        assertTrue(result.isSuccess)
        val translations = result.getOrNull()
        assertEquals(3, translations?.size)
        assertEquals(listOf("Translated", "Translated", "Translated"), translations)
    }
}
