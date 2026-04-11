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
 * 翻译服务接口
 *
 * 提供文本翻译功能，支持单个文本和批量翻译。
 * 所有翻译操作都是挂起函数，支持异步执行。
 */
interface TranslationService {
    /**
     * 翻译单个文本
     *
     * @param text 要翻译的文本
     * @param sourceLang 源语言代码（如 "en", "zh"）
     * @param targetLang 目标语言代码（如 "zh", "en"）
     * @return Result<String> 翻译结果，成功时包含翻译文本，失败时包含异常
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>

    /**
     * 批量翻译文本列表
     *
     * @param texts 要翻译的文本列表
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return Result<List<String>> 翻译结果列表，顺序与输入一致
     */
    suspend fun translateBatch(texts: List<String>, sourceLang: String, targetLang: String): Result<List<String>>
}
