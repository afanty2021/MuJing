/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mujingx.di

import com.mujingx.translation.TranslationService
import com.mujingx.translation.MultiProviderTranslationService
import com.mujingx.translation.TranslationCacheRepository
import com.mujingx.translation.provider.OpenAITranslationProvider
import com.mujingx.translation.provider.YoudaoTranslationProvider
import com.mujingx.translation.provider.AzureTranslationProvider
import org.koin.dsl.module

val translationModule = module {
    single { TranslationCacheRepository(System.getProperty("user.home") + "/.MuJing/translation_cache.db") }
    single<TranslationService> {
        MultiProviderTranslationService(
            providers = listOf(
                OpenAITranslationProvider(getSettings("openai_api_key")),
                YoudaoTranslationProvider(
                    getSettings("youdao_app_key"),
                    getSettings("youdao_app_secret")
                ),
                AzureTranslationProvider(
                    getSettings("azure_translation_key"),
                    getSettings("azure_translation_region")
                )
            ),
            cache = get()
        )
    }
}

private fun getSettings(key: String): String = ""