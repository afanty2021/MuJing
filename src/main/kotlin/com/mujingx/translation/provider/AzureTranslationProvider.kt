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
 * Azure 认知服务翻译提供商
 *
 * 使用 Azure Cognitive Services Translator API 提供翻译服务。
 * 支持多种语言和文本翻译功能。
 *
 * @param subscriptionKey Azure 认知服务订阅密钥
 * @param region Azure 服务区域（默认为 "global"）
 * @param baseUrl API 基础 URL（默认为官方 API 地址）
 */
class AzureTranslationProvider(
    private val subscriptionKey: String,
    private val region: String = "global",
    private val baseUrl: String = "https://api.cognitive.microsofttranslator.com"
) : TranslationProvider {

    override val name = "Azure"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    /**
     * 检查提供商是否已正确配置
     *
     * @return true 如果订阅密钥非空，false 否则
     */
    override fun isConfigured(): Boolean = subscriptionKey.isNotBlank()

    /**
     * 翻译文本
     *
     * 使用 Azure Translator API 进行翻译。
     *
     * @param text 要翻译的文本
     * @param from 源语言代码（如 "en", "zh-Hans"）
     * @param to 目标语言代码（如 "zh-Hans", "en"）
     * @return 翻译后的文本
     * @throws RuntimeException 如果翻译失败（网络错误、API 错误、解析错误等）
     */
    override suspend fun translate(text: String, from: String, to: String): String {
        if (!isConfigured()) {
            throw RuntimeException("Azure subscription key is not configured")
        }

        try {
            val requestBody = buildJsonArray {
                add(buildJsonObject {
                    put("text", text)
                })
            }

            val url = "$baseUrl/translate?api-version=3.0&from=$from&to=$to"

            val response = client.post(url) {
                headers {
                    append("Ocp-Apim-Subscription-Key", subscriptionKey)
                    append("Ocp-Apim-Subscription-Region", region)
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(requestBody.toString())
            }

            if (response.status.value !in 200..299) {
                val errorText = response.body<String>()
                throw RuntimeException("Azure API error: ${response.status.value} - $errorText")
            }

            val responseBody: String = response.body()
            val jsonResponse = Json.parseToJsonElement(responseBody).jsonArray

            val firstResult = jsonResponse.firstOrNull()?.jsonObject
                ?: throw RuntimeException("Invalid response: empty response array")

            val translations = firstResult["translations"]?.jsonArray
                ?: throw RuntimeException("Invalid response: missing 'translations' field")

            val firstTranslation = translations.firstOrNull()?.jsonObject
                ?: throw RuntimeException("Invalid response: empty translations array")

            val translatedText = firstTranslation["text"]?.jsonPrimitive?.content
                ?: throw RuntimeException("Invalid response: missing 'text' field")

            return translatedText.trim()
        } catch (e: Exception) {
            throw RuntimeException("Failed to translate with Azure: ${e.message}", e)
        }
    }

    /**
     * 关闭 HTTP 客户端并释放资源
     */
    fun close() {
        client.close()
    }
}
