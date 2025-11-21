package com.metrolist.subsonic

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.subsonic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

/**
 * Parse useful data with [SubsonicClient] sending requests.
 * Maps Subsonic API responses to Metrolist's YTItem models.
 */
object Subsonic {
    private var subsonicClient: SubsonicClient? = null
    
    // In-memory cache for song metadata to avoid repeated network calls
    private val metadataCache = ConcurrentHashMap<String, SongMetadata>()

    fun initialize(credentials: SubsonicCredentials) {
        subsonicClient = SubsonicClient(credentials)
        // Clear cache when reinitializing with new credentials
        metadataCache.clear()
    }

    var proxy: Proxy?
        get() = subsonicClient?.proxy
        set(value) {
            subsonicClient?.proxy = value
        }

    suspend fun ping() = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        currentClient.ping()
    }

    suspend fun search3(
        query: String,
        songCount: Int = 50,
        albumCount: Int = 50,
        artistCount: Int = 50
    ): Result<SearchResult> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.search3(query, songCount, albumCount, artistCount)
        SearchResult(
            songs = response.song?.mapNotNull { childToSongItem(it) }.orEmpty(),
            albums = response.album?.mapNotNull { albumToAlbumItem(it) }.orEmpty(),
            artists = response.artist?.mapNotNull { artistToArtistItem(it) }.orEmpty()
        )
    }

    suspend fun getAlbum(id: String): Result<AlbumPage> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getAlbum(id)
        AlbumPage(
            album = albumToAlbumItemFromWithSongs(response),
            songs = response.song?.mapNotNull { childToSongItem(it) }.orEmpty(),
            songsWithMetadata = response.song?.map { child ->
                Pair(child.id, SongMetadata(
                    bitRate = child.bitRate,
                    samplingRate = child.samplingRate,
                    bitDepth = child.bitDepth,
                    channelCount = child.channelCount,
                    size = child.size,
                    contentType = child.contentType,
                    suffix = child.suffix
                ))
            }?.toMap() ?: emptyMap()
        )
    }


    suspend fun getArtist(id: String): Result<ArtistPage> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getArtist(id)
        // Extract artist info from ArtistWithAlbumsID3
        val artist = ArtistID3(
            id = response.id,
            name = response.name,
            coverArt = response.coverArt,
            artistImageUrl = response.artistImageUrl,
            albumCount = response.albumCount,
            starred = response.starred,
            musicBrainzId = response.musicBrainzId,
            sortName = response.sortName,
            roles = response.roles
        )
        ArtistPage(
            artist = artistToArtistItem(artist) ?: throw Exception("Invalid artist response"),
            albums = response.album?.mapNotNull { albumToAlbumItem(it) }.orEmpty()
        )
    }

    suspend fun getPlaylist(id: String): Result<PlaylistPage> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getPlaylist(id)
        PlaylistPage(
            playlist = playlistToPlaylistItem(response),
            songs = response.entry?.mapNotNull { childToSongItem(it) }.orEmpty()
        )
    }

    suspend fun getPlaylists(): Result<List<PlaylistItem>> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getPlaylists()
        response.playlist?.mapNotNull { playlist ->
            PlaylistItem(
                id = playlist.id,
                title = playlist.name,
                author = null,
                songCountText = playlist.songCount?.toString(),
                thumbnail = playlist.coverArt?.let { currentClient.getCoverArtUrl(it) },
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
                isEditable = playlist.owner != null
            )
        }.orEmpty()
    }

    suspend fun getAlbumList2(
        type: String,
        size: Int = 20,
        offset: Int = 0
    ): Result<List<AlbumItem>> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getAlbumList2(type, size, offset)
        response.album?.mapNotNull { albumToAlbumItem(it) }.orEmpty()
    }

    suspend fun getArtists(): Result<List<ArtistItem>> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getArtists()
        val artists = mutableListOf<ArtistItem>()
        response.index?.forEach { index ->
            index.artist?.forEach { artist ->
                artistToArtistItem(artist)?.let { artists.add(it) }
            }
        }
        artists
    }

    suspend fun getStarred2(): Result<StarredResult> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        val response = currentClient.getStarred2()
        StarredResult(
            songs = response.song?.mapNotNull { childToSongItem(it) }.orEmpty(),
            albums = response.album?.mapNotNull { albumToAlbumItem(it) }.orEmpty(),
            artists = response.artist?.mapNotNull { artistToArtistItem(it) }.orEmpty()
        )
    }

    suspend fun star(id: String? = null, albumId: String? = null, artistId: String? = null): Result<Unit> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        currentClient.star(id, albumId, artistId)
    }

    suspend fun unstar(id: String? = null, albumId: String? = null, artistId: String? = null): Result<Unit> = runCatching {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        currentClient.unstar(id, albumId, artistId)
    }

    fun getStreamUrl(id: String, maxBitRate: Int? = null, format: String? = null): String {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        return currentClient.getStreamUrl(id, maxBitRate, format)
    }

    fun getCoverArtUrl(id: String, size: Int? = null): String {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
        return currentClient.getCoverArtUrl(id, size)
    }

    /**
    * Get song metadata by fetching it via getAlbum if we know the album ID,
    * or by using the SubsonicClient directly with getSong endpoint if available.
    * Results are cached in memory to avoid repeated network calls.
     */
    suspend fun getSongMetadata(songId: String): Result<SongMetadata> = withContext(Dispatchers.IO) {
        // Check cache first
        metadataCache[songId]?.let { cached ->
            return@withContext Result.success(cached)
        }
        
        runCatching {
            val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized. Call Subsonic.initialize() first.")
            val child = runCatching { currentClient.getSong(songId) }.getOrNull()
            if (child == null) {
                // Direct stream header parse fallback when getSong 404s
                val streamUrl = currentClient.getStreamUrl(songId, format = "mp3")
                val contentLength = Mp3HeaderParser.headContentLength(streamUrl, currentClient)
                val parseResult = Mp3HeaderParser.fetchAndParse(streamUrl, currentClient)
                val metadata = SongMetadata(
                    bitRate = (parseResult.avgBitrate ?: parseResult.bitrate)?.div(1000),
                    samplingRate = parseResult.sampleRate,
                    bitDepth = null,
                    channelCount = null,
                    size = contentLength,
                    contentType = "audio/mpeg",
                    suffix = "mp3"
                )
                // Cache the result
                metadataCache[songId] = metadata
                return@runCatching metadata
            }
            var bitRate = child.bitRate?.let { if (it > 0) it else null }
            var samplingRate = child.samplingRate?.let { if (it > 0) it else null }
            var size = child.size
            val contentType = child.contentType
            val suffix = child.suffix
            val channelCount = child.channelCount
            val bitDepth = child.bitDepth

            // If bitrate or sample rate missing, attempt header parse
            if (bitRate == null || samplingRate == null) {
                val streamUrl = currentClient.getStreamUrl(child.id, format = suffix ?: "mp3")
                val contentLength = Mp3HeaderParser.headContentLength(streamUrl, currentClient)
                val parseResult = Mp3HeaderParser.fetchAndParse(streamUrl, currentClient)
                if (bitRate == null) bitRate = (parseResult.avgBitrate ?: parseResult.bitrate)?.div(1000)
                if (samplingRate == null) samplingRate = parseResult.sampleRate
                if (size == null) size = contentLength
            }

            // Normalize bitrate back to kbps if mistakenly in bps
            if (bitRate != null && bitRate > 1000) {
                // If larger than typical kbps range, assume bps and convert
                bitRate = when {
                    bitRate in 320000..350000 -> 320
                    bitRate in 256000..270000 -> 256
                    bitRate in 192000..210000 -> 192
                    bitRate in 160000..170000 -> 160
                    else -> bitRate / 1000
                }
            }

            val metadata = SongMetadata(
                bitRate = bitRate,
                samplingRate = samplingRate,
                bitDepth = bitDepth,
                channelCount = channelCount,
                size = size,
                contentType = contentType,
                suffix = suffix
            )
            // Cache the result
            metadataCache[songId] = metadata
            metadata
        }
    }

    // Conversion functions
    private fun childToSongItem(child: Child): SongItem? {
        val currentClient = subsonicClient ?: return null
        val coverArtUrl = child.coverArt?.let { currentClient.getCoverArtUrl(it) } ?: ""
        return SongItem(
            id = child.id,
            title = child.title,
            artists = listOfNotNull(
                child.artist?.let { Artist(name = it, id = child.artistId) }
                    ?: child.displayArtist?.let { Artist(name = it, id = child.artistId) }
                    ?: Artist(name = "Unknown Artist", id = null)
            ),
            album = child.album?.let { com.metrolist.innertube.models.Album(name = it, id = child.albumId ?: "") },
            duration = child.duration,
            thumbnail = coverArtUrl,
            explicit = child.explicitStatus == "explicit",
            endpoint = null,
            setVideoId = null,
            libraryAddToken = null,
            libraryRemoveToken = null,
            historyRemoveToken = null
        )
    }

    private fun albumToAlbumItem(album: AlbumID3): AlbumItem? {
        val currentClient = subsonicClient ?: return null
        val coverArtUrl = album.coverArt?.let { currentClient.getCoverArtUrl(it) } ?: ""
        return AlbumItem(
            browseId = album.id,
            playlistId = album.id, // Use album ID as playlist ID for now
            id = album.id,
            title = album.name,
            artists = listOfNotNull(
                album.artist?.let { Artist(name = it, id = album.artistId) }
                    ?: album.displayArtist?.let { Artist(name = it, id = album.artistId) }
            ),
            year = album.year,
            thumbnail = coverArtUrl,
            explicit = album.explicitStatus == "explicit"
        )
    }

    private fun albumToAlbumItemFromWithSongs(album: AlbumID3WithSongs): AlbumItem {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized")
        val coverArtUrl = album.coverArt?.let { currentClient.getCoverArtUrl(it) } ?: ""
        return AlbumItem(
            browseId = album.id,
            playlistId = album.id, // Use album ID as playlist ID for now
            id = album.id,
            title = album.name,
            artists = listOfNotNull(
                album.artist?.let { Artist(name = it, id = album.artistId) }
                    ?: album.displayArtist?.let { Artist(name = it, id = album.artistId) }
            ),
            year = album.year,
            thumbnail = coverArtUrl,
            explicit = album.explicitStatus == "explicit"
        )
    }

    private fun artistToArtistItem(artist: ArtistID3): ArtistItem? {
        val currentClient = subsonicClient ?: return null
        val thumbnail = artist.coverArt?.let { currentClient.getCoverArtUrl(it) }
            ?: artist.artistImageUrl
        return ArtistItem(
            id = artist.id,
            title = artist.name,
            thumbnail = thumbnail,
            channelId = null,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null
        )
    }

    private fun playlistToPlaylistItem(playlist: PlaylistWithSongs): PlaylistItem {
        val currentClient = subsonicClient ?: throw IllegalStateException("Subsonic not initialized")
        val coverArtUrl = playlist.coverArt?.let { currentClient.getCoverArtUrl(it) }
        return PlaylistItem(
            id = playlist.id,
            title = playlist.name,
            author = playlist.owner?.let { Artist(name = it, id = null) },
            songCountText = playlist.songCount?.toString(),
            thumbnail = coverArtUrl,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
            isEditable = playlist.owner != null
        )
    }

    // Result data classes
    data class SearchResult(
        val songs: List<SongItem>,
        val albums: List<AlbumItem>,
        val artists: List<ArtistItem>
    )

    data class AlbumPage(
        val album: AlbumItem,
        val songs: List<SongItem>,
        val songsWithMetadata: Map<String, SongMetadata> = emptyMap()
    )

    data class ArtistPage(
        val artist: ArtistItem,
        val albums: List<AlbumItem>
    )

    data class PlaylistPage(
        val playlist: PlaylistItem,
        val songs: List<SongItem>
    )

    data class StarredResult(
        val songs: List<SongItem>,
        val albums: List<AlbumItem>,
        val artists: List<ArtistItem>
    )

    data class SongMetadata(
        val bitRate: Int?,
        val samplingRate: Int?,
        val bitDepth: Int?,
        val channelCount: Int?,
        val size: Long?,
        val contentType: String?,
        val suffix: String?
    )
}

