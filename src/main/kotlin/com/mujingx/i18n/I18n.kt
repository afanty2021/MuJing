package com.mujingx.i18n

import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream

object I18n {
    private var bundles: Map<String, String> = emptyMap()
    private var currentLang: String = "zh-CN"
    private val json = Json { ignoreUnknownKeys = true }

    fun init(lang: String = "zh-CN") {
        currentLang = lang
        bundles = loadBundle(lang)
    }

    fun t(key: String): String = bundles[key] ?: key
    fun t(key: String, vararg args: Any): String = bundles[key]?.format(*args) ?: key
    fun currentLanguage(): String = currentLang

    private fun loadBundle(lang: String): Map<String, String> {
        val resource: InputStream? = javaClass.classLoader?.getResourceAsStream("i18n/$lang.json")
        if (resource == null) {
            println("Warning: Language bundle '$lang' not found")
            return emptyMap()
        }
        val text = resource.bufferedReader().use { it.readText() }
        val jsonObject = json.parseToJsonElement(text).jsonObject
        return jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
    }
}

val LocalI18n = compositionLocalOf { I18n }