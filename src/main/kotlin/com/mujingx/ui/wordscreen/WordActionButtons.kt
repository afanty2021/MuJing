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

package com.mujingx.ui.wordscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import com.mujingx.icons.AddNotes
import com.mujingx.theme.LocalCtrl
import com.mujingx.player.isMacOS
import java.util.Timer
import kotlin.concurrent.schedule

/** 删除按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun DeleteButton(
    onClick: () -> Unit,
    tooltipAlignment: Alignment = Alignment.TopCenter,
) {
    val offset = if (tooltipAlignment == Alignment.TopCenter) {
        DpOffset(0.dp, 0.dp)
    } else {
        DpOffset(0.dp, 10.dp)
    }

    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "删除单词")
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "  $ctrl Delete" else "  $ctrl+Delete"
                    // 在视频播放器不显示快捷键
                    val text = if (tooltipAlignment == Alignment.TopCenter) shortcutText else ""
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = text)
                    }
                }
            }
        },

        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = tooltipAlignment,
            alignment = tooltipAlignment,
            offset = offset
        )
    ) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier.onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else {
                    false
                }
            }
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** 添加单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun AddButton(
    onClick: () -> Unit,
    tooltipAlignment: Alignment = Alignment.TopCenter,
) {

    val offset = if (tooltipAlignment == Alignment.TopCenter) {
        DpOffset(0.dp, 0.dp)
    } else {
        DpOffset(0.dp, 10.dp)
    }

    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "添加单词")

                }
            }
        },

        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = tooltipAlignment,
            alignment = tooltipAlignment,
            offset = offset
        )
    ) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier
        ) {
            Icon(
                AddNotes,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }

}


/** 编辑按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun EditButton(onClick: () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "编辑", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = {
            //            showEditWordDialog = true
            onClick()
        }) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** 困难单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HardButton(
    contains: Boolean,
    onClick: () -> Unit,
    fontFamily: FontFamily,
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    val text = if (contains) "从困难词库中移除  " else "添加到困难词库  "
                    Text(text = text)

                    val ctrl = LocalCtrl.current
                    val shortcut = if (isMacOS()) "$ctrl " else "$ctrl+"
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = "  $shortcut ")
                        Text(text = "I", fontFamily = fontFamily)
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {

        IconButton(onClick = { onClick() }) {
            val icon = if (contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            val tint = if (contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = tint
            )
        }
    }
}

/** 熟悉单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FamiliarButton(
    onClick: () -> Unit,
    tooltipAlignment: Alignment = Alignment.TopCenter,
) {
    val offset = if (tooltipAlignment == Alignment.TopCenter) {
        DpOffset(0.dp, 0.dp)
    } else {
        DpOffset(0.dp, 10.dp)
    }

    val text = if (tooltipAlignment == Alignment.TopCenter) "移动到熟悉词库" else "添加到熟悉词库"
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = text)
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "  $ctrl Y" else "  $ctrl+Y"
                    // 在视频播放器不显示快捷键
                    val shortcut = if (tooltipAlignment == Alignment.TopCenter) shortcutText else ""
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = shortcut)
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = tooltipAlignment,
            alignment = tooltipAlignment,
            offset = offset
        )
    ) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier.onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else {
                    false
                }
            }
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** 使用快捷键 Ctrl + I,把当前单词加入到困难单词时显示 0.3 秒后消失 */
@Composable
fun BookmarkButton(
    modifier: Modifier,
    contains: Boolean,
    disappear: () -> Unit
) {
    IconButton(
        onClick = {},
        modifier = modifier
    ) {
        val icon = if (contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
        val tint = if (contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
        Icon(
            icon,
            contentDescription = "Localized description",
            tint = tint,
        )
        SideEffect {
            Timer("不显示 Bookmark 图标", false).schedule(300) {
                disappear()
            }
        }
    }

}

/** 复制按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CopyButton(
    wordValue: String,
    tooltipAlignment: Alignment = Alignment.TopCenter,
) {
    val offset = if (tooltipAlignment == Alignment.TopCenter) {
        DpOffset(0.dp, 0.dp)
    } else {
        DpOffset(0.dp, 10.dp)
    }

    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "复制  ")
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "$ctrl C" else "$ctrl+C"
                    // 在视频播放器不显示快捷键
                    val shortcut = if (tooltipAlignment == Alignment.TopCenter) shortcutText else ""
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = shortcut)
                    }
                }

            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = tooltipAlignment,
            alignment = tooltipAlignment,
            offset
        )
    ) {
        val clipboardManager = LocalClipboardManager.current
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(wordValue))
            },
            modifier = Modifier.onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    clipboardManager.setText(AnnotatedString(wordValue))
                    true
                } else {
                    false
                }
            }
        ) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}
