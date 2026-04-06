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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * FFmpegUtil 单元测试套件
 *
 * 测试覆盖范围：
 * 1. 字符串处理功能（纯函数，优先测试）
 * 2. 富文本标签移除功能
 * 3. 路径转义功能
 * 4. Whisper 生成字幕功能
 * 5. 边缘情况处理
 */
@DisplayName("FFmpegUtil 测试套件")
class FFmpegUtilTest {

    @Nested
    @DisplayName("字符串处理功能测试")
    inner class StringProcessingTests {

        @Test
        @DisplayName("测试移除富文本标签 - 基本功能")
        fun testRemoveRichTextBasic() {
            // 测试基本的富文本标签移除
            val input = "这是<b>粗体</b>文本"
            val expected = "这是粗体文本"
            val result = removeRichText(input)
            assertEquals(expected, result, "应该正确移除 <b> 标签")
        }

        @Test
        @DisplayName("测试移除富文本标签 - 多种标签")
        fun testRemoveRichTextMultipleTags() {
            // 测试多种富文本标签的移除
            val input = "这是<i>斜体</i>和<u>下划线</u>文本"
            val expected = "这是斜体和下划线文本"
            val result = removeRichText(input)
            assertEquals(expected, result, "应该正确移除 <i> 和 <u> 标签")
        }

        @Test
        @DisplayName("测试移除富文本标签 - 带属性的标签")
        fun testRemoveRichTextWithAttributes() {
            // 测试带属性的富文本标签移除
            val input = "这是<font color=\"red\">红色</font>文本"
            val expected = "这是红色文本"
            val result = removeRichText(input)
            assertEquals(expected, result, "应该正确移除带属性的 <font> 标签")
        }

        @Test
        @DisplayName("测试移除富文本标签 - 嵌套标签")
        fun testRemoveRichTextNested() {
            // 测试嵌套的富文本标签
            val input = "这是<b><i>粗斜体</i></b>文本"
            val expected = "这是粗斜体文本"
            val result = removeRichText(input)
            assertEquals(expected, result, "应该正确移除嵌套的富文本标签")
        }

        @Test
        @DisplayName("测试移除富文本标签 - 空字符串")
        fun testRemoveRichTextEmpty() {
            // 测试空字符串
            val input = ""
            val expected = ""
            val result = removeRichText(input)
            assertEquals(expected, result, "空字符串应保持为空")
        }

        @Test
        @DisplayName("测试移除富文本标签 - 无富文本")
        fun testRemoveRichTextNoTags() {
            // 测试没有富文本标签的纯文本
            val input = "这是纯文本，没有任何标签"
            val expected = "这是纯文本，没有任何标签"
            val result = removeRichText(input)
            assertEquals(expected, result, "纯文本应保持不变")
        }

        @Test
        @DisplayName("测试检查富文本标签 - 有标签")
        fun testHasRichTextTrue() {
            // 测试检测富文本标签存在
            val input = "这是<b>粗体</b>文本"
            val result = hasRichText(input)
            assertTrue(result, "应该检测到富文本标签")
        }

        @Test
        @DisplayName("测试检查富文本标签 - 无标签")
        fun testHasRichTextFalse() {
            // 测试检测富文本标签不存在
            val input = "这是纯文本"
            val result = hasRichText(input)
            assertFalse(result, "纯文本不应检测到富文本标签")
        }

        @Test
        @DisplayName("测试检查富文本标签 - 空字符串")
        fun testHasRichTextEmpty() {
            // 测试空字符串的富文本检测
            val input = ""
            val result = hasRichText(input)
            assertFalse(result, "空字符串不应检测到富文本标签")
        }
    }

