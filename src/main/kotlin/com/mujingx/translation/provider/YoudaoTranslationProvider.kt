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
import java.security.MessageDigest
import kotlin.random.Random

/**
 * 有道翻译服务提供商
 *
 * 使用有道翻译 API 提供翻译服务。
 * 支持有道翻译 API v3 版本。
 *
 * @param appKey 有道开放平台应用密钥
 * @param appSecret 有道开放平台应用密钥
 * @param baseUrl API 基础 URL（默认为官方 API 地址）
 */
class YoudaoTranslationProvider(
    private val appKey: String,
    private val appSecret: String,
    private val baseUrl: String = "https://openapi.youdao.com/api"
) : TranslationProvider {

    override val name = "Youdao"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    /**
     * 检查提供商是否已正确配置
     *
     * @return true 如果应用密钥和应用密钥都非空，false 否则
     */
    override fun isConfigured(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()

    /**
     * 翻译文本
     *
     * 使用有道翻译 API 进行翻译。
     *
     * @param text 要翻译的文本
     * @param from 源语言代码（如 "en", "zh-CHS"）
     * @param to 目标语言代码（如 "zh-CHS", "en"）
     * @return 翻译后的文本
     * @throws RuntimeException 如果翻译失败（网络错误、API 错误、解析错误等）
     */
    override suspend fun translate(text: String, from: String, to: String): String {
        if (!isConfigured()) {
            throw RuntimeException("Youdao appKey or appSecret is not configured")
        }

        try {
            val salt = Random.nextInt(100000, 999999).toString()
            val curtime = (System.currentTimeMillis() / 1000).toString()
            val sign = generateSign(text, salt, curtime)

            val response = client.post(baseUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                }
                setBody(
                    listOf(
                        "q" to text,
                        "from" to from,
                        "to" to to,
                        "appKey" to appKey,
                        "salt" to salt,
                        "curtime" to curtime,
                        "sign" to sign,
                        "signType" to "v3"
                    ).formUrlEncode()
                )
            }

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                throw RuntimeException("Youdao API error: ${response.status.value} - $errorText")
            }

            val responseBody: String = response.body()
            val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

            val errorCode = jsonResponse["errorCode"]?.jsonPrimitive?.content
            if (errorCode != null && errorCode != "0") {
                val errorMessage = getErrorMessage(errorCode)
                throw RuntimeException("Youdao API error: $errorMessage (code: $errorCode)")
            }

            val translations = jsonResponse["translation"]?.jsonArray
                ?: throw RuntimeException("Invalid response: missing 'translation' field")

            val firstTranslation = translations.firstOrNull()?.jsonPrimitive?.content
                ?: throw RuntimeException("Invalid response: empty translation array")

            return firstTranslation.trim()
        } catch (e: Exception) {
            throw RuntimeException("Failed to translate with Youdao: ${e.message}", e)
        }
    }

    /**
     * 生成 API 签名
     *
     * 签名算法: SHA-256(appKey + truncate(q) + salt + curtime + appSecret)
     *
     * @param text 要翻译的文本
     * @param salt 随机数
     * @param curtime 当前时间戳（秒）
     * @return 十六进制签名字符串
     */
    private fun generateSign(text: String, salt: String, curtime: String): String {
        val truncatedText = truncate(text)
        val signInput = appKey + truncatedText + salt + curtime + appSecret

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(signInput.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 截断文本用于签名计算
     *
     * 规则: 如果文本长度 <= 20，返回原文本；否则取前10个字符 + 长度 + 后10个字符
     *
     * @param text 原始文本
     * @return 截断后的文本
     */
    private fun truncate(text: String): String {
        return if (text.length <= 20) {
            text
        } else {
            text.take(10) + text.length + text.takeLast(10)
        }
    }

    /**
     * 获取错误码对应的错误信息
     *
     * @param errorCode 错误码
     * @return 错误信息
     */
    private fun getErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "101" -> "缺少必填参数"
            "102" -> "不支持的语言类型"
            "103" -> "翻译文本过长"
            "104" -> "不支持的API类型"
            "105" -> "不支持的签名类型"
            "106" -> "不支持的响应类型"
            "107" -> "不支持的传输加密类型"
            "108" -> "appKey无效"
            "109" -> "batchLog格式不正确"
            "110" -> "无相关服务的有效实例"
            "111" -> "开发者账号无效"
            "201" -> "解密失败"
            "202" -> "签名检验失败"
            "203" -> "访问IP地址不在可访问IP列表"
            "301" -> "辞典查询失败"
            "302" -> "翻译查询失败"
            "303" -> "服务端的其它异常"
            "401" -> "账户已经欠费"
            else -> "未知错误"
        }
    }

    /**
     * 关闭 HTTP 客户端并释放资源
     */
    fun close() {
        client.close()
    }
}
