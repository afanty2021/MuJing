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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 多提供商翻译服务
 *
 * 实现 TranslationService 接口，支持多个翻译提供商的自动 fallback 机制。
 * 优先从缓存获取结果，缓存未命中时按顺序尝试已配置的提供商。
 *
 * @property providers 翻译提供商列表，按优先级排序
 * @property cache 翻译缓存仓库，可选
 */
class MultiProviderTranslationService(
    private val providers: List<TranslationProvider>,
    private val cache: TranslationCacheRepository? = null
) : TranslationService {

    /**
     * 翻译单个文本
     *
     * 翻译流程：
     * 1. 检查缓存，如果命中直接返回
     * 2. 过滤出已配置的提供商
     * 3. 按顺序尝试提供商，直到成功或全部失败
     * 4. 缓存成功的翻译结果
     *
     * @param text 要翻译的文本
     * @param sourceLang 源语言代码（如 "en", "zh"）
     * @param targetLang 目标语言代码（如 "zh", "en"）
     * @return Result<String> 翻译结果，成功时包含翻译文本，失败时包含异常
     */
    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // 先查缓存
        cache?.get(text, sourceLang, targetLang)?.let { cachedResult ->
            return@withContext Result.success(cachedResult)
        }

        // 按顺序尝试已配置的 Provider
        val configuredProviders = providers.filter { it.isConfigured() }
        if (configuredProviders.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("没有已配置的翻译 Provider")
            )
        }

        var lastError: Throwable? = null
        for (provider in configuredProviders) {
            try {
                val result = provider.translate(text, sourceLang, targetLang)
                // 缓存成功的翻译结果
                cache?.put(text, sourceLang, targetLang, result, provider.name)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastError = e
                // 继续尝试下一个提供商
                continue
            }
        }

        // 所有提供商都失败
        Result.failure(
            lastError ?: RuntimeException("翻译失败")
        )
    }

    /**
     * 批量翻译文本列表
     *
     * 逐条调用 translate 方法，优先使用缓存。
     * 注意：批量翻译是串行执行的，每个文本独立尝试 fallback。
     *
     * @param texts 要翻译的文本列表
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return Result<List<String>> 翻译结果列表，顺序与输入一致。
     *         如果任何一个翻译失败，整个结果为失败。
     */
    override suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val results = texts.map { text ->
                val result = translate(text, sourceLang, targetLang)
                if (!result.isSuccess) {
                    // 如果任何一个翻译失败，抛出异常让外层捕获
                    throw result.exceptionOrNull() ?: RuntimeException("未知翻译错误")
                }
                result.getOrNull()!!
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
