package com.metrolist.innertube

import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.getContinuation
import com.metrolist.innertube.models.response.BrowseResponse
import com.metrolist.innertube.models.response.GetSearchSuggestionsResponse
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.innertube.pages.LibraryContinuationPage
import com.metrolist.innertube.pages.LibraryPage
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.innertube.pages.PlaylistContinuationPage
import com.metrolist.innertube.pages.PlaylistPage
import com.metrolist.innertube.pages.SearchSuggestionPage
import com.metrolist.innertube.pages.SearchSuggestionsResult
import io.ktor.client.call.body
import java.net.Proxy

object YouTube {
    private val innerTube = InnerTube()

    var cookie: String?
        get() = innerTube.cookie
        set(value) { innerTube.cookie = value }

    var visitorData: String?
        get() = innerTube.visitorData
        set(value) { innerTube.visitorData = value }

    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) { innerTube.dataSyncId = value }

    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }

    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) {
            innerTube.proxyAuth = value
        }

    suspend fun playlist(id: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = if (id.startsWith("VL")) id else "VL$id"
        ).body<BrowseResponse>()

        val playlist = response.header?.musicDetailHeaderRenderer?.let {
            PlaylistItem(
                id = id,
                title = it.title.runs?.firstOrNull()?.text ?: "",
                author = it.subtitle.runs?.getOrNull(2)?.let { run ->
                    Artist(
                        name = run.text,
                        id = run.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                songCountText = it.secondSubtitle.runs?.firstOrNull()?.text,
                thumbnail = it.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: "",
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
        } ?: throw Exception("Playlist not found")

        val songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { renderer ->
                PlaylistPage.fromMusicResponsiveListItemRenderer(renderer)
            }
        } ?: emptyList()

        val songsContinuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.continuations?.getContinuation()

        PlaylistPage(
            playlist = playlist,
            songs = songs,
            songsContinuation = songsContinuation,
            continuation = null
        )
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            continuation = continuation
        ).body<BrowseResponse>()

        val continuationContents = response.continuationContents?.musicPlaylistShelfContinuation
            ?: throw Exception("No continuation contents")

        val songs = continuationContents.contents.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { renderer ->
                PlaylistPage.fromMusicResponsiveListItemRenderer(renderer)
            }
        }

        val nextContinuation = continuationContents.continuations?.getContinuation()

        PlaylistContinuationPage(
            songs = songs,
            continuation = nextContinuation
        )
    }

    suspend fun libraryContinuation(continuation: String): Result<LibraryContinuationPage> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            continuation = continuation
        ).body<BrowseResponse>()

        val items = response.continuationContents?.gridContinuation?.items?.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                LibraryPage.fromMusicTwoRowItemRenderer(renderer)
            }
        } ?: response.continuationContents?.musicShelfContinuation?.contents?.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { renderer ->
                LibraryPage.fromMusicResponsiveListItemRenderer(renderer)
            }
        } ?: emptyList()

        val nextContinuation = response.continuationContents?.gridContinuation?.continuations?.getContinuation()
            ?: response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()

        LibraryContinuationPage(
            items = items,
            continuation = nextContinuation
        )
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId
        ).body<BrowseResponse>()

        val title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
            ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
            ?: ""

        val thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
            ?: response.header?.musicVisualHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
            ?: ""

        val artistItem = ArtistItem(
            id = browseId,
            title = title,
            thumbnail = thumbnail,
            shuffleEndpoint = null,
            radioEndpoint = null
        )

        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull {
            ArtistPage.fromSectionListRendererContent(it)
        } ?: emptyList()

        ArtistPage(
            artist = artistItem,
            sections = sections,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text
        )
    }

    suspend fun album(browseId: String): Result<AlbumPage> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId
        ).body<BrowseResponse>()

        val albumItem = AlbumItem(
            browseId = browseId,
            playlistId = AlbumPage.getPlaylistId(response) ?: "",
            title = AlbumPage.getTitle(response) ?: "",
            artists = AlbumPage.getArtists(response),
            year = AlbumPage.getYear(response),
            thumbnail = AlbumPage.getThumbnail(response) ?: ""
        )

        val songs = AlbumPage.getSongs(response, albumItem)

        AlbumPage(
            album = albumItem,
            songs = songs,
            otherVersions = emptyList()
        )
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestionsResult> = runCatching {
        val response = innerTube.getSearchSuggestions(
            client = YouTubeClient.WEB_REMIX,
            input = query
        ).body<GetSearchSuggestionsResponse>()

        val contents = response.contents?.firstOrNull()?.searchSuggestionsSectionRenderer?.contents ?: emptyList()

        val queries = contents.mapNotNull {
            it.searchSuggestionRenderer?.suggestion?.runs?.firstOrNull()?.text
        }

        val recommendedItems = contents.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { renderer ->
                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
            }
        }

        SearchSuggestionsResult(queries, recommendedItems)
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = "FEmusic_new_releases"
        ).body<BrowseResponse>()

        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items?.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                LibraryPage.fromMusicTwoRowItemRenderer(renderer) as? AlbumItem
            }
        } ?: emptyList()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = "FEmusic_moods_and_genres"
        ).body<BrowseResponse>()

        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull {
            MoodAndGenres.fromSectionListRendererContent(it)
        } ?: emptyList()
    }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
        signatureTimestamp: Int? = null
    ): Result<PlayerResponse> = runCatching {
        innerTube.player(
            client = client,
            videoId = videoId,
            playlistId = playlistId,
            signatureTimestamp = signatureTimestamp
        ).body<PlayerResponse>()
    }
}
