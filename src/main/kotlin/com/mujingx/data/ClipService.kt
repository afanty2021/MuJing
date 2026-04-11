package com.mujingx.data

import java.util.UUID

class ClipService(
    private val clipRepository: ClipRepository
) {
    fun saveClip(clip: Clip) { clipRepository.saveClip(clip) }
    fun deleteClip(id: String) { clipRepository.deleteClip(id) }
    fun getClipsByVideo(videoPath: String): List<Clip> = clipRepository.getClipsByVideo(videoPath)

    fun createClip(videoPath: String, startTime: Long, endTime: Long, subtitleText: String): Clip {
        val clip = Clip(id = UUID.randomUUID().toString(), videoPath = videoPath, startTime = startTime, endTime = endTime, subtitleText = subtitleText)
        clipRepository.saveClip(clip)
        return clip
    }
}