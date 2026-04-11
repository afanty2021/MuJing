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

/**
 * 翻译提供商接口
 *
 * 定义具体的翻译服务提供商（如 Google Translate、DeepL、Azure Translator 等）
 * 需要实现的基本功能。
 */
interface TranslationProvider {
    /**
     * 提供商名称（用于日志和调试）
     */
    val name: String

    /**
     * 翻译文本
     *
     * @param text 要翻译的文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @return 翻译后的文本
     * @throws Exception 如果翻译失败（网络错误、API 错误、配置错误等）
     */
    suspend fun translate(text: String, from: String, to: String): String

    /**
     * 检查提供商是否已正确配置
     *
     * @return true 如果已配置必要的 API 密钥等，false 否则
     */
    fun isConfigured(): Boolean
}
