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

package com.mujingx.ffmpeg

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.io.File

/**
 * Whisper 集成测试
 *
 * 测试 Whisper 模型的实际字幕生成功能
 */
@DisplayName("Whisper 集成测试")
class WhisperIntegrationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    @DisplayName("测试 Whisper 模型文件存在性")
    fun testWhisperModelExists() {
        val modelPath = "src/test/resources/whisper/ggml-base.en.bin"
        val modelFile = File(modelPath)

        assertTrue(modelFile.exists(), "Whisper 模型文件应该存在: $modelPath")
        assertTrue(modelFile.length() > 0, "Whisper 模型文件应该有内容")

        println("✓ Whisper 模型文件存在: ${modelFile.absolutePath}")
        println("✓ 模型文件大小: ${modelFile.length() / 1024 / 1024} MB")
    }

    @Test
    @DisplayName("测试 Whisper 生成字幕 - 简单音频")
    fun testGenerateSrtWithWhisperSimpleAudio() {
        val modelPath = "src/test/resources/whisper/ggml-base.en.bin"
        val inputPath = "src/test/resources/Sintel.2010.480.mp3"
        val outputPath = File(tempDir, "output.srt").absolutePath

        val modelFile = File(modelPath)
        val inputFile = File(inputPath)

        // 验证输入文件存在
        assertTrue(modelFile.exists(), "模型文件应该存在")
        assertTrue(inputFile.exists(), "输入音频文件应该存在")

        println("开始生成字幕...")
        println("输入: ${inputFile.name}")
        println("模型: ${modelFile.name}")
        println("输出: $outputPath")

        // 执行字幕生成
        val result = generateSrtWithWhisper(
            input = inputPath,
            output = outputPath,
            modelPath = modelPath,
            language = "en",
            queue = 3,
            useGpu = false
        )

        // 验证结果
        assertTrue(result.isSuccess, "字幕生成应该成功: ${result.exceptionOrNull()?.message}")

        val outputFile = File(outputPath)
        assertTrue(outputFile.exists(), "输出字幕文件应该存在")
        assertTrue(outputFile.length() > 0, "输出字幕文件应该有内容")

        println("✓ 字幕生成成功")
        println("✓ 输出文件大小: ${outputFile.length()} bytes")
        println("\n生成的字幕内容预览:")
        println(outputFile.readText().take(500))
    }

    @Test
    @DisplayName("测试 Whisper 生成字幕 - 视频文件")
    fun testGenerateSrtWithWhisperVideo() {
        val modelPath = "src/test/resources/whisper/ggml-base.en.bin"
        val inputPath = "src/test/resources/Sintel.2010.480.mp4"
        val outputPath = File(tempDir, "video_output.srt").absolutePath

        val modelFile = File(modelPath)
        val inputFile = File(inputPath)

        assertTrue(modelFile.exists(), "模型文件应该存在")
        assertTrue(inputFile.exists(), "输入视频文件应该存在")

        println("开始从视频生成字幕...")
        println("输入: ${inputFile.name}")
        println("模型: ${modelFile.name}")

        val result = generateSrtWithWhisper(
            input = inputPath,
            output = outputPath,
            modelPath = modelPath,
            language = "en",
            queue = 3,
            useGpu = false
        )

        assertTrue(result.isSuccess, "视频字幕生成应该成功")

        val outputFile = File(outputPath)
        assertTrue(outputFile.exists(), "输出字幕文件应该存在")

        println("✓ 视频字幕生成成功")
        println("输出文件: ${outputFile.absolutePath}")
    }

    @Test
    @DisplayName("测试 Whisper 错误处理 - 模型文件不存在")
    fun testWhisperModelNotFound() {
        val modelPath = File(tempDir, "non-existent-model.bin").absolutePath
        val inputPath = "src/test/resources/Sintel.2010.480.mp3"
        val outputPath = File(tempDir, "error_output.srt").absolutePath

        val result = generateSrtWithWhisper(
            input = inputPath,
            output = outputPath,
            modelPath = modelPath,
            language = "en"
        )

        assertTrue(result.isFailure, "应该返回失败结果")
        val errorMessage = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            errorMessage.contains("不存在"),
            "错误消息应该包含'不存在'"
        )

        println("✓ 错误处理正确: 模型文件不存在")
    }

    @Test
    @DisplayName("测试 Whisper 错误处理 - 输入文件不存在")
    fun testWhisperInputNotFound() {
        val modelPath = "src/test/resources/whisper/ggml-base.en.bin"
        val inputPath = File(tempDir, "non-existent-audio.mp3").absolutePath
        val outputPath = File(tempDir, "error_output2.srt").absolutePath

        val result = generateSrtWithWhisper(
            input = inputPath,
            output = outputPath,
            modelPath = modelPath,
            language = "en"
        )

        assertTrue(result.isFailure, "应该返回失败结果")
        println("✓ 错误处理正确: 输入文件不存在")
    }
}
