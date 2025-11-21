package com.metrolist.music.lyrics

import android.content.Context
// YouTube import removed for Subsonic build
// import com.metrolist.innertube.YouTube

// YouTubeSubtitleLyricsProvider disabled - not available with Subsonic
object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle (Disabled)"

    override fun isEnabled(context: Context) = false  // Disabled for Subsonic

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = Result.failure(UnsupportedOperationException("YouTube subtitles not available with Subsonic"))
    // YouTube.transcript(id)
}
