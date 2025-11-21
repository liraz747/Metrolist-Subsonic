package com.metrolist.music.utils

import android.net.ConnectivityManager
import com.metrolist.music.constants.AudioQuality
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.subsonic.Subsonic
import timber.log.Timber

object SubsonicPlayerUtils {
    private const val logTag = "SubsonicPlayerUtils"

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    suspend fun playerResponseForPlayback(
        songId: String,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching Subsonic stream for songId: $songId")
        
        val maxBitRate = when (audioQuality) {
            AudioQuality.HIGH -> 320
            AudioQuality.LOW -> 128
            AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) 128 else 320
        }
        
        val streamUrl = Subsonic.getStreamUrl(
            id = songId,
            maxBitRate = maxBitRate
        )
        
        Timber.tag(logTag).d("Subsonic stream URL obtained: $streamUrl")
        
        // Create a minimal Format object for compatibility
        val format = PlayerResponse.StreamingData.Format(
            itag = 0,
            url = streamUrl,
            mimeType = "audio/mpeg",
            bitrate = maxBitRate * 1000,
            width = null,
            height = null,
            contentLength = null,
            quality = "medium",
            fps = null,
            qualityLabel = null,
            averageBitrate = maxBitRate * 1000,
            audioQuality = "AUDIO_QUALITY_MEDIUM",
            approxDurationMs = "0",
            audioSampleRate = 44100,
            audioChannels = 2,
            loudnessDb = null,
            lastModified = null,
            signatureCipher = null,
            audioTrack = null
        )
        
        PlaybackData(
            audioConfig = null,
            videoDetails = null,
            playbackTracking = null,
            format = format,
            streamUrl = streamUrl,
            streamExpiresInSeconds = 3600, // Subsonic URLs typically don't expire quickly
        )
    }
}

