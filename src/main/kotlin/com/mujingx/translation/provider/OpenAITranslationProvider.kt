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

package com.mujingx.translation.provider

import com.mujingx.translation.TranslationProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * OpenAI 翻译服务提供商
 *
 * 使用 OpenAI Chat Completions API 提供翻译服务。
 * 支持所有 GPT-4 和 GPT-3.5 系列模型。
 *
 * @param apiKey OpenAI API 密钥
 * @param baseUrl API 基础 URL（默认为官方 API 地址）
 * @param model 使用的模型名称（默认为 gpt-4o-mini）
 */
class OpenAITranslationProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val model: String = "gpt-4o-mini"
) : TranslationProvider {

    override val name = "OpenAI"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    /**
     * 检查提供商是否已正确配置
     *
     * @return true 如果 API 密钥非空，false 否则
     */
    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    /**
     * 翻译文本
     *
     * 使用 OpenAI Chat Completions API 进行翻译。
     *
     * @param text 要翻译的文本
     * @param from 源语言代码（如 "en", "zh"）
     * @param to 目标语言代码（如 "zh", "en"）
     * @return 翻译后的文本
     * @throws RuntimeException 如果翻译失败（网络错误、API 错误、解析错误等）
     */
    override suspend fun translate(text: String, from: String, to: String): String {
        if (!isConfigured()) {
            throw RuntimeException("OpenAI API key is not configured")
        }

        try {
            val systemPrompt = "You are a professional translator. Translate the following text from $from to $to. Return only the translation."

            val requestBody = buildJsonObject {
                put("model", model)
                putJsonArray("messages") {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", text)
                    })
                }
                put("temperature", 0.3)
                put("max_tokens", 1000)
            }

            val response = client.post("$baseUrl/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(requestBody.toString())
            }

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                throw RuntimeException("OpenAI API error: ${response.status.value} - $errorText")
            }

            val responseBody: String = response.body()
            val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

            val choices = jsonResponse["choices"]?.jsonArray
                ?: throw RuntimeException("Invalid response: missing 'choices' field")

            val firstChoice = choices.firstOrNull()?.jsonObject
                ?: throw RuntimeException("Invalid response: empty choices array")

            val message = firstChoice["message"]?.jsonObject
                ?: throw RuntimeException("Invalid response: missing 'message' field")

            val content = message["content"]?.jsonPrimitive?.content
                ?: throw RuntimeException("Invalid response: missing 'content' field")

            return content.trim()
        } catch (e: Exception) {
            throw RuntimeException("Failed to translate with OpenAI: ${e.message}", e)
        }
    }

    /**
     * 关闭 HTTP 客户端并释放资源
     */
    fun close() {
        client.close()
    }
}
