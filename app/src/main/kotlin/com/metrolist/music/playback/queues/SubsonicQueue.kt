package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.subsonic.Subsonic
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class SubsonicQueue(
    private val albumId: String? = null,
    private val playlistId: String? = null,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var currentOffset = 0
    private val pageSize = 50

    override suspend fun getInitialStatus(): Queue.Status {
        val items = withContext(IO) {
            when {
                albumId != null -> {
                    Subsonic.getAlbum(albumId).getOrNull()?.songs?.map { it.toMediaItem() } ?: emptyList()
                }
                playlistId != null -> {
                    Subsonic.getPlaylist(playlistId).getOrNull()?.songs?.map { it.toMediaItem() } ?: emptyList()
                }
                else -> emptyList()
            }
        }
        
        return Queue.Status(
            title = when {
                albumId != null -> "Album"
                playlistId != null -> "Playlist"
                else -> null
            },
            items = items,
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean {
        // Subsonic doesn't support pagination for albums/playlists in the same way
        // All songs are returned at once
        return false
    }

    override suspend fun nextPage(): List<MediaItem> {
        // Subsonic doesn't support pagination
        return emptyList()
    }

    companion object {
        fun radio(song: MediaMetadata): SubsonicQueue {
            // For radio, use the song's album if available
            val albumId = song.album?.id
            return if (albumId != null) {
                SubsonicQueue(albumId = albumId, preloadItem = song)
            } else {
                // Fallback: create a queue with just this song
                SubsonicQueue(preloadItem = song)
            }
        }
        
        fun fromAlbum(albumId: String) = SubsonicQueue(albumId = albumId)
        fun fromPlaylist(playlistId: String) = SubsonicQueue(playlistId = playlistId)
    }
}