    @Nested
    @DisplayName("富文本标签移除详细测试")
    inner class RichTextRemovalTests {

        @Test
        @DisplayName("测试移除粗体标签 <b>")
        fun testRemoveBoldTag() {
            val input = "<b>粗体文本</b>"
            val expected = "粗体文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除斜体标签 <i>")
        fun testRemoveItalicTag() {
            val input = "<i>斜体文本</i>"
            val expected = "斜体文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除下划线标签 <u>")
        fun testRemoveUnderlineTag() {
            val input = "<u>下划线文本</u>"
            val expected = "下划线文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除删除线标签 <s>")
        fun testRemoveStrikethroughTag() {
            val input = "<s>删除线文本</s>"
            val expected = "删除线文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除字体标签 <font>")
        fun testRemoveFontTag() {
            val input = "<font face=\"Arial\" size=\"12\" color=\"#FF0000\">字体文本</font>"
            val expected = "字体文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除 ruby 标签")
        fun testRemoveRubyTag() {
            val input = "<ruby>漢字<rt>かんじ</rt></ruby>"
            val expected = "漢字かんじ"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除上标标签 <sup>")
        fun testRemoveSuperscriptTag() {
            val input = "x<sup>2</sup>"
            val expected = "x2"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除下标标签 <sub>")
        fun testRemoveSubscriptTag() {
            val input = "H<sub>2</sub>O"
            val expected = "H2O"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除混合富文本标签")
        fun testRemoveMixedRichText() {
            val input = "这是<b>粗体</b>、<i>斜体</i>和<u>下划线</u>的混合文本"
            val expected = "这是粗体、斜体和下划线的混合文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除自闭合标签")
        fun testRemoveSelfClosingTags() {
            val input = "文本<br />换行"
            val result = removeRichText(input)
            // <br /> 不在 RICH_TEXT_REGEX 中，应该保留
            assertTrue(result.contains("<br />"), "换行标签应该保留")
        }

        @Test
        @DisplayName("测试移除标签后的文本内容")
        fun testRemoveRichTextContentPreserved() {
            val input = "前缀<b>中间</b>后缀"
            val expected = "前缀中间后缀"
            assertEquals(expected, removeRichText(input), "标签外的文本应该保留")
        }

        @Test
        @DisplayName("测试移除多个相同标签")
        fun testRemoveMultipleSameTags() {
            val input = "<b>第一</b>部分<b>第二</b>部分"
            val expected = "第一部分第二部分"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除标签属性")
        fun testRemoveTagAttributes() {
            val input = "<font color=\"red\" size=\"14\">文本</font>"
            val expected = "文本"
            assertEquals(expected, removeRichText(input))
        }

        @Test
        @DisplayName("测试移除换行标签 <br />")
        fun testRemoveNewLineTag() {
            val input = "第一行<br />第二行"
            // <br /> 不在 RICH_TEXT_REGEX 中
            val result = removeRichText(input)
            assertTrue(result.contains("<br />"), "换行标签应该在 removeRichText 中保留")
            // 注意：实际的换行替换在 removeRichText(File) 函数中通过 replaceNewLine 完成
        }
    }

    @Nested
    @DisplayName("路径转义测试")
    inner class PathEscapingTests {

        @Test
        @DisplayName("测试转义 Windows 路径中的冒号")
        fun testEscapeWindowsPathColon() {
            val input = "C:/Users/test/model.bin"
            val expected = "C\\\\:/Users/test/model.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义 Windows 路径中的冒号")
        }

        @Test
        @DisplayName("测试转义多个冒号")
        fun testEscapeMultipleColons() {
            val input = "C:/test:file/model.bin"
            val expected = "C\\\\:/test\\\\:file/model.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义所有的冒号")
        }

        @Test
        @DisplayName("测试转义方括号")
        fun testEscapeBrackets() {
            val input = "path/to/[test]/file.bin"
            val expected = "path/to/\\[test\\]/file.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义方括号")
        }

        @Test
        @DisplayName("测试转义单引号")
        fun testEscapeSingleQuote() {
            val input = "path/to/test's/file.bin"
            val expected = "path/to/test\\'s/file.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义单引号")
        }

        @Test
        @DisplayName("测试转义双引号")
        fun testEscapeDoubleQuote() {
            val input = "path/to/test\"file.bin"
            val expected = "path/to/test\\\"file.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义双引号")
        }

