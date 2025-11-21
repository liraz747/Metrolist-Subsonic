package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.subsonic.Subsonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubsonicArtistRadio(
    private val artistId: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status = withContext(Dispatchers.IO) {
        val artistPage = Subsonic.getArtist(artistId).getOrThrow()
        val songs = artistPage.albums.flatMap { album ->
            Subsonic.getAlbum(album.id).getOrNull()?.songs ?: emptyList()
        }
        Queue.Status(
            title = artistPage.artist.title,
            items = songs.map { it.toMediaItem() },
            mediaItemIndex = 0
        )
    }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
