package com.metrolist.music.lyrics

import android.content.Context
// YouTube import removed for Subsonic build
// import com.metrolist.innertube.YouTube
// import com.metrolist.innertube.models.WatchEndpoint

// YouTubeLyricsProvider disabled - not available with Subsonic
object YouTubeLyricsProvider : LyricsProvider {
    override val name = "YouTube Music (Disabled)"

    override fun isEnabled(context: Context) = false  // Disabled for Subsonic

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> =
        Result.failure(UnsupportedOperationException("YouTube lyrics not available with Subsonic"))
        // runCatching {
        //     val nextResult = YouTube.next(WatchEndpoint(videoId = id)).getOrThrow()
        //     YouTube
        //         .lyrics(
        //             endpoint = nextResult.lyricsEndpoint
        //                 ?: throw IllegalStateException("Lyrics endpoint not found"),
        //         ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
        // }
}