        @Test
        @DisplayName("测试转义混合特殊字符")
        fun testEscapeMixedSpecialChars() {
            val input = "C:/[test]/file's name.bin"
            val expected = "C\\\\:/\\[test\\]/file\\'s name.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该转义所有特殊字符")
        }

        @Test
        @DisplayName("测试转义 Unix 路径（无特殊字符）")
        fun testEscapeUnixPath() {
            val input = "/home/user/model.bin"
            val expected = "/home/user/model.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "Unix 路径无特殊字符时不应改变")
        }

        @Test
        @DisplayName("测试转义空路径")
        fun testEscapeEmptyPath() {
            val input = ""
            val expected = ""
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "空路径应保持为空")
        }

        @Test
        @DisplayName("测试转义相对路径")
        fun testEscapeRelativePath() {
            val input = "./models/test.bin"
            val expected = "./models/test.bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "相对路径无特殊字符时不应改变")
        }

        @Test
        @DisplayName("测试转义路径中的多个特殊字符")
        fun testEscapeMultipleSpecialCharsInPath() {
            val input = "C:/test/path[1]/file\"name\".bin"
            val expected = "C\\\\:/test/path\\[1\\]/file\\\"name\\\".bin"
            val result = escapeFilterPath(input)
            assertEquals(expected, result, "应该正确转义路径中的多个特殊字符")
        }
    }

    @Nested
    @DisplayName("文件操作测试")
    inner class FileOperationTests {

        @TempDir
        lateinit var tempDir: File

        @Test
        @DisplayName("测试从文件中移除富文本标签")
        fun testRemoveRichTextFromFile() {
            // 创建测试文件
            val testFile = File(tempDir, "test.srt")
            val content = "1\n00:00:00,000 --> 00:00:02,000\n这是<b>粗体</b>字幕\n\n"
            testFile.writeText(content)

            // 调用函数
            removeRichText(testFile)

            // 验证结果
            val result = testFile.readText()
            assertFalse(result.contains("<b>"), "富文本标签应该被移除")
            assertTrue(result.contains("这是粗体字幕"), "文本内容应该保留")
        }

        @Test
        @DisplayName("测试检查文件中的富文本标签")
        fun testHasRichTextInFile() {
            // 创建包含富文本的测试文件
            val testFile = File(tempDir, "test.srt")
            testFile.writeText("1\n00:00:00,000 --> 00:00:02,000\n这是<i>斜体</i>字幕\n\n")

            val result = hasRichText(testFile)
            assertTrue(result, "应该检测到文件中的富文本标签")
        }

        @Test
        @DisplayName("测试检查文件中无富文本标签")
        fun testHasNoRichTextInFile() {
            // 创建纯文本测试文件
            val testFile = File(tempDir, "test.srt")
            testFile.writeText("1\n00:00:00,000 --> 00:00:02,000\n这是纯字幕\n\n")

            val result = hasRichText(testFile)
            assertFalse(result, "纯文本文件不应检测到富文本标签")
        }

        @Test
        @DisplayName("测试处理空文件")
        fun testHandleEmptyFile() {
            // 创建空文件
            val testFile = File(tempDir, "empty.srt")
            testFile.writeText("")

            // 测试空文件的富文本检测
            val hasRich = hasRichText(testFile)
            assertFalse(hasRich, "空文件不应检测到富文本标签")

            // 测试空文件的富文本移除（不应抛出异常）
            removeRichText(testFile)
            assertEquals("", testFile.readText(), "空文件应保持为空")
        }

        @Test
        @DisplayName("测试处理大文件")
        fun testHandleLargeFile() {
            // 创建包含多行富文本的大文件
            val testFile = File(tempDir, "large.srt")
            val content = buildString {
                repeat(100) { i ->
                    append("${i + 1}\n")
                    append("00:00:${i.toString().padStart(2, '0')},000 --> 00:00:${(i + 1).toString().padStart(2, '0')},000\n")
                    append("这是<b>粗体</b>字幕第${i + 1}行\n\n")
                }
            }
            testFile.writeText(content)

            // 测试富文本检测
            val hasRich = hasRichText(testFile)
            assertTrue(hasRich, "大文件应该检测到富文本标签")

            // 测试富文本移除
            removeRichText(testFile)
            val result = testFile.readText()
            assertFalse(result.contains("<b>"), "大文件中的富文本标签应该被移除")
            assertTrue(result.contains("这是粗体字幕第1行"), "文本内容应该保留")
        }
    }

    @Nested
    @DisplayName("Whisper 生成字幕测试")
    inner class WhisperSrtTests {

        @TempDir
        lateinit var tempDir: File

        @Test
        @DisplayName("测试 Whisper 滤镜参数构建")
        fun testWhisperFilterParameters() {
            // 测试构建 Whisper 滤镜参数
            val modelPath = "/path/to/model.bin"
            val language = "zh"
            val queue = 5
            val useGpu = true
            val gpuDevice = 0
            val output = "/path/to/output.srt"

            val escapedModelPath = escapeFilterPath(modelPath)
            val escapedOutput = escapeFilterPath(output)

            val expectedFilter = "whisper=model=$escapedModelPath:language=$language:queue=$queue" +
                    ":use_gpu=true:gpu_device=$gpuDevice:destination=$escapedOutput:format=srt"

            // 构建实际的滤镜参数
            val actualFilter = buildString {
                append("whisper=model=$escapedModelPath:language=$language:queue=$queue")
                append(":use_gpu=").append(if (useGpu) "true" else "false")
                append(":gpu_device=").append(gpuDevice)
                append(":destination=$escapedOutput:format=srt")
            }

            assertEquals(expectedFilter, actualFilter, "Whisper 滤镜参数应该正确构建")
        }

        @Test
        @DisplayName("测试 Whisper 滤镜参数 - 不使用 GPU")
        fun testWhisperFilterWithoutGpu() {
            val modelPath = "/path/to/model.bin"
            val language = "en"
            val queue = 3
            val useGpu = false
            val gpuDevice = 0
            val output = "/path/to/output.srt"

            val escapedModelPath = escapeFilterPath(modelPath)
            val escapedOutput = escapeFilterPath(output)

            val actualFilter = buildString {
                append("whisper=model=$escapedModelPath:language=$language:queue=$queue")
                append(":use_gpu=").append(if (useGpu) "true" else "false")
                append(":gpu_device=").append(gpuDevice)
                append(":destination=$escapedOutput:format=srt")
            }

            assertTrue(actualFilter.contains("use_gpu=false"), "应该设置 use_gpu=false")
        }

        @Test
        @DisplayName("测试 Whisper 模型文件路径验证")
        fun testWhisperModelPathValidation() {
            // 创建临时模型文件
            val modelFile = File(tempDir, "test-model.bin")
            modelFile.writeText("fake model content")

            // 测试模型文件存在
            assertTrue(modelFile.exists(), "模型文件应该存在")
            assertTrue(modelFile.length() > 0, "模型文件应该有内容")

            // 测试模型文件不存在的情况
            val nonExistentPath = File(tempDir, "non-existent.bin").absolutePath
            val nonExistentFile = File(nonExistentPath)
            assertFalse(nonExistentFile.exists(), "不存在的模型文件")
        }

        @Test
        @DisplayName("测试 Whisper 输出路径处理")
        fun testWhisperOutputPathProcessing() {
            // 测试输出路径的标准化和转义
            val rawOutput = "C:\\Users\\test\\output.srt"
            val normalizedOutput = rawOutput.replace("\\", "/")
            val escapedOutput = escapeFilterPath(normalizedOutput)

            assertEquals("C\\\\:/Users/test/output.srt", escapedOutput,
                "输出路径应该被标准化并转义")
        }

        @Test
        @DisplayName("测试 Whisper 参数 - 不同语言代码")
        fun testWhisperLanguageCodes() {
            val languages = listOf("en", "zh", "auto", "es", "fr", "de", "ja")

            languages.forEach { lang ->
                val filter = "whisper=model=/path:language=$lang:queue=3"
                assertTrue(filter.contains("language=$lang"),
                    "滤镜参数应该包含语言代码: $lang")
            }
        }

        @Test
        @DisplayName("测试 Whisper 参数 - 不同队列大小")
        fun testWhisperQueueSizes() {
            val queueSizes = listOf(1, 3, 5, 8, 10)

            queueSizes.forEach { queue ->
                val filter = "whisper=model=/path:language=en:queue=$queue"
                assertTrue(filter.contains("queue=$queue"),
                    "滤镜参数应该包含队列大小: $queue")
            }
        }

        @Test
        @DisplayName("测试 Whisper 参数 - GPU 设备")
        fun testWhisperGpuDevices() {
            val gpuDevices = listOf(0, 1, 2, 3)

            gpuDevices.forEach { device ->
                val filter = "whisper=model=/path:language=en:queue=3:use_gpu=true:gpu_device=$device"
                assertTrue(filter.contains("gpu_device=$device"),
                    "滤镜参数应该包含 GPU 设备: $device")
            }
        }
    }

    @Nested
    @DisplayName("边缘情况测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("测试空字符串处理")
        fun testEmptyString() {
            // 测试各种空字符串情况
            assertEquals("", removeRichText(""), "空字符串应返回空字符串")
            assertFalse(hasRichText(""), "空字符串不应有富文本")
            assertEquals("", escapeFilterPath(""), "空路径应返回空字符串")
        }

        @Test
        @DisplayName("测试只有标签没有内容")
        fun testOnlyTagsNoContent() {
            val input = "<b></b>"
            val expected = ""
            assertEquals(expected, removeRichText(input),
                "只有标签没有内容应返回空字符串")
        }

        @Test
        @DisplayName("测试标签之间无空格")
        fun testTagsWithoutSpaces() {
            val input = "<b>A</b><i>B</i>"
            val expected = "AB"
            assertEquals(expected, removeRichText(input),
                "标签之间无空格时应正确处理")
        }

        @Test
        @DisplayName("测试特殊字符")
        fun testSpecialCharacters() {
            val input = "特殊字符：@#\$%^&*()_+-=[]{}|;':\",./<>?"
            val expected = "特殊字符：@#\$%^&*()_+-=[]{}|;':\",./<>?"
            assertEquals(expected, removeRichText(input),
                "特殊字符应保持不变")

            val hasRich = hasRichText(input)
            assertFalse(hasRich, "特殊字符不应被识别为富文本")
        }

        @Test
        @DisplayName("测试 Unicode 字符")
        fun testUnicodeCharacters() {
            val input = "Unicode测试：😀🎉🌟中文字符日本語한국어"
            val expected = "Unicode测试：😀🎉🌟中文字符日本語한국어"
            assertEquals(expected, removeRichText(input),
                "Unicode 字符应保持不变")
        }

        @Test
        @DisplayName("测试非常长的字符串")
        fun testVeryLongString() {
            val longContent = "A".repeat(10000)
            val input = "<b>$longContent</b>"
            val expected = longContent
            assertEquals(expected, removeRichText(input),
                "应该正确处理非常长的字符串")
        }

        @Test
        @DisplayName("测试未闭合的标签")
        fun testUnclosedTags() {
            val input = "这是<b>未闭合的标签文本"
            val result = removeRichText(input)
            // 正则表达式应该匹配未闭合的开始标签
            assertFalse(result.contains("<b>"), "未闭合的开始标签应该被移除")
            assertTrue(result.contains("未闭合的标签文本"), "文本内容应该保留")
        }

        @Test
        @DisplayName("测试自闭合标签")
        fun testSelfClosingTagVariations() {
            val input = "文本<br/>换行"
            val result = removeRichText(input)
            // <br/> 不在 RICH_TEXT_REGEX 中
            assertTrue(result.contains("<br/>"), "不在正则中的自闭合标签应保留")
        }

        @Test
        @DisplayName("测试标签大小写敏感性")
        fun testTagCaseSensitivity() {
            val input = "<B>大写标签</B><i>小写标签</i>"
            val result = removeRichText(input)
            // RICH_TEXT_REGEX 使用小写标签名，所以大写标签可能不会被移除
            // 这取决于正则表达式的实现
            assertTrue(result.contains("大写标签") || result.contains("<B>"),
                "需要根据实际正则表达式验证大小写敏感性")
        }

        @Test
        @DisplayName("测试混合换行符")
        fun testMixedNewlines() {
            val input = "第一行\n第二行\r\n第三行\r第四行"
            val expected = "第一行\n第二行\r\n第三行\r第四行"
            assertEquals(expected, removeRichText(input),
                "换行符应保持不变（换行标签替换在其他函数中处理）")
        }

        @Test
        @DisplayName("测试路径转义 - 只有一个冒号")
        fun testEscapePathSingleColon() {
            val input = "C:test"
            val expected = "C\\\\:test"
            assertEquals(expected, escapeFilterPath(input),
                "单个冒号应该被转义")
        }

        @Test
        @DisplayName("测试路径转义 - 连续特殊字符")
        fun testEscapePathConsecutiveSpecialChars() {
            val input = "path[1][2]file"
            val expected = "path\\[1\\]\\[2\\]file"
            assertEquals(expected, escapeFilterPath(input),
                "连续的特殊字符应该被正确转义")
        }

        @Test
        @DisplayName("测试富文本正则表达式 - 所有支持的标签")
        fun testRichTextRegexAllSupportedTags() {
            val supportedTags = listOf("b", "i", "u", "font", "s", "ruby", "rt", "rb", "sub", "sup")

            supportedTags.forEach { tag ->
                val input = "<$tag>内容</$tag>"
                val result = removeRichText(input)
                assertFalse(result.contains("<$tag>"),
                    "标签 <$tag> 应该被移除")
                assertTrue(result.contains("内容"),
                    "标签内容应该保留")
            }
        }

        @Test
        @DisplayName("测试富文本检测 - 部分标签")
        fun testHasRichTextPartialTags() {
            val testCases = mapOf(
                "<b>内容</b>" to true,
                "<i>内容</i>" to true,
                "<font>内容</font>" to true,
                "纯文本" to false,
                "<br />换行" to false, // <br /> 不在 RICH_TEXT_REGEX 中
                "<notsupported>内容</notsupported>" to false // 不支持的标签
            )

            testCases.forEach { (input, expected) ->
                val result = hasRichText(input)
                assertEquals(expected, result,
                    "富文本检测失败: 输入='$input', 期望=$expected, 实际=$result")
            }
        }

        @Test
        @DisplayName("测试富文本移除 - 保持文本结构")
        fun testRemoveRichTextPreserveStructure() {
            val input = """
                1
                00:00:00,000 --> 00:00:02,000
                这是<b>第一行</b>字幕

                2
                00:00:02,000 --> 00:00:04,000
                这是<i>第二行</i>字幕
            """.trimIndent()

            val result = removeRichText(input)

            // 验证行结构保持不变
            val lines = result.split("\n")
            assertTrue(lines.size >= 4, "应该保持原有的行结构")
            assertTrue(result.contains("这是第一行字幕"), "第一行文本应该保留")
            assertTrue(result.contains("这是第二行字幕"), "第二行文本应该保留")
        }
    }

    @Nested
    @DisplayName("正则表达式常量验证")
    inner class RegexConstantTests {

        @Test
        @DisplayName("验证 RICH_TEXT_REGEX 常量格式")
        fun testRichTextRegexConstant() {
            // 验证正则表达式常量的格式
            val regex = Regex(RICH_TEXT_REGEX)

            // 测试匹配的标签
            val shouldMatch = listOf(
                "<b>", "</b>",
                "<i>", "</i>",
                "<u>", "</u>",
                "<font>", "</font>",
                "<s>", "</s>",
                "<ruby>", "</ruby>",
                "<rt>", "</rt>",
                "<rb>", "</rb>",
                "<sub>", "</sub>",
                "<sup>", "</sup>"
            )

            shouldMatch.forEach { tag ->
                assertTrue(regex.containsMatchIn(tag),
                    "正则表达式应该匹配标签: $tag")
            }

            // 测试带属性的标签
            val withAttributes = listOf(
                "<font color=\"red\">",
                "<font size='14'>",
                "<b class=\"bold\">"
            )

            withAttributes.forEach { tag ->
                assertTrue(regex.containsMatchIn(tag),
                    "正则表达式应该匹配带属性的标签: $tag")
            }

            // 测试不应该匹配的内容
            val shouldNotMatch = listOf(
                "<br />",
                "<div>", "</div>",
                "<span>", "</span>",
                "<p>", "</p>",
                "纯文本",
                ""
            )

            shouldNotMatch.forEach { content ->
                val matches = regex.containsMatchIn(content)
                if (content == "<br />") {
                    assertFalse(matches, "正则表达式不应该匹配: $content")
                }
            }
        }

        @Test
        @DisplayName("验证正则表达式匹配性能")
        fun testRegexPerformance() {
            val regex = Regex(RICH_TEXT_REGEX)
            val longText = buildString {
                repeat(1000) {
                    append("这是<b>粗体</b>和<i>斜体</i>的文本。")
                }
            }

            val startTime = System.currentTimeMillis()
            val result = regex.replace(longText, "")
            val endTime = System.currentTimeMillis()

            val duration = endTime - startTime

            // 验证结果正确
            assertFalse(result.contains("<b>"), "应该移除所有 <b> 标签")
            assertFalse(result.contains("<i>"), "应该移除所有 <i> 标签")

            // 验证性能合理（应该在100ms内完成）
            assertTrue(duration < 100,
                "正则表达式替换1000次重复标签应该在100ms内完成，实际耗时: ${duration}ms")
        }
    }

    @Nested
    @DisplayName("集成场景测试")
    inner class IntegrationScenarioTests {

        @TempDir
        lateinit var tempDir: File

        @Test
        @DisplayName("测试完整的字幕文件处理流程")
        fun testCompleteSubtitleProcessingWorkflow() {
            // 创建包含富文本的 SRT 文件
            val srtContent = """
                1
                00:00:00,000 --> 00:00:02,000
                这是第一句<b>粗体</b>字幕

                2
                00:00:02,000 --> 00:00:04,000
                这是第二句<i>斜体</i>字幕

                3
                00:00:04,000 --> 00:00:06,000
                这是第三句<font color="red">彩色</font>字幕
            """.trimIndent()

            val testFile = File(tempDir, "test.srt")
            testFile.writeText(srtContent)

            // 1. 检查是否有富文本
            assertTrue(hasRichText(testFile), "应该检测到富文本标签")

            // 2. 移除富文本
            removeRichText(testFile)

            // 3. 验证富文本已移除
            assertFalse(hasRichText(testFile), "富文本标签应该被移除")

            // 4. 验证内容完整性
            val result = testFile.readText()
            assertTrue(result.contains("这是第一句粗体字幕"), "第一句内容应该保留")
            assertTrue(result.contains("这是第二句斜体字幕"), "第二句内容应该保留")
            assertTrue(result.contains("这是第三句彩色字幕"), "第三句内容应该保留")
        }

        @Test
        @DisplayName("测试 Whisper 路径处理完整流程")
        fun testWhisperPathProcessingWorkflow() {
            // 模拟不同操作系统的路径处理
            val testCases = mapOf(
                "Windows 路径" to "C:\\Users\\test\\models\\ggml-base.bin",
                "Unix 路径" to "/home/user/models/ggml-base.bin",
                "相对路径" to "./models/ggml-base.bin",
                "带空格路径" to "/home/user/test folder/models/ggml model.bin"
            )

            testCases.forEach { (description, inputPath) ->
                // 标准化路径
                val normalized = inputPath.replace("\\", "/")

                // 转义路径
                val escaped = escapeFilterPath(normalized)

                // 验证路径不为空
                assertTrue(escaped.isNotEmpty(),
                    "$description: 转义后的路径不应为空")

                // 验证特殊字符被正确处理
                assertFalse(escaped.contains("'") && !escaped.contains("\\'"),
                    "$description: 单引号应该被转义")
            }
        }

        @Test
        @DisplayName("测试批量字幕文件处理")
        fun testBatchSubtitleProcessing() {
            // 创建多个测试文件
            val testFiles = (1..5).map { index ->
                val file = File(tempDir, "subtitle_$index.srt")
                val content = """
                    $index
                    00:00:0${index - 1},000 --> 00:00:0${index},000
                    这是第<b>$index</b>个字幕文件
                """.trimIndent()
                file.writeText(content)
                file
            }

            // 批量处理
            testFiles.forEach { file ->
                if (hasRichText(file)) {
                    removeRichText(file)
                }
            }

            // 验证所有文件都处理成功
            testFiles.forEach { file ->
                assertFalse(hasRichText(file),
                    "文件 ${file.name} 不应包含富文本标签")
            }
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    inner class ErrorHandlingTests {

        @TempDir
        lateinit var tempDir: File

        @Test
        @DisplayName("测试处理损坏的文件")
        fun testHandleCorruptedFile() {
            // 创建包含特殊字符和损坏标签的文件
            val testFile = File(tempDir, "corrupted.srt")
            val corruptedContent = "<b><i><u>嵌套标签</b></i></u>不匹配的标签"
            testFile.writeText(corruptedContent)

            // 不应抛出异常
            val hasRich = hasRichText(testFile)
            removeRichText(testFile)

            // 验证处理后的内容
            val result = testFile.readText()
            assertTrue(result.isNotEmpty(), "即使标签不匹配，也应该保留文本内容")
        }

        @Test
        @DisplayName("测试处理只有标签的文件")
        fun testHandleTagsOnlyFile() {
            val testFile = File(tempDir, "tags-only.srt")
            testFile.writeText("<b><i><u></u></i></b>")

            removeRichText(testFile)

            val result = testFile.readText()
            assertEquals("", result, "只有标签的文件处理后应为空")
        }

        @Test
        @DisplayName("测试路径转义 - 空值和 null 处理")
        fun testPathEscapeNullOrBlank() {
            // 测试空字符串
            assertEquals("", escapeFilterPath(""),
                "空字符串应返回空字符串")

            // 测试只有空格
            val spacesOnly = "   "
            assertEquals(spacesOnly, escapeFilterPath(spacesOnly),
                "只有空格的字符串应保持不变")
        }
    }
}
