package com.mujingx.data

import kotlinx.serialization.Serializable

@Serializable
data class Clip(
    val id: String,
    val videoPath: String,
    val startTime: Long,
    val endTime: Long,
    var subtitleText: String,
    var translatedText: String? = null,
    var note: String? = null,
    val tags: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var videoClipPath: String? = null
)