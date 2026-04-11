package com.mujingx.data

import kotlinx.serialization.Serializable

@Serializable
data class ClipCollection(
    val id: String,
    val name: String,
    val clips: MutableList<Clip> = mutableListOf(),
    var tags: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)