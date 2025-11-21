package com.metrolist.music.utils

import com.metrolist.subsonic.Subsonic
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val isSyncingStarredSongs = MutableStateFlow(false)
    private val isSyncingLibrarySongs = MutableStateFlow(false)
    private val isSyncingUploadedSongs = MutableStateFlow(false)
    private val isSyncingStarredAlbums = MutableStateFlow(false)
    private val isSyncingUploadedAlbums = MutableStateFlow(false)
    private val isSyncingStarredArtists = MutableStateFlow(false)
    private val isSyncingPlaylists = MutableStateFlow(false)

    fun runAllSyncs() {
        syncScope.launch {
            syncStarredSongs()
            syncStarredAlbums()
            syncStarredArtists()
            syncPlaylists()
            // Library and uploaded syncs are disabled - not supported by Subsonic
        }
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            runCatching {
                if (s.liked) {
                    Subsonic.unstar(id = s.id)
                } else {
                    Subsonic.star(id = s.id)
                }
            }
        }
    }

    suspend fun syncStarredSongs() {
        if (isSyncingStarredSongs.value) return
        isSyncingStarredSongs.value = true
        try {
            Subsonic.getStarred2().onSuccess { starredResult ->
                val remoteSongs = starredResult.songs
                val remoteIds = remoteSongs.map { it.id }
                val localSongs = database.likedSongsByNameAsc().first()

                localSongs.filterNot { it.song.id in remoteIds }.forEach {
                    try {
                        database.transaction { update(it.song.localToggleLike()) }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                remoteSongs.forEachIndexed { index, song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                update(dbSong.song.copy(liked = true, likedDate = timestamp))
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }.onFailure { e ->
                // 404 means endpoint not supported, silently skip
                if (e.message?.contains("404") != true) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingStarredSongs.value = false
        }
    }

    suspend fun syncLibrarySongs() {
        // DISABLED: Subsonic doesn't have a separate "library" concept distinct from starred items
        // All music in Subsonic is accessible through music folders
        // Users should use starred songs instead
    }

    suspend fun syncUploadedSongs() {
        // DISABLED: Subsonic doesn't distinguish between "uploaded" and regular content
        // All music is treated equally in Subsonic's music folders
    }

    suspend fun syncStarredAlbums() {
        if (isSyncingStarredAlbums.value) return
        isSyncingStarredAlbums.value = true
        try {
            Subsonic.getStarred2().onSuccess { starredResult ->
                val remoteAlbums = starredResult.albums
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsLikedByNameAsc().first()

                localAlbums.filterNot { it.album.id in remoteIds }.forEach { database.update(it.album.localToggleLike()) }

                remoteAlbums.forEach { album ->
                    val dbAlbum = database.album(album.id).firstOrNull()
                    if (dbAlbum == null) {
                        // TODO: Fetch full album details and insert
                        // For now, just create a basic album entity
                        database.transaction {
                            insert(
                                com.metrolist.music.db.entities.AlbumEntity(
                                    id = album.id,
                                    title = album.title ?: "Unknown Album",
                                    thumbnailUrl = album.thumbnail,
                                    year = album.year,
                                    songCount = 0, // TODO: Fetch full album details
                                    duration = 0, // TODO: Fetch full album details  
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        }
                    } else if (dbAlbum.album.bookmarkedAt == null) {
                        database.update(dbAlbum.album.localToggleLike())
                    }
                }
            }.onFailure { e ->
                // 404 means endpoint not supported, silently skip
                if (e.message?.contains("404") != true) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingStarredAlbums.value = false
        }
    }

    suspend fun syncUploadedAlbums() {
        // DISABLED: Subsonic doesn't distinguish between "uploaded" and regular content
        // All music is treated equally in Subsonic's music folders
    }

    suspend fun syncStarredArtists() {
        if (isSyncingStarredArtists.value) return
        isSyncingStarredArtists.value = true
        try {
            Subsonic.getStarred2().onSuccess { starredResult ->
                val remoteArtists = starredResult.artists
                val remoteIds = remoteArtists.map { it.id }.toSet()
                val localArtists = database.artistsBookmarkedByNameAsc().first()

                localArtists.filterNot { it.artist.id in remoteIds }.forEach { database.update(it.artist.localToggleLike()) }

                remoteArtists.forEach { artist ->
                    val dbArtist = database.artist(artist.id).firstOrNull()
                    database.transaction {
                        if (dbArtist == null) {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        } else if (dbArtist.artist.bookmarkedAt == null) {
                            update(dbArtist.artist.localToggleLike())
                        }
                    }
                }
            }.onFailure { e ->
                // 404 means endpoint not supported, silently skip
                if (e.message?.contains("404") != true) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingStarredArtists.value = false
        }
    }

    suspend fun syncPlaylists() {
        if (isSyncingPlaylists.value) return
        isSyncingPlaylists.value = true
        try {
            Subsonic.getPlaylists().onSuccess { remotePlaylists ->
                val remoteIds = remotePlaylists.map { it.id }.toSet()
                val localPlaylists = database.playlistsByNameAsc().first()

                localPlaylists.filterNot { it.playlist.browseId in remoteIds }.filterNot { it.playlist.browseId == null }.forEach { database.update(it.playlist.localToggleLike()) }

                remotePlaylists.forEach { remotePlaylist ->
                    val existingPlaylist = localPlaylists.find { it.playlist.browseId == remotePlaylist.id }?.playlist
                    if (existingPlaylist == null) {
                        val newEntity = PlaylistEntity(
                            name = remotePlaylist.title,
                            browseId = remotePlaylist.id,
                            bookmarkedAt = LocalDateTime.now()
                        )
                        database.insert(newEntity)
                    } else if (existingPlaylist.bookmarkedAt == null) {
                        database.update(existingPlaylist.copy(bookmarkedAt = LocalDateTime.now()))
                    }
                }
            }.onFailure { e ->
                // 404 means endpoint not supported, silently skip
                if (e.message?.contains("404") != true) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) {
        try {
            Subsonic.getPlaylist(browseId).onSuccess { playlistPage ->
                val songs = playlistPage.songs
                val remoteIds = songs.map { it.id }
                val localIds = database.playlistSongs(playlistId).first().sortedBy { it.map.position }.map { it.song.id }

                if (remoteIds == localIds) return@onSuccess
                if (database.playlist(playlistId).firstOrNull() == null) return@onSuccess

                database.transaction {
                    clearPlaylist(playlistId)
                    val songEntities = songs.onEach { song ->
                        if (runBlocking { database.song(song.id).firstOrNull() } == null) {
                            insert(song.toMediaMetadata())
                        }
                    }
                    val playlistSongMaps = songEntities.mapIndexed { position, song ->
                        PlaylistSongMap(songId = song.id, playlistId = playlistId, position = position)
                    }
                    playlistSongMaps.forEach { insert(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllSyncedContent() {
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()

            likedSongs.forEach {
                try { database.transaction { update(it.song.copy(liked = false, likedDate = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            librarySongs.forEach {
                if (it.song.inLibrary != null) {
                    try { database.transaction { update(it.song.copy(inLibrary = null)) } } catch (e: Exception) { e.printStackTrace() }
                }
            }
            likedAlbums.forEach {
                try { database.transaction { update(it.album.copy(bookmarkedAt = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            subscribedArtists.forEach {
                try { database.transaction { update(it.artist.copy(bookmarkedAt = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            savedPlaylists.forEach {
                if (it.playlist.browseId != null) {
                    try { database.transaction { delete(it.playlist) } } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
